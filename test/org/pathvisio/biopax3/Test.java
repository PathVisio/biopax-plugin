package org.pathvisio.biopax3;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jdom.JDOMException;
import org.pathvisio.biopax3.exporter.ExportHelper;
import org.pathvisio.biopax3.importer.ImportHelper;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.view.MIMShapes;

import junit.framework.TestCase;

public class Test extends TestCase 
{

	public void setUp()
	{
		MIMShapes.registerShapes();
	}
	
	private static final File EXAMPLE = new File("exemple_l3.owl"); 
	
	public void testFiles()
	{
		assertTrue (EXAMPLE.exists());
	}
	
	public void testImportSimple() throws JDOMException, IOException, ConverterException
	{
		assertTrue(EXAMPLE.exists());
		ImportHelper bpf = new ImportHelper(EXAMPLE);
		
		List<org.biopax.paxtools.model.level3.Pathway> pathway_list;
		pathway_list = bpf.getPathways();
		
		assertEquals (1, pathway_list.size());
		
//		File f = new File(owl);
		System.out.println(" Opening biopax ");
		System.out.println("size : "+pathway_list.size());
		
		List<Pathway> pathways = bpf.convert();
		
		assertTrue (pathways.get(0).getDataObjects().size() > 10);
		
	}
	
	private static final File EXPORT_DIR = new File("testData/export");
	private static final File IMPORT_DIR = new File("testData/import");
	
	private static final File[] EXPORT_FILES = new File[] {
		new File (EXPORT_DIR, "one-metabolite.gpml"),
		new File (EXPORT_DIR, "one-protein.gpml"),
		new File (EXPORT_DIR, "reaction.gpml"),
		new File (EXPORT_DIR, "complex.gpml"),
		new File (EXPORT_DIR, "publication.gpml"),
	};
	
	public void testExport() throws ConverterException, IOException
	{
		// simply test if all files can be exported without exceptions
		for (File f : EXPORT_FILES)
		{
			assertTrue (f.exists());
			Pathway pwy = new Pathway();
			pwy.readFromXml(f, true);
			ExportHelper helper = new ExportHelper(pwy);
			File target = new File (f.getAbsoluteFile() + ".owl");
			helper.export(target, false);
		}
	}
	
	public void testImport() throws JDOMException, IOException, ConverterException
	{
		// simply test if all files can be imported without exceptions
		for (File f : IMPORT_DIR.listFiles())
		{
			if (!f.getName().endsWith("owl")) continue;
			assertTrue (f.exists());
			ImportHelper helper = new ImportHelper(f);
			Pathway pwy = helper.convertAll();
			System.out.println(pwy.getDataNodeXrefs().size());
		}
	}
	
}
