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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.orc2hdl.design.ResourceCache;
import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.BlockIf;
import net.sf.orcc.ir.BlockWhile;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprBinary;
import net.sf.orcc.ir.ExprVar;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstPhi;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractIrVisitor;

import org.eclipse.emf.ecore.EObject;

/**
 * This class finds the inputs and outputs of an If and While block
 * 
 * @author Endri Bezati
 * 
 */
public class ModuleIO extends AbstractIrVisitor<Void> {

	/** Map of a Block Input Variables **/
	private Map<Block, Set<Var>> blkInputVars;

	/** Map of a Block Output Variables **/
	private Map<Block, Set<Var>> blkOutputVars;

	/** The current visited If Block **/
	private Block currentBlock = null;

	/** The current visited Block **/
	private BlockBasic currentBlockBasic = null;

	/** Map of a Decision Input Variables **/
	private Map<Block, Set<Var>> decisionInputVars;

	/** Map containing the join node **/
	private Map<Block, Map<Var, List<Var>>> joinVarMap;

	/** Map of a Module Input Variables **/
	private Map<Block, Set<Var>> moduleInputVars;

	/** Map of a Module Output Variables **/
	private Map<Block, Set<Var>> moduleOutputVars;

	private Boolean phiVisit;

	private Block previousBlock = null;

	/** Design Resources **/
	private ResourceCache resources;

	public ModuleIO(ResourceCache resources) {
		super(true);
		decisionInputVars = new HashMap<Block, Set<Var>>();
		blkInputVars = new HashMap<Block, Set<Var>>();
		blkOutputVars = new HashMap<Block, Set<Var>>();
		moduleInputVars = new HashMap<Block, Set<Var>>();
		moduleOutputVars = new HashMap<Block, Set<Var>>();
		joinVarMap = new HashMap<Block, Map<Var, List<Var>>>();
		this.resources = resources;
	}

	@Override
	public Void caseBlockBasic(BlockBasic block) {
		// Visit only the instruction of the If block
		if (block.eContainer() == currentBlock) {
			currentBlockBasic = block;
			super.caseBlockBasic(block);
		}
		return null;
	}

	@Override
	public Void caseBlockIf(BlockIf nodeIf) {
		currentBlock = nodeIf;
		moduleInputVars.put(nodeIf, new HashSet<Var>());
		moduleOutputVars.put(nodeIf, new HashSet<Var>());

		/** Get Condition **/
		Expression condExpr = nodeIf.getCondition();
		Var condVar = ((ExprVar) condExpr).getUse().getVariable();
		resources.addBranchDecisionInput(nodeIf, condVar);

		/** Visit Join Block **/
		joinVarMap.put(nodeIf, new HashMap<Var, List<Var>>());
		phiVisit = true;
		doSwitch(nodeIf.getJoinBlock());
		resources.addBranchPhi(nodeIf, joinVarMap.get(nodeIf));
		phiVisit = false;

		/** Visit Then Block **/
		blkInputVars.put(nodeIf, new HashSet<Var>());
		blkOutputVars.put(nodeIf, new HashSet<Var>());
		doSwitch(nodeIf.getThenBlocks());
		otherBlockIO(currentBlock, nodeIf, nodeIf.getThenBlocks());
		moduleInputVars.get(nodeIf).addAll(blkInputVars.get(nodeIf));
		resources.addBranchThenInput(nodeIf, blkInputVars.get(nodeIf));
		resources.addBranchThenOutput(nodeIf, blkOutputVars.get(nodeIf));

		/** Visit Else Block **/
		previousBlock = currentBlock;
		currentBlock = nodeIf;
		if (!nodeIf.getElseBlocks().isEmpty()) {
			blkInputVars.put(nodeIf, new HashSet<Var>());
			blkOutputVars.put(nodeIf, new HashSet<Var>());
			doSwitch(nodeIf.getElseBlocks());
			otherBlockIO(previousBlock, nodeIf, nodeIf.getElseBlocks());
			moduleInputVars.get(nodeIf).addAll(blkInputVars.get(nodeIf));
			resources.addBranchElseInput(nodeIf, blkInputVars.get(nodeIf));
			resources.addBranchElseOutput(nodeIf, blkOutputVars.get(nodeIf));
		}
		return null;
	}

	@Override
	public Void caseBlockWhile(BlockWhile nodeWhile) {
		currentBlock = nodeWhile;
		moduleInputVars.put(nodeWhile, new HashSet<Var>());
		moduleOutputVars.put(nodeWhile, new HashSet<Var>());

		/** Visit the Join Block **/
		joinVarMap.put(nodeWhile, new HashMap<Var, List<Var>>());
		decisionInputVars.put(nodeWhile, new HashSet<Var>());
		phiVisit = true;
		doSwitch(nodeWhile.getJoinBlock());
		if (decisionInputVars.get(nodeWhile).isEmpty()) {
			Var whileCondition = ((ExprVar) nodeWhile.getCondition()).getUse()
					.getVariable();
			decisionInputVars.get(nodeWhile).add(whileCondition);
		}
		resources.addDecisionInput(nodeWhile, decisionInputVars.get(nodeWhile));
		resources.addLoopPhi(nodeWhile, joinVarMap.get(nodeWhile));
		phiVisit = false;

		/** Visit Then Block **/
		blkInputVars.put(nodeWhile, new HashSet<Var>());
		blkOutputVars.put(nodeWhile, new HashSet<Var>());
		doSwitch(nodeWhile.getBlocks());
		otherBlockIO(currentBlock, nodeWhile, nodeWhile.getBlocks());
		moduleInputVars.get(nodeWhile).addAll(blkInputVars.get(nodeWhile));
		resources.addLoopOtherInputs(nodeWhile, blkInputVars.get(nodeWhile));

		previousBlock = currentBlock;
		return null;
	}

