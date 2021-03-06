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

package net.sf.openforge.backend.hdl.ucf;

public class UCFType {
	public final static UCFType NET = new UCFType("NET");
	public final static UCFType INST = new UCFType("INST");
	public final static UCFType PIN = new UCFType("PIN");
	public final static UCFType SET = new UCFType("SET");

	private final String type;

	private UCFType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return type;
	}
}
