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

package net.sf.openforge.optimize.constant.rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.openforge.app.EngineThread;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.OutBuf;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.op.AndOp;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.GreaterThanEqualToOp;
import net.sf.openforge.lim.op.MinusOp;
import net.sf.openforge.lim.op.ModuloOp;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.openforge.optimize.ComponentSwapVisitor;
import net.sf.openforge.optimize._optimize;
import net.sf.openforge.optimize.constant.TwoPassPartialConstant;

/**
 * ModuloOpRule.java
 * 
 * <pre>
 * 0 % a = 0
 * a % 1 = 0
 * a % -1 = 0
 * a % 2^n = if(a >= 0) a & (2^n - 1);
 *           else -((-a) & (2^n - 1));
 * </pre>
 * <p>
 * Created: Thu Jul 18 09:24:39 2002
 * 
 * @author imiller
 * @version $Id: ModuloOpRule.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ModuloOpRule {

	public static boolean halfConstant(ModuloOp op, Number[] consts,
			ComponentSwapVisitor visit) {
		assert consts.length == 2 : "Expecting exactly 2 port constants for Modulo Op";
		final Number p1 = consts[0];
		final Number p2 = consts[1];
		final int p2size = op.getRightDataPort().getValue().getSize();

		if ((p1 == null && p2 == null) || (p1 != null && p2 != null)) {
			return false;
		}

		Number constant = p1 == null ? p2 : p1;
		Port nonConstantPort = p1 == null ? (Port) op.getDataPorts().get(0)
				: (Port) op.getDataPorts().get(1);
		// Port constantPort = p1 != null ? (Port) op.getDataPorts().get(0)
		// : (Port) op.getDataPorts().get(1);

		if (p1 != null && p1.longValue() == 0) {
			if (_optimize.db) {
				_optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op
						+ " due to (0 % a)");
			}
			// 0 % a = 0. Simply delete the component and replace with constant
			// 0
			visit.replaceComponent(op, new SimpleConstant(0, op.getResultBus()
					.getValue().getSize(), op.getResultBus().getValue()
					.isSigned()));

			return true;
		} else if (p2 != null && p2.longValue() == 1) {
			if (_optimize.db) {
				_optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op
						+ " due to (a % 1)");
			}
			// a % 1 = 0. Simply delete the component and replace with constant
			// 0
			visit.replaceComponent(op, new SimpleConstant(0, op.getResultBus()
					.getValue().getSize(), op.getResultBus().getValue()
					.isSigned()));

			return true;
		} else if (p2 != null && DivideOpRule.isAllOnes(p2, p2size)) // p2 is -1
		{
			if (_optimize.db) {
				_optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op
						+ " due to (a % -1)");
			}
			// a % -1 = 0. Simply delete the component and replace with constnat
			// 0
			visit.replaceComponent(op, new SimpleConstant(0, op.getResultBus()
					.getValue().getSize(), op.getResultBus().getValue()
					.isSigned()));

			return true;
		} else if (p2 != null && p2.longValue() == 0) {
			if (_optimize.db) {
				_optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op
						+ " due to (a % 0)");
			}
			// Simply delete the component and replace with constant
			// 0. A warning will be caught and shown in logging.
			visit.replaceComponent(op, new SimpleConstant(0, op.getResultBus()
					.getValue().getSize(), op.getResultBus().getValue()
					.isSigned()));

			EngineThread.getGenericJob().warn(
					"    Found Modulo by 0 at line "
							+ op.getIDSourceInfo().getSourceLine()
							+ ", replacing with 0.");

			return true;
		} else if (p2 != null && p2.longValue() > 1) {
			int power = getPowerOfTwo(constant.longValue());
			if (power != -1) {
				if (_optimize.db) {
					_optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op
							+ " due to (a % 2^n)");
				}
				Module owner = op.getOwner();
				assert owner != null : "Cannot replace a component which is not contained in a module";

				Value argValue = nonConstantPort.getValue();

				//
				// Decision
				//
				Constant zero = new SimpleConstant(0, argValue.getSize(),
						argValue.isSigned());
				GreaterThanEqualToOp testComponent = new GreaterThanEqualToOp();
				List<Component> testList = new ArrayList<Component>();
				testList.add(zero);
				testList.add(testComponent);
				Block testBlock = new Block(testList);
				Port tb_port = testBlock.makeDataPort();
				// make dependency
				Entry gte_entry = testComponent.getEntries().get(0);
				Dependency dep0 = new DataDependency(tb_port.getPeer());
				gte_entry.addDependency(testComponent.getDataPorts().get(0),
						dep0);
				Dependency dep1 = new DataDependency(zero.getValueBus());
				gte_entry.addDependency(testComponent.getDataPorts().get(1),
						dep1);
				Decision decision = new Decision(testBlock, testComponent);
				Port dec_port = decision.makeDataPort();
				Entry testb_entry = testBlock.getEntries().get(0);
				Dependency testb_dep0 = new DataDependency(dec_port.getPeer());
				testb_entry.addDependency(testBlock.getDataPorts().get(0),
						testb_dep0);
				// sizing info
				// assert false : "New constant prop: fix these lines. --SGE";
				// ((Port)testComponent.getDataPorts().get(0)).setValue(argValue);
				// ((Port)testComponent.getDataPorts().get(1)).setValue(zero.getValueBus().getValue());
				// Component.getDataBus(testComponent).setBits(argValue.size());
				// ((Port)testBlock.getDataPorts().get(0)).getPeer().setBits(argValue.size());
				// ((Port)testBlock.getDataPorts().get(0)).setValue(argValue);
				// Component.getDataBus(testBlock).setBits(argValue.size());
				// ((Port)decision.getDataPorts().get(0)).getPeer().setBits(argValue.size());
				// ((Port)decision.getDataPorts().get(0)).setValue(argValue);
				// ((Port)decision.getNot().getDataPorts().get(0)).setValue(new
				// Value(1));
				// end making Decision

				//
				// True Branch
				//
				Constant true_power = new SimpleConstant((1 << power) - 1,
						argValue.getSize(), argValue.isSigned());
				AndOp trueShift = new AndOp();
				List<Component> trueList = new ArrayList<Component>();
				trueList.add(true_power);
				trueList.add(trueShift);
				Block trueBranch = new Block(trueList);
				Port trueB_port = trueBranch.makeDataPort();
				Exit trueBranch_exit = trueBranch.getExits().iterator().next();
				trueBranch_exit.makeDataBus();
				// make dependency
				Entry ts_entry = trueShift.getEntries().get(0);
				Dependency ts_dep0 = new DataDependency(trueB_port.getPeer());
				ts_entry.addDependency(trueShift.getDataPorts().get(0), ts_dep0);
				Dependency ts_dep1 = new DataDependency(
						true_power.getValueBus());
				ts_entry.addDependency(trueShift.getDataPorts().get(1), ts_dep1);
				OutBuf trueBranch_out = trueBranch.getOutBufs().iterator()
						.next();
				Entry trueBranch_outEntry = trueBranch_out.getEntries().get(0);
				Dependency trueBranch_outDep = new DataDependency(
						trueShift.getResultBus());
				trueBranch_outEntry.addDependency(trueBranch_out.getDataPorts()
						.get(0), trueBranch_outDep);
				// sizing info
				// assert false : "New constant prop: fix these lines. --SGE";
				// ((Port)trueShift.getDataPorts().get(0)).setValue(argValue);
				// ((Port)trueShift.getDataPorts().get(1)).setValue(true_power.getValueBus().getValue());
				// Component.getDataBus(trueShift).setBits(argValue.size());
				// ((Port)trueBranch.getDataPorts().get(0)).getPeer().setBits(argValue.size());
				// ((Port)trueBranch.getDataPorts().get(0)).setValue(argValue);
				// Component.getDataBus(trueBranch).setBits(argValue.size());
				// end making True Branch

				//
				// False Branch
				//
				MinusOp minusA = new MinusOp();
				Constant false_power = new SimpleConstant((1 << power) - 1,
						argValue.getSize(), argValue.isSigned());
				AndOp falseShift = new AndOp();
				MinusOp minusShift = new MinusOp();
				List<Component> falseList = new ArrayList<Component>();
				falseList.add(minusA);
				falseList.add(false_power);
				falseList.add(falseShift);
				falseList.add(minusShift);
				Block falseBranch = new Block(falseList);
				Port fb_port = falseBranch.makeDataPort();
				Exit falseBranch_exit = falseBranch.getExits().iterator()
						.next();
				falseBranch_exit.makeDataBus();
				// make minusA dependency
				Entry ma_entry = minusA.getEntries().get(0);
				Dependency ma_dep0 = new DataDependency(fb_port.getPeer());
				ma_entry.addDependency(minusA.getDataPort(), ma_dep0);
				// make falseShift dependency
				Entry fs_entry = falseShift.getEntries().get(0);
				Dependency fs_dep0 = new DataDependency(minusA.getResultBus());
				fs_entry.addDependency(falseShift.getDataPorts().get(0),
						fs_dep0);
				Dependency fs_dep1 = new DataDependency(
						false_power.getValueBus());
				fs_entry.addDependency(falseShift.getDataPorts().get(1),
						fs_dep1);
				// make minusShift dependency
				Entry ms_entry = minusShift.getEntries().get(0);
				Dependency ms_dep0 = new DataDependency(
						falseShift.getResultBus());
				ms_entry.addDependency(minusShift.getDataPort(), ms_dep0);
				// make OutBuf dependency
				OutBuf falseBranch_out = falseBranch.getOutBufs().iterator()
						.next();
				Entry falseBranch_outEntry = falseBranch_out.getEntries()
						.get(0);
				Dependency falseBranch_outDep = new DataDependency(
						minusShift.getResultBus());
				falseBranch_outEntry.addDependency(falseBranch_out
						.getDataPorts().get(0), falseBranch_outDep);
				// sizing info
				// assert false : "New constant prop: fix these lines. --SGE";
				// minusA.getDataPort().setValue(argValue);
				// Component.getDataBus(minusA).setBits(argValue.size());
				// ((Port)falseShift.getDataPorts().get(0)).setValue(argValue);
				// ((Port)falseShift.getDataPorts().get(1)).setValue(false_power.getValueBus().getValue());
				// Component.getDataBus(falseShift).setBits(argValue.size());
				// minusShift.getDataPort().setValue(argValue);
				// Component.getDataBus(minusShift).setBits(argValue.size());
				// ((Port)falseBranch.getDataPorts().get(0)).getPeer().setBits(argValue.size());
				// ((Port)falseBranch.getDataPorts().get(0)).setValue(argValue);
				// Component.getDataBus(falseBranch).setBits(argValue.size());
				// ((Port)falseBranch_out.getDataPorts().get(0)).setValue(argValue);
				// end making False Branch

				//
				// Branch, and make dependency between decision, true & false
				// branches
				//
				Branch branch = new Branch(decision, trueBranch, falseBranch);
				Port branch_port = branch.makeDataPort();

				Exit exit = op.getExits().iterator().next();
				Exit branch_exit = branch.getExits().iterator().next();
				branch_exit.makeDataBus();
				// branch_exit.makeDataBus();

				Entry dec_entry = decision.getEntries().get(0);
				Dependency dec_dep0 = new DataDependency(branch_port.getPeer());
				dec_entry.addDependency(decision.getDataPorts().get(0),
						dec_dep0);
				Entry true_entry = trueBranch.getEntries().get(0);
				Dependency true_dataDep = new DataDependency(
						branch_port.getPeer());
				true_entry.addDependency(trueBranch.getDataPorts().get(0),
						true_dataDep);
				Entry false_entry = falseBranch.getEntries().get(0);
				Dependency false_dataDep = new DataDependency(
						branch_port.getPeer());
				false_entry.addDependency(falseBranch.getDataPorts().get(0),
						false_dataDep);

				OutBuf branch_out = branch.getOutBufs().iterator().next();
				Entry forTrueBus_entry = branch_out.getEntries().get(0);
				Dependency outTrue_dep0 = new DataDependency(
						Component.getDataBus(trueBranch));
				forTrueBus_entry.addDependency((Port) forTrueBus_entry
						.getDataPorts().get(0), outTrue_dep0);
				Entry forFalseBus_entry = branch_out.getEntries().get(1);
				Dependency outFalse_dep0 = new DataDependency(
						Component.getDataBus(falseBranch));
				forFalseBus_entry.addDependency((Port) forFalseBus_entry
						.getDataPorts().get(0), outFalse_dep0);

				// map the newly created Branch dependencies/connnections
				Map<Port, Port> portCorrelation = new HashMap<Port, Port>();
				portCorrelation.put(op.getClockPort(), branch.getClockPort());
				portCorrelation.put(op.getResetPort(), branch.getResetPort());
				portCorrelation.put(op.getGoPort(), branch.getGoPort());
				portCorrelation.put(nonConstantPort,
						branch.getDataPorts().get(0));

				Map<Bus, Bus> busCorrelation = new HashMap<Bus, Bus>();
				busCorrelation.put(exit.getDataBuses().get(0), branch_exit
						.getDataBuses().get(0));
				busCorrelation.put(exit.getDoneBus(), branch_exit.getDoneBus());
				Map<Exit, Exit> exitCorrelation = new HashMap<Exit, Exit>();
				exitCorrelation.put(exit, branch_exit);

				ComponentSwapVisitor.replaceConnections(portCorrelation,
						busCorrelation, exitCorrelation);

				// set the Branch sizing
				// assert false : "New constant prop: fix these lines. --SGE";
				// ((Port)branch.getDataPorts().get(0)).getPeer().setBits(argValue.size());
				// ((Port)branch.getDataPorts().get(0)).setValue(argValue);
				// // XXX Cheat
				// ((Bus)branch.getExit(Exit.DONE).getDataBuses().get(0)).setBits(argValue.size());
				// ((Bus)branch.getExit(Exit.DONE).getDataBuses().get(1)).setBits(argValue.size());
				// ((Port)((OutBuf)branch.getOutBufs().get(0)).getDataPorts().get(0)).setValue(argValue);
				// ((Port)((OutBuf)branch.getOutBufs().get(0)).getDataPorts().get(1)).setValue(argValue);

				if (owner instanceof Block) {
					((Block) owner).replaceComponent(op, branch);
				} else {
					owner.removeComponent(op);
					owner.addComponent(branch);
				}
				op.disconnect();

				// Run the partial constant visitor to propagate values info
				// to the newly created Decision from the owner.
				TwoPassPartialConstant.forward(owner);

				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}// halfConstant

	/**
	 * @param number
	 *            a number
	 * @return if the number is a power of 2, return the power otherwise return
	 *         -1
	 */
	private static int getPowerOfTwo(long number) {
		int power = -1;
		for (int i = 0; i < 64; i++) {
			if (1L << i == number) {
				power = i;
				break;
			}
		}
		return power;
	}
}// ModuloOpRule