	@Override
	public Void caseExprBinary(ExprBinary expr) {
		// Get e1 var and if it defined not in this visited block added as an
		// input
		Var varE1 = ((ExprVar) expr.getE1()).getUse().getVariable();

		if (phiVisit) {
			if (definedInOtherBlock(varE1, currentBlockBasic)
					|| joinVarMap.get(currentBlock).containsKey(varE1)) {
				decisionInputVars.get(currentBlock).add(varE1);
			}

		} else {
			if (definedInOtherBlock(varE1, currentBlockBasic)) {
				blkInputVars.get(currentBlock).add(varE1);
			}

		}

		// Get e2 var and if it defined not in this visited block added as an
		// input
		Var varE2 = ((ExprVar) expr.getE2()).getUse().getVariable();
		if (phiVisit) {
			if (definedInOtherBlock(varE2, currentBlockBasic)
					|| joinVarMap.get(currentBlock).containsKey(varE2)) {
				decisionInputVars.get(currentBlock).add(varE2);
			}

		} else {
			if (definedInOtherBlock(varE2, currentBlockBasic)) {
				blkInputVars.get(currentBlock).add(varE2);
			}

		}
		return null;
	}

	@Override
	public Void caseExprVar(ExprVar exprVar) {
		Var var = exprVar.getUse().getVariable();
		if (!phiVisit) {
			if (definedInOtherBlock(var, currentBlockBasic)) {
				blkInputVars.get(currentBlock).add(var);
			}
		}
		return null;
	}

	@Override
	public Void caseInstAssign(InstAssign assign) {
		super.caseInstAssign(assign);
		Var target = assign.getTarget().getVariable();
		for (List<Var> vars : joinVarMap.get(currentBlock).values()) {
			if (vars.contains(target)) {
				blkOutputVars.get(currentBlock).add(target);
			}
		}

		return null;
	}

	@Override
	public Void caseInstLoad(InstLoad load) {
		Var loadIndexVar = null;
		List<Expression> indexes = load.getIndexes();
		for (Expression expr : new ArrayList<Expression>(indexes)) {
			loadIndexVar = ((ExprVar) expr).getUse().getVariable();
		}
		if (definedInOtherBlock(loadIndexVar, currentBlockBasic)) {
			blkInputVars.get(currentBlock).add(loadIndexVar);
		}
		return null;
	}

	@Override
	public Void caseInstPhi(InstPhi phi) {
		List<Var> phiVars = new ArrayList<Var>();
		Var target = phi.getTarget().getVariable();
		// Add to the Block output
		moduleOutputVars.get(currentBlock).add(target);

		for (Expression expr : phi.getValues()) {
			Var value = ((ExprVar) expr).getUse().getVariable();
			phiVars.add(value);
		}
		joinVarMap.get(currentBlock).put(target, phiVars);
		return null;
	}

	private Boolean definedInOtherBlock(Var var, BlockBasic block) {
		for (Def def : var.getDefs()) {
			EObject container = def.eContainer();
			while (!(container instanceof BlockBasic)) {
				container = container.eContainer();
			}
			if (container != block && container.eContainer() != currentBlock) {
				return true;
			}
		}
		return false;
	}

	private void otherBlockIO(Block previousBlock, Block currentBlock,
			List<Block> currentBlocks) {
		if (currentBlocks.contains(previousBlock)) {
			for (Var previousInputVar : moduleInputVars.get(previousBlock)) {
				List<Var> assignTargets = new ArrayList<Var>();
				for (Block block : currentBlocks) {
					if (block.isBlockBasic()) {
						for (Instruction inst : ((BlockBasic) block)
								.getInstructions()) {
							if (inst.isInstAssign()) {
								Var target = ((InstAssign) inst).getTarget()
										.getVariable();
								assignTargets.add(target);
							}
						}

					}
				}
				if (!assignTargets.contains(previousInputVar)
						&& !joinVarMap.get(currentBlock).containsKey(
								previousInputVar)) {
					blkInputVars.get(currentBlock).add(previousInputVar);
				}
			}

			for (Var thenOutputVar : moduleOutputVars.get(previousBlock)) {
				List<Var> assignTargets = new ArrayList<Var>();
				for (Block block : currentBlocks) {
					if (block.isBlockBasic()) {
						for (Instruction inst : ((BlockBasic) block)
								.getInstructions()) {
							if (inst.isInstAssign()) {
								Var target = ((InstAssign) inst).getTarget()
										.getVariable();
								assignTargets.add(target);
							}
						}

					}
				}
				if (!assignTargets.contains(thenOutputVar)) {
					blkOutputVars.get(currentBlock).add(thenOutputVar);
				}
			}

		}
	}
}
