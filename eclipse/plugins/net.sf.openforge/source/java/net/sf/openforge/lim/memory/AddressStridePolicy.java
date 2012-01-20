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

package net.sf.openforge.lim.memory;

import java.util.*;

/**
 * AddressStridePolicy defines the way in which a particular
 * allocation of bits in a memory is converted to addressable
 * locations.  In the most common and basic configuration the policy
 * is simply to define that every 8 bits (each byte) is a unique
 * address. 
 *
 * <p>Created: Tue Nov 22 11:40:18 2005
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: AddressStridePolicy.java 70 2005-12-01 17:43:11Z imiller $
 */
public class AddressStridePolicy
{
    /** Revision */
    private static final String _RCS_ = "$Rev: 70 $";

    public static final AddressStridePolicy BYTE_ADDRESSING = new AddressStridePolicy(8);

    /** positive integer, bits per address for this policy */
    private int stride;
    
    public int hashCode ()
    {
        return getStride();
    }

    public boolean equals (Object o)
    {
        if (!(o instanceof AddressStridePolicy))
            return false;
        
        return ((AddressStridePolicy)o).getStride() == this.getStride();
    }

    /**
     * Constructs a new AddressStridePolicy with the given non-zero,
     * positive stride value.
     *
     * @param stride, a positive integer indicating the number of bits
     * per address for this address stride policy.
     * @throws IllegalArgumentException if stride is negative or 0.
     */
    public AddressStridePolicy (int stride)
    {
        if (stride <= 0)
            throw new IllegalArgumentException("Cannot build address stride policy with stride of " + stride);
        
        this.stride = stride;
    }
    
    /**
     * Returns the number of bits which define the progression to the
     * next addressable unit.  
     */
    public int getStride ()
    {
        return this.stride;
    }

    public String toString ()
    {
        return super.toString().replaceAll("net.sf.openforge.lim.","") + "[" + Integer.toHexString(System.identityHashCode(this)) + "]=" + this.stride;
    }
    

} // AddressStridePolicy