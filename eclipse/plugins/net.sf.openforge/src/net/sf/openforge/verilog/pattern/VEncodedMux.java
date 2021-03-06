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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import net.sf.openforge.lim.primitive.EncodedMux;
import net.sf.openforge.verilog.model.Assign;
import net.sf.openforge.verilog.model.BinaryNumber;
import net.sf.openforge.verilog.model.CaseBlock;
import net.sf.openforge.verilog.model.Control;
import net.sf.openforge.verilog.model.EventControl;
import net.sf.openforge.verilog.model.EventExpression;
import net.sf.openforge.verilog.model.Keyword;
import net.sf.openforge.verilog.model.Lexicality;
import net.sf.openforge.verilog.model.Net;
import net.sf.openforge.verilog.model.NetFactory;
import net.sf.openforge.verilog.model.Symbol;

/**
 * A VEncodedMux uses case block style implementations based on a LIM
 * {@link EncodedMux} object.
 * <P>
 * Example:<BR>
 * <CODE>
 * always @(select or in1 or in2 or in3 or in4)<BR>
 * begin<BR>
 *     case(select)<BR>
 *       2'b00 : out = in1;<BR>
 *       2'b01 : out = in2;<BR>
 *       2'b10 : out = in3;<BR>
 *       2'b11 : out = in4;<BR>
 *    endcase<BR>
 * end<BR>
 * </CODE>
 * <P>
 * Created: Tue Jun 25, 2002
 * 
 * @author cwu
 * @version $Id: VEncodedMux.java 2 2005-06-09 20:00:48Z imiller $
 */
public class VEncodedMux implements ForgePattern {

	private EventControl eventControl;
	private CaseBlock caseBlock;

	private Set<Net> produced_nets = new LinkedHashSet<Net>();
	private Set<Net> consumed_nets = new LinkedHashSet<Net>();

	/**
	 * Constructs a VEncodedMux in the form of case block based on a LIM
	 * EncodedMux.
	 * 
	 * @param enMux
	 *            the LIM EncodedMux upon which to base the verilog
	 *            implementation
	 */
	public VEncodedMux(EncodedMux enMux) {
		PortWire caseController = new PortWire(enMux.getSelectPort());
		consumed_nets.add(caseController);
		EventExpression sensitiveList = new EventExpression(caseController);
		for (int i = 0; i < enMux.getSize() - 1; i++) {
			// if (!enMux.getDataPort(i).getBus().getValue().isConstant())
			if (!enMux.getDataPort(i).getValue().isConstant()) {
				Net sensitiveNet = new PortWire(enMux.getDataPort(i));
				sensitiveList.add(sensitiveNet);
			}
		}
		eventControl = new EventControl(sensitiveList);
		caseBlock = new CaseBlock(caseController);
		Net result = NetFactory.makeNet(enMux.getResultBus());
		produced_nets.add(result);
		int caseCount = enMux.getSize() - 1;
		for (int i = 0; i < caseCount; i++) {
			Net selected = new PortWire(enMux.getDataPort(i));
			BinaryNumber caseNumber = new BinaryNumber(i,
					caseController.getWidth());
			caseBlock.add(caseNumber.toString(), new Assign.Blocking(result,
					selected));
			consumed_nets.add(selected);
		}
	} // EncodedMux()

	public EventControl getEventControl() {
		return eventControl;
	}

	public CaseBlock getCaseBlock() {
		return caseBlock;
	}

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();

		lex.append(Control.NEWLINE);
		lex.append(Keyword.ALWAYS);
		lex.append(Control.WHITESPACE);
		// lex.append(eventControl);
		lex.append(Symbol.EVENT);
		lex.append(Symbol.OPEN_PARENTHESIS);
		lex.append(Symbol.SENSITIVE_ALL);
		lex.append(Symbol.CLOSE_PARENTHESIS);

		lex.append(Control.NEWLINE);
		lex.append(Keyword.BEGIN);
		lex.append(Control.NEWLINE);
		lex.append(caseBlock);
		lex.append(Control.NEWLINE);
		lex.append(Keyword.END);

		return lex;
	} // lexicalify()

	@Override
	public Collection<Net> getNets() {
		Set<Net> nets = new HashSet<Net>();

		nets.addAll(eventControl.getNets());
		nets.addAll(caseBlock.getNets());

		return nets;
	} // getNets()

	/**
	 * Provides the collection of Nets which this statement of verilog uses as
	 * input signals.
	 */
	@Override
	public Collection<Net> getConsumedNets() {
		return consumed_nets;
	}

	/**
	 * Provides the collection of Nets which this statement of verilog produces
	 * as output signals.
	 */
	@Override
	public Collection<Net> getProducedNets() {
		return produced_nets;
	}

	@Override
	public String toString() {
		return lexicalify().toString();
	}

} // EecodedMux

