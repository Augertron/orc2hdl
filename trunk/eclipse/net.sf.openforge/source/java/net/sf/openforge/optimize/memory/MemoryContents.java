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

package net.sf.openforge.optimize.memory;

import java.util.*;

import net.sf.openforge.lim.memory.*;
import net.sf.openforge.util.naming.ID;

/**
 * MemoryContents is a mutable record of Location and Accesses that
 * are to be contained in a common LogicalMemory.  New accesses and
 * the Locations they target may be added at any time.  Similarly, two
 * MemoryContents may be merged to combine their records.  The
 * MemoryContents can also generate a LogicalMemory representing the
 * complete set of accesses and Locations.
 *
 * <p>The set of contained accesses and Locations will not contain
 * null.
 *
 * <p>Created: Wed Aug 20 13:57:28 2003
 *
 * @author imiller, jensen; last modified by $Author: imiller $
 * @version $Id: MemoryContents.java 107 2006-02-23 15:46:07Z imiller $
 */
public class MemoryContents 
{
    private static final String _RCS_ = "$Rev: 107 $";

    /** Map of LValue to Set of accessed Locations */
    private Map accessToLocation = new HashMap();

    /** Set of all Locations accessed by the LValues in this MemoryContents */
    private Set locations = new HashSet();

    /**
     * Set of address source LocationValueSources for all the access LValues
     * in this MemoryContents
     */
    private Set addressSources = new HashSet();

    /**
     * The correlation of old Allocation to new Allocation that is
     * generated when a new memory is built for this memory contents.
     * It MUST be kept locally with this MemoryContens (and used by
     * the retargetAddressSources method) because one source
     * Allocation may end up being copied into multiple new (split)
     * memories.  eg, a struct whose fields are being split into
     * different memories.  Thus it is necessary to know which new
     * Allocation to use for THIS memory contents so that the address
     * sources target it correctly.
     */
    private Map allocationCorrelation = Collections.EMPTY_MAP;
    
    /**
     * Constructs a new empty <code>MemoryContents<code>.
     */
    public MemoryContents ()
    {}

    /**
     * Records for a given {@link LValue} a set of {@link Location Locations}
     * that it accesses and a set of {@link LocationValueSource LocationValueSources}
     * on which it depends.  Duplicate {@link Location Locations} and
     * {@link LocationValueSource LocationValueSources} are ignored.
     *
     * <p>requires: non-null arguments.
     * <p>modifies: this
     * <p>effects: none
     *
     * @param access the memory access to be added
     * @param locations a set of {@link Location Locations} that are accessed
     *          by <code>access</code>; these will be added to any that have already
     *          been recorded for this <code>access</code>
     * @param sources a set of {@link LocationValueSource LocationValueSources} on
     *          which the <code>access</code> depends and which must reference the
     *          same {@link LogicalMemory} as the given <code>locations</code>
     *
     * @throws IllegalArgumentException if <code>access</code>, <code>locations</code>,
     *           or <code>addressSources</code> is null, or if <code>locations</code>
     *           or <code>addressSources</code> contains null
     */
    public void addContents (LValue access, Set locations, Set sources)
    {
        if (access == null)
        {
            throw new IllegalArgumentException("null access");
        }

        if (locations == null)
        {
            throw new IllegalArgumentException("null locations");
        }

        if (locations.contains(null))
        {
            throw new IllegalArgumentException("null Location");
        }

        if (sources == null)
        {
            throw new IllegalArgumentException("null addressSources");
        }

        if (sources.contains(null))
        {
            throw new IllegalArgumentException("null LocationValueSource");
        }

        Set accessLocations = (Set)accessToLocation.get(access);
        if (accessLocations == null)
        {
            accessLocations = new HashSet();
            accessToLocation.put(access, accessLocations);
        }
        accessLocations.addAll(locations);

        this.locations.addAll(locations);
        this.addressSources.addAll(sources);
    }

