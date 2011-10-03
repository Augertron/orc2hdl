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
package net.sf.openforge.verilog.model;

/**
 * VerilogSyntaxException represents a verilog syntax violation.
 *
 * <P>
 *
 * Created: Fri Feb 02 2001
 *
 * @author abk
 * @version $Id: VerilogSyntaxException.java 2 2005-06-09 20:00:48Z imiller $
 */
public class VerilogSyntaxException extends RuntimeException
{

    private static final String _RCS_ = "RCS_REVISION: $Rev: 2 $";

    public VerilogSyntaxException()
    {
        super("Verilog syntax error");
    }   

    public VerilogSyntaxException(String violation) 
    {
        super (violation);
    } // VerilogSyntaxException()
    
} // end of class VerilogSyntaxException
