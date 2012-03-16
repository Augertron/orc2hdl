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

package net.sf.openforge.lim.io.actor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sf.openforge.lim.And;
import net.sf.openforge.lim.Bus;
import net.sf.openforge.lim.Component;
import net.sf.openforge.lim.Exit;
import net.sf.openforge.lim.Latch;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.Not;
import net.sf.openforge.lim.Or;
import net.sf.openforge.lim.Port;
import net.sf.openforge.lim.Referencer;
import net.sf.openforge.lim.Reg;
import net.sf.openforge.lim.Value;
import net.sf.openforge.lim.io.FifoAccess;
import net.sf.openforge.lim.io.FifoID;
import net.sf.openforge.lim.io.FifoIF;
import net.sf.openforge.lim.io.FifoOutput;
import net.sf.openforge.lim.io.FifoWrite;
import net.sf.openforge.lim.io.SimpleFifoPin;
import net.sf.openforge.lim.io.SimplePin;
import net.sf.openforge.lim.io.SimplePinRead;
import net.sf.openforge.lim.io.SimplePinWrite;
import net.sf.openforge.lim.op.CastOp;
import net.sf.openforge.lim.op.Constant;
import net.sf.openforge.lim.op.SimpleConstant;

/**
 * ActorScalarOutput is a specialized fifo output interface which contains the
 * necessary infrastructure to support scalar data types. This includes:
 * <p>
 * <ul>
 * <li>Data output</li>
 * <li>Send output</li>
 * <li>Ack input</li>
 * <li>Ready input</li>
 * <li>count output</li>
 * </ul>
 * 
 * 
 * <p>
 * Created: Fri Aug 26 15:14:55 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: ActorScalarOutput.java 280 2006-08-11 17:00:32Z imiller $
 */
public class ActorScalarOutput extends FifoOutput implements ActorPort {

	private String baseName;
	private SimplePin data;
	private SimplePin send;
	private SimplePin ack;
	private SimplePin rdy;
	private SimplePin tokenCount;

	// public ActorScalarOutput (String idString, int width)
	public ActorScalarOutput(FifoID fifoID) {
		super(fifoID.getBitWidth());

		this.baseName = fifoID.getName();
		final String pinBaseName = buildPortBaseName(this.baseName);

		this.data = new SimpleFifoPin(this, getWidth(), pinBaseName + "_DATA");
		this.send = new SimpleFifoPin(this, 1, pinBaseName + "_SEND");
		this.ack = new SimpleFifoPin(this, 1, pinBaseName + "_ACK");
		this.rdy = new SimpleFifoPin(this, 1, pinBaseName + "_RDY");
		this.tokenCount = new SimpleFifoPin(this, ActorPort.COUNT_PORT_WIDTH,
				pinBaseName + "_COUNT");

		this.addPin(this.data);
		this.addPin(this.send);
		this.addPin(this.ack);
		this.addPin(this.rdy);
		this.addPin(this.tokenCount);
	}

	/**
	 * <code>getType</code> returns {@link FifoIF#TYPE_ACTOR_QUEUE}
	 * 
	 * @return an <code>int</code> value
	 */
	public int getType() {
		return FifoIF.TYPE_ACTOR_QUEUE;
	}

	public String getPortBaseName() {
		return this.baseName;
	}

	/**
	 * ActorScalarOutput ports have no special naming requirements, this method
	 * returns portname
	 */
	protected String buildPortBaseName(String portName) {
		return portName;
	}

	/**
	 * asserts false
	 */
	public void setAttribute(int type, String value) {
		assert false : "No supported attributes";
	}

	/**
	 * Returns a subset of {@link #getPins} that are the output pins of the
	 * interface, containing only the data, write, and ctrl pins.
	 */
	public Collection<SimplePin> getOutputPins() {
		List<SimplePin> list = new ArrayList<SimplePin>();
		list.add(this.data);
		list.add(this.send);
		list.add(this.tokenCount);

		return Collections.unmodifiableList(list);
	}

	/**
	 * Returns a {@link FifoWrite} object that is used to obtain data from this
	 * FifoIF.
	 * 
	 * @return a blocking {@link FifoAccess}
	 */
	public FifoAccess getAccess() {
		return getAccess(true);
	}

