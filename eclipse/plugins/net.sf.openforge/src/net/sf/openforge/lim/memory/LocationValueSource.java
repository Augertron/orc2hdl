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

/**
 * LocationValueSource is an interface that is conformed to by all objects that
 * provide a base address in the LIM. This may be a LocationConstant in the LIM
 * or it may be a Pointer LogicalValue in memory, but the common theme is that
 * they are initialized by a Location.
 * 
 * <p>
 * Created: Tue Nov 4 18:04:25 2003
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: LocationValueSource.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface LocationValueSource {

	public Location getTarget();

	public void setTarget(Location target);

}// LocationValueSource
