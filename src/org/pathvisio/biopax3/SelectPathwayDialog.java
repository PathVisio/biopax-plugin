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

package org.pathvisio.biopax3;

import java.awt.Component;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.gui.dialogs.OkCancelDialog;

/**
 * this class is needed if there is more than 1 pathway in a BioPAX file
 * PathVisio is able to open just 1 pathway at the time so the user
 * has to choose the pathway to open
 * @author adem
 */
public class SelectPathwayDialog extends OkCancelDialog {
	private JComboBox cbPathwaylist;
	private List<org.biopax.paxtools.model.level3.Pathway> bpPathwayList;
	
	public SelectPathwayDialog(List<org.biopax.paxtools.model.level3.Pathway> pwList, PvDesktop desktop){
		super(desktop.getFrame(), "Open Pathway", desktop.getFrame(), true);
		this.bpPathwayList = pwList;
		setDialogComponent(createDialogPane(pwList));
		setSize(500, 120);
	}
	
	public static String namePicker (org.biopax.paxtools.model.level3.Pathway pat)
	{
		// first try display name
		String result = pat.getDisplayName();
		if (result == null)
		{
			// then try the first item in the set of all names
			if (pat.getName().size() > 0)
			{
				result = pat.getName().iterator().next();
			}
			// if all else fails, use Id
			else
			{
				result = pat.getRDFId();
			}
		}
		return result;
	}
	
	protected Component createDialogPane(List<org.biopax.paxtools.model.level3.Pathway> pwyList) {
		String [] pathways = new String [pwyList.size()]; 
		JPanel panel = new JPanel();	    
		int i=0;
	    for (org.biopax.paxtools.model.level3.Pathway pat: pwyList)
	    {
	    	pathways[i] = namePicker(pat);
	    	i++;
	    }
	    cbPathwaylist = new JComboBox(pathways);
	    panel.add(cbPathwaylist);
	    return panel;
	}	
	
	private org.biopax.paxtools.model.level3.Pathway selectedPathway = null;
	
	public org.biopax.paxtools.model.level3.Pathway getSelected() { return selectedPathway; }
	
	protected void okPressed()
	{
		selectedPathway = bpPathwayList.get(cbPathwaylist.getSelectedIndex());
		super.okPressed();
	}
}