    /**
     * Tests whether a given {@link Location} overlaps a {@link Location}
     * for any {@link LValue} that has been recorded in this contents.
     *
     * <p>requires: non-null location
     * <p>modifies: none
     * <p>effects: returns true if the location designates a region of
     * memory already contained within this.
     * 
     * @param location a location to be tested for overlap
     * @param addressSources a set of {@link LocationValueSource LocationValueSources}
     *          that are also tested
     *
     * @return true if <code>location</code> overlaps any {@link Location} recorded
     *         in this contents, or if <code>addressSources</code> contains any of
     *         the {@link LocationValueSource LocationValueSources} recorded in this contents
     *
     * @throws IllegalArgumentException if <code>location</code> or
     *           <code>addressSources</code> is null
     */
    public boolean overlaps (Location location, Set addressSources)
    {
        if (location == null)
        {
            throw new IllegalArgumentException("null location");
        }

        if (addressSources == null)
        {
            throw new IllegalArgumentException("null addressSources");
        }
        for (Iterator iter = this.locations.iterator(); iter.hasNext();)
        {
            final Location nextLocation = (Location)iter.next();
            if (location.overlaps(nextLocation))
            {
                return true;
            }
        }

        for (Iterator iter = addressSources.iterator(); iter.hasNext();)
        {
            if (this.addressSources.contains(iter.next()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Merges all information from a given <code>MemoryContents</code> into
     * this object by copying.
     *
     * <p>requires: non-null MemoryContents parameter
     * <p>modifies: this
     * <p>effects: none
     * 
     * @param contents the contents to be merged
     * @throws IllegalArgumentException if <code>contents</code> is null
     */
    public void merge (MemoryContents contents)
    {
        if (contents == null)
        {
            throw new IllegalArgumentException("null contents");
        }

        if (contents == this)
        {
            return;
        }

        for (Iterator iter = contents.accessToLocation.entrySet().iterator(); iter.hasNext();)
        {
            final Map.Entry entry = (Map.Entry)iter.next();
            final LValue access = (LValue)entry.getKey();
            final Set locations = (Set)entry.getValue();
            addContents(access, locations, Collections.EMPTY_SET);
        }
        
        this.addressSources.addAll(contents.addressSources);
    }

    /**
     * Returns a LogicalMemory object whose contents represent the
     * complete set of Locations added to this MemoryContents and
     * which is the target (or Reference) of every LValue access added
     * to this MemoryContents.
     *
     * <p>requires: none
     * <p>modifies: all LValue accesses added to this MemoryContents
     * <p>effects: returns a LogicalMemory containing all Locations
     * and accesses from this MemoryContents.
     *
     * @return a LogicalMemory containing all Locations and accesses
     * from this MemoryContents.
     */
    public MemCopyTuple buildMemory ()
    {
        /*
         * build set of absolute base locations & find max size necessary
         */
        HashSet absoluteBaseLocationSet=new HashSet();
        int maxAddressPortSize=0;
        for (Iterator iter = this.locations.iterator(); iter.hasNext();)
        {
            Location location = (Location)iter.next();
            absoluteBaseLocationSet.add(location.getAbsoluteBase());
            maxAddressPortSize = Math.max(maxAddressPortSize,
                location.getLogicalMemory().getMaxAddressWidth());
        }

        /*
         * create the new memory & a port
         */
        LogicalMemory newMemory = new LogicalMemory(maxAddressPortSize);
        LogicalMemoryPort newMemoryPort = newMemory.createLogicalMemoryPort();
        
        /*
         * allocate space in the new memory, based upon copies of the
         * original locations
         */
        Map oldToNewAllocationMap = new HashMap();
        LogicalValueCopier copier = new LogicalValueCopier();
        for (Iterator iter = absoluteBaseLocationSet.iterator(); iter.hasNext();)
        {
            Allocation alloc = (Allocation)iter.next();
            alloc.getInitialValue().accept(copier);
            LogicalValue copy = copier.getCopy();
            Allocation newAlloc = newMemory.allocate(copy);
            ID.copy(alloc, newAlloc);
            newAlloc.copyBlockElements(alloc);
            oldToNewAllocationMap.put(alloc, newAlloc);
        }
        
        /*
         * and finally retarget the original accesses
         */
        RetargetVisitor vis = new RetargetVisitor(oldToNewAllocationMap);
        for (Iterator iter = accessToLocation.keySet().iterator(); iter.hasNext();)
        {
            LValue lvalue = (LValue)iter.next();
            lvalue.getLogicalMemoryPort().removeAccess(lvalue);
            newMemoryPort.addAccess(lvalue);
            lvalue.accept(vis);
        }
        
        this.allocationCorrelation = oldToNewAllocationMap;
        return new MemCopyTuple(copier.pointerMap, newMemory);
    }

    /**
     * Retargets all of the identified address sources based on the
     * two correlations given.  The pointerCorrelation is used to
     * inform this memory contents that due to the rebuilding of other
     * memories that any Pointer address source may be obsolete now.
     * Thus the mapping provides a correlation from the Pointer
     * address source (original) that may be contained in this class
     * with a Set of new Pointer objects that were created from that
     * original Pointer.  Only one of the set of new pointer objects
     * is really the address source, but the others are 'non used' and
     * can safely be retargetted as well.
     * The correlation of old to new Allocation is taken care of by a
     * mapping which is stored in this MemoryContents in order to
     * protect against conflict with other MemoryContents which are
     * creating an Allocation for the same base.  This correlation is 
     * used to find the new value of the target of the
     * LocationValueSources (address sources) in this class.
     *
     * @param pointerCorrelation a Map of Pointer (the address sources
     * in this class) to a Set of Pointers (the new objects, created
     * when other memories were split)
     */
    public void retargetAddressSources (Map pointerCorrelation)
    {
        // retarget the LocationValueSources that have been identified.
        for (Iterator iter = this.addressSources.iterator(); iter.hasNext();)
        {
            LocationValueSource src = (LocationValueSource)iter.next();
            Set sources = Collections.singleton(src);
            if (pointerCorrelation.containsKey(src))
            {
                sources = (Set)pointerCorrelation.get(src);
            }
            // Retarget ALL new address sources created from the
            // original address source captured in this memory
            // contents.
            for (Iterator sourceIter = sources.iterator(); sourceIter.hasNext();)
            {
                LocationValueSource source = (LocationValueSource)sourceIter.next();
                Location oldLoc = source.getTarget();
                Location newLoc = Variable.getCorrelatedLocation(this.allocationCorrelation,oldLoc);
                assert newLoc != null : "Illegal null value for correlated location";
                source.setTarget(newLoc);
            }
        }
    }

    // for unit tests
    int getNumberOfAccesses ()
    {
        return accessToLocation.size();
    }

    public void debug ()
    {
        System.out.println(this);
        System.out.println("\tLocs: " + this.locations);
        System.out.println("\taccToLoc: " + this.accessToLocation);
        System.out.println("\taddrSrc: " + this.addressSources);
    }

    /**
     * A convenience class to return all of the data associated with a
     * copied memory.
     */
    class MemCopyTuple
    {
        private Map pointerCorr;
        private LogicalMemory copy;

        public MemCopyTuple (Map pointers, LogicalMemory mem)
        {
            this.pointerCorr = pointers;
            this.copy = mem;
        }
        
        public Map getPointerCorrelation () { return this.pointerCorr; }
        public LogicalMemory getCopiedMemory () { return this.copy; }
    }
    
    /**
     * This visitor is used to make deep copies of a LogicalValue
     * while keeping track of the correlation between old Pointer and
     * new Pointer (which is needed to fix address sources).  For this
     * reason, we cannot just use the LogicalValue.copy method.
     */
    class LogicalValueCopier implements MemoryVisitor
    {
        LogicalValue current;
        Map pointerMap = new HashMap();
        public LogicalValue getCopy ()
        {
            assert this.current != null;
            return this.current;
        }
        
        public void visit (LogicalMemory mem) {throw new IllegalArgumentException("Unexpected traversal of memory");}
        public void visit (Allocation alloc) {throw new IllegalArgumentException("Unexpected traversal of allocation");}
        public void visit (Pointer ptr)
        {
            this.current = new Pointer(ptr.getTarget());
            this.pointerMap.put(ptr, this.current);
        }
        public void visit (Record rec)
        {
            List values = new ArrayList();
            for (Iterator iter = rec.getComponentValues().iterator(); iter.hasNext();)
            {
                ((MemoryVisitable)iter.next()).accept(this);
                values.add(this.current);
            }
            this.current = new Record(values);
        }
        public void visit (Scalar sclr)
        {
            this.current = sclr.copy();
        }
        public void visit (Slice slice)
        {
            slice.getSourceValue().accept(this);
            this.current = new Slice(this.current, slice.getDelta(), slice.getSize());
        }
    }
    
}// MemoryContents

