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

package net.sf.openforge.lim.op;

import java.util.Collections;
import java.util.Map;

import net.sf.openforge.lim.Bit;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Emulatable;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.Visitor;
import net.sf.openforge.report.FPGAResource;
import net.sf.openforge.util.SizedInteger;

/**
 * A binary conditional operation in a form of &&.
 * 
 * Created: Thu Mar 08 16:39:34 2002
 * 
 * @author Conor Wu
 * @version $Id: ConditionalAndOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ConditionalAndOp extends BinaryOp implements Emulatable {

	/**
	 * Constructs a conditional and operation.
	 * 
	 */
	public ConditionalAndOp() {
		super();
	}

	/**
	 * Accept method for the Visitor interface
	 */
	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Gets the gate depth of this component. This is the maximum number of
	 * gates that any input signal must traverse before reaching an {@link Exit}
	 * .
	 * 
	 * @return a non-negative integer
	 */
	@Override
	public int getGateDepth() {
		return 1;
	}

	/**
	 * Gets the FPGA hardware resource usage of this component.
	 * 
	 * @return a FPGAResource objec
	 */
	@Override
	public FPGAResource getHardwareResourceUsage() {
		int lutCount = 0;

		Value leftValue = getLeftDataPort().getValue();
		Value rightValue = getRightDataPort().getValue();

		for (int i = 0; i < Math.max(leftValue.getSize(), rightValue.getSize()); i++) {
			Bit leftBit = null;
			Bit rightBit = null;
			if (i < leftValue.getSize()) {
				leftBit = leftValue.getBit(i);
			}
			if (i < rightValue.getSize()) {
				rightBit = rightValue.getBit(i);
			}

			if ((leftBit != null) && (rightBit != null)) {
				if (leftBit.isCare() && rightBit.isCare()
						&& (!leftBit.isConstant() || !rightBit.isConstant())) {
					lutCount++;
				}
			}
		}

		FPGAResource hwResource = new FPGAResource();
		hwResource.addLUT(lutCount);

		return hwResource;
	}

	/**
	 * Performes a high level numerical emulation of this component.
	 * 
	 * @param portValues
	 *            a map of owner {@link Port} to {@link SizedInteger} input
	 *            value
	 * @return a map of {@link Bus} to {@link SizedInteger} result value
	 */
	@Override
	public Map<Bus, SizedInteger> emulate(Map<Port, SizedInteger> portValues) {
		final SizedInteger lval = portValues.get(getLeftDataPort());
		final SizedInteger rval = portValues.get(getRightDataPort());
		final int resultInt = (lval.isZero() || rval.isZero()) ? 0 : 1;
		final Value resultValue = getResultBus().getValue();
		return Collections.singletonMap(getResultBus(), SizedInteger.valueOf(
				resultInt, resultValue.getSize(), resultValue.isSigned()));
	}

	/*
	 * =================================================== Begin new constant
	 * prop rules implementation.
	 */

	/**
	 * Pushes size, care, and constant information forward through this
	 * ConditionalAndOp according to this rule:
	 * 
	 * <pre>
	 * x = Dont Care     c = care (non constant)   0 = zero   1 = one
	 *    x x : x        c x : x ???    0 x : x    1 x : x
	 *    x c : x ???    c c : c        0 c : 0    1 c : c
	 *    x 0 : x        c 0 : 0        0 0 : 0    1 0 : 0
	 *    x 1 : x        c 1 : c        0 1 : 0    1 1 : 1
	 * </pre>
	 * 
	 * Result is a 1 bit care.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesForward() {
		boolean mod = false;

		Value in0 = getLeftDataPort().getValue();
		Value in1 = getRightDataPort().getValue();

		// Always a unsigned Value
		Value newValue = new Value(1, false);

		if (!in0.getBit(0).isCare() || !in1.getBit(0).isCare()) {
			newValue.setBit(0, Bit.DONT_CARE);
		} else {
			if (in0.getBit(0).isConstant() && in1.getBit(0).isConstant()) {
				if (!in0.getBit(0).isOn() || !in1.getBit(0).isOn()) {
					newValue.setBit(0, Bit.ZERO);
				} else {
					newValue.setBit(0, Bit.ONE);
				}
			} else if (in0.getBit(0).isConstant() && in0.getBit(0).isOn()) {
				newValue.setBit(0, in1.getBit(0));
			} else if (in0.getBit(0).isConstant() && !in0.getBit(0).isOn()) {
				newValue.setBit(0, Bit.ZERO);
			} else if (in1.getBit(0).isConstant() && in1.getBit(0).isOn()) {
				newValue.setBit(0, in0.getBit(0));
			} else if (in1.getBit(0).isConstant() && !in1.getBit(0).isOn()) {
				newValue.setBit(0, Bit.ZERO);
			} else {
				newValue.setBit(0, Bit.CARE);
			}
		}

		mod |= getResultBus().pushValueForward(newValue);

		return mod;
	}

	/**
	 * No rules can be applied on a NotOp.
	 * 
	 * @return a value of type 'boolean'
	 */
	@Override
	public boolean pushValuesBackward() {
		boolean mod = false;

		// No rules.

		return mod;
	}

	/*
	 * End new constant prop rules implementation.
	 * =================================================
	 */
}
