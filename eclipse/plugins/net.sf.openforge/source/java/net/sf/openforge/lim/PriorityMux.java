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

package net.sf.openforge.lim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * PriorityMux is a {@link Module} composed of a tree of 2-1 {@link EncodedMux
 * EncodedMuxes}, which protects against any problems resulting from multiple
 * selects being asserted. There is only one data bus, representing the selected
 * data, and a Done bus which reflects the assertion of any select signal.
 * 
 * @version $Id: PriorityMux.java 2 2005-06-09 20:00:48Z imiller $
 */
public class PriorityMux extends Module implements Cloneable {
	private static final String _RCS_ = "$Rev: 2 $";

	private Map selectToData = new LinkedHashMap();

	private Bus resultBus;

	/**
	 * Creates and builds the internals of a priority based Mux, in which the
	 * 0th data port is the <b>LEAST</b> priority and the nth data port is the
	 * <b>HIGHEST</b> priority input.
	 */
	public PriorityMux(int inputs) {
		Exit main_exit = makeExit(0);
		resultBus = main_exit.makeDataBus();
		resultBus.setIDLogical("PriorityMux_result");
		Bus done_bus = main_exit.getDoneBus();

		// Build all the ports (data and select) that are necessary.
		LinkedList queue = new LinkedList();
		for (int i = 0; i < inputs; i++) {
			Port select_port = makeDataPort();
			Port data_port = makeDataPort();
			selectToData.put(select_port, data_port);

			SelectDataPair sdp = new SelectDataPair(select_port.getPeer(),
					data_port.getPeer());
			queue.add(sdp);
		}

		// Run through the data/select pairs until we have merged them
		// all down to 1.
		while (queue.size() > 1) {
			List subList = new LinkedList();
			while (queue.size() > 1) {
				SelectDataPair low = (SelectDataPair) queue.remove(0);
				SelectDataPair high = (SelectDataPair) queue.remove(0);
				EncodedMux emux = new EncodedMux(2);
				Or or = new Or(2);
				addComponent(or);
				addComponent(emux);

				// wire up the new mux and or
				((Port) or.getDataPorts().get(0)).setBus(high.getSelect());
				((Port) or.getDataPorts().get(1)).setBus(low.getSelect());

				emux.getSelectPort().setBus(high.getSelect());
				// Reversed order to eliminate need for a 'not'
				emux.getDataPort(0).setBus(low.getData());
				emux.getDataPort(1).setBus(high.getData());
				subList.add(
						0,
						new SelectDataPair(or.getResultBus(), emux
								.getResultBus()));
			}
			for (Iterator iter = subList.iterator(); iter.hasNext();) {
				// This reverses the order so that the highest
				// priority mux stays first in the queue.
				queue.add(0, iter.next());
			}
		}

		SelectDataPair last = (SelectDataPair) queue.get(0);
		resultBus.getPeer().setBus(last.getData());
		done_bus.getPeer().setBus(last.getSelect());
	}

	/**
	 * Retrieves the List of select {@link Port Ports} for this encoded mux in
	 * increasing priority order.
	 * 
	 * @return a 'List' of {@link Port Ports}
	 */
	public List getSelectPorts() {
		return Collections
				.unmodifiableList(new ArrayList(selectToData.keySet()));
	}

	/**
	 * Retrieves the data port that corresponds to a given select port.
	 */
	public Port getDataPort(Port select) {
		assert (selectToData.containsKey(select)) : "Unknown select Port, can't return a data Port";
		return (Port) selectToData.get(select);
	}

	/**
	 * Get's the output result bus for this component.
	 */
	public Bus getResultBus() {
		return resultBus;
	}

	/**
	 * Throws an exception, replacement in this class not supported.
	 */
	public boolean replaceComponent(Component removed, Component inserted) {
		throw new UnsupportedOperationException("Cannot replace components in "
				+ getClass());
	}

	/**
	 * Calls the super, then removes any reference to the given bus in this
	 * class.
	 */
	public boolean removeDataBus(Bus bus) {
		if (super.removeDataBus(bus)) {
			if (bus == this.resultBus)
				this.resultBus = null;
			return true;
		}
		return false;
	}

	public void accept(Visitor v) {
		v.visit(this);
	}

	protected void cloneNotify(Module moduleClone, Map cloneMap) {
		super.cloneNotify(moduleClone, cloneMap);
		PriorityMux clone = (PriorityMux) moduleClone;
		clone.selectToData = new LinkedHashMap();
		for (Iterator iter = selectToData.entrySet().iterator(); iter.hasNext();) {
			final Map.Entry entry = (Map.Entry) iter.next();
			final Port selectClone = getPortClone((Port) entry.getKey(),
					cloneMap);
			final Port dataClone = getPortClone((Port) entry.getValue(),
					cloneMap);
			clone.selectToData.put(selectClone, dataClone);
		}
		clone.resultBus = getBusClone(resultBus, cloneMap);
	}

	private class SelectDataPair {
		Bus select;
		Bus data;

		public SelectDataPair(Bus select, Bus data) {
			this.select = select;
			this.data = data;
		}

		public Bus getSelect() {
			return select;
		}

		public Bus getData() {
			return data;
		}
	}

} // class PriorityMux