	/**
	 * Returns a {@link FifoWrite} object that is used to obtain data from this
	 * FifoIF.
	 * 
	 * @param blocking
	 *            if set true returns a blocking fifo write otherwise a
	 *            non-blocking access.
	 * @return a {@link FifoAccess}
	 */
	public FifoAccess getAccess(boolean blocking) {
		if (blocking) {
			return new ActorScalarOutputWrite(this);
		} else {
			return new ActorScalarSimpleOutputWrite(this);
		}
	}

	/** Returns the output data pin for this interface */
	public SimplePin getDataPin() {
		return this.data;
	}

	/**
	 * Returns the output send pin, indicating that the interface is outputting
	 * valid data
	 */
	public SimplePin getSendPin() {
		return this.send;
	}

	/**
	 * Returns the input acknowledge pin, indicating that the queue that the
	 * interface is sending to has acknowledged reciept of the data
	 */
	public SimplePin getAckPin() {
		return this.ack;
	}

	/**
	 * Returns the input ready pin, indicating that the queue is ready to accept
	 * at least one token.
	 */
	public SimplePin getReadyPin() {
		return this.rdy;
	}

	/**
	 * Unsupported on output interface
	 * 
	 * @throws UnsupportedOperationException
	 *             always
	 */
	public Component getCountAccess() {
		throw new UnsupportedOperationException(
				"Output channels do not have token count facility");
	}

	public Component getPeekAccess() {
		throw new UnsupportedOperationException(
				"Peeking at output interface not yet supported");
		// return new ActionTokenPeek(this);
	}

	public Component getStatusAccess() {
		// throw new
		// UnsupportedOperationException("Status of output interface not yet supported");
		return new ActionPortStatus(this);
	}

	/**
	 * Tests the referencer types and then returns 1 or 0 depending on the types
	 * of each accessor.
	 * 
	 * @param from
	 *            the prior accessor in source document order.
	 * @param to
	 *            the latter accessor in source document order.
	 */
	public int getSpacing(Referencer from, Referencer to) {
		// Options for accesses to an output are
		// FifoWrite (ActorScalarOutputWrite)
		// ActionPortStatus

		if (from instanceof FifoWrite) {
			return 1;
		} else if (from instanceof ActionPortStatus) {
			return 0;
		} else {
			throw new IllegalArgumentException("Source access to " + this
					+ " is of unknown type " + from.getClass());
		}
	}

