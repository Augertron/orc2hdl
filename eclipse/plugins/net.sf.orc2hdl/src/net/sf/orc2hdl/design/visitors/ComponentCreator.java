/*
 * Copyright (c) 2012, Ecole Polytechnique Fédérale de Lausanne
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Ecole Polytechnique Fédérale de Lausanne nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package net.sf.orc2hdl.design.visitors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sf.openforge.frontend.slim.builder.ActionIOHandler;
import net.sf.openforge.lim.Block;
import net.sf.openforge.lim.Branch;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.Decision;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.HeapRead;
import net.sf.openforge.lim.HeapWrite;
import net.sf.openforge.lim.Loop;
import net.sf.openforge.lim.Module;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.TaskCall;
import net.sf.openforge.lim.memory.AddressStridePolicy;
import net.sf.openforge.lim.memory.Location;
import net.sf.openforge.lim.memory.LogicalMemoryPort;
import net.sf.openforge.lim.memory.LogicalValue;
import net.sf.openforge.lim.op.AddOp;
import net.sf.openforge.lim.op.AndOp;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.lim.op.ComplementOp;
import net.sf.openforge.lim.op.DivideOp;
import net.sf.openforge.lim.op.EqualsOp;
import net.sf.openforge.lim.op.GreaterThanEqualToOp;
import net.sf.openforge.lim.op.GreaterThanOp;
import net.sf.openforge.lim.op.LeftShiftOp;
import net.sf.openforge.lim.op.LessThanEqualToOp;
import net.sf.openforge.lim.op.LessThanOp;
import net.sf.openforge.lim.op.MinusOp;
import net.sf.openforge.lim.op.ModuloOp;
import net.sf.openforge.lim.op.MultiplyOp;
import net.sf.openforge.lim.op.NoOp;
import net.sf.openforge.lim.op.NotEqualsOp;
import net.sf.openforge.lim.op.NotOp;
import net.sf.openforge.lim.op.OrOp;
import net.sf.openforge.lim.op.RightShiftOp;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.openforge.lim.op.SubtractOp;
import net.sf.openforge.lim.op.XorOp;
import net.sf.openforge.lim.primitive.And;
import net.sf.openforge.lim.primitive.Or;
import net.sf.openforge.util.MathStuff;
import net.sf.openforge.util.naming.IDSourceInfo;
import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orc2hdl.design.ResourceDependecies;
import net.sf.orc2hdl.design.util.DesignUtil;
import net.sf.orc2hdl.design.util.ModuleUtil;
import net.sf.orc2hdl.design.util.PortUtil;
import net.sf.orc2hdl.ir.InstPortPeek;
import net.sf.orc2hdl.ir.InstPortRead;
import net.sf.orc2hdl.ir.InstPortStatus;
import net.sf.orc2hdl.ir.InstPortWrite;
import net.sf.orcc.backends.ir.InstCast;
import net.sf.orcc.df.Action;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.ExprUnary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstCall;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.InstReturn;
import net.sf.orcc.ir.InstSpecific;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.OpBinary;
import net.sf.orcc.ir.OpUnary;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

/**
 * This visitor visit the procedural blocks and it creates a Design component
 * 
 * @author Endri Bezati
 * 
 */
public class ComponentCreator extends AbstractIrVisitor<List<Component>> {

	private Def assignTarget;
	/** Dependency between Components and Bus-Var **/
	protected Map<Bus, Var> busDependency;

	private Integer castIndex = 0;

	/** Action component Counter **/
	protected Integer componentCounter;
	/** Current List Component **/
	private List<Component> componentList;

	/** Current Component **/
	private Component currentComponent;

	/** Dependency between Components and Done Bus **/
	protected Map<Bus, Integer> doneBusDependency;

	private Integer listIndexes = 0;

	/** Dependency between Components and Port-Var **/
	protected Map<Port, Var> portDependency;

	/** Dependency between Components and Port-Var **/
	protected Map<Port, Integer> portGroupDependency;

	/** Design Resources **/
	protected ResourceCache resources;

	/** Design stateVars **/
	protected Map<LogicalValue, Var> stateVars;

	private boolean STM_DEBUG = true;

	private Var returnVar;

