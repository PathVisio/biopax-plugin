package org.pathvisio.biopax3.layout;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;

import org.pathvisio.biopax3.layout.SpringLayout.Edge;
import org.pathvisio.biopax3.layout.SpringLayout.Node;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;

/**
 * This class define the plugin for the Spring Embedded Layout
 * @author adem
 */
public class SpringPlugin implements Plugin{

	static PvDesktop desktop;
	
	public SpringPlugin(){
		PreferenceManager.init();
		Logger.log.setLogLevel(true, true, true, true, true, true);
	}
	
	public void init(PvDesktop desktop) {
		this.desktop = desktop;
		desktop.registerMenuAction ("View", spring_action);
	}

	public void done() {}

	private final SpringAction spring_action = new SpringAction();
	
	/**
	 * The Action to lauch the Spring Embedded Layout
	 */
	private class SpringAction extends AbstractAction
	{
		SpringAction()
		{
			putValue (NAME,"Spring Embedded Layout ");
		}

		public void actionPerformed(ActionEvent arg0) 
		{
			SpringGraph spg = new SpringGraph(desktop.getSwingEngine().getEngine().getActivePathway(),
					desktop.getSwingEngine().getEngine().getActiveVPathway().getVHeight(),
					desktop.getSwingEngine().getEngine().getActiveVPathway().getVWidth());
			System.out.println("Spring Layout");
			SpringLayout2 spl = new SpringLayout2(spg);
			System.out.println("spl : "+spl);
			spl.run();
		}
	}
}
