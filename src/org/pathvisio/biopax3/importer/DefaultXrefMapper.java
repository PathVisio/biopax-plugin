// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2015 BiGCaT Bioinformatics
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

import java.util.HashMap;
import java.util.Map;

import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.pathvisio.core.model.PathwayElement;

/**
 * Default Xref mapper for BioPAX to PathVisio pathway
 * @author adem
 *
 */
public class DefaultXrefMapper implements XrefMapper 
{
	public void mapXref(Entity e, PathwayElement pwElm) 
	{
		for(org.biopax.paxtools.model.level3.Xref xref : e.getXref()) {
			Xref gpmlXref = getDataNodeXref(xref);
			if(gpmlXref != null) {
				pwElm.setElementID(gpmlXref.getId());
				pwElm.setDataSource(gpmlXref.getDataSource());
				break; //Stop after first valid xref
			}
		}
		
		// still no Xref? try the EntityRef of this Entity
		if (e instanceof SimplePhysicalEntity)
		{
			EntityReference ref = ((SimplePhysicalEntity)e).getEntityReference();
			if (ref != null) for (org.biopax.paxtools.model.level3.Xref xref : e.getXref())
			{
				Xref gpmlXref = getDataNodeXref(xref);
				if(gpmlXref != null) {
					pwElm.setElementID(gpmlXref.getId());
					pwElm.setDataSource(gpmlXref.getDataSource());
					break; //Stop after first valid xref
				}				
			}
		}
	}
	
	private static Map<String, DataSource> dsMap;
	static 
	{
		dsMap = new HashMap<String, DataSource>();
		dsMap.put ("uniprot", DataSource.getBySystemCode("S"));
		dsMap.put ("UniProt", DataSource.getBySystemCode("S"));
		dsMap.put ("kegg compound", DataSource.getBySystemCode("Ck"));
		dsMap.put ("KEGG compound", DataSource.getBySystemCode("Ck"));
	}
	
	
	Xref getDataNodeXref(org.biopax.paxtools.model.level3.Xref x) {
		String db = x.getDb();
		DataSource ds = dsMap.containsKey(db) ? dsMap.get(db) : DataSource.getByFullName(db);
		String id = x.getId();
		if (id == null || ds == null) return null;
		return new Xref(id, ds);
	}
}
