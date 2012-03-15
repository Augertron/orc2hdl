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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.openforge.lim.Design;
import net.sf.openforge.lim.Referenceable;
import net.sf.openforge.lim.Referencer;
import net.sf.openforge.lim.StateHolder;

/**
 * LogicalMemory is a symbolic representation of a memory space in a
 * {@link Design}. The two primary attributes of a LogicalMemory are its
 * <i>allocations</i> and <i>accesses</i>.
 * <P>
 * An {@linkplain Allocation allocation} is a region of memory that is defined
 * by the program via a variable declaration. Each allocation has associated
 * with it a size (the number of bytes needed to represent the variable's type)
 * and an {@linkplain LogicalValue initial value}. Allocations may be added to
 * and deleted from a LogicalMemory, as well as queried. Together the
 * allocations represent the memory space that is legally usable by the
 * application.
 * <P>
 * An <i>access</i> is a read or write of LogicalMemory. Accesses are denoted by
 * {@link LValue LValues}, each of which is either a read or a write. The
 * LogicalMemory can record all of the {@link Location Locations} that are
 * referenced by a given {@link LValue}, and determine whether a given
 * {@link Location} refers to all, part, or none of an existing allocation.
 * <P>
 * Each accessing {@link LValue} is also associated with a single
 * {@link LogicalMemoryPort}, through which all the {@link LValue LValue's}
 * accesses to memory are mediated. Accesses are added using
 * {@link LogicalMemoryPort#addAccess(LValue,Location)}. New
 * {@link LogicalMemoryPort LogicalMemoryPorts} may be created as needed to
 * represent the ports of multi-port memories. If a {@link LogicalMemoryPort} is
 * removed from its memory, all of its accesses will be discarded along with it.
 * <P>
 * To support memory optimizations, a LogicalMemory supports the addition and
 * removal of both {@link Allocation Allocations} and {@link LValue} accesses,
 * so that either may be moved among an arbitrary set of LogicalMemory
 * instances.
 * <P>
 * At any time, the method {@link #getSize()} may be called to determine the
 * number of addressable locations needed to create a physical instantiation of
 * a memory that can support the current allocation set.
 * 
 * @version $Id: LogicalMemory.java 538 2007-11-21 06:22:39Z imiller $
 */
