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
 * DefaultMemoryVisitor.java
 *
 *
 * <p>Created: Tue Sep 16 15:30:44 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: DefaultMemoryVisitor.java 2 2005-06-09 20:00:48Z imiller $
 */
public class DefaultMemoryVisitor implements MemoryVisitor
{
    private static final String _RCS_ = "$Rev: 2 $";

    /**
     * No direct instances of this class please, create subclasses
     * with specific behavior.
     */
    protected DefaultMemoryVisitor ()
    {}

    /**
     * Visits the given {@link LogicalMemory} and all of its
     * constituent {@link Allocation Allocations}
     *
     * @param mem a non null {@link LogicalMemory} object
     * @throws NullPointerException if mem is null
     */
    public void visit (LogicalMemory mem)
    {
        _memory.ln(_memory.MEMVISIT, "Visiting " + mem);
        for (Iterator iter = mem.getAllocations().iterator(); iter.hasNext();)
        {
            MemoryVisitable alloc = (MemoryVisitable)iter.next();
            alloc.accept(this);
        }
    }
    
    /**
     * Visits the given {@link Allocation} and continues the traversal
     * by calling the accept method of its {@link LogicalValue}
     *
     * @param alloc a non null {@link Allocation} object
     * @throws NullPointerException if alloc is null
     */
    public void visit (Allocation alloc)
    {
        _memory.ln(_memory.MEMVISIT, "Visiting " + alloc);
        assert alloc.getInitialValue() != null : "Illegal internal structure.  An allocation has null initial value";
        MemoryVisitable value = (MemoryVisitable)alloc.getInitialValue();
        value.accept(this);
    }
    
    /**
     * Visits the given {@link Pointer}.
     *
     * @param ptr a non null {@link Pointer} object
     * @throws NullPointerException if ptr is null
     */
    public void visit (Pointer ptr)
    {
        _memory.ln(_memory.MEMVISIT, "Visiting " + ptr);
        if (ptr == null)
            throw new NullPointerException("Null visit to pointer");
    }
    
    /**
     * Visits the given {@link Record} and continues the traversal by
     * calling the accept method of each constitutent {@link
     * LogicalValue}
     *
     * @param rec a non null {@link Record} object
     * @throws NullPointerException if rec is null
     */
    public void visit (Record rec)
    {
        _memory.ln(_memory.MEMVISIT, "Visiting " + rec);
        for (Iterator iter = rec.getComponentValues().iterator(); iter.hasNext();)
        {
            MemoryVisitable value = (MemoryVisitable)iter.next();
            value.accept(this);
        }
    }
    
    /**
     * Visits the given {@link Scalar}.
     *
     * @param sclr a non null {@link Scalar} object
     * @throws NullPointerException if ptr is null
     */
    public void visit (Scalar sclr)
    {
        _memory.ln(_memory.MEMVISIT, "Visiting " + sclr);
        if (sclr == null)
            throw new NullPointerException("Null visit to scalar");
    }
    
    /**
     * Visits the given {@link Slice}.
     *
     * @param slice a non null {@link Slice} object
     * @throws NullPointerException if slice is null
     */
    public void visit (Slice slice)
    {
        _memory.ln(_memory.MEMVISIT, "Visiting " + slice);
        if (slice == null)
            throw new NullPointerException("Null visit to Slice");
    }
    
    
}// DefaultMemoryVisitor
