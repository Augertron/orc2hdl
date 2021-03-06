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

package net.sf.openforge.report.throughput;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import net.sf.openforge.lim.Access;
import net.sf.openforge.lim.Latency;
import net.sf.openforge.lim.Task;
import net.sf.openforge.lim.memory.LogicalMemoryPort;
import net.sf.openforge.util.naming.ID;

/**
 * MemAddrLimit is a ThroughputLimit that is used for tracking the limitation
 * that a particular MemoryPort address port puts on a design. Because we must
 * guarantee that no two accesses use that address port simultaneously, there is
 * a limitation that the method cannot be re-called until the last access
 * (regardless of the first access type) has completed. This limitation on
 * throughput is not dependent on the backing data remaining consistent, but
 * rather ensuring that no 2 accesses collide on the port.
 * <p>
 * <b>NOTE: More advanced analysis could be used to detect if the suggested GO
 * spacing would keep the accesses from colliding. In that case the restriction
 * based on this limit could be relaxed.</b>
 */
public class MemAddrLimit implements ThroughputLimit {
	private LogicalMemoryPort resource;
	private MemMark base;
	private Map<Object, Latency> latest;

	public MemAddrLimit(LogicalMemoryPort resource, Access base, Latency lat,
			ID location) {
		this.resource = resource;
		this.base = new MemMark(base, lat, location);
		latest = new HashMap<Object, Latency>();
	}

	/**
	 * Marks the given access as another access to the resource being tracked
	 * here.
	 */
	public void mark(Access access, Latency lat, ID location) {
		latest.put(new MemMark(access, lat, location), lat);
		latest = Latency.getLatest(latest);
	}

	/**
	 * Returns the maximum distance (clock cycles)between any 2 accesses to the
	 * resource (memory port) being tracked here. This is the differenct between
	 * the latest access' max clocks and the base access' min clocks.
	 */
	@Override
	public int getLimit() {
		// If there is no 2nd access to the resource, then there is no
		// restriction on throughput so return 0.
		if (latest.isEmpty()) {
			return 0;
		}

		if (base.lat.isOpen()) {
			return Task.INDETERMINATE_GO_SPACING;
		}

		int longest = -1;
		for (Latency lat : latest.values()) {
			if (lat.getMaxClocks() == Latency.UNKNOWN) {
				return Task.INDETERMINATE_GO_SPACING;
			}
			longest = Math.max(longest, lat.getMaxClocks());
		}

		return longest - base.lat.getMinClocks();
	}

	@Override
	public void writeReport(PrintStream ps, int tabDepth) {
		if (latest.isEmpty()) {
			// No path, so don't write anything into the report!
			return;
		}
		ps.println("Resource: \"" + resource.showIDLogical()
				+ "\" limit imposed by address port of memory");
		ps.println("  Path start point: " + base.acc.showIDLocation()
				+ " in method/function " + base.loc.showIDLogical());

		for (Object obj : latest.keySet()) {
			MemMark mark = (MemMark) obj;
			ps.println("\tend point: " + mark.acc.showIDLocation()
					+ " in method/function " + mark.loc.showIDLogical()
					+ " length is: " + ResourceMark.latDiff(base.lat, mark.lat));
		}
	}

	static class MemMark {
		final Access acc;
		final Latency lat;
		final ID loc;

		MemMark(Access a, Latency l, ID i) {
			acc = a;
			lat = l;
			loc = i;
		}
	}
}
