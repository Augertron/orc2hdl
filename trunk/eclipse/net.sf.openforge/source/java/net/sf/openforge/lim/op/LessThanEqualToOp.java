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

import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.report.*;
import net.sf.openforge.util.SizedInteger;


/**
 * A binary relational operation in a form of <=.
 *
 * Created: Thu Mar 08 16:39:34 2002
 *
 * @author  Conor Wu
 * @version $Id: LessThanEqualToOp.java 2 2005-06-09 20:00:48Z imiller $
 */
public class LessThanEqualToOp extends ConditionalOp implements Emulatable
{
    private static final String _RCS_ = "$Rev: 2 $";

    /**
     * Constructs a relational less than or equal to operation.
     *
     */
    public LessThanEqualToOp ()
    {
        super();
    }

    /**
     * Gets the gate depth of this component.  This is the maximum number of gates
     * that any input signal must traverse before reaching an {@link Exit}.
     *
     * @return a non-negative integer
     */
    public int getGateDepth ()
    {
        final int width = Math.max(getLeftDataPort().getValue().getSize(),
            getRightDataPort().getValue().getSize());
        return width + log2(width);
    }

    /**
     * Gets the FPGA hardware resource usage of this component.
     *
     * @return a FPGAResource objec
     */
    public FPGAResource getHardwareResourceUsage ()
    {
        int lutCount = 1;
        
        Value leftValue = getLeftDataPort().getValue();
        Value rightValue = getRightDataPort().getValue();
        
        for (int i = 0; i < Math.max(leftValue.getSize(), rightValue.getSize()) -1; i++)
        {
            Bit leftBit = null;
            Bit rightBit = null;
            if (i < leftValue.getSize())
            {
                leftBit = leftValue.getBit(i);
            }
            if (i < rightValue.getSize())
            {
                rightBit = rightValue.getBit(i);
            }
            
            if ((leftBit != null) && (rightBit != null))
            {
                if (leftBit.isCare() && rightBit.isCare() && (!leftBit.isConstant() || !rightBit.isConstant()))
                {
                    lutCount ++;
                }
            }
        }

        final int muxCount = lutCount -1;
        
        FPGAResource hwResource = new FPGAResource();
        hwResource.addLUT(lutCount);
        
        return hwResource;
    }

    /**
     * Accept method for the Visitor interface
     */ 
    public void accept (Visitor visitor)
    {
        visitor.visit(this);
    }

    /**
     * Performes a high level numerical emulation of this component.
     *
     * @param portValues a map of owner {@link Port} to {@link SizedInteger}
     *          input value
     * @return a map of {@link Bus} to {@link SizedInteger} result value
     */
    public Map emulate (Map portValues)
    {
        final SizedInteger lval = (SizedInteger)portValues.get(getLeftDataPort());
        final SizedInteger rval = (SizedInteger)portValues.get(getRightDataPort());

        final Value resultValue = getResultBus().getValue();
        final int intValue = (lval.compareTo(rval) <= 0 ? 1 : 0);
        final SizedInteger result = SizedInteger.valueOf(intValue,
            resultValue.getSize(), resultValue.isSigned());

        return Collections.singletonMap(getResultBus(), result);
    }

    /*
     * ===================================================
     *    Begin new constant prop rules implementation.
     */

    /**
     * Pushes size, care, and constant information forward through
     * this LessThanEqualToOp according to this rule:
     *
     * Result only has 1 care bit. 
     *
     * @return a value of type 'boolean'
     */
    public boolean pushValuesForward ()
    {
        boolean mod = false;

        Value newValue = new Value(1, false);
        
        mod |= getResultBus().pushValueForward(newValue);
        
        return mod;
    }

    /**
     * No rules can be applied on a LessThanEqualToOp.  
     *
     * @return a value of type 'boolean'
     */
    public boolean pushValuesBackward ()
    {
        boolean mod = false;
        
        // No rules.
        
        return mod;
    }
    
    /*
     *    End new constant prop rules implementation.
     * =================================================
     */        
}
