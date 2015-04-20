package org.pathvisio.biopax3.bots;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.rpc.ServiceException;

import org.pathvisio.biopax3.BiopaxFormat;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;
import org.wikipathways.client.WikiPathwaysClient;

/**
 * Test wikipathways. Unlike the other testing script, 
 * the conversion is not done locally,
 * but requested on the server.
 */
public class WikipathwaysExerciser
{
	final WikiPathwaysClient client;
	final BiopaxFormat bpFormat;
	
	public WikipathwaysExerciser() throws ServiceException, MalformedURLException
	{
		client = new WikiPathwaysClient(new URL("http://webservice.wikipathways.org"));
		bpFormat = new BiopaxFormat();
	}

	public void run() throws IOException
	{
		File logFile = new File ("bpValidatorLog.txt");
		PrintWriter writer = new PrintWriter (new FileWriter (logFile));
		
		WSPathwayInfo[] infos = client.listPathways();
		for (WSPathwayInfo info : infos)
		{
			try
			{
				System.out.println ("Checking " + info.getId() + " " + info.getName() + " " + info.getSpecies());
				writer.println ("Checking " + info.getId() + " " + info.getName() + " " + info.getSpecies());
				checkPathway (info.getId());
				
				System.out.println ("Success");
				writer.println ("Success");
				Thread.sleep(10000);
				
			}
			catch (Exception ex)
			{
				System.out.println ("Failure: " + ex.getMessage());
				writer.println ("Failure: " + ex.getMessage());
			}
			
		}
		writer.close();
	}
	
	public void checkPathway(String id) throws NumberFormatException, IOException, ConverterException
	{
		WSPathwayInfo info = client.getPathwayInfo(id);
		byte[] bytes = client.getPathwayAs("owl", id, Integer.parseInt (info.getRevision()));

		File tmpFile = File.createTempFile(id, ".owl");
		FileWriter writer = new FileWriter(tmpFile);
		writer.write(new String(bytes));
		writer.close();
		
		Pathway pwy = bpFormat.doImport(tmpFile);
		System.out.println(pwy.getDataNodeXrefs().size());
	}
	
	public static void main(String [] args) throws IOException, ServiceException
	{
		new WikipathwaysExerciser().run();
	}
}
