/*******************************************************************************
 * Copyright 2002-2009  Xilinx Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*
 * 
 *
 * 
 */

package net.sf.openforge.optimize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.Access;
import net.sf.openforge.lim.ArrayRead;
import net.sf.openforge.lim.ArrayWrite;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Call;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Composable;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.DefaultVisitor;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.InBuf;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.MemoryAccessBlock;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.OffsetMemoryRead;
import net.sf.openforge.lim.Operation;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Procedure;
import net.sf.openforge.lim.Reference;
import net.sf.openforge.lim.Register;
import net.sf.openforge.lim.RegisterRead;
import net.sf.openforge.lim.RegisterWrite;
import net.sf.openforge.lim.Switch;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.Visitable;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoRead;
import net.sf.openforge.lim.io.FifoWrite;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;
import net.sf.openforge.lim.memory.AbsoluteMemoryRead;
import net.sf.openforge.lim.memory.AbsoluteMemoryWrite;
import net.sf.openforge.lim.memory.LocationConstant;
import net.sf.openforge.lim.memory.LogicalMemory;
import net.sf.openforge.lim.op.NoOp;
import net.sf.openforge.lim.op.TimingOp;
import net.sf.openforge.lim.primitive.Not;
import net.sf.openforge.lim.primitive.Primitive;
import net.sf.openforge.util.naming.ID;

import org.eclipse.core.runtime.jobs.Job;

/**
 * DeadComponentVisitor is a visitor used to remove Components from the LIM
 * which have no logical dependents. When accepting a Design, this visitor will
 * continue to visit that design until no more modifications have been made.
 * <p>
 * The initial version of this visitor may leave some modules in place even
 * though they do no productive work. This is the case for Branches, loops,
 * switches, etc. At some point we need to go back in and make sure that we
 * remove these modules when we can because they will keep a higher level module
 * from removing itself (such as a procedure/call).
 * 
 * 
 * Created: Thu Jul 11 10:08:39 2002
 * 
 * @author imiller
 * @version $Id: DeadComponentVisitor.java 44 2005-10-27 16:21:59Z imiller $
 */
