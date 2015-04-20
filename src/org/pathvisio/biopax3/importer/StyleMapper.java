// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2009 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License"); 
// you may not use this file except in compliance with the License. 
// You may obtain a copy of the License at 
// 
// http://www.apache.org/licenses/LICENSE-2.0 
//  
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, 
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
// See the License for the specific language governing permissions and 
// limitations under the License.
//

package org.pathvisio.biopax3.importer;

import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.Conversion;
import org.pathvisio.core.model.PathwayElement;

/**
 * Implement this interface to provide custom graphical style mappings
 * for BioPAX to GPML elements.
 * @author adem
 */
public interface StyleMapper {
	/**
	 * Set the visual style for the given control element.
	 * @param c The BioPAX control element
	 * @param line The GPML line that maps to the BioPAX element
	 */
	public void mapControl(Control c, PathwayElement line);
	
	/**
	 * Set the visual style for the given conversion element.
	 * @param c The BioPAX conversion element
	 * @param line The GPML line that maps to the BioPAX element
	 */
	public void mapConversion(Conversion c, PathwayElement line);
	
	/**
	 * Set the visual style for the given conversion element. This method will
	 * be called for all conversions that have multiple input (left) participants.
	 * @param c The BioPAX conversion element
	 * @param line The GPML line that will connect the input (left) participant
	 * with the main conversion line (see {@link #mapConversion(conversion, PathwayElement)}.
	 */
	public void mapConversionLeft(Conversion c, PathwayElement line);
	
	/**
	 * Set the visual style for the given conversion element. This method will
	 * be called for all conversions that have multiple output (right) participants.
	 * @param c The BioPAX conversion element
	 * @param line The GPML line that will connect the output (right) participant
	 * with the main conversion line (see {@link #mapConversion(conversion, PathwayElement)}.
	 */
	public void mapConversionRight(Conversion c, PathwayElement line);
	
	/**
	 * Set the visual style for the given Gene or physicalEntity element.
	 * @param c The BioPAX conversion element
	 * @param line The GPML datanode that maps to the BioPAX element
	 */
	public void mapEntity(Entity e, PathwayElement datanode);
	
	/**
	 * Create a GPML element that represents an unknown or unspecified participant.
	 * This is used for conversions or controls where a participant is not
	 * specified (e.g. a catalysis with unknown enzyme).
	 * @param c The BioPAX conversion element
	 * @param line The GPML line that maps to the BioPAX element
	 */
	public PathwayElement createUnknownParticipant();
}