	public ComponentCreator(ResourceCache resources,
			Map<Port, Var> portDependency, Map<Bus, Var> busDependency,
			Map<Port, Integer> portGroupDependency,
			Map<Bus, Integer> doneBusDependency) {
		super(true);
		this.portDependency = portDependency;
		this.busDependency = busDependency;
		this.portGroupDependency = portGroupDependency;
		this.doneBusDependency = doneBusDependency;
		this.resources = resources;
		componentList = new ArrayList<Component>();
	}

	public ComponentCreator(ResourceCache resources,
			ResourceDependecies resourceDependecies) {
		super(true);
		this.portDependency = resourceDependecies.getPortDependency();
		this.busDependency = resourceDependecies.getBusDependency();
		this.portGroupDependency = resourceDependecies.getPortGroupDependency();
		this.doneBusDependency = resourceDependecies.getDoneBusDependency();
		this.resources = resources;
		componentList = new ArrayList<Component>();
	}

	@Override
	public List<Component> caseBlockIf(BlockIf blockIf) {
		List<Component> oldComponents = new ArrayList<Component>(componentList);
		// Create the decision
		Var decisionVar = resources.getBlockDecisionInput(blockIf).get(0);
		Decision decision = null;
		String condName = "decision_" + procedure.getName() + "_"
				+ decisionVar.getIndexedName();

		decision = ModuleUtil.createDecision(decisionVar, condName,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);

		// Visit the then block
		componentList = new ArrayList<Component>();
		doSwitch(blockIf.getThenBlocks());
		// Get the then Input Vars
		List<Var> thenInputs = resources.getBranchThenInput(blockIf);
		List<Var> thenOutputs = resources.getBranchThenOutput(blockIf);
		Block thenBlock = (Block) ModuleUtil.createModule(componentList,
				thenInputs, thenOutputs, "thenBlock", false, Exit.DONE, 0,
				portDependency, busDependency, portGroupDependency,
				doneBusDependency);
		thenBlock.setIDLogical("thenBlock");
		// Visit the else block
		Block elseBlock = null;

		if (!blockIf.getElseBlocks().isEmpty()) {
			componentList = new ArrayList<Component>();
			doSwitch(blockIf.getElseBlocks());

			List<Var> elseInputs = resources.getBranchElseInput(blockIf);
			List<Var> elseOutputs = resources.getBranchElseOutput(blockIf);
			elseBlock = (Block) ModuleUtil.createModule(componentList,
					elseInputs, elseOutputs, "elseBlock", false, Exit.DONE, 1,
					portDependency, busDependency, portGroupDependency,
					doneBusDependency);
			thenBlock.setIDLogical("elseBlock");
		} else {
			elseBlock = (Block) ModuleUtil.createModule(
					Collections.<Component> emptyList(),
					Collections.<Var> emptyList(),
					Collections.<Var> emptyList(), "elseBlock", false,
					Exit.DONE, 1, portDependency, busDependency,
					portGroupDependency, doneBusDependency);
			elseBlock.setIDLogical("elseBlock");
		}
		// Get All input Vars
		List<Var> ifInputVars = resources.getBlockInput(blockIf);
		List<Var> ifOutputVars = resources.getBlockOutput(blockIf);
		// Get Phi target Vars, aka branchIf Outputs
		Map<Var, List<Var>> phiOuts = resources.getBlockPhi(blockIf);
		Branch branch = (Branch) ModuleUtil.createBranch(decision, thenBlock,
				elseBlock, ifInputVars, ifOutputVars, phiOuts, "ifBLOCK",
				Exit.DONE, portDependency, busDependency, portGroupDependency,
				doneBusDependency);
		if (STM_DEBUG) {
			System.out.println("Branch: line :" + blockIf.getLineNumber()
					+ " Name: " + procedure.getName());
			System.out.println("Branch: thenBlock :" + thenBlock.toString());
			System.out.println("Branch: elseBlock :" + elseBlock.toString());
			System.out.println("Branch: Branch :" + branch.toString() + "\n");
		}
		IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(),
				blockIf.getLineNumber());

