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

import java.io.File;
import java.io.IOException;

import org.jdom.JDOMException;
import org.pathvisio.biopax3.exporter.ExportHelper;
import org.pathvisio.biopax3.importer.ImportHelper;
import org.pathvisio.core.model.AbstractPathwayFormat;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;

public class BiopaxFormat extends AbstractPathwayFormat
{
	public void doExport(File file, Pathway pathway)
	throws ConverterException 
	{
		try {
			ExportHelper helper = new ExportHelper(pathway);
			helper.export(file, true);
		} catch (IOException e) {
			throw new ConverterException (e);
		}
	}

	private final String[] EXTENSIONS = new String[] { "owl" }; 

	public String[] getExtensions() 
	{
		return EXTENSIONS;
	}

	public String getName() 
	{
		return "BioPAX Level 3";
	}

	public Pathway doImport(File file)
	throws ConverterException 
	{
		Pathway result = null;
		try {
			ImportHelper bpf = new ImportHelper(file);


			/*
			List<org.biopax.paxtools.model.level3.Pathway> pathway_list = bpf.getPathways(); 
			//TODO: what if pathway list size is 0?
			if (pathway_list.size() == 1)
			{
				result = bpf.convert().get(0);
			}
			else
			{
				SelectPathwayRunnable runnable = new SelectPathwayRunnable(pathway_list);
				try {
					SwingUtilities.invokeAndWait(runnable);
				} catch (InterruptedException e) {
					throw new ConverterException(e); 
				} catch (InvocationTargetException e) {
					throw new ConverterException(e);
				}
				result = bpf.simple_convert(runnable.selection);
			}
			*/
			result = bpf.convertAll();
		} 
		catch (JDOMException e) 
		{
			throw new ConverterException (e);
		} 
		catch (IOException e) 
		{
			throw new ConverterException (e);
		}
		return result;
	}

}	
