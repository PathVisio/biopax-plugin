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

import java.awt.Color;
import org.biopax.paxtools.model.level3.ControlType;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.Dna;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.Gene;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.Rna;
import org.biopax.paxtools.model.level3.SmallMolecule;
import org.pathvisio.core.model.ConnectorType;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.LineType;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.PathwayElement;

public class DefaultStyleMapper implements StyleMapper {
	
	public void mapControl(Control c, PathwayElement line) {
		line.setConnectorType(ConnectorType.CURVED);
		ControlType type = c.getControlType();
		if(type != null){
			switch(type) {
			case ACTIVATION:
			case ACTIVATION_ALLOSTERIC:
			case ACTIVATION_NONALLOSTERIC:
			case ACTIVATION_UNKMECH:
				line.setEndLineType(LineType.fromName("mim-catalysis"));
				break;
			case INHIBITION:
			case INHIBITION_ALLOSTERIC:
			case INHIBITION_COMPETITIVE:
			case INHIBITION_IRREVERSIBLE:
			case INHIBITION_NONCOMPETITIVE:
			case INHIBITION_OTHER:
			case INHIBITION_UNCOMPETITIVE:
			case INHIBITION_UNKMECH:
				line.setEndLineType(LineType.TBAR);
				break;
			}
		}
	}
	
	public void mapConversion(Conversion c, PathwayElement line) {
		line.setEndLineType(LineType.ARROW);
		line.setConnectorType(ConnectorType.CURVED);
	}
	
	public void mapConversionLeft(Conversion c, PathwayElement line) {
		line.setConnectorType(ConnectorType.CURVED);
	}
	
	public void mapConversionRight(Conversion c, PathwayElement line) {
		line.setEndLineType(LineType.ARROW);
		line.setConnectorType(ConnectorType.CURVED);
	}

	public void mapEntity(Entity e, PathwayElement datanode) {
		//Map type and apply color mapping
		if(e instanceof SmallMolecule) {
			datanode.setDataNodeType(DataNodeType.METABOLITE);
			datanode.setColor(Color.BLUE);
		} else if (e instanceof Protein) {
			datanode.setDataNodeType(DataNodeType.PROTEIN);
			datanode.setColor(Color.BLACK);
		} else if (e instanceof Dna) {
			datanode.setDataNodeType(DataNodeType.UNKOWN);
			datanode.setColor(Color.GREEN);
		} else if (e instanceof Rna) {
			datanode.setDataNodeType(DataNodeType.RNA);
			datanode.setColor(Color.ORANGE);
		} 
		else if (e instanceof Gene)
		{
			datanode.setDataNodeType(DataNodeType.GENEPRODUCT);
			datanode.setColor(Color.BLACK);
		}
	}
	
	public PathwayElement createUnknownParticipant() {
		PathwayElement pwe = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		pwe.setInitialSize();
		pwe.setColor(Color.lightGray);
		pwe.setTextLabel("?");
		return pwe;
	}
}
