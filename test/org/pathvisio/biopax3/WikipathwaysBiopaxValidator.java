package org.pathvisio.biopax3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.bind.JAXBException;
import javax.xml.rpc.ServiceException;

import org.biopax.paxtools.client.BiopaxValidatorClient;
import org.biopax.paxtools.client.BiopaxValidatorClient.RetFormat;
import org.biopax.validator.jaxb.ErrorCaseType;
import org.biopax.validator.jaxb.ErrorType;
import org.biopax.validator.jaxb.Validation;
import org.biopax.validator.jaxb.ValidatorResponse;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.wikipathways.webservice.WSPathway;
import org.wikipathways.client.WikiPathwaysClient;

/**
 * This script reads a pathway from WikiPathways, converts it to BioPAX (locally), and
 * then submits it to the BioPAX validator webservice.
 */
public class WikipathwaysBiopaxValidator
{
	static class ValidationResult 
	{
	}
	
	public static void main(String [] args) throws ConverterException, ServiceException, IOException, JAXBException
	{
		new WikipathwaysBiopaxValidator().run();	
	}

	private void run() throws ConverterException, ServiceException, IOException, JAXBException
	{
		//WP157: glycolysis M. musculus
		checkPathway ("WP157");
	}

	final WikiPathwaysClient client;
	final BiopaxFormat format;
	final BiopaxValidatorClient bpValidator;
	
	public WikipathwaysBiopaxValidator() throws ServiceException, MalformedURLException
	{
		client = new WikiPathwaysClient(new URL("http://webservice.wikipathways.org"));
		format = new BiopaxFormat();
		bpValidator = new BiopaxValidatorClient();
	}
	
	private void checkPathway(String id) throws ConverterException, IOException, JAXBException
	{
		WSPathway wpwy = client.getPathway(id);
		Pathway pwy = WikiPathwaysClient.toPathway(wpwy);
		
//		File tempFile = File.createTempFile("biopaxTest.", ".owl");
		File tempFile = new File("biopaxTest.owl");
		format.doExport(tempFile, pwy);
		
		System.out.println ("Writing to temp file: " + tempFile);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bpValidator.validate(false, "strict", RetFormat.XML, null, null, null,  new File[] { tempFile }, baos);
//		bpValidator.validate(
//				false, false, 
//				RetFormat.XML, null, 
//				null, new File[] { tempFile }, baos);
		
		String data = baos.toString("UTF-8"); // TODO: not sure about encoding
		ValidatorResponse resp = BiopaxValidatorClient.unmarshal(data);
	
		for (Validation v : resp.getValidation())
		{
			System.out.println (v.getDescription() + " " + v.getMaxErrors());
			for (ErrorType e : v.getError())
			{
				System.out.println (e.getCode() + " " + e.getMessage() + " " + e.getType() + " " + e.getTotalCases());
				for (ErrorCaseType c : e.getErrorCase())
				{
					// NB: c.getObject() returns biopax id.
					System.out.println (c.getObject() + " " + c.getMessage());
					
				}
			}
			System.out.println ();
		}
	}
	
}
