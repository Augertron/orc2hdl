package net.sf.orc2hdl.backend.transform;

import java.util.ArrayList;
import java.util.List;

import net.sf.orcc.ir.Block;
import net.sf.orcc.ir.BlockBasic;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.InstAssign;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.impl.IrFactoryImpl;
import net.sf.orcc.ir.util.AbstractIrVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.util.EcoreHelper;

/**
 * This visitor initialize all non-initialize local variables that are not Lists
 * 
 * @author Endri Bezati
 * 
 */
public class LocalVarInitializer extends AbstractIrVisitor<Void> {

	private List<Var> varToAssign;

	@Override
	public Void caseProcedure(Procedure procedure) {
		varToAssign = new ArrayList<Var>();

		for (Var var : new ArrayList<Var>(procedure.getLocals())) {
			if (!var.getType().isList()) {
				if (!var.isInitialized()) {
					for (Def def : var.getDefs()) {
						Block block = EcoreHelper.getContainerOfType(def,
								Block.class);
						if (!(block.eContainer() instanceof Procedure)) {
							Block container = (Block) block.eContainer();
							if (container.isBlockIf()
									|| container.isBlockWhile()) {
								varToAssign.add(var);
							}
						}
					}
				}
			}
		}

		// Find the First BlockBasic
		List<Block> blocks = procedure.getBlocks();
		Block firstBlock = IrUtil.getFirst(blocks);
		BlockBasic blockBasic = null;

		if (!firstBlock.isBlockBasic()) {
			blockBasic = IrFactoryImpl.eINSTANCE.createBlockBasic();
			procedure.getBlocks().add(0, blockBasic);
		} else {
			blockBasic = (BlockBasic) firstBlock;
		}

		// Add the instructions
		for (Var var : varToAssign) {
			InstAssign assign = null;
			// Get the type and create an assign instruction
			Type type = var.getType();
			if (type.isBool()) {
				var.setValue(false);
				ExprBool exprBool = IrFactory.eINSTANCE.createExprBool(false);
				assign = IrFactory.eINSTANCE.createInstAssign(var, exprBool);
			} else if (type.isInt() || type.isUint()) {
				var.setValue(0);
				ExprInt exprInt = IrFactory.eINSTANCE.createExprInt(0);
				assign = IrFactory.eINSTANCE.createInstAssign(var, exprInt);
			}
			if (assign != null) {
				blockBasic.add(assign);
			}
		}

		return null;
	}
}
