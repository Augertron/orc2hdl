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

import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.op.ConditionalAndOp;
import net.sf.openforge.lim.op.ReductionOrOp;
import net.sf.openforge.lim.op.SimpleConstant;
import net.sf.openforge.optimize.ComponentSwapVisitor;
import net.sf.openforge.optimize._optimize;

/**
 * ConditionalAndOpRule.java
 * 
 * <pre>
 * a && 0 = 0
 * a && 1 = a
 * </pre>
 * 
 * Created: Thu Jul 18 09:24:39 2002
 * 
 * @author cwu
 * @version $Id: ConditionalAndOpRule.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ConditionalAndOpRule {

	public static boolean halfConstant(ConditionalAndOp op, Number[] consts,
			ComponentSwapVisitor visit) {
		assert consts.length == 2 : "Expecting exactly 2 port constants for ConditionalAnd Op";
		Number p1 = consts[0];
		Number p2 = consts[1];

		if ((p1 == null && p2 == null) || (p1 != null && p2 != null)) {
			return false;
		}

		Number constant = p1 == null ? p2 : p1;
		Port nonConstantPort = p1 == null ? (Port) op.getDataPorts().get(0)
				: (Port) op.getDataPorts().get(1);
		Port constantPort = p1 != null ? (Port) op.getDataPorts().get(0)
				: (Port) op.getDataPorts().get(1);

		if (constant.longValue() != 0) {
			if (nonConstantPort.getValue().getSize() > 1) {
				if (_optimize.db) {
					_optimize.ln(_optimize.HALF_CONST, "\tReplacing " + op
							+ " due to (a && 1)");
				}
				// a && 1 = |a. Replace the component by ReductionOrOp
				op.removeDataPort(constantPort);
				visit.swapComponents(op, new ReductionOrOp());
			} else {
				if (_optimize.db) {
					_optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op
							+ " due to (a && 1)");
					// a && 1 = a. Simply delete the component and wire
					// through the non-constant port
				}

				// wire through the control.
				ComponentSwapVisitor.wireControlThrough(op);

				// Wire the non constant port through.
				ComponentSwapVisitor.shortCircuit(nonConstantPort,
						op.getResultBus());

				// Delete the op.
				// op.getOwner().removeComponent(op);
				boolean removed = ComponentSwapVisitor.removeComp(op);

				assert removed : "ConditionalAndOp was not able to be removed!";
			}

			return true;
		} else if (constant.longValue() == 0) {
			if (_optimize.db) {
				_optimize.ln(_optimize.HALF_CONST, "\tRemoving " + op
						+ " due to (a && 0)");
			}
			// a && 0 = 0. Simply delete the component and replace with the
			// constant
			visit.replaceComponent(op, new SimpleConstant(0, op.getResultBus()
					.getValue().getSize(), op.getResultBus().getValue()
					.isSigned()));

			return true;
		} else {
			return false;
		}
	}
}// ConditionalAndOpRule
