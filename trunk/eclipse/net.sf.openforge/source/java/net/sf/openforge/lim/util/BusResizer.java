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
package net.sf.openforge.lim.util;

import java.util.Iterator;

import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.ControlDependency;
import net.sf.openforge.lim.DataDependency;
import net.sf.openforge.lim.Dependency;
import net.sf.openforge.lim.Entry;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.op.CastOp;

 
/**
 * @author gandhij
 *
 * This class helps create a mask structure to apply onto a bus
 * to reduce the  circuit size based on the mask. The structure
 * used to mask is  castop(castop(bus)) . The first cast op casts
 * the bus to the required size and then the next castop casts to
 * its original size. This is required to keep constant prop happy.
 * Doing this is equivalent to throwing away the unwanted bits. 
 * 
 */
public class BusResizer {
    
    private CastOp castOp = null;
    private CastOp reCastOp = null;
    private int size = 0;
    private Bus busToMask = null;
    private boolean createControlDependency = true;
    
    /**
     * Creates a BusResize object using the given size
     * 
     * @param size Number of bits the bus has to be resized to
     */
    public BusResizer(int size) 
    {
        super();
        this.size = size;   
    }
    
    /**
     * This Constructor is cretaed for resizing the loop variable bus.
     *
     * @param size Number of bits the bus has to be resized to
     * @param needControlDependency boolean flag of determine if the control
     *                              dependency is needed or not.
     */
    public BusResizer(int size, boolean needControlDependency)
    {
        this(size);
        this.createControlDependency = needControlDependency;
    }
    
    /**
     * Create a control Dependency between the source --> target components 
     * for the given entry
     * 
     * @param entry 
     * @param source
     * @param target
     */
    
    private void createControlDependency(Entry entry, Component source, Exit target)
    {
        ControlDependency controlDependency = new ControlDependency(target.getDoneBus());
        entry.addDependency(source.getGoPort(), controlDependency);
    }
    
    /**
     * Create a data dependency between the entry/port and bus pair
     * 
     * @param entry
     * @param port
     * @param bus
     */
    private void createDataDependency(Entry entry, Port port, Bus bus)
    {
        DataDependency dataDependency = new DataDependency(bus);
        entry.addDependency(port,dataDependency);
    }
    
    /**
     * Create the mask structure which contains the cast ops. 
     */
    private void createMaskStructure()
    {
        assert busToMask != null :"Bus to be resized is null !";
        assert busToMask.getValue() != null :"Bus Value is null !";
        // create the castops and connect them //
        castOp = new CastOp(size, busToMask.getValue().isSigned()); 
        castOp.makeEntry(busToMask.getOwner());
        
        reCastOp = new CastOp(busToMask.getSize(), busToMask.getValue().isSigned());
        Entry reCastOpEntry = reCastOp.makeEntry(castOp.getOnlyExit());
        
        createControlDependency(reCastOpEntry, reCastOp, castOp.getOnlyExit());
    }
    
    /**
     * Insert the mask structure between the bus and its dependents.
     *
     */
    private void insertMaskStructure()
    {
        // add the castOps to the same module as the bus // 
        busToMask.getOwner().getOwner().getOwner().addComponent(castOp);
        busToMask.getOwner().getOwner().getOwner().addComponent(reCastOp);
        
        // move all bus dependencies from busToMask to recastOp's result bus // 
        Iterator iter = busToMask.getLogicalDependents().iterator();
        while(iter.hasNext())
        {
            Dependency dependency  = (Dependency)iter.next();
            Port port = dependency.getPort();
            Entry entry = dependency.getEntry();
            createDataDependency(entry,port,reCastOp.getResultBus());
            if (createControlDependency)
            {
                createControlDependency(entry, port.getOwner(), reCastOp.getOnlyExit());
            }
        }
        
        // clear all bus dependents //
        busToMask.clearDependents();
        
        // connect up cast op and recastop //
        Entry reCastOpEntry = (Entry)reCastOp.getEntries().iterator().next();
        createDataDependency(reCastOpEntry, reCastOp.getDataPort(), castOp.getResultBus());
        
        Entry castOpEntry = (Entry)castOp.getEntries().iterator().next();
        // connect castOp's input port to busToMask // 
        createDataDependency(castOpEntry, castOp.getDataPort(), busToMask);
        createControlDependency(castOpEntry, castOp, busToMask.getOwner());
    }
    
    /**
     * Resize the bus. This will effectively resize all the logic
     * assiociated with the bus.
     * 
     * @param bus Bus to be resized
     */
    public void resizeBus(Bus bus)
    {
        // assert bus is not null //
        assert bus != null :"Bus to be masked is null !";
        busToMask = bus;
        createMaskStructure();
        insertMaskStructure();
    }
}