	private class ActorScalarOutputWrite extends FifoWrite {
		private ActorScalarOutputWrite(ActorScalarOutput aso) {
			// super(aso, Latency.ZERO.open(new Object()));
			super(aso, null);
			final Exit exit = getExit(Exit.DONE);
			exit.setLatency(Latency.ZERO.open(exit));

			this.setProducesDone(true);
			this.setDoneSynchronous(true);
			// super(aso);
			// Because the write protocol is different from FSL we
			// need to fully populate the logic here.

			final Port data = (Port) getDataPorts().get(0);
			final Bus done = exit.getDoneBus();
			/*
			 * Build the following code: dataLatch = writeData(port) : en GO
			 * fifo_data = writeData (port); pending = (GO || GO'); send =
			 * pending GO' <= pending & !ack write_done = pending & ack
			 */
			// Needs RESET b/c it is in the control path
			final Reg flop = Reg.getConfigurableReg(Reg.REGR,
					"fifoWritePending");
			final Or pending = new Or(2);
			final And done_and = new And(2);
			final And flop_and = new And(2);
			final Not not = new Not();
			final CastOp doutCast = new CastOp(aso.getDataPin().getWidth(),
					false);
			final SimplePinWrite dout = new SimplePinWrite(aso.getDataPin());
			final SimplePinWrite send = new SimplePinWrite(aso.getSendPin());
			final SimplePinRead ack = new SimplePinRead(aso.getAckPin());
			final Latch capture = new Latch();

			// Give the flops an initial size.
			flop.getResultBus().pushValueForward(new Value(1, false));

			// Connect the clock ports
			flop.getClockPort().setBus(this.getClockPort().getPeer());
			flop.getResetPort().setBus(this.getResetPort().getPeer());
			flop.getInternalResetPort().setBus(this.getResetPort().getPeer());
			capture.getClockPort().setBus(this.getClockPort().getPeer());

			// add all the components
			this.addComponent(flop);
			this.addComponent(pending);
			this.addComponent(done_and);
			this.addComponent(flop_and);
			this.addComponent(not);
			this.addComponent(doutCast);
			this.addComponent(dout);
			this.addComponent(send);
			this.addComponent(ack);
			this.addComponent(capture);

			// Hook up data capture latch
			capture.getDataPort().setBus(data.getPeer());
			capture.getEnablePort().setBus(getGoPort().getPeer());

			// Hook fifo data through
			doutCast.getDataPort().setBus(capture.getResultBus());
			dout.getDataPort().setBus(doutCast.getResultBus());
			dout.getGoPort().setBus(done_and.getResultBus());

			// Calculate pending
			((Port) pending.getDataPorts().get(0)).setBus(flop.getResultBus());
			((Port) pending.getDataPorts().get(1))
					.setBus(getGoPort().getPeer());

			// calculate the pending term
			not.getDataPort().setBus(ack.getResultBus());
			((Port) flop_and.getDataPorts().get(0)).setBus(pending
					.getResultBus());
			((Port) flop_and.getDataPorts().get(1)).setBus(not.getResultBus());

			// Connect the flop input
			flop.getDataPort().setBus(flop_and.getResultBus());

			// Connect the fifoWR. It is set HIGH during the entire
			// pending write.
			send.getDataPort().setBus(pending.getResultBus());
			send.getGoPort().setBus(pending.getResultBus());

			// calculate the write complete term
			((Port) done_and.getDataPorts().get(0)).setBus(pending
					.getResultBus());
			((Port) done_and.getDataPorts().get(1)).setBus(ack.getResultBus());

			// Connect the done
			done.getPeer().setBus(done_and.getResultBus());

			// Define the feedback point
			// this.feedbackPoints = Collections.singleton(flop);
			addFeedbackPoint(flop);

			// Add a write of a constant 1 to the output tokenCount.
			final SimplePinWrite countWrite = new SimplePinWrite(aso.tokenCount);
			final Constant index1 = new SimpleConstant(1,
					aso.tokenCount.getWidth());
			final Constant index1_1 = new SimpleConstant(1, 1);
			index1.pushValuesForward(); // ensures the bus has a value.
			index1_1.pushValuesForward(); // ensures the bus has a value.
			addComponent(index1);
			addComponent(index1_1);
			addComponent(countWrite);
			countWrite.getDataPort().setBus(index1.getValueBus());
			countWrite.getGoPort().setBus(index1_1.getValueBus()); // ALWAYS
																	// enabled
		}
	}

	private class ActorScalarSimpleOutputWrite extends FifoWrite {
		// This class assumes that the output write will always
		// succeed. So, set the data, strobe the send high and
		// finish.
		private ActorScalarSimpleOutputWrite(ActorScalarOutput aso) {
			// super(aso, Latency.ONE);
			// The spacing (handled by the getSpacing method) ensures
			// that we do not have a conflict on the resource.
			super(aso, Latency.ZERO);

			final SimplePinWrite dout = new SimplePinWrite(aso.getDataPin());
			final SimplePinWrite write = new SimplePinWrite(aso.getSendPin());
			addComponent(dout);
			addComponent(write);

			dout.getDataPort().setBus(((Port) getDataPorts().get(0)).getPeer());
			dout.getGoPort().setBus(getGoPort().getPeer());

			write.getDataPort().setBus(getGoPort().getPeer());
			write.getGoPort().setBus(getGoPort().getPeer());

			// Add a write of a constant 1 to the output tokenCount.
			final SimplePinWrite countWrite = new SimplePinWrite(aso.tokenCount);
			final Constant index1 = new SimpleConstant(1,
					aso.tokenCount.getWidth());
			final Constant index1_1 = new SimpleConstant(1, 1);
			index1.pushValuesForward(); // ensures the bus has a value.
			index1_1.pushValuesForward(); // ensures the bus has a value.
			addComponent(index1);
			addComponent(index1_1);
			addComponent(countWrite);
			countWrite.getDataPort().setBus(index1.getValueBus());
			countWrite.getGoPort().setBus(index1_1.getValueBus()); // ALWAYS
																	// enabled
		}
	}

}// ActorScalarOutput