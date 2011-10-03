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

package net.sf.openforge.verilog.pattern;


import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.util.naming.ID;
import net.sf.openforge.verilog.model.*;

/**
 * GenericModule is used to create a verilog Module for any given LIM
 * Module.  This class is closely coupled with GenericInstance.  NOTE
 * that in order to have everything work correctly you must call the
 * {@link InBuf#setOpaque} and {@link OutBuf#setOpaque}, setting both
 * to <i>true</i>.  This ensures that constant prop does not push bus
 * bits across a verilog module boundry.
 *
 * <p>Created: Thu Feb 20 12:47:22 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: GenericModule.java 2 2005-06-09 20:00:48Z imiller $
 */
public class GenericModule extends net.sf.openforge.verilog.model.Module 
    implements MappedModuleSpecifier
{
    private static final String _RCS_ = "$Rev: 2 $";

    private Set mappedModules= new HashSet();

    private Map interfaceMap = new HashMap();
    private net.sf.openforge.lim.Module mod;
    private int modCount = 0;
    
    /**
     * Constructs a verilog Module based on the interface defined by
     * the given LIM Module.
     *
     * @param module a value of type 'Module'
     */
    public GenericModule (net.sf.openforge.lim.Module module)
    {
        this(module, ID.showLogical(module));
    }
    
    /**
     * Constructs a verilog Module based on the interface defined by
     * the given LIM Module.
     *
     * @param module a value of type 'Module'
     * @param name the name to give the module
     */
    public GenericModule (net.sf.openforge.lim.Module module, String name)
    {
        super (ID.toVerilogIdentifier(name));
        
        this.mod = module;
        defineInterface(module);
    }

    /**
     * Defines the Module ports based on the Ports and Buses
     * of the Modules ports/buses
     */
    public void defineInterface(net.sf.openforge.lim.Module module)
    {
        // define input ports (based on the body's Ports)
        for (Iterator mod_ports = module.getPorts().iterator(); mod_ports.hasNext();)
        {
            addInput((Port)mod_ports.next());
        }
        
        // define output ports (based on the body's buses)
        for (Iterator mod_buses = module.getBuses().iterator(); mod_buses.hasNext();)
        {
            addOutput((Bus)mod_buses.next());   
        }
    } // defineInterface
    
    private void addInput(Port p)
    {
        if (p.isUsed())
        {
            assert (p.getPeer() != null) : "Can't add input for Port with null source.";
            assert (p.getPeer().getValue() != null) : "Can't add input for Port with null value.";
            BusInput in = new BusInput(p.getPeer());
            addPort(in);
            interfaceMap.put(p, in.getIdentifier());
        }
    } // addInput()
    
    private void addOutput(Bus b)
    {
        if (b.isUsed())
        {
            /* CWU - Since the partial constant prop has resolved the source
             * bus of each bits stored in Value, all the pass through
             * components has been removed at this point. we just need to
             * make sure that the bus's value is not null.   
//             assert (b.getSource() != null) : "Can't add output for Bus with null source.";
//             assert (b.getSource().getValue() != null) : "Can't add output for Bus with null value.";
            */
            assert (b.getValue() != null) : "Can't add output for Bus with null value.";

            BusOutput out = new BusOutput(b);
            addPort(out);
            interfaceMap.put(b, out.getIdentifier());
        }
    } // addOutput()

    public GenericInstance makeInstance()
    {
        modCount++;
        return makeInstance(getIdentifier().getToken() +
                            ((modCount > 0) ? "_" + Integer.toString(modCount) : ""));
    }
    
    public GenericInstance makeInstance (String instanceName)
    {
        GenericInstance inst = new GenericInstance(this, instanceName);

        for (Iterator iter = this.mod.getPorts().iterator(); iter.hasNext();)
        {
            Port p = (Port)iter.next();
            if (!p.isUsed())
                continue;
            Identifier id = (Identifier)interfaceMap.get(p);
            Input in = new Input(id, p.getValue().getSize());
            inst.connect(in, new PortWire(p));
        }
        for (Iterator iter = this.mod.getBuses().iterator(); iter.hasNext();)
        {
            Bus b = (Bus)iter.next();
            Identifier id = (Identifier)interfaceMap.get(b);
            if (id == null)
                continue;
            Output out = new Output(id, b.getValue().getSize());
            inst.connect(out, NetFactory.makeNet(b));
        }
        return inst;
    }

    /**
     * Adds a statement to the statement block of the module,
     * and a declaration for each undeclared Net produced by
     * the statement.
     */
    public void state(Statement statement)
    {
        assert (
            (statement instanceof ForgePattern) ||
            (statement instanceof InlineComment)
            ) : "DesignModule only supports stating ForgePatterns.";
        
        if (statement instanceof ForgePattern)
        {
            for (Iterator it = ((ForgePattern)statement).getProducedNets().iterator(); it.hasNext();)
            {
                Net net = (Net)it.next();
                if (!isDeclared(net))
                {
                    declare(net);
                }
            }
        }
        this.statements.add(statement);
        
        if (statement instanceof MappedModuleSpecifier)
        {
            mappedModules.addAll(((MappedModuleSpecifier)statement).getMappedModules());
        }
        
    } // state()

    /**
     * Provides the Set of mapped Modules
     */
    public Set getMappedModules()
    {
        return mappedModules;
    }
    
}// GenericModule