		branch.setIDSourceInfo(sinfo);
		currentComponent = branch;
		componentList = new ArrayList<Component>();
		componentList.addAll(oldComponents);
		if (blockIf.getAttribute("isMutex") != null) {
			Module mutexFsmBlock = (Module) ModuleUtil.createModule(
					Arrays.asList((Component) branch), ifInputVars,
					ifOutputVars, "mutex_BranchBlock", true, Exit.DONE, 0,
					portDependency, busDependency, portGroupDependency,
					doneBusDependency);
			componentList.add(mutexFsmBlock);
		} else {
			componentList.add(currentComponent);
		}
		return null;
	}

	@Override
	public List<Component> caseBlockWhile(BlockWhile blockWhile) {
		List<Component> oldComponents = new ArrayList<Component>(componentList);
		componentList = new ArrayList<Component>();

		/** Get Decision Components **/
		doSwitch(blockWhile.getJoinBlock());
		Var decisionVar = ((ExprVar) blockWhile.getCondition()).getUse()
				.getVariable();

		Component decisionComponent = ModuleUtil.findDecisionComponent(
				componentList, decisionVar, busDependency);

		if (decisionComponent == null) {
			Var target = IrFactory.eINSTANCE.createVar(decisionVar.getType(),
					"decisionVar_" + decisionVar.getIndexedName(), true,
					decisionVar.getIndex());
			decisionComponent = ModuleUtil.assignComponent(target, decisionVar,
					portDependency, busDependency, portGroupDependency,
					doneBusDependency);
			componentList.add(decisionComponent);
		}

		List<Component> decisionBodyComponents = new ArrayList<Component>(
				componentList);

		componentList = new ArrayList<Component>();

		/** Get Loop Body Components **/
		doSwitch(blockWhile.getBlocks());
		List<Component> bodyComponents = new ArrayList<Component>(componentList);

		/** Create the Loop **/
		List<Var> decisionInVars = resources.getBlockDecisionInput(blockWhile);
		List<Var> loopInVars = resources.getBlockInput(blockWhile);
		List<Var> loopOutVars = resources.getBlockOutput(blockWhile);
		List<Var> loopBodyInVars = resources.getLoopBodyInput(blockWhile);
		List<Var> loopBodyOutVars = resources.getLoopBodyOutput(blockWhile);

		Map<Var, List<Var>> loopPhi = resources.getBlockPhi(blockWhile);

		Loop loop = (Loop) ModuleUtil.createLoop(decisionComponent,
				decisionBodyComponents, decisionInVars, bodyComponents,
				loopPhi, loopInVars, loopOutVars, loopBodyInVars,
				loopBodyOutVars, portDependency, busDependency,
				portGroupDependency, doneBusDependency);
		if (STM_DEBUG) {
			System.out.println("Loop: line :" + blockWhile.getLineNumber()
					+ " Name: " + procedure.getName());
			System.out.println("Loop: LoopBody: " + loop.getBody().toString());
			System.out.println("Loop: " + loop.toString() + "\n");
		}

		IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(),
				blockWhile.getLineNumber());
		loop.setIDSourceInfo(sinfo);

		/** Clean componentList **/
		componentList = new ArrayList<Component>();
		/** Put back all previous components and add the loop **/
		componentList.addAll(oldComponents);
		componentList.add(loop);
		return null;
	}

	@Override
	public List<Component> caseExprBinary(ExprBinary expr) {
		// Get the size of the target and give it to the component
		int sizeInBits = assignTarget.getVariable().getType().getSizeInBits();
		// Get the Variables
		Var e1 = ((ExprVar) expr.getE1()).getUse().getVariable();
		Var e2 = ((ExprVar) expr.getE2()).getUse().getVariable();
		List<Var> inVars = new ArrayList<Var>();
		inVars.add(e1);
		inVars.add(e2);
		// Component component = null;
		if (expr.getOp() == OpBinary.BITAND) {
			currentComponent = new AndOp();
			// currentComponent = binaryMapInCast(component, expr);
		} else if (expr.getOp() == OpBinary.BITOR) {
			currentComponent = new OrOp();
			// currentComponent = binaryMapInCast(component, expr);
		} else if (expr.getOp() == OpBinary.BITXOR) {
			currentComponent = new XorOp();
			// currentComponent = binaryMapInCast(component, expr);
		} else if (expr.getOp() == OpBinary.DIV) {
			currentComponent = new DivideOp(sizeInBits);
			// currentComponent = binaryMapInCast(component, expr);
		} else if (expr.getOp() == OpBinary.DIV_INT) {
			currentComponent = new DivideOp(sizeInBits);
			// currentComponent = binaryMapInCast(component, expr);
		} else if (expr.getOp() == OpBinary.EQ) {
			currentComponent = new EqualsOp();
			// currentComponent = binaryMapInLogic(component, expr);
		} else if (expr.getOp() == OpBinary.GE) {
			currentComponent = new GreaterThanEqualToOp();
			// currentComponent = binaryMapInLogic(component, expr);
		} else if (expr.getOp() == OpBinary.GT) {
			currentComponent = new GreaterThanOp();
			// currentComponent = binaryMapInLogic(component, expr);
		} else if (expr.getOp() == OpBinary.LE) {
			currentComponent = new LessThanEqualToOp();
			// currentComponent = binaryMapInLogic(component, expr);
		} else if (expr.getOp() == OpBinary.LOGIC_AND) {
			currentComponent = new And(2);
			// currentComponent = binaryMapInCast(component, expr);
		} else if (expr.getOp() == OpBinary.LOGIC_OR) {
			currentComponent = new Or(2);
			// currentComponent = binaryMapInCast(component, expr);
		} else if (expr.getOp() == OpBinary.LT) {
			currentComponent = new LessThanOp();
			// currentComponent = binaryMapInLogic(component, expr);
		} else if (expr.getOp() == OpBinary.MINUS) {
			currentComponent = new SubtractOp();
			// currentComponent = binaryMapInCast(component, expr);
		} else if (expr.getOp() == OpBinary.MOD) {
			currentComponent = new ModuloOp();
			// currentComponent = binaryMapInCast(component, expr);
		} else if (expr.getOp() == OpBinary.NE) {
			currentComponent = new NotEqualsOp();
			// currentComponent = binaryMapInCast(component, expr);
		} else if (expr.getOp() == OpBinary.PLUS) {
			currentComponent = new AddOp();
			// currentComponent = binaryMapInCast(component, expr);
		} else if (expr.getOp() == OpBinary.SHIFT_LEFT) {
			int log2N = MathStuff.log2(sizeInBits);
			currentComponent = new LeftShiftOp(log2N);
			// currentComponent = binaryMapInCast(component, expr);
		} else if (expr.getOp() == OpBinary.SHIFT_RIGHT) {
			int log2N = MathStuff.log2(sizeInBits);
			currentComponent = new RightShiftOp(log2N);
			// currentComponent = binaryMapInCast(component, expr);
		} else if (expr.getOp() == OpBinary.TIMES) {
			currentComponent = new MultiplyOp(expr.getType().getSizeInBits());
			// currentComponent = binaryMapInCast(component, expr);
		}
		PortUtil.mapInDataPorts(currentComponent, inVars, portDependency,
				portGroupDependency);
		return null;
	}

	@Override
	public List<Component> caseExprBool(ExprBool expr) {
		final long value = expr.isValue() ? 1 : 0;
		currentComponent = new SimpleConstant(value, 1, true);
		return null;
	}

	@Override
	public List<Component> caseExprInt(ExprInt expr) {
		final long value = expr.getIntValue();
		int sizeInBits = 32;

		if (assignTarget != null) {
			sizeInBits = assignTarget.getVariable().getType().getSizeInBits();
		} else {
			sizeInBits = expr.getType().getSizeInBits();
		}

		currentComponent = new SimpleConstant(value, sizeInBits, expr.getType()
				.isInt());
		return null;
	}

	@Override
	public List<Component> caseExprUnary(ExprUnary expr) {
		if (expr.getOp() == OpUnary.BITNOT) {
			currentComponent = new ComplementOp();
		} else if (expr.getOp() == OpUnary.LOGIC_NOT) {
			currentComponent = new NotOp();
		} else if (expr.getOp() == OpUnary.MINUS) {
			currentComponent = new MinusOp();
		}
		return null;
	}

	@Override
	public List<Component> caseExprVar(ExprVar var) {
		Component noop = new NoOp(1, Exit.DONE);
		Var useVar = var.getUse().getVariable();
		Var inVar = useVar;
		/** Cast if necessary **/
		Integer newMaxSize = assignTarget.getVariable().getType()
				.getSizeInBits();
		if (newMaxSize != var.getType().getSizeInBits()) {
			Boolean isSigned = assignTarget.getVariable().getType().isInt();
			Var castedVar = unaryCastOp(var.getUse().getVariable(), newMaxSize,
					isSigned);
			inVar = castedVar;
		}
		PortUtil.mapInDataPorts(noop, inVar, portDependency,
				portGroupDependency);
		currentComponent = noop;
		return null;
	}

	@Override
	public List<Component> caseInstAssign(InstAssign assign) {
		// Get the size in bits of the target
		assignTarget = assign.getTarget();
		super.caseInstAssign(assign);
		if (currentComponent != null) {
			IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(),
					assign.getLineNumber());
			currentComponent.setIDSourceInfo(sinfo);

			Var outVar = assign.getTarget().getVariable();
			PortUtil.mapOutDataPorts(currentComponent, outVar, busDependency,
					doneBusDependency);

			componentList.add(currentComponent);
		}
		// Put target to null
		assignTarget = null;
		return null;
	}

	@Override
	public List<Component> caseInstCall(InstCall call) {
		Procedure procedure = call.getProcedure();
		if (call.getTarget() != null) {
			returnVar = call.getTarget().getVariable();
		}
		// Test if the container is an Action, visit
		if (procedure.eContainer() instanceof Action) {
			Action action = (Action) procedure.eContainer();
			if (procedure == action.getScheduler()) {
				doSwitch(procedure);
			} else if (procedure == action.getBody()) {
				TaskCall taskCall = new TaskCall();
				taskCall.setTarget(resources.getTaskFromAction(action));
				PortUtil.mapOutControlPort(taskCall, 0, doneBusDependency);
				IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(),
						call.getLineNumber());
				taskCall.setIDSourceInfo(sinfo);
				componentList.add(taskCall);
				// resources.addTaskCall(call, (TaskCall) currentComponent);
			}
		}

		return null;
	}

	@Override
	public List<Component> caseInstLoad(InstLoad load) {
		Var sourceVar = load.getSource().getVariable();

		// At this moment the load should have only one index
		Var loadIndexVar = null;
		List<Expression> indexes = load.getIndexes();
		for (Expression expr : new ArrayList<Expression>(indexes)) {
			loadIndexVar = ((ExprVar) expr).getUse().getVariable();
		}

		if (sourceVar.getType().isList()) {
			TypeList typeList = (TypeList) sourceVar.getType();
			Type type = typeList.getInnermostType();

			Boolean isSigned = type.isInt();
			Location targetLocation = resources.getLocation(sourceVar);

			LogicalMemoryPort memPort = targetLocation.getLogicalMemory()
					.getLogicalMemoryPorts().iterator().next();

			AddressStridePolicy addrPolicy = targetLocation.getAbsoluteBase()
					.getInitialValue().getAddressStridePolicy();

			int dataSize = type.getSizeInBits();
			HeapRead read = new HeapRead(dataSize / addrPolicy.getStride(), 32,
					0, isSigned, addrPolicy);
			CastOp castOp = new CastOp(dataSize, isSigned);
			Block block = DesignUtil.buildAddressedBlock(read, targetLocation,
					Collections.singletonList((Component) castOp));
			Bus result = block.getExit(Exit.DONE).makeDataBus();
			castOp.getEntries()
					.get(0)
					.addDependency(castOp.getDataPort(),
							new DataDependency(read.getResultBus()));
			result.getPeer()
					.getOwner()
					.getEntries()
					.get(0)
					.addDependency(result.getPeer(),
							new DataDependency(castOp.getResultBus()));

			memPort.addAccess(read, targetLocation);

			Var indexVar = procedure.newTempLocalVariable(
					IrFactory.eINSTANCE.createTypeInt(32), "index"
							+ listIndexes);
			listIndexes++;

			InstAssign assign = IrFactory.eINSTANCE.createInstAssign(indexVar,
					loadIndexVar);
			doSwitch(assign);

			PortUtil.mapInDataPorts(block, indexVar, portDependency,
					portGroupDependency);

			// Check if the load target should be casted

			Var target = load.getTarget().getVariable();
			int targetSize = target.getType().getSizeInBits();

			if (targetSize > dataSize) {
				// NOTE: OpenForge does not accept a cast here
				if (target.getType().isInt()) {
					target.setType(IrFactory.eINSTANCE.createTypeInt(dataSize));
				} else if (target.getType().isUint()) {
					target.setType(IrFactory.eINSTANCE.createTypeUint(dataSize));
				}
			}

			PortUtil.mapOutDataPorts(block, target, busDependency,
					doneBusDependency);
			IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(),
					load.getLineNumber());
			block.setIDSourceInfo(sinfo);
			componentList.add(block);
		} else {
			Var target = load.getTarget().getVariable();
			Component absoluteMemoryRead = ModuleUtil.absoluteMemoryRead(
					sourceVar, target, resources, busDependency,
					doneBusDependency);
			componentList.add(absoluteMemoryRead);
		}
		return null;
	}

	@Override
	public List<Component> caseInstPhi(InstPhi phi) {
		// Do nothing
		return null;
	}

	@Override
	public List<Component> caseInstReturn(InstReturn returnInstr) {
		if (returnInstr.getValue() != null) {
			if (returnInstr.getValue().isExprVar()) {
				Var instReturnVar = ((ExprVar) returnInstr.getValue()).getUse()
						.getVariable();
				InstAssign noop = IrFactory.eINSTANCE.createInstAssign(
						returnVar,
						IrFactory.eINSTANCE.createExprVar(instReturnVar));
				doSwitch(noop);
			}
		}
		return null;
	}

	@Override
	public List<Component> caseInstSpecific(InstSpecific object) {
		if (object instanceof InstCast) {
			InstCast cast = (InstCast) object;
			Var target = cast.getTarget().getVariable();
			Var source = cast.getSource().getVariable();
			Integer castedSize = target.getType().getSizeInBits();

			Component castOp = new CastOp(castedSize, target.getType().isInt());
			PortUtil.mapInDataPorts(castOp, source, portDependency,
					portGroupDependency);
			PortUtil.mapOutDataPorts(castOp, target, busDependency,
					doneBusDependency);
			componentList.add(castOp);
		} else if (object instanceof InstPortRead) {
			InstPortRead portRead = (InstPortRead) object;
			net.sf.orcc.df.Port port = (net.sf.orcc.df.Port) portRead.getPort();
			ActionIOHandler ioHandler = resources.getIOHandler(port);

			Component pinRead = ioHandler.getReadAccess(false);
			pinRead.setNonRemovable();

			Var pinReadVar = portRead.getTarget().getVariable();
			PortUtil.mapOutDataPorts(pinRead, pinReadVar, busDependency,
					doneBusDependency);
			componentList.add(pinRead);
		} else if (object instanceof InstPortWrite) {
			InstPortWrite portWrite = (InstPortWrite) object;
			net.sf.orcc.df.Port port = (net.sf.orcc.df.Port) portWrite
					.getPort();
			ActionIOHandler ioHandler = resources.getIOHandler(port);
			Component pinWrite = ioHandler.getWriteAccess(false);
			pinWrite.setNonRemovable();

			ExprVar value = (ExprVar) portWrite.getValue();
			Var pinWriteVar = value.getUse().getVariable();

			PortUtil.mapInDataPorts(pinWrite, pinWriteVar, portDependency,
					portGroupDependency);
			PortUtil.mapOutControlPort(pinWrite, 0, doneBusDependency);
			componentList.add(pinWrite);
		} else if (object instanceof InstPortStatus) {
			InstPortStatus portStatus = (InstPortStatus) object;

			net.sf.orcc.df.Port port = (net.sf.orcc.df.Port) portStatus
					.getPort();
			Var statusVar = portStatus.getTarget().getVariable();

			ActionIOHandler ioHandler = resources.getIOHandler(port);
			Component pinStatusComponent = ioHandler.getStatusAccess();
			pinStatusComponent.setNonRemovable();
			PortUtil.mapOutDataPorts(pinStatusComponent, statusVar,
					busDependency, doneBusDependency);
			componentList.add(pinStatusComponent);
		} else if (object instanceof InstPortPeek) {
			InstPortPeek portPeek = (InstPortPeek) object;

			net.sf.orcc.df.Port port = (net.sf.orcc.df.Port) portPeek.getPort();
			Var peekVar = portPeek.getTarget().getVariable();
			ActionIOHandler ioHandler = resources.getIOHandler(port);
			Component pinPeekComponent = ioHandler.getTokenPeekAccess();
			pinPeekComponent.setNonRemovable();
			PortUtil.mapOutDataPorts(pinPeekComponent, peekVar, busDependency,
					doneBusDependency);
			componentList.add(pinPeekComponent);
		}

		return null;
	}

	@Override
	public List<Component> caseInstStore(InstStore store) {
		Var targetVar = store.getTarget().getVariable();

		if (targetVar.isGlobal()) {

			// At this moment the load should have only one index
			Var storeIndexVar = null;
			List<Expression> indexes = store.getIndexes();
			for (Expression expr : new ArrayList<Expression>(indexes)) {
				storeIndexVar = ((ExprVar) expr).getUse().getVariable();
			}

			// Get store Value Var, after TAC the expression is a ExprVar
			ExprVar value = (ExprVar) store.getValue();
			Var valueVar = value.getUse().getVariable();

			// Get the inner type of the list
			Type type = null;
			if (targetVar.getType().isList()) {
				TypeList typeList = (TypeList) targetVar.getType();
				type = typeList.getInnermostType();

				// Get Location form resources
				Location targetLocation = resources.getLocation(targetVar);
				LogicalMemoryPort memPort = targetLocation.getLogicalMemory()
						.getLogicalMemoryPorts().iterator().next();
				AddressStridePolicy addrPolicy = targetLocation
						.getAbsoluteBase().getInitialValue()
						.getAddressStridePolicy();

				int dataSize = type.getSizeInBits();
				Boolean isSigned = type.isInt();

				HeapWrite heapWrite = new HeapWrite(dataSize
						/ addrPolicy.getStride(), 32, // max address width
						0, // fixed offset
						isSigned, // is signed?
						addrPolicy); // addressing policy

				Block block = DesignUtil.buildAddressedBlock(heapWrite,
						targetLocation, Collections.<Component> emptyList());
				Port data = block.makeDataPort();
				heapWrite
						.getEntries()
						.get(0)
						.addDependency(heapWrite.getValuePort(),
								new DataDependency(data.getPeer()));

				memPort.addAccess(heapWrite, targetLocation);

				Var indexVar = procedure.newTempLocalVariable(
						IrFactory.eINSTANCE.createTypeInt(32), "index"
								+ listIndexes);
				listIndexes++;
				currentComponent = new CastOp(32, isSigned);

				PortUtil.mapInDataPorts(currentComponent, storeIndexVar,
						portDependency, portGroupDependency);
				Var castedIndexVar = procedure.newTempLocalVariable(
						IrFactory.eINSTANCE.createTypeInt(32),
						"casted_" + castIndex + "_"
								+ storeIndexVar.getIndexedName());

				PortUtil.mapOutDataPorts(currentComponent, castedIndexVar,
						busDependency, doneBusDependency);
				componentList.add(currentComponent);
				castIndex++;
				// add the assign instruction for each index
				InstAssign assign = IrFactory.eINSTANCE.createInstAssign(
						indexVar, castedIndexVar);
				doSwitch(assign);

				IDSourceInfo sinfo = new IDSourceInfo(procedure.getName(),
						store.getLineNumber());
				block.setIDSourceInfo(sinfo);

				currentComponent = block;
				List<Var> inVars = new ArrayList<Var>();
				inVars.add(indexVar);
				inVars.add(valueVar);
				PortUtil.mapInDataPorts(currentComponent, inVars,
						portDependency, portGroupDependency);
				PortUtil.mapOutControlPort(currentComponent, 0,
						doneBusDependency);
				componentList.add(currentComponent);
			} else {
				type = targetVar.getType();
				Component absoluteMemoryWrite = ModuleUtil.absoluteMemoryWrite(
						targetVar, valueVar, resources, portDependency,
						portGroupDependency, doneBusDependency);
				componentList.add(absoluteMemoryWrite);
			}
		} else {
			if (store.getValue().isExprVar()) {
				Var sourceVar = ((ExprVar) store.getValue()).getUse()
						.getVariable();
				InstAssign assign = IrFactory.eINSTANCE.createInstAssign(
						targetVar, sourceVar);
				doSwitch(assign);
			}
		}
		return null;
	}

	@Override
	public List<Component> caseProcedure(Procedure procedure) {
		// componentList = new ArrayList<Component>();
		super.caseProcedure(procedure);
		return componentList;
	}

	protected Var unaryCastOp(Var var, Integer newMaxSize, Boolean isSigned) {
		Var newVar = var;
		Integer sizeVar = var.getType().getSizeInBits();

		if (sizeVar != newMaxSize) {
			Component castOp = new CastOp(newMaxSize, isSigned);

			PortUtil.mapInDataPorts(castOp, var, portDependency,
					portGroupDependency);
			Var castedVar = procedure.newTempLocalVariable(
					IrFactory.eINSTANCE.createTypeInt(newMaxSize), "casted_"
							+ castIndex + "_" + var.getIndexedName());

			PortUtil.mapOutDataPorts(castOp, castedVar, busDependency,
					doneBusDependency);
			componentList.add(castOp);
			newVar = castedVar;
			castIndex++;
		}

		return newVar;
	}

}
