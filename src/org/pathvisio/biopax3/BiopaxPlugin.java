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

package org.pathvisio.biopax3;

import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;

/**
 * A main class for the BioPAX converter.
 * Will call the converter for the BioPAX file
 * provided in the first command line argument and
 * convert the pathway entities to GPML pathways.
 * The resulting pathways will be saved as a GPML file in 
 * the working directory.
 * @author adem
 */
public class BiopaxPlugin implements Plugin 
{	
	public void init(PvDesktop desktop) 
	{
		BiopaxFormat format = new BiopaxFormat();
		
		desktop.getSwingEngine().getEngine().addPathwayExporter(format);
		desktop.getSwingEngine().getEngine().addPathwayImporter(format);
	}

	public void done() {}
}