public class LogicalMemory extends net.sf.openforge.util.naming.ID implements
		MemoryVisitable, StateHolder, Referenceable {

	/**
	 * The width of the 'size' port on any memory in a design. 2 bits is
	 * sufficient to cover all accesses up to a 64 bit wide memory accessed as
	 * bytes. HOWEVER, to ensure that the size is always seen as a positive
	 * value it must be represented as 3 bits.
	 */
	public static final int SIZE_WIDTH = 3;

	/** List of Allocation instances */
	private Collection<Allocation> allocations;

	/** Set of ports for this memory */
	private Set<LogicalMemoryPort> logicalMemoryPorts;

	/**
	 * Map of Location to Integer identifying the numeric base address of the
	 * given Location. Given in terms of bytes offset from location 0.
	 */
	private Map<Location, Integer> baseAddressMap = Collections.emptyMap();

	/**
	 * The structural implementation of this memory. Set by the MemoryBuilder
	 */
	private StructuralMemory structMem = null;

	/**
	 * The specific implementation that identifies the types of resources that
	 * will be used to build this memory.
	 */
	private MemoryImplementation implementation = null;

	/**
	 * Max number of bits in an address of this memory (language specific, since
	 * really it is the size of a pointer).
	 */
	private final int maxAddressWidth;

	private AccessMap accessors = new AccessMap();

	/**
	 * Constructs a new LogicalMemory that initially contain no
	 * {@link Allocation Allocations}. This object is constructed using a
	 * maximum address width; the actuall width of all address buses will
	 * eventually be downsized from this width by constant propagation.
	 * 
	 * @param maxAddressWidth
	 *            the maximum number of bits needed to address this memory;
	 *            should be the size of a pointer in the source language; must
	 *            be at least 1
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>maxAddressWidth</code> is less than 1
	 */
	public LogicalMemory(int maxAddressWidth) {
		setIDLogical("Logical Memory (bits: " + maxAddressWidth + ") ["
				+ Integer.toHexString(hashCode()) + "]");
		allocations = new LinkedHashSet<Allocation>();
		logicalMemoryPorts = new HashSet<LogicalMemoryPort>();

		if (maxAddressWidth < 1) {
			throw new IllegalArgumentException("invalid address width: "
					+ maxAddressWidth);
		}
		this.maxAddressWidth = maxAddressWidth;
	}

	/**
	 * Implementation of the MemoryVisitable interface.
	 * 
	 * @param memVis
	 *            a non null 'MemoryVisitor'
	 * @throws NullPointerException
	 *             if memVis is null
	 */
	public void accept(MemoryVisitor memVis) {
		memVis.visit(this);
	}

	/**
	 * Gets the maximum possible number of bits in an address for this memory.
	 * 
	 * @return the nonzero number of bits
	 */
	public int getMaxAddressWidth() {
		return maxAddressWidth;
	}

	/**
	 * Gets the {@link LogicalMemoryPort LogicalMemoryPorts} that have been
	 * created on this LogicalMemory.
	 * 
	 * @return a collection of {@link LogicalMemoryPort objects}
	 */
	public Collection<LogicalMemoryPort> getLogicalMemoryPorts() {
		return Collections.unmodifiableSet(logicalMemoryPorts);
	}

	/**
	 * Creates a new {@link LogicalMemoryPort} on this LogicalMemory.
	 * 
	 * @return the newly created {@link LogicalMemoryPort}
	 */
	public LogicalMemoryPort createLogicalMemoryPort() {
		final LogicalMemoryPort port = new LogicalMemoryPort(this);
		logicalMemoryPorts.add(port);
		return port;
	}

	/**
	 * Removes a {@link LogicalMemoryPort} from this LogicalMemory. Any
	 * accessors that reference the port will also be removed.
	 * 
	 * @param logicalMemoryPort
	 *            the memory port to remove
	 * 
	 * @throws NullPointerException
	 *             if <code>logicalMemoryPort</code> is null
	 * @throws IllegalArgumentException
	 *             if <code>logicalMemoryPort</code> does not belong to this
	 *             LogicalMemory
	 */
	public void removeLogicalMemoryPort(LogicalMemoryPort logicalMemoryPort) {
		if (logicalMemoryPort == null) {
			throw new NullPointerException("null logicalMemoryPort arg");
		}
		if (!logicalMemoryPorts.contains(logicalMemoryPort)) {
			throw new IllegalArgumentException("unknown logicalMemoryPort");
		}

		for (LValue lvalue : getLValues()) {
			@SuppressWarnings("unused")
			final Accessor_ accessor = this.accessors.getForLValue(lvalue);
			if (getLogicalMemoryPort(lvalue) == logicalMemoryPort) {
				removeAccessor(lvalue);
			}
		}

		logicalMemoryPorts.remove(logicalMemoryPort);
	}

	/**
	 * Gets the {@link LogicalMemoryPort}, if any, that is accessed by a given
	 * {@link LValue}.
	 * 
	 * @param lvalue
	 *            an {@link LValue} that references a {@link LogicalMemoryPort}
	 *            of this memory
	 * @return the memory port for the lvalue, or null if the lvalue does not
	 *         access this memory
	 * @throws NullPointerException
	 *             if <code>lvalue</code> is null
	 */
	public LogicalMemoryPort getLogicalMemoryPort(LValue lvalue) {
		if (lvalue == null) {
			throw new NullPointerException("null lvalue");
		}

		final Accessor_ accessor = (Accessor_) this.accessors
				.getForLValue(lvalue);
		return accessor == null ? null : accessor.getLogicalMemoryPort();
	}

	/**
	 * Defines the {@link StructuralMemory} constructed to implement this
	 * LogicalMemory.
	 */
	public void setStructuralImplementation(StructuralMemory implementation) {
		this.structMem = implementation;
	}

	/**
	 * Retrieves the {@link StructuralMemory} constructed to implement this
	 * LogicalMemory as defined by the {@link MemoryBuilder}.
	 */
	public StructuralMemory getStructuralMemory() {
		return this.structMem;
	}

	/**
	 * Defines the type of resources that will be used in the construction of
	 * this memory, which in turn identifies the access characteristics to this
	 * memory.
	 */
	public void setImplementation(MemoryImplementation impl) {
		this.implementation = impl;
	}

	/**
	 * Returns the selected implementation for this memory, may be null if not
	 * yet identified by the {@link MemoryBuilder}
	 */
	public MemoryImplementation getImplementation() {
		return this.implementation;
	}

	/**
	 * Allocates a new region of memory that is accessible to the user
	 * application. The {@link Allocation} that is returned may be used to
	 * uniquely identify the new allocation within this memory.
	 * 
	 * @param initialValue
	 *            the initial value of the region of memory; the region that is
	 *            allocated will be the size in bytes of the this value, i.e.
	 *            {@link LogicalValue#getAddressableSize()
	 *            initialValue.getAddressableSize()}
	 * @return a new allocation instance
	 * 
	 * @throws NullPointerException
	 *             if <code>initialValue</code> is null,
	 * @throws IllegalArgumentException
	 *             if {@link LogicalValue#getSize() initialValue.getSize()}
	 *             returns a negative number
	 */
	public Allocation allocate(LogicalValue initialValue) {
		final Allocation allocation = new Allocation(this, initialValue);
		allocations.add(allocation);
		return allocation;
	}

	/**
	 * Removes an {@link Allocation} that was previously created in this memory
	 * with {@link #allocate(LogicalValue)}.
	 * 
	 * @param allocation
	 *            an {@link Allocation allocation} that was previously created
	 *            in this memory
	 * 
	 * @throws NullPointerException
	 *             if <code>allocation</code> is null
	 * @throws IllegalArgumentException
	 *             if <code>allocation</code> is unknown by this memory
	 */
	public void delete(Allocation allocation) {
		if (allocation == null) {
			throw new NullPointerException("allocation is null");
		}
		if (!allocations.remove(allocation)) {
			throw new IllegalArgumentException("unknown allocation");
		}
	}

	/**
	 * Gets the {@link Allocation allocations} that are currently defined in
	 * this memory.
	 * 
	 * @return the collection of {@link Allocation allocations} that have been
	 *         created (but not deleted) in this memory
	 */
	public Collection<Allocation> getAllocations() {
		return Collections.unmodifiableCollection(allocations);
	}

	/**
	 * Gets the {@link LValue LValues} for which accesses to one or more
	 * {@link Location Locations} in this memory are currently recorded.
	 * 
	 * @return a collection of {@link LValue lvalues}, one for each that
	 *         currently accesses a {@link Location location} in this memory
	 */
	public Collection<LValue> getLValues() {
		return this.accessors.getLValues();
	}

	/**
	 * Gets the {@link Location locations} in this memory that are currently
	 * recorded as being accessed by a given {@link LValue}.
	 * 
	 * @param lvalue
	 *            an {@link LValue lvalue} expression
	 * @return the collection of {@link Location Locations} that are currently
	 *         recorded as being accessed by <code>lvalue</code>; this
	 *         collection may be empty if <code>lvalue</code> does not access
	 *         any {@link Location locations} in this memory
	 * @throws NullPointerException
	 *             if <code>lvalue</code> is null
	 */
	public Collection<Location> getAccesses(LValue lvalue) {
		if (lvalue == null) {
			throw new NullPointerException("null lvalue");
		}

		final Accessor_ accessor = (Accessor_) this.accessors
				.getForLValue(lvalue);
		return (accessor == null) ? Collections.emptyList() : accessor
				.getTargets();
	}

	/**
	 * Remove the given LocationConstant as a reference to this memory.
	 */
	public void removeAccessor(LocationConstant constant) {
		this.accessors.remove(constant);
	}

	/**
	 * Remove the given Pointer as a reference to this memory.
	 */
	public void removeAccessor(Pointer pointer) {
		this.accessors.remove(pointer);
	}

	/**
	 * Removes the given LValue as an identified accessor of this memory.
	 */
	public void removeAccessor(LValue lvalue) {
		this.accessors.remove(lvalue);
	}

	/**
	 * Gets the {@link LocationConstant LocationConstants} which have been added
	 * to this memory.
	 * 
	 * @return a set of {@link LocationConstant}
	 */
	public Collection<LocationConstant> getLocationConstants() {
		return this.accessors.getLocationConstants();
	}

	/**
	 * Adds a {@link LocationConstant} to this memory.
	 * 
	 * @param locationConstant
	 *            the constant to be added; it represents a pointer to a
	 *            {@link Location} in this memory
	 * 
	 * @return true if <code>locationConstant</code> was added, false if was
	 *         already added to this memory previously
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>locationConstant</code> is null or if its target
	 *             {@link Location} does not refer to this memory
	 */
	public boolean addLocationConstant(LocationConstant locationConstant) {
		// System.out.println("Adding " + locationConstant + " to " + this);
		if (locationConstant == null) {
			throw new IllegalArgumentException("null locationConstant");
		}

		if (locationConstant.getTarget().getLogicalMemory() != this) {
			throw new IllegalArgumentException("LogicalMemory mismatch");
		}

		boolean added = !getLocationConstants().contains(locationConstant);
		Accessor_ accessor = this.accessors.getForReference(locationConstant);
		accessor.addTarget(locationConstant.getTarget());
		return added;
		// return locationConstants.add(locationConstant);
	}

	/**
	 * Removes a {@link LocationConstant} from this memory.
	 * 
	 * @param locationConstant
	 *            a previously added constant to be removed
	 * 
	 * @return true if <code>locationConstant</code> was removed, false if it
	 *         was not found
	 */
	public boolean removeLocationConstant(LocationConstant locationConstant) {
		// System.out.println("Removing " + locationConstant + " from " +
		// locationConstant.showOwners() + " " + this);
		boolean removed = getLocationConstants().contains(locationConstant);
		this.accessors.remove(locationConstant);
		return removed;

		// return locationConstants.remove(locationConstant);
	}

	/**
	 * Returns an unmodifiable collection of all the Pointer objects whose
	 * target Location is contained in this memory.
	 * 
	 * @return a Collection of Pointer objects
	 */
	public Collection getAccessingPointers() {
		return Collections.unmodifiableCollection(this.accessors.getPointers());
	}

	/**
	 * Gets the size of this memory.
	 * 
	 * @return the number of bytes needed to represent the current set of
	 *         {@link Allocation allocations} without taking into account extra
	 *         padding that may be introduced when converting to a physical
	 *         memory layout.
	 */
	public int getAddressableSize() {
		int totalAddresses = 0;

		for (Iterator allocIter = this.allocations.iterator(); allocIter
				.hasNext();) {
			final Allocation alloc = (Allocation) allocIter.next();
			totalAddresses += alloc.getAddressableSize();
		}
		return totalAddresses;
	}

	/**
	 * Returns the size of this memory in bytes, however this is an imprecise
	 * calculation as it multiplies the addresses by stride div 8
	 */
	public int getSizeInBytes() {
		int addresses = getAddressableSize();
		// Catch the case of an empty memory, in which case the
		// getAddressStridePolicy method fails.
		if (addresses == 0)
			return 0;

		AddressStridePolicy policy = getAddressStridePolicy();
		return (int) Math.ceil(addresses * policy.getStride() / 8.0);
	}

	/**
	 * Returns the {@link AddressStridePolicy} for this memory after checking
	 * all allocations for consistent policies.
	 * 
	 * @throws IllegalAddressingStateException
	 *             if the address stride is inconsistent for this memory.
	 */
	public AddressStridePolicy getAddressStridePolicy() {
		if (this.allocations.isEmpty())
			throw new IllegalAddressingStateException(
					"Cannot determine address stride policy for empty memory");

		final AddressStridePolicy policy = ((Allocation) this.allocations
				.iterator().next()).getInitialValue().getAddressStridePolicy();
		for (Iterator iter = this.allocations.iterator(); iter.hasNext();) {
			final Allocation alloc = (Allocation) iter.next();
			if (!alloc.getInitialValue().getAddressStridePolicy()
					.equals(policy)) {
				throw new IllegalAddressingStateException(
						"Inconsistent addressing schemes in memory "
								+ policy.toString()
								+ " "
								+ alloc.getInitialValue()
										.getAddressStridePolicy().toString());
			}
		}

		return policy;
	}

	/**
	 * Gets the physical byte address of a given {@link Location} in this
	 * memory.
	 * 
	 * @param location
	 *            a {@link Location location} which references all or part of an
	 *            {@link Allocation allocation} in this memory
	 * @return the byte address denoted by <code>location</code> in this
	 *         memory's physical implementation
	 * 
	 * @throws NullPointerException
	 *             if <code>location</code> is null
	 * @throws IllegalArgumentException
	 *             if <code>location</code> does not denote an address within
	 *             one of the {@link Allocation allocations} in this memory
	 */
	public int getAddress(Location location) {
		if (location == null) {
			throw new NullPointerException("null location");
		}

		int base = 0;
		if (location.getBaseLocation() != location) {
			base = getAddress(location.getBaseLocation());
		} else if (this.baseAddressMap.containsKey(location)) {
			base = ((Integer) this.baseAddressMap.get(location)).intValue();
		} else {
			System.err.println("LogicalMemory.baseAddressMap="
					+ this.baseAddressMap + " is unchanged="
					+ (this.baseAddressMap == Collections.EMPTY_MAP));
			throw new IllegalArgumentException("no base address: " + location);
		}
		return base + location.getMinDelta();
	}

	/**
	 * Assigns a Map of Location to Integer identifying the numeric value of the
	 * base address of that Location in this memory, as implemented.
	 * 
	 * @param baseMap
	 *            a 'Map' of {@link Location} to Integer.
	 */
	public void setBaseAddressMap(Map<Location, Integer> baseMap) {
		this.baseAddressMap = baseMap;
	}

	/**
	 * Gets the {@link LValue LValues} that read from a particular
	 * {@link LogicalMemoryPort}.
	 * 
	 * @param logicalMemoryPort
	 *            a port of this memory
	 * @return the (possible empty) list of {@link LValue LValues} that read
	 *         from the specified port
	 * @throws NullPointerException
	 *             if <code>logicalMemoryPort</code> is null
	 * @throws IllegalArgumentException
	 *             if <code>logicalMemoryPort</code> is not a port of this
	 *             memory
	 */
	List<LValue> getReadLValues(LogicalMemoryPort logicalMemoryPort) {
		return getLValues(logicalMemoryPort, false);
	}

	/**
	 * Gets the {@link LValue LValues} that write to a particular
	 * {@link LogicalMemoryPort}.
	 * 
	 * @param logicalMemoryPort
	 *            a port of this memory
	 * @return the (possible empty) list of {@link LValue LValues} that write to
	 *         the specified port
	 * @throws NullPointerException
	 *             if <code>logicalMemoryPort</code> is null
	 * @throws IllegalArgumentException
	 *             if <code>logicalMemoryPort</code> is not a port of this
	 *             memory
	 */
	List<LValue> getWriteLValues(LogicalMemoryPort logicalMemoryPort) {
		return getLValues(logicalMemoryPort, true);
	}

	/**
	 * Gets the {@link LValue LValues} that access a particular
	 * {@link LogicalMemoryPort}.
	 * 
	 * @param logicalMemoryPort
	 *            a port of this memory
	 * @param isWrite
	 *            true if the list of writing accesses are to be returned, false
	 *            if the reading accesses are to be returned
	 * @return the (possible empty) list of {@link LValue LValues} that access
	 *         the specified port
	 * @throws NullPointerException
	 *             if <code>logicalMemoryPort</code> is null
	 * @throws IllegalArgumentException
	 *             if <code>logicalMemoryPort</code> is not a port of this
	 *             memory
	 */
	private List<LValue> getLValues(LogicalMemoryPort logicalMemoryPort, boolean isWrite) {
		if (logicalMemoryPort == null) {
			throw new NullPointerException("null logicalMemoryPort");
		}
		if (logicalMemoryPort.getLogicalMemory() != this) {
			throw new IllegalArgumentException("unknown LogicalMemoryPort");
		}

		final List<LValue> lvalues = new LinkedList<LValue>();
		for (Iterator iter = this.accessors.getLValues().iterator(); iter
				.hasNext();) {
			final LValue lvalue = (LValue) iter.next();
			final Accessor_ accessor = (Accessor_) this.accessors
					.getForLValue(lvalue);
			if (accessor.getLogicalMemoryPort() == logicalMemoryPort) {
				if (lvalue.isWrite() == isWrite) {
					lvalues.add(lvalue);
				}
			}
		}

		return lvalues;
	}

	/**
	 * Records an access in this memory. An access consists of an {@link LValue}
	 * and the {@link Location} it references. A given {@link Location} will
	 * only be recorded once for an {@link LValue}.
	 * 
	 * @param lvalue
	 *            the {@link LValue} that denotes the access
	 * @param location
	 *            the {@link Location} accessed by <code>lvalue</code>
	 * 
	 * @throws NullPointerException
	 *             if <code>lvalue</code> or <code>location</code> is null
	 * @throws IllegalArgumentException
	 *             if <code>location</code> does not refer to this memory or if
	 *             <code>logicalMemoryPort</code> does not belong to this memory
	 */
	void addAccess(LValue lvalue, Location location,
			LogicalMemoryPort logicalMemoryPort) {
		addAccess(lvalue, logicalMemoryPort);

		if (location == null) {
			throw new NullPointerException("null location");
		}

		if (location.getLogicalMemory() != this) {
			throw new IllegalArgumentException("unknown location");
		}

		((Accessor_) this.accessors.getForLValue(lvalue)).addTarget(location);
	}

	void addAccess(LValue lvalue, LogicalMemoryPort logicalMemoryPort) {
		// System.out.println("Adding " + lvalue + " lmp " + logicalMemoryPort);
		if (lvalue == null) {
			throw new NullPointerException("null lvalue");
		}

		if (logicalMemoryPort == null) {
			throw new NullPointerException("null memory port");
		}

		Accessor_ accessor = (Accessor_) this.accessors.getForLValue(lvalue);
		accessor.setLogicalMemoryPort(logicalMemoryPort);
		assert accessor.getLogicalMemoryPort() != null;
	}

	public void clearAccessors() {
		// System.out.println("Clearing "+ this);
		this.accessors.clear();
	}

	public void addAccessor(Pointer ptr) {
		this.accessors.getForReference(ptr).addTarget(ptr.getTarget());
	}

	public void addAccessor(LocationConstant constant) {
		// System.out.println("Use add location constant");
		addLocationConstant(constant);
		// this.accessors.getForReference(constant).addTarget(constant.getTarget());
	}

	/**
	 * Tests the referencer types and returns the necessary spacing
	 * 
	 * @param from
	 *            the prior accessor in source document order.
	 * @param to
	 *            the latter accessor in source document order.
	 */
	public int getSpacing(Referencer from, Referencer to) {
		if (!(from instanceof MemoryAccess))
			throw new IllegalArgumentException(
					"Wrong context for determining spacing.");
		if (!(to instanceof MemoryAccess))
			throw new IllegalArgumentException(
					"Wrong context for determining spacing.");

		//boolean isReadFirst = getImplementation().isDPReadFirst();
		boolean fromIsWrite = from instanceof MemoryWrite;
		boolean toIsWrite = to instanceof MemoryWrite;
		boolean isCombinational = ((MemoryAccess) from).getLatency()
				.getMinClocks() == 0;

		// System.out.println("From is " + fromIsWrite);
		// System.out.println("to is " + toIsWrite);
		// System.out.println("read first " + isReadFirst);

		// Rules
		// If single port
		// ** 1 if combinational, 0 otherwise
		// If dual port
		// Accesses to same port, same as single port case
		// Accesses to different ports
		// WW 1 if combinational, 0 otherwise
		// WR 1 if write is combinational 0 otherwise
		// RR 0
		// RW 0
		LogicalMemoryPort portFrom = ((MemoryAccess) from).getMemoryPort();
		LogicalMemoryPort portTo = ((MemoryAccess) to).getMemoryPort();
		assert portFrom.getLogicalMemory() == this;
		assert portTo.getLogicalMemory() == this;
		if (portFrom == portTo) // single port or same port of dual port
		{
			if (isCombinational)
				return 1;
			return 0;
		}

		// dual port case
		if (fromIsWrite) {
			if (isCombinational) // likely never true
				return 1;
			else
				return 0; // done to go spacing. Cycle offset handled by latency
							// of from
		}

		if (!fromIsWrite && !toIsWrite)
			return 0;

		return 0; // done to go spacing.

	}

	/**
	 * Returns -1 or 0 depending on the type of accesses and memory
	 * characteristics (eg dual port, read-first, and memory access types).
	 */
	public int getGoSpacing(Referencer from, Referencer to) {
		boolean isReadFirst = getImplementation().isDPReadFirst();
		boolean fromIsWrite = from instanceof MemoryWrite;
		boolean toIsWrite = to instanceof MemoryWrite;
		//boolean isCombinational = ((MemoryAccess) from).getLatency()
		//		.getMinClocks() == 0;

		// Rules (assumes GO to GO spacing)
		// If single port
		// -1 resort to typical done to go
		// If dual port
		// Accesses to same port, same as single port case
		// Accesses to different ports
		// WW -1 potential for write collision, sequence from done to go
		// RR 0
		// RW 0 if read first, -1 otherwise
		// WR 0 if write first, -1 otherwise
		LogicalMemoryPort portFrom = ((MemoryAccess) from).getMemoryPort();
		LogicalMemoryPort portTo = ((MemoryAccess) to).getMemoryPort();
		assert portFrom.getLogicalMemory() == this;
		assert portTo.getLogicalMemory() == this;
		if (portFrom == portTo) // single port or same port of dual port
		{
			return -1;
		}

		// dual port case
		if (fromIsWrite && toIsWrite)
			return -1;
		if (!fromIsWrite && !toIsWrite)
			return 0;
		if (isReadFirst) {
			if (!fromIsWrite && toIsWrite)
				return 0;
			return -1;
		} else {
			if (fromIsWrite && !toIsWrite)
				return 0;
			return -1;
		}
	}

	private class Accessor_ {
		private Object accessor;
		private LogicalMemoryPort memPort;
		protected Collection<Location> locations = new HashSet<Location>();

		Accessor_(Object accessor) {
			this.accessor = accessor;
		}

		void setLogicalMemoryPort(LogicalMemoryPort lmp) {
			if (this.memPort == null)
				this.memPort = lmp;
			if (this.memPort != lmp) {
				throw new IllegalArgumentException(
						"conflicting LogicalMemoryPort");
			}
		}

		private Object getAccessor() {
			return this.accessor;
		}

		void addTarget(Location target) {
			if (target == null) {
				throw new NullPointerException("null location");
			}

			this.locations.add(target);
		}

		@SuppressWarnings("unused")
		void removeTarget(Location location) {
			if (location == null) {
				throw new NullPointerException("null location");
			} else if (!this.locations.remove(location)) {
				throw new IllegalArgumentException("unknown location");
			}
		}

		Collection getTargets() {
			return Collections.unmodifiableCollection(this.locations);
		}

		LogicalMemoryPort getLogicalMemoryPort() {
			return this.memPort;
		}

		public String toString() {
			return ("Accessor@" + Integer.toHexString(hashCode()) + " for: "
					+ getAccessor() + " " + getTargets() + " lmp: " + getLogicalMemoryPort());
		}

		public void debug() {
			System.out.println("Accessor@"
					+ Integer.toHexString(this.hashCode()));
			System.out.println("\t" + getAccessor() + " "
					+ getLogicalMemoryPort());
			for (Iterator iter = getTargets().iterator(); iter.hasNext();) {
				System.out.println("\t\t" + iter.next());
			}
		}
	}

	private class SingleAccessor extends Accessor_ {
		SingleAccessor(Object accessor) {
			super(accessor);
		}

		void addTarget(Location target) {
			if (this.locations.size() > 0 && !this.locations.contains(target)) {
				throw new IllegalArgumentException(
						"Changing target of single target accessor");
			}

			super.addTarget(target);
		}
	}

	private class AccessMap {
		private HashMap lvalueAccesses = new HashMap();
		private HashMap pointerAccesses = new HashMap();
		private HashMap constantAccesses = new HashMap();

		public void clear() {
			this.lvalueAccesses.clear();
			this.pointerAccesses.clear();
			this.constantAccesses.clear();
		}

		public Accessor_ getForLValue(LValue lvalue) {
			Accessor_ acc = (Accessor_) this.lvalueAccesses.get(lvalue);
			if (acc == null) {
				acc = new Accessor_(lvalue);
				this.lvalueAccesses.put(lvalue, acc);
			}
			return acc;
		}

		public Accessor_ getForReference(Pointer ptr) {
			Accessor_ acc = (Accessor_) this.pointerAccesses.get(ptr);
			if (acc == null) {
				acc = new SingleAccessor(ptr);
				this.pointerAccesses.put(ptr, acc);
			}
			return acc;
		}

		public Accessor_ getForReference(LocationConstant ptr) {
			Accessor_ acc = (Accessor_) this.constantAccesses.get(ptr);
			if (acc == null) {
				acc = new SingleAccessor(ptr);
				this.constantAccesses.put(ptr, acc);
			}
			return acc;
		}

		public void remove(LValue lvalue) {
			if (lvalue == null) {
				throw new NullPointerException("null lvalue arg");
			}

			final Accessor_ accessor = (Accessor_) this.lvalueAccesses
					.get(lvalue);
			if (accessor == null) {
				throw new IllegalArgumentException("unknown lvalue " + lvalue);
			}
			this.lvalueAccesses.remove(lvalue);
		}

		public void remove(Pointer pointer) {
			if (pointer == null) {
				throw new NullPointerException("null pointer arg");
			}

			final Accessor_ accessor = (Accessor_) this.pointerAccesses
					.get(pointer);
			if (accessor == null) {
				throw new IllegalArgumentException("unknown pointer " + pointer);
			}
			this.pointerAccesses.remove(pointer);
		}

		public void remove(LocationConstant constant) {
			if (constant == null) {
				throw new NullPointerException("null constant arg");
			}

			final Accessor_ accessor = (Accessor_) this.constantAccesses
					.get(constant);
			if (accessor == null) {
				throw new IllegalArgumentException("unknown constant "
						+ constant);
			}
			this.constantAccesses.remove(constant);
		}

		public Collection<LValue> getLValues() {
			return Collections.unmodifiableCollection(this.lvalueAccesses
					.keySet());
		}

		public Collection<LocationConstant> getLocationConstants() {
			return Collections.unmodifiableCollection(this.constantAccesses
					.keySet());
		}

		public Collection getPointers() {
			return Collections.unmodifiableCollection(this.pointerAccesses
					.keySet());
		}

		public void debug() {
			for (Iterator iter = lvalueAccesses.values().iterator(); iter
					.hasNext();) {
				((Accessor_) iter.next()).debug();
			}
			for (Iterator iter = pointerAccesses.values().iterator(); iter
					.hasNext();) {
				((Accessor_) iter.next()).debug();
			}
			for (Iterator iter = constantAccesses.values().iterator(); iter
					.hasNext();) {
				((Accessor_) iter.next()).debug();
			}
		}

	}

	public void showContents() {
		System.out.println(this.toString());
		for (Iterator iter = getAllocations().iterator(); iter.hasNext();) {
			Location loc = (Location) iter.next();
			System.out.println("\t" + loc.debug());
		}
	}

	public void showAccessors() {
		System.out.println(this.toString());
		this.accessors.debug();
	}

	public String toString() {
		return "Logical Memory [" + Integer.toHexString(hashCode()) + "]";
	}

	class IllegalAddressingStateException extends RuntimeException {
		public IllegalAddressingStateException(String msg) {
			super(msg);
		}
	}

}