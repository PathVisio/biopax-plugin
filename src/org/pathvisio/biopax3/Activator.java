package org.pathvisio.biopax3;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.pathvisio.desktop.plugin.Plugin;

public class Activator implements BundleActivator
{
	private BiopaxPlugin plugin = null;

	@Override
	public void start(BundleContext context) throws Exception
	{	
		plugin = new BiopaxPlugin();
		context.registerService(Plugin.class.getName(), plugin, null);
	}

	@Override
	public void stop(BundleContext context) throws Exception
	{
		if (plugin != null)
			plugin.done();
	}

}
