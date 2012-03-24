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

package net.sf.openforge.forge.api.pin;

/**
 * ControlPin is the super class for all non-data pins in a design.
 * 
 */
public abstract class ControlPin extends Buffer {
	/**
	 * Constructs a new named ControlPin.
	 * 
	 * @param name
	 */
	public ControlPin(String name) {
		super(name, 1);
	}

	/**
	 * Control pins are always unsigned, so this always returns false.
	 * 
	 * @see net.sf.openforge.forge.api.pin.Buffer#isSigned()
	 */
	@Override
	public boolean isSigned() {
		return false;
	}

}
