package org.pathvisio.biopax3.bots;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.helixsoft.xml.Html;
import nl.helixsoft.xml.HtmlStream;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ValidationResultSet implements Serializable
{
	static final long serialVersionUID = 1L;
	
	Date previousRunDate = null;
	
	private static ValidationResultSet tryFile(File filename) throws IOException, ClassNotFoundException
	{
		FileInputStream fis = null;
		ObjectInputStream in = null;
		ValidationResultSet result = null;
		fis = new FileInputStream(filename);
		in = new ObjectInputStream(fis);
		result = (ValidationResultSet)in.readObject();
		in.close();
		System.out.println ("INFO: loaded previously stored ValidationResultSet");
		return result;
	}
	
	private static File getBackupFilename(File filename)
	{
		return new File (filename.toString() + ".bak");
	}
	
	public static ValidationResultSet readOrCreate (File filename)
	{
		ValidationResultSet result = null;
		if (filename.exists())
		{
			try
			{
				try
				{
					result = tryFile(filename);
				}
				catch(IOException ex)
				{
					try
					{
						result = tryFile(getBackupFilename(filename));
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					ex.printStackTrace();
				}				
			}
			catch (ClassNotFoundException e)
			{
				e.printStackTrace();
			}
		}
		if (result == null) 
		{
			result = new ValidationResultSet();
			System.out.println ("INFO: created ValidationResultSet from scratch");
		}
		return result;
	}
	
	public void store(File filename)
	{
		try
		{
			File bkup = getBackupFilename(filename);
			if (filename.exists()) { FileUtils.copyFile(filename, bkup); }
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
		
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try
		{
			
			fos = new FileOutputStream(filename);
			out = new ObjectOutputStream(fos);
			out.writeObject(this);
			out.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	private List<ValidationResult> results = new ArrayList<ValidationResult>();
	
	public void record (String pwyId, String pwyTitle, String ruleId, String eltId, String ruleDesc, String msg)
	{
		ValidationResult result = new ValidationResult (pwyId, pwyTitle, ruleId, eltId, ruleDesc, msg);
		results.add (result);
	}
	
	public void evict (String pathway)
	{
		Iterator<ValidationResult> i = results.iterator();
		while (i.hasNext())
		{
			ValidationResult vr = i.next();
			if (vr.pwyId.equals (pathway)) { i.remove(); };
		}
	}
		
	public static class IdComparator implements Comparator<String>
	{
		@Override
		public int compare(String arg0, String arg1)
		{
			if (arg0.matches("WP\\d+") && arg1.matches("WP\\d+"))
			{
				Integer i0 = Integer.parseInt(arg0.substring(2));
				Integer i1 = Integer.parseInt(arg1.substring(2)); 
				return i0.compareTo(i1);
			}
			else return arg0.compareTo(arg1);
		}
	}
	
	public static <T> List<T> sortSet (Collection<T> set, Comparator<T> comparator)
	{
		List<T> list = new ArrayList<T>();
		list.addAll(set);
		Collections.sort(list, comparator);
		return list;
	}
	
	private Html asList(String coarseName, String fineName)
	{
		boolean coarse = coarseName.equalsIgnoreCase("rule");
		boolean fine = !coarse;
		
		Multimap<String, ValidationResult> resultByCoarse = HashMultimap.create();
		Multimap<String, String> fineByCoarse = HashMultimap.create();
		Map<String, String> fineMessages = new HashMap<String, String>();
		Map<String, String> coarseMessages = new HashMap<String, String>();
		
		for (ValidationResult vr : results)
		{
			String coarseId = vr.getGroupingId(coarse);
			String fineId = vr.getGroupingId(fine);
			
			resultByCoarse.put(coarseId, vr);
			fineByCoarse.put(coarseId, fineId);
			fineMessages.put(fineId, vr.getMessage(fine));
			coarseMessages.put(coarseId, vr.getMessage(coarse));
		}

		Html list = Html.ul(); // coarse list
		for (String coarseId : sortSet(resultByCoarse.keySet(), new IdComparator()))
		{			
			Html contents = Html.ul(); // fine list
			for (String fineId : sortSet(fineByCoarse.get(coarseId), new IdComparator()))
			{
				Html subContents = Html.ul(); // innermost list
				for (ValidationResult vr : results)
				{
					if (vr.getGroupingId(coarse).equals (coarseId) &&
						vr.getGroupingId(fine).equals (fineId))
					{
						subContents.addChild (Html.li(vr.eltId, ": ", Html.i(vr.msg)));
					}
				}
				contents.addChild(Html.li(
						Html.b(fineId), Html.br(),
						Html.i(fineMessages.get(fineId)), Html.br(),
						Html.collapseDiv(fineName + " details...", subContents))
					);
			}
			
			list.addChild (Html.li (
					Html.b(coarseId), " - ", resultByCoarse.keys().count(coarseId), " errors, divided over ", fineByCoarse.keys().count(coarseId), " " + fineName + "(s)", Html.br(),
					Html.i(coarseMessages.get(coarseId)), Html.br(),
					Html.collapseDiv (coarseName + " details...", contents)
			));
		}

		return list;
	}

	public void printHtmlOverview(PrintStream stream)
	{
		HtmlStream out = new HtmlStream(stream);
		out.begin ("html");
		out.begin ("head");
		out.add (Html.collapseScript());
		out.end ("head");
		
		out.begin ("body");
		
		out.add(Html.h1("BioPAX Validator results"));
		out.add(Html.p("For more information on how these errors are dealt with, see the ", Html.a("BioPAX Plug-in page").href("http://pathvisio.org/wiki/BiopaxPluginHelp#Validation")));
		out.add (Html.collapseDiv (
				Html.h2("By rule"), 
				asList("Rule", "Pathway")
			));
		
		out.add (Html.collapseDiv (
				Html.h2("By pathway"), 
				asList("Pathway", "Rule")
			));
		
		out.add(Html.p().style("font: small").addChild("Generated: ", new Date()));
		
		out.end("body");
		out.end("html");
	}
	
	public Set<String> getIds()
	{
		Set<String> result = new HashSet<String>();
		for (ValidationResult vr : results)
		{
			result.add (vr.pwyId);
		}
		return result;
	}
}