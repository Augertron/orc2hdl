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

package net.sf.openforge.optimize;

import java.io.PrintStream;

import net.sf.openforge.util.Debug;

/**
 * _optimize debug class
 * 
 * Use like this:
 * 
 * if (_optimize.db) { _optimize.ln("ugh"); }
 * 
 * if (_yourpkg.db) { _yourpkg.ln("glub"); }
 * 
 ************************************* 
 * 
 * You can additionally control your debug by using the system property: -
 * debug.internal.<tag> = true|false or the envriement variable: -
 * DEBUG_INTERNAL_<tag> = true|false
 * 
 * By default, your outputs are using the DEFAULT level. I.e., if you use
 * _optimize.ln("Blug"); it is the default level.
 * 
 * For now, that is enough ;-)
 * 
 */
public class _optimize {
	// Add your tag here!
	public final static String TAG = "Optimize";

	public final static int FULL_CONST = 0x1;
	public final static int DEAD_CODE = 0x2;
	public final static int HALF_CONST = 0x4;
	public final static int SIZE_NORMALIZE = 0x8;
	public final static int LOOP_UNROLLING = 0x10;
	public final static int PARTIAL = 0x20;
	public final static int REPLACE = 0x40;
	public final static int OBJECT_REFS = 0x80;
	public final static int MEM_READONLY = 0x100;
	public final static int ROM_REP = 0x200;
	public final static int BLOCK_UNNEST = 0x400;
	public final static int DP_MEM = 0x800;

	// COMPILED_IN when you want debug info left in
	public final static boolean db = Debug.COMPILED_OUT;

	// set to true to see your debug, false to suppress
	public final static boolean VISIBLE = true;

	private final static Debug debug = new Debug(_optimize.class, TAG, db,
			VISIBLE, BLOCK_UNNEST | PARTIAL);
	public final static Debug d = debug; // backwards compatibility with
											// previous version

	/**
	 * Accessor for the output PrintStream
	 * 
	 * @return PrintStream
	 */
	public static PrintStream getPrintStream() {
		return debug.getPrintStream();
	}

	/**
	 * Set the current preface.
	 * 
	 * @param preface
	 *            String value for the preface.
	 */
	public static void setPreface(String preface) {
		debug.setPreface(preface);
	}

	/**
	 * use this to turn on (true) or off (false) debug
	 */
	public static void setVisible(boolean vis) {
		debug.setVisible(vis);
	}

	/**
	 * Set the debug bitmask levels
	 * 
	 * @param level
	 *            a value of type 'int'
	 */
	public static void setLevels(int level) {
		debug.setLevels(level);
	}

	/**
	 * Work-alike for PrintStream.println(); Uses the DEFAULT level.
	 * 
	 */
	public static void ln() {
		debug.ln();
	}

	/**
	 * Work-alike for PrintStream.print(); USes the DEFAULT level
	 * 
	 * @param v
	 *            Object to print out.
	 */
	public static void o(Object v) {
		debug.o(v);
	}

	/**
	 * Work-alike for PrintStream.println(Object); Uses the DEFAULT level.
	 * 
	 * @param v
	 *            Object to print out.
	 */
	public static void ln(Object v) {
		debug.ln(v);
	}

	/**
	 * Work-alike for PrintStream.println();
	 * 
	 * @param level
	 *            level of this debug statement
	 */
	public static void ln(int level) {
		debug.ln(level);
	}

	/**
	 * Work-alike for PrintStream.print(Object);
	 * 
	 * @param level
	 *            level of this debug statement
	 * @param v
	 *            Object to print out.
	 */
	public static void o(int level, Object v) {
		debug.o(level, v);
	}

	/**
	 * Work-alike for PrintStream.println(Object);
	 * 
	 * @param level
	 *            level of this debug statement
	 * @param v
	 *            Object to print out.
	 */
	public static void ln(int level, Object v) {
		debug.ln(level, v);
	}

	/**
	 * Used to display a stack trace from the current location. Uses level
	 * DEFAULT.
	 * 
	 * @param v
	 *            Displayable object to makr where you are
	 */
	public static void whereAmI(Object v) {
		debug.whereAmI(v);
	}

	/**
	 * Used to display a stack trace from the current location. Uses level
	 * DEFAULT.
	 * 
	 * @param v
	 *            Displayable object to makr where you are
	 * @param level
	 *            level of this debug statement
	 */
	public static void whereAmI(int level, Object v) {
		debug.whereAmI(level, v);
	}
}
