package org.pathvisio.biopax3.bots;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.rpc.ServiceException;

import org.biopax.validator.BiopaxValidatorClient;
import org.biopax.validator.BiopaxValidatorClient.RetFormat;
import org.biopax.validator.jaxb.ErrorCaseType;
import org.biopax.validator.jaxb.ErrorType;
import org.biopax.validator.jaxb.Validation;
import org.biopax.validator.jaxb.ValidatorResponse;
import org.pathvisio.biopax3.BiopaxFormat;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;
import org.pathvisio.wikipathways.webservice.WSPathway;
import org.wikipathways.client.WikiPathwaysCache;
import org.wikipathways.client.WikiPathwaysClient;


/**
 * This script reads a pathway from WikiPathways, converts it to BioPAX (locally), and
 * then submits it to the BioPAX validator webservice.
 */
public class WikipathwaysBiopaxValidator
{
	public static void main(String [] args) throws ConverterException, ServiceException, IOException, JAXBException
	{
		if (args.length == 1)
		{
			new WikipathwaysBiopaxValidator().runOne(args[0]);
		}
		else
		{
			new WikipathwaysBiopaxValidator().run();
		}
	}

	private void runOne(String id) throws ConverterException, IOException, JAXBException
	{
		results = new ValidationResultSet();
		checkPathway (id);
	}

	private Set<String> getAllIds() throws RemoteException
	{
		Set<String> result = new HashSet<String>();
		for (WSPathwayInfo info : client.listPathways())
		{
			// Do only human for now
			if (!"Homo sapiens".equals(info.getSpecies())) continue;
			
			result.add(info.getId());
		}
		return result;
	}

	private Set<String> needsUpdate(Date cutoff) throws RemoteException
	{
		Set<String> result = new HashSet<String>();
		for (WSPathwayInfo info : client.getRecentChanges(cutoff))
		{
			result.add(info.getId());
		}
		return result;
	}
	
	private void writeReport() throws IOException
	{
		FileOutputStream str = new FileOutputStream (new File ("biopaxreport.html"));
		results.printHtmlOverview(new PrintStream(str));
		str.close();
	}
	
	private void run() throws ConverterException, ServiceException, IOException, JAXBException
	{
		File resultsStoreFile = new File ("validator.objectstore");
		results = ValidationResultSet.readOrCreate(resultsStoreFile);

		try
		{
			Date now = new Date(); // store date before start of run, to account for changes during the run.
			Date lastChangeDate = results.previousRunDate;
			
			
			Set<String> alreadyDone = results.getIds();
			if (lastChangeDate != null)
				alreadyDone.remove(needsUpdate(lastChangeDate));
			
			//create list of pathways to update
			Set<String> todo = new HashSet<String>();
			todo = getAllIds();
			todo.removeAll(alreadyDone);		
//			todo.add ("WP15");
			
			System.out.println ("Already done: " + alreadyDone.size());
			System.out.println ("TODO: " + todo.size() + " pathways\n" + todo);
	
			//WP157: glycolysis M. musculus
			for (String id : todo)
			{
				checkPathway (id);
				results.store(resultsStoreFile);
				writeReport();
			}
	
			results.previousRunDate = now;
			
			results.store(resultsStoreFile);
		}
		finally
		{
			writeReport();
		}
	}

	final WikiPathwaysClient client;
	final BiopaxFormat format;
	final BiopaxValidatorClient bpValidator;
	ValidationResultSet results;
	
	public WikipathwaysBiopaxValidator() throws ServiceException
	{
		client = new WikiPathwaysClient();
		format = new BiopaxFormat();
		bpValidator = new BiopaxValidatorClient();
	}	
	
	private void checkPathway(String id) throws ConverterException, IOException, JAXBException
	{
		System.out.println ("CHECKING " + id);
		WSPathway wpwy = client.getPathway(id);
		Pathway pwy = WikiPathwaysClient.toPathway(wpwy);
		
		File tempFile = File.createTempFile("biopaxTest.", ".owl");
		format.doExport(tempFile, pwy);
		
		System.out.println ("Writing to temp file: " + tempFile);
		                                 
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bpValidator.validate(
				false, false, 
				RetFormat.XML, null, 
				null, new File[] { tempFile }, baos);
		
		String data = baos.toString("UTF-8"); // TODO: not sure about encoding
		ValidatorResponse resp = BiopaxValidatorClient.unmarshal(data);
		
		results.evict(id);
		
		for (Validation v : resp.getValidationResult())
		{
			System.out.println (v.getDescription() + " " + v.getMaxErrors());
			for (ErrorType e : v.getError())
			{
				for (ErrorCaseType c : e.getErrorCase())
				{
					results.record(id, wpwy.getName(), e.getCode(), c.getObject(), e.getMessage(), c.getMessage());
				}
			}
		}
		System.out.println ("DONE with " + id);
	}
	
}