public class DeadComponentVisitor extends ComponentSwapVisitor implements
		Optimization {

	/** set of call's from each Task. Don't remove these */
	private Set<Call> topLevelCalls = new HashSet<Call>(3);

	/**
	 * Blocks which are procedure bodies. Their exits don't drive anything
	 * directly, but don't delete the connections on the inside. This is a map
	 * of Module => Procedure to facilitate determining if a module is a
	 * procedure body, but then having access to that procedure.
	 */
	private Map<Block, Procedure> procedureBlocks = new HashMap<Block, Procedure>(
			3);

	/**
	 * Removes unused memories from a given design, which normal visiting does
	 * not do.
	 * 
	 * @param design
	 *            a <code>Design</code> whose unaccessed memories are to be
	 *            removed
	 * @return true if one or more memories were removed, false otherwise
	 */
	public static boolean pruneMemories(Design design) {
		boolean isModified = false;

		// Remove any memory with no references to it.
		for (LogicalMemory memory : (new HashSet<LogicalMemory>(
				design.getLogicalMemories()))) {
			if (memory.getLValues().isEmpty()
					&& memory.getLocationConstants().isEmpty()) {
				EngineThread.getGenericJob().verbose(
						"Removing: " + ID.showLogical(memory));
				design.removeMemory(memory);
				isModified = true;
			}
		}

		for (Register reg : (new HashSet<Register>(design.getRegisters()))) {
			if (reg.getReferences().size() == 0) {
				EngineThread.getGenericJob().verbose(
						"Removing: " + ID.showLogical(reg));
				design.removeRegister(reg);
				isModified = true;
			}
		}

		return isModified;
	}

	/**
	 * Applies this optimization to a given target.
	 * 
	 * @param target
	 *            the target on which to run this optimization
	 */
	@Override
	public void run(Visitable target) {
		target.accept(this);
	}

	/**
	 * Accepts a Design to be pruned of LIM Components which have no logical
	 * dependents. The Design is iterated over until no more nodes can be
	 * eliminated.
	 * 
	 * @param design
	 *            a 'Design' to traverse.
	 */
	@Override
	public void visit(Design design) {
		int i = 0;
		do {
			if (_optimize.db)
				_optimize.ln("===============================================");
			if (_optimize.db)
				_optimize.ln("# Starting Dead Component Removal iteration "
						+ (i++));
			if (_optimize.db)
				_optimize.ln("===============================================");
			reset();
			super.visit(design);
		} while (isModified());

		// if (_optimize.db) _optimize.d.launchGraph(design, "POST OPTIMIZE",
		// Debug.GR_DEFAULT, false);
	}

	/**
	 * Keeps track of the {@link Call} for each task so that we don't remove a
	 * top level call.
	 * 
	 * @param task
	 *            a value of type 'Task'
	 */
	@Override
	public void visit(Task task) {
		topLevelCalls.add(task.getCall());
		super.visit(task);
	}

	/**
	 * 
	 * Removes the branch and replaces it with a new block containing the
	 * Decision and the executed branch iff the Decision evaluates to a constant
	 * value, indicating that one branch will always be taken to the exclusion
	 * of the other. <b>Note:</b> This simplification will not be executed until
	 * after at least 1 pass of the Partial Constant Propagation since the
	 * determination of whether one branch is always executed is made based off
	 * of the Decision's {@link Not}'s result bus' value whose value is set
	 * during constant propagation.
	 * 
	 * @param branch
	 *            a value of type 'Branch'
	 */
	@Override
	public void visit(Branch branch) {
		branch.getTrueBranch().setNonRemovable();
		branch.getFalseBranch().setNonRemovable();
		branch.getDecision().setNonRemovable();

		super.visit(branch);

		final Decision decision = branch.getDecision();

		final Bus decisionNot = decision.getNot().getResultBus();

		// Only removable if the 'Not' has a constant value AND there
		// is only one exit, and that exit is the DONE exit. Also,
		// only remove (swap for an empty block) the non taken branch
		// if there are 2 entries on the branch output buffer. If
		// there is only 1 entry on that outbuf that means that this
		// Branch has already been optimized and the entry already
		// removed.
		if (decisionNot.getValue() != null
				&& decisionNot.getValue().isConstant()
				&& branch.getExits().size() == 1
				&& branch.getExit(Exit.DONE) != null
				&& branch.getExit(Exit.DONE).getPeer().getEntries().size() == 2) {
			@SuppressWarnings("unused")
			final Component executed; // The branch that is always taken
			final Component excluded; // The branch that is never taken
			if (decisionNot.getValue().getBit(0).isOn()) {
				executed = branch.getFalseBranch();
				excluded = branch.getTrueBranch();
			} else {
				executed = branch.getTrueBranch();
				excluded = branch.getFalseBranch();
			}

			// Create a new empty block to put in place of the
			// 'deleted' side of the branch.
			final Block repBlock = new Block(
					Collections.<Component> emptyList(), false);
			repBlock.setNonRemovable();

			// Set up the correlations so that the done of the empty
			// block follows the connection of the done of the
			// replaced branch. Ditto the GO port.
			final Exit excludedDone = excluded.getExit(Exit.DONE);
			final Exit repDone = repBlock.getExit(Exit.DONE);
			assert excludedDone != null && repDone != null;
			final Map<Port, Port> pCor = Collections.singletonMap(
					excluded.getGoPort(), repBlock.getGoPort());
			final Map<Bus, Bus> bCor = Collections.singletonMap(
					excludedDone.getDoneBus(), repDone.getDoneBus());
			final Map<Exit, Exit> eCor = Collections.singletonMap(excludedDone,
					repDone);

			replaceConnections(pCor, bCor, eCor);

			// Now, in order to get rid of the Mux that merges the
			// data back together we must eliminate one of the 2
			// entries on the outbuf of the branch. Eliminate the
			// one connected to the new replacement block (since
			// the connections have already been replaced).
			Entry excludedOBEntry = null;
			final OutBuf branchOutbuf = branch.getExit(Exit.DONE).getPeer();
			assert branchOutbuf.getEntries().size() <= 2;
			for (Entry e : branchOutbuf.getEntries()) {
				for (Dependency dep : e.getDependencies(branchOutbuf
						.getGoPort())) {
					if (dep.getLogicalBus().getOwner().getOwner() == repBlock) {
						assert excludedOBEntry == null : "Removed branch has multiple paths to exit";
						excludedOBEntry = e;
					}
				}
			}
			assert excludedOBEntry != null;
			branchOutbuf.removeEntry(excludedOBEntry);

			branch.replaceComponent(excluded, repBlock);

			// Process any Access components in the removed branch so
			// that they do not linger (by being stored in the Referent)
			MemoryAccessCleaner.clean(excluded);
		}
	}

	/**
	 * Used to remove any {@link MemoryAccesBlock MemoryAccessBlocks} that may
	 * reside in a pruned module from their respective {@link LogicalMemory
	 * LogicalMemorys}.
	 */
	private static class MemoryAccessCleaner extends DefaultVisitor {
		/** The set of MemoryAccessBlocks that are encountered during visiting. */
		private Set<MemoryAccessBlock> accesses = new HashSet<MemoryAccessBlock>();

		private Set<Access> registerAccesses = new HashSet<Access>();

		/**
		 * Cleans up the MemoryAccessBlocks that are found when visiting a given
		 * Visitable.
		 * 
		 * @param target
		 *            the target in which to look for memory accesses; when
		 *            found, they will be removed from their memories
		 */
		static void clean(Visitable target) {
			final MemoryAccessCleaner cleaner = new MemoryAccessCleaner();
			target.accept(cleaner);
			cleaner.clean();
		}

		private void clean() {
			for (MemoryAccessBlock access : accesses) {
				access.removeFromMemory();
			}

			for (Access access : registerAccesses) {
				access.removeFromResource();
			}
		}

		@Override
		public void visit(HeapRead heapRead) {
			accesses.add(heapRead);
		}

		@Override
		public void visit(HeapWrite heapWrite) {
			accesses.add(heapWrite);
		}

		@Override
		public void visit(AbsoluteMemoryRead absoluteMemoryRead) {
			accesses.add(absoluteMemoryRead);
		}

		@Override
		public void visit(AbsoluteMemoryWrite absoluteMemoryWrite) {
			accesses.add(absoluteMemoryWrite);
		}

		@Override
		public void visit(ArrayRead arrayRead) {
			accesses.add(arrayRead);
		}

		@Override
		public void visit(ArrayWrite arrayWrite) {
			accesses.add(arrayWrite);
		}

		@Override
		public void visit(RegisterRead registerRead) {
			registerAccesses.add(registerRead);
		}

		@Override
		public void visit(RegisterWrite registerWrite) {
			registerAccesses.add(registerWrite);
		}
	}

	@Override
	public void visit(TimingOp top) {
		top.setNonRemovable();
		super.visit(top);
	}

	/**
	 * Implemented to keep the test block from being removed from the decision.
	 * 
	 * @param decision
	 *            a value of type 'Decision'
	 */
	@Override
	public void visit(Decision decision) {
		decision.getTestBlock().setNonRemovable();
		super.visit(decision);
	}

	@Override
	public void visit(Loop loop) {
		if (loop.getBody() != null) {
			loop.getBody().setNonRemovable();
			// We determine the output latency of a bounded loop by
			// including the latency of the loop body's body, so it
			// must have one, even if it does nothing.
			loop.getBody().getBody().setNonRemovable();
		}
		loop.getInitBlock().setNonRemovable();
		super.visit(loop);
	}

	@Override
	public void visit(Switch swich) {
		visit((Block) swich);
	}

	/**
	 * Removes NoOps since they are simply wire-throughs of control and data.
	 * 
	 * @param nop
	 *            a value of type 'NoOp'
	 */
	@Override
	public void visit(NoOp nop) {
		@SuppressWarnings("unused")
		Component owner = nop.getOwner();
		super.visit(nop);

		// The 'super' may have removed the NoOp if no-one listened to
		// it's output. If so, it's owner will be null and we don't
		// need to do this step....
		if (nop.getOwner() == null || nop.isNonRemovable()) {
			return;
		}

		// No-Ops are just passthroughs. Map the go port to each port
		// targetted by the NoOp's done. Ditto the data path.
		assert nop.getExits().size() == 1 : "NoOp's are only to have 1 exit";
		Exit nopExit = nop.getExit();
		wireControlThrough(nop);

		assert nopExit.getDataBuses().size() == nop.getDataPorts().size() : "Expecting 1:1 correlation between data ports and buses on nop";
		for (Iterator<?> busIter = nopExit.getDataBuses().iterator(), portIter = nop
				.getDataPorts().iterator(); busIter.hasNext();) {
			shortCircuit((Port) portIter.next(), (Bus) busIter.next());
		}

		assert nop.getEntries().size() <= 1;
		if (nop.getEntries().size() == 1) {
			Entry entry = nop.getEntries().get(0);
			for (Entry e : new LinkedList<Entry>(nopExit.getDrivenEntries())) {
				e.setDrivingExit(entry.getDrivingExit());
			}
		}

		// Now that the nop has been bypassed (by copying connections)
		// we can remove it.
		removeComponent(nop);
	}

	/**
	 * Implemented to test any non-{@link Reference} nodes and remove them if
	 * there are no logical dependencies on their data buses or there are no
	 * dependencies attached to any of the data ports.
	 * 
	 * @param o
	 *            any 'Operation'
	 */
	@Override
	public void filter(Operation o) {
		super.filter(o);
		if (o instanceof Reference) {
			if (_optimize.db)
				_optimize
						.ln(_optimize.DEAD_CODE, "NOT testing Reference: " + o);
		} else {
			testAndRemove(o);
		}
	}

	/**
	 * Will remove any {@link Primitive} which has no logical dependents on any
	 * data {@link Port} or no dependents on any data {@link Bus}
	 * 
	 * @param p
	 *            any 'Primitive'
	 */
	@Override
	public void filter(Primitive p) {
		testAndRemove(p);
	}

	/**
	 * Marks the procedure body as non-removable.
	 * 
	 * @param c
	 *            a value of type 'Call'
	 */
	@Override
	public void preFilter(Call c) {
		procedureBlocks.put(c.getProcedure().getBody(), c.getProcedure());
		c.getProcedure().getBody().setNonRemovable();
		preFilterAny(c);
	}

	/**
	 * Removes the Call if the called procedure contains no components (other
	 * than in or out bufs) unless that call is one of the 'top level calls'
	 * identified when visiting a {@link Task}.
	 * 
	 * @param call
	 *            a value of type 'Call'
	 */
	@Override
	public void filter(Call call) {
		super.filter(call);

		if (!topLevelCalls.contains(call) && !call.isNonRemovable()) {
			Procedure proc = call.getProcedure();
			Block body = proc.getBody();
			// Can't use isRemovable(body) because that method looks
			// for only a DONE exit and procedure bodies have a return exit.
			if (body.getExits().size() == 1
					&& body.getExit(Exit.RETURN) != null
					&& body.getEntries().size() <= 1 && // Procedure bodies have
														// 0 entries.
					body.getComponents().size() == (body.getOutBufs().size() + 1)) {
				for (Bus moduleBus : body.getBuses()) {
					Port modulePort = getModulePortForBus(moduleBus);
					if (modulePort == null) {
						continue;
					}
					// We actually want to copy the port/bus from the
					// call. So....
					Port callPort = call.getPortFromProcedurePort(modulePort);
					Bus callBus = call.getBusFromProcedureBus(moduleBus);
					assert callPort != null : "Call port not found";
					assert callBus != null : "Call bus not found";
					shortCircuit(callPort, callBus);
				}
				removeComponent(call);
			}
		}
	}

	/**
	 * Visits an {@link ArrayRead}. Skips preFilter'ing of the component, but
	 * will remove the operation as an atomic component if there are no
	 * consumers of the data bus.
	 * 
	 * @param arrayRead
	 *            the operation to visit
	 */
	@Override
	public void visit(ArrayRead arrayRead) {
		/*
		 * Skip the call to preFilter(), which might rip out the ArrayRead's
		 * result Bus. Later users will expect that Bus to be there, even if it
		 * isn't connected.
		 */
		traverse(arrayRead);
		filter(arrayRead);
		testAndRemoveOffsetMemRead(arrayRead);
	}

	/**
	 * Visits an {@link ArrayWrite}. Skips preFilter'ing of the component.
	 * 
	 * @param arrayWrite
	 *            the operation to visit
	 */
	@Override
	public void visit(ArrayWrite arrayWrite) {
		/*
		 * Skip the call to preFilter(), which might rip out the ArrayWrite's
		 * ValuePort or OffsetPort.
		 */
		traverse(arrayWrite);
		filter(arrayWrite);
	}

	/**
	 * Visits a {@link HeapRead}. Skips preFilter'ing of the component, but will
	 * remove the operation as an atomic component if there are no consumers of
	 * the data bus.
	 * 
	 * @param heapRead
	 *            the operation to visit
	 */
	@Override
	public void visit(HeapRead heapRead) {
		traverse(heapRead);
		filter(heapRead);
		testAndRemoveOffsetMemRead(heapRead);
	}

	/**
	 * Visits an {@link HeapWrite}. Skips preFilter'ing of the component.
	 * 
	 * @param heapWrite
	 *            the operation to visit
	 */
	@Override
	public void visit(HeapWrite heapWrite) {
		/*
		 * Skip the call to preFilter(), which might rip out the HeapWrite's
		 * ValuePort.
		 */
		traverse(heapWrite);
		filter(heapWrite);
	}

	private void testAndRemoveOffsetMemRead(OffsetMemoryRead omr) {
		// If there are no listeners to the result bus, remove the
		// whole module in one shot.
		if (omr.getResultBus().getLogicalDependents().size() == 0
				&& !omr.getResultBus().isConnected() && !omr.isNonRemovable()) {
			// Wire the go of the read to anything that listens to the
			// done.
			wireControlThrough(omr);

			// Then delete any dependencies on the exit's done bus.
			for (Dependency dep : omr.getExit(Exit.DONE).getDoneBus()
					.getLogicalDependents()) {
				dep.zap();
			}
			removeComponent(omr);

			// This should remove the reference from the memory port.
			omr.removeFromMemory();
		}
	}

	@Override
	public void visit(AbsoluteMemoryRead read) {
		// Dont pre-filter since we handle it specially.
		traverse(read);
		filter(read);
		if (read.getResultBus().getLogicalDependents().size() == 0
				&& !read.getResultBus().isConnected() && !read.isNonRemovable()) {
			wireControlThrough(read);

			// Then delete any dependencies on the exit's done bus.
			for (Dependency dep : read.getExit(Exit.DONE).getDoneBus()
					.getLogicalDependents()) {
				dep.zap();
			}
			removeComponent(read);

			// This should remove the reference from the memory port.
			read.removeFromMemory();
		}
	}

	@Override
	public void visit(FifoAccess comp) {
		traverse(comp);
		filter(comp);
	}

	@Override
	public void visit(FifoRead comp) {
		traverse(comp);
		filter(comp);
		Bus result = comp.getExit(Exit.DONE).getDataBuses().get(0);
		if ((result.getLogicalDependents().size() == 0)
				&& !result.isConnected() && !comp.isNonRemovable()) {
			wireControlThrough(comp);

			// Then delete any dependencies on the exit's done bus.
			for (Dependency dep : result.getOwner().getDoneBus()
					.getLogicalDependents()) {
				dep.zap();
			}

			removeComponent(comp);
		}
	}

	@Override
	public void visit(FifoWrite comp) {
		traverse(comp);
		filter(comp);
		Port din = comp.getDataPorts().get(0);
		boolean used = din.isConnected();
		for (Iterator<Entry> iter = comp.getEntries().iterator(); iter
				.hasNext() && !used;) {
			Entry entry = iter.next();
			used |= entry.getDependencies(din).size() > 0;
		}
		if (!used && !comp.isNonRemovable()) {
			wireControlThrough(comp);

			// Then delete any dependencies on the exit's done bus.
			for (Dependency dep : comp.getExit(Exit.DONE).getDoneBus()
					.getLogicalDependents()) {
				dep.zap();
			}

			removeComponent(comp);
		}
	}

	@Override
	public void visit(SimplePinRead comp) {
		// Remove a pin read when there is nothing that is consuming
		// its result.
		if (!comp.isNonRemovable() && !comp.getResultBus().isConnected()
				&& comp.getResultBus().getLogicalDependents().size() == 0) {
			wireControlThrough(comp);

			// Then delete any dependencies on the exit's done bus.
			for (Dependency dep : comp.getResultBus().getOwner().getDoneBus()
					.getLogicalDependents()) {
				dep.zap();
			}
			removeComponent(comp);
		}
	}

	@Override
	public void visit(SimplePinWrite comp) {
		// Remove a pin write when there is nothing coming into its
		// data port.
		if (!comp.getDataPort().isConnected() && !comp.isNonRemovable()) {
			boolean hasDep = false;
			for (Iterator<Entry> iter = comp.getEntries().iterator(); iter
					.hasNext() && !hasDep;) {
				Entry entry = iter.next();
				hasDep |= entry.getDependencies(comp.getDataPort()).size() > 0;
			}
			if (!hasDep) {
				wireControlThrough(comp);

				// Then delete any dependencies on the exit's done bus.
				for (Dependency dep : comp.getExit(Exit.DONE).getDoneBus()
						.getLogicalDependents()) {
					dep.zap();
				}

				removeComponent(comp);
			}
		}
	}

	/**
	 * Try to push lack of connectivity across module boundries, removing any
	 * unused ports and buses on modules. Special care is taken with procedure
	 * bodies and top level calls.
	 * 
	 * @param m
	 *            a value of type 'Module'
	 */
	@Override
	public void preFilter(Module m) {
		super.preFilter(m);
		if (m instanceof Composable) {
			return;
		}

		if (!isRemovable(m) || m.isNonRemovable()) {
			boolean topLevel = false;
			// Procedure blocks have no 'sink' for their buses
			// directly (only through calls) so special case them
			if (!procedureBlocks.containsKey(m)) {
				for (Exit exit : m.getExits()) {
					List<Bus> buses = new ArrayList<Bus>(exit.getDataBuses());
					for (Bus bus : buses) {
						if (bus.getLogicalDependents().size() == 0
								&& !bus.isConnected()) {
							if (_optimize.db)
								_optimize.ln(_optimize.DEAD_CODE,
										"Module output bus " + bus
												+ " has no dependents");
							removeBus(bus);
						}
					}
				}
			} else // Procedures...
			{
				// Make sure it isn't a top level call. All top level
				// calls need all ports/buses.
				Procedure proc = procedureBlocks.get(m);
				Set<Reference> calls = new HashSet<Reference>(proc.getCalls());
				topLevel = calls.removeAll(topLevelCalls);
				if (!topLevel) {
					for (Exit exit : proc.getBody().getExits()) {
						List<Bus> buses = new ArrayList<Bus>(
								exit.getDataBuses());
						for (Bus procBus : buses) {
							int depCount = 0;
							for (Reference ref : proc.getCalls()) {
								Call call = (Call) ref;
								Bus callBus = call
										.getBusFromProcedureBus(procBus);
								depCount += callBus.getLogicalDependents()
										.size();
								if (callBus.isConnected()) {
									// Consider it a dependency if the
									// bus is connected!
									depCount++;
								}
							}
							if (depCount == 0) {
								// removeBus takes care of removing the
								// bus from all Calls since this is a
								// procedure.
								removeBus(procBus);
							}
						}
					}
				}
			}

			// We don't want to remove ports from a top level call.
			if (!topLevel) {
				// If an inbuf bus isn't used remove the port. removeBus
				// will take care of the case where this is a procedure
				// input and remove the port from all calls.
				List<Bus> buses = new ArrayList<Bus>(m.getInBuf()
						.getDataBuses());
				for (Bus bus : buses) {
					if (bus.getLogicalDependents().size() == 0
							&& !bus.isConnected()) {
						if (_optimize.db)
							_optimize.ln(_optimize.DEAD_CODE,
									"Module inbuf bus " + bus
											+ " has no dependents");
						removeBus(bus);
					}
				}
			}
		}
	}

	/**
	 * Any module that contains nothing but In/OutBufs will be eliminated unless
	 * it is a procedure body {@link Block}
	 * 
	 * @param module
	 *            a value of type 'Module'
	 */
	@Override
	public void filter(Module module) {
		super.filter(module);
		if (!module.isNonRemovable() && isRemovable(module)) {
			remove(module);
		}
	}

	@Override
	protected void reset() {
		super.reset();
		procedureBlocks.clear();
		topLevelCalls.clear();
	}

	// ////////////////////////////////////////////////////
	// ////////////////////////////////////////////////////
	// ////////////////////////////////////////////////////
	// ////////////////////////////////////////////////////

	/**
	 * Returns true if the module can be removed (because it performs no actual
	 * functionality beyond a wire through of inputs to outputs).
	 * 
	 * @param module
	 *            a value of type 'Module'
	 * @return a value of type 'boolean'
	 */
	private static boolean isRemovable(Module module) {
		//
		// If you make a change to these criteria, then you need to
		// update the criteria in filter (Call) as well.
		//

		/*
		 * The Module can't be removed if it is needed to generate branch
		 * control signals. This is indicated by the presence of one or more
		 * Exits with tags other than DONE (e.g., RETURN, BREAK, CONTINUE).
		 * 
		 * XXX: We need a more sophisticated approach, since this will prevent
		 * Calls to empty Procedures from being removed (since the Procedure
		 * body Block will have a RETURN Exit).
		 */
		if ((module.getExits().size() > 1)
				|| (module.getExit(Exit.DONE) == null)) {
			if (_optimize.db)
				_optimize.ln(_optimize.DEAD_CODE,
						"Module unremovable. exit count "
								+ module.getExits().size() + " done exit is "
								+ module.getExit(Exit.DONE));
			return false;
		}

		/**
		 * If there are multiple entries on this module don't remove it.
		 * 
		 * XXX: Here too we need a better approach to determine whether we can
		 * figure out how to wire the control through even though there are
		 * multiple dependencies.
		 */
		if (module.getEntries().size() > 1) {
			if (_optimize.db)
				_optimize.ln(_optimize.DEAD_CODE,
						"Module unremovable. entry count "
								+ module.getEntries().size());
			return false;
		}

		/*
		 * We can remove iff there's nothing left but OutBufs and the InBuf.
		 */
		return module.getComponents().size() == module.getOutBufs().size() + 1;
	}

	/**
	 * Removes the given module after reconnecting any data or control flow
	 * which 'percolates' through the module.
	 * 
	 * @param module
	 *            a value of type 'Module'
	 */
	private void remove(Module module) {
		// Now that the connections are copied, remove the module. If
		// a module is the body of a procedure so we don't remove it.
		if (module.isNonRemovable()) {
			return;
		}

		//
		// Remove the module, but first figure out what data flows
		// exist through the module and re-create those flows
		// without going through the module.
		//
		for (Exit exit : module.getExits()) {
			for (Bus bus : exit.getBuses()) {
				// Each bus has a peer port. That port
				// should/must have exactly 1 entry and 1 logical
				// dependency otherwise this module isn't
				// removable.
				Port modulePort = getModulePortForBus(bus);
				if (modulePort == null) {
					continue;
				}

				assert modulePort != null : "Module inbuf bus has no peer";
				shortCircuit(modulePort, bus);
			}
		}

		removeComponent(module);
	}

	/**
	 * Analyzes each data bus of the given {@link Component} to see if there are
	 * any logical or structural dependents, or any physical connections to that
	 * bus and if none exist, the Component is removed from the LIM.
	 * 
	 * @param c
	 *            a value of type 'Component'
	 */
	private void testAndRemove(Component c) {
		if (c instanceof TimingOp)
			return;

		if (c.isNonRemovable())
			return;

		if (_optimize.db)
			_optimize.ln(_optimize.DEAD_CODE, "Examining " + c.toString());
		boolean dataBusUsed = isDataBusUsed(c);// false;

		// dataPortUsed is initialized to true if component has 0 ports.
		boolean dataPortUsed = (c.getDataPorts().size() == 0);
		for (Entry entry : c.getEntries()) {
			for (Port port : c.getDataPorts()) {
				// Assume that if it has a dependency that the logical
				// bus is not null....
				if (entry.getDependencies(port).size() > 0) {
					dataPortUsed = true;
					break;
				}
			}
		}

		/*
		 * Check for Bus connections, too.
		 */
		if (!dataPortUsed) {
			for (Port port : c.getDataPorts()) {
				if (port.isConnected()) {
					dataPortUsed = true;
					break;
				}
			}
		}

		// if we have no output or input
		if (!dataBusUsed || !dataPortUsed) {
			wireControlThrough(c);
			removeComponent(c);
		}
	}

	/**
	 * Returns true if there are any dependencies for any data bus of the given
	 * component or any of its data buses are connected.
	 * 
	 * @param c
	 *            the {@link Component} to test.
	 * @return true if any data bus is used in any way.
	 */
	private static boolean isDataBusUsed(Component c) {
		for (Exit exit : c.getExits()) {
			for (Bus bus : exit.getDataBuses()) {
				if ((bus.getLogicalDependents().size() > 0)
						|| bus.isConnected()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * This method is used to remove a Bus which has a peer, including module
	 * output buses, inbuf buses, procedure output buses, and procedure inbufs.
	 * 
	 * @param bus
	 *            a value of type 'Bus'
	 */
	private void removeBus(Bus bus) {
		Port peer = bus.getPeer();
		Component owner = peer.getOwner();
		assert owner != null : "This method only meant to be used for buses with peers.";
		for (Entry entry : owner.getEntries()) {
			for (Dependency dep : new ArrayList<Dependency>(
					entry.getDependencies(peer))) {
				dep.zap();
			}
		}
		peer.setUsed(false);
		// This will remove Port from outbuf if a Module and port of
		// Module if bus is on an inbuf
		if (_optimize.db)
			_optimize.ln(_optimize.DEAD_CODE, "Removing Bus " + bus
					+ " and peer " + bus.getPeer());
		bus.getOwner().getOwner().removeDataBus(bus);

		// If Port is input to a procedure, remove port from calls
		if (procedureBlocks.containsKey(owner)) {
			Procedure proc = procedureBlocks.get(owner);
			for (Reference ref : proc.getCalls()) {
				Call call = (Call) ref;
				Port callPort = call.getPortFromProcedurePort(peer);
				call.removeDataPort(callPort);
			}
		}

		// If bus is output from procedure, remove bus from calls
		if (procedureBlocks.containsKey(bus.getOwner().getOwner())) {
			Procedure proc = procedureBlocks.get(bus.getOwner().getOwner());
			for (Reference ref : proc.getCalls()) {
				Call call = (Call) ref;
				Bus callBus = call.getBusFromProcedureBus(bus);
				call.removeDataBus(callBus);
			}
		}

		setModified(true);
	}

	protected void removeComponent(Component c) {
		assert !c.isNonRemovable();

		// IDM 01/10/2005 Too much information is generated when
		// reporting every removed node. Also, when obfuscated, the
		// reported information is non-usefull
		// (often just obfuscated class names)
		// EngineThread.getGenericJob().verbose("Removing: "+ID.showGlobal(c));
		Set<Component> drivers = new HashSet<Component>();
		for (Port port : c.getDataPorts()) {
			drivers.addAll(getDrivers(port));
		}
		if (removeComp(c)) {
			if (c instanceof LocationConstant) {
				((LocationConstant) c).getTarget().getLogicalMemory()
						.removeLocationConstant((LocationConstant) c);
			}
			removedNodeCount++;
			removedNodeCountTotal++;
			setModified(true);
		}

		// Visit everything that drove what was removed. This will
		// help us to remove the chain much faster.
		for (Component comp : drivers) {
			((Visitable) comp).accept(this);
		}
	}

	/**
	 * Returns a Set of {@link Component}s that drive the specified port
	 * directly if port.getBus() is not null or via dependencies if port.getBus
	 * is null.
	 * 
	 * @param port
	 *            the port to find drivers of
	 * @return a 'Set' of Components that drive the specified port.
	 */
	private static Set<Component> getDrivers(Port port) {
		if (port.getBus() != null) {
			return Collections.singleton(port.getBus().getOwner().getOwner());
		}
		Set<Component> drivers = new HashSet<Component>();
		for (Entry entry : port.getOwner().getEntries()) {
			for (Dependency dep : entry.getDependencies(port)) {
				drivers.add(dep.getLogicalBus().getOwner().getOwner());
			}
		}
		return drivers;
	}

	/**
	 * Traverses backwards through an empty module (contains <b>only</b>) an
	 * inbuf and outbufs to find the module Port which supplies the value for a
	 * given module Bus.
	 * 
	 * @param bus
	 *            a value of type 'Bus'
	 * @return a value of type 'Port'
	 */
	private static Port getModulePortForBus(Bus bus) {
		// Port of outbuf inside module
		Port peer = bus.getPeer();
		assert peer != null : "Module bus has no peer";
		return getModulePortForPort(peer);
	}

	/**
	 * Returns the Port of the module which sources the specified port (if any).
	 * The given port must have a single logical dependency which is from the
	 * inbuf.
	 * 
	 * @param port
	 *            a value of type 'Port'
	 * @return a value of type 'Port'
	 */
	private static Port getModulePortForPort(Port port) {
		assert port.getOwner().getEntries().size() == 1 : "Port can only have 1 entry to find module port for port";
		Entry entry = port.getOwner().getEntries().get(0);
		Collection<Dependency> dependencies = entry.getDependencies(port);

		if (dependencies.size() == 0) {
			return null;
		}

		assert dependencies.size() == 1 : "Must be only 1 dependency for finding module port for port. Found "
				+ dependencies.size() + " " + dependencies;
		// Dependency for outbuf port
		Dependency dep = dependencies.iterator().next();
		// Bus driving outbuf. Must be from inbuf
		Bus inBufBus = dep.getLogicalBus();
		assert inBufBus.getOwner().getOwner() instanceof InBuf : "Source of connection to port must be inbuf for finding module port from port";
		// Port on the module which is, ultimately, directly
		// connected to module output bus.
		return inBufBus.getPeer();
	}

	/**
	 * Reports, via {@link Job#info}, what optimization is being performed
	 */
	@Override
	public void preStatus() {
		EngineThread.getGenericJob().info("pruning dead code...");
	}

	/**
	 * Reports, via {@link Job#verbose}, the results of <b>this</b> pass of the
	 * optimization.
	 */
	@Override
	public void postStatus() {
		EngineThread.getGenericJob().verbose(
				"pruned " + getRemovedNodeCount() + " expressions");
	}

}// DeadComponentVisitor
