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

package net.sf.openforge.schedule.loop;


import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.schedule.*;

/**
 * LoopFlopConflict is a lightweight class that associates the
 * relevant elements that define a resource conflict which would keep
 * a loop flop from being removed.  
 *
 * @author imiller
 * @version $Id: LoopFlopConflict.java 88 2006-01-11 22:39:52Z imiller $
 */
public class LoopFlopConflict
{
    private static final String _RCS_ = "$Rev: 88 $";

    private Component conflictHead;
    private List conflictHeadPath;
    private Component conflictTail;
    private List conflictTailPath;

    public LoopFlopConflict (Component head, List headPath, Component tail, List tailPath)
    {
        this.conflictHead = head;
        this.conflictHeadPath = Collections.unmodifiableList(new ArrayList(headPath));
        this.conflictTail = tail;
        this.conflictTailPath = Collections.unmodifiableList(new ArrayList(tailPath));
    }

    /**
     * Returns the access which occurs in the first cycle of the
     * containing loop body and is conflicted with the tail access of
     * this conflict object.
     */
    public Component getHeadAccess ()
    {
        return this.conflictHead;
    }

    /**
     * Returns the 'relevant' components in the hierarchical path from
     * the loop body to the head access, including blocks, branches,
     * loops, loop bodies, and taskCalls.  {@see LoopFlopAnalysis}
     * which builds the 'pathToComponent' variable through the
     * moduleDive method.  This method returns a copy of that stack.
     */
    public List getHeadPath ()
    {
        return this.conflictHeadPath;
    }
    
    /**
     * Returns the access which occurs in the last cycle of the
     * containing loop body and is conflicted with the head access of 
     * this conflict object.
     */
    public Component getTailAccess ()
    {
        return this.conflictTail;
    }
    
    /**
     * Returns the 'relevant' components in the hierarchical path from
     * the loop body to the tail access, including blocks, branches,
     * loops, loop bodies, and taskCalls.  {@see LoopFlopAnalysis}
     * which builds the 'pathToComponent' variable through the
     * moduleDive method.  This method returns a copy of that stack.
     */
    public List getTailPath ()
    {
        return this.conflictTailPath;
    }
    
}
