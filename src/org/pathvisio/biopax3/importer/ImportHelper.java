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

package org.pathvisio.biopax3.importer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.biopax.paxtools.io.jena.JenaIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BiochemicalPathwayStep;
import org.biopax.paxtools.model.level3.Complex;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.Controller;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.Gene;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.Level3Element;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.PathwayStep;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.pathvisio.biopax3.BiopaxFormat;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.ConverterException;
import org.pathvisio.core.model.GpmlFormat;
import org.pathvisio.core.model.GroupStyle;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElement.MAnchor;
import org.pathvisio.core.util.FileUtils;

/**
 * BioPAX to GPML importer. This class provides the basic conversion from BioPAX
 * to GPML and can be subclassed to add additional conversions 
 * (e.g. to include Reactome specific comment properties).
 * @author adem
 */

public class ImportHelper {
	static final String COMMENT_SRC = "biopax";

	Model bpModel;
	BioPAXFactory bpFactory;
	Map<String, Element> rdfid2element = new HashMap<String, Element>();
	Document bpDoc ;
	double x;
	double y;
	BioPAXFactory l3fact = BioPAXLevel.L3.getDefaultFactory();
	
	/**
	 * Remembers all BioPAX elements that are converted to
	 * a GPML element.
	 */
	Map<BioPAXElement, PathwayElement> converted = new HashMap<BioPAXElement, PathwayElement>();
	private final File biopaxFile;
	
	/**
	 * Initialize the BioPAX importer for the given BioPAX file.
	 * Use {@link #convert()} to convert all pathways in the BioPAX file
	 * to GPML pathways.
	 * @throws ConverterException when the BioPAX level is wrong. 
	 */
	public ImportHelper(File biopaxFile) throws JDOMException, IOException, ConverterException 
	{
		this.biopaxFile = biopaxFile;
		Logger.log.info(" Opening File : "+biopaxFile);
		//Read in JDOM to give access to the raw xml
		SAXBuilder builder = new SAXBuilder(false);
		bpDoc = builder.build(biopaxFile);
		Logger.log.info("Building RDF:ID map");
		mapRdfIds(bpDoc.getRootElement());
		Logger.log.info("Finished RDF:ID map");
		
		//Read in paxtools
		JenaIOHandler ioh = new JenaIOHandler(l3fact,BioPAXLevel.L3);
		
		bpModel = ioh.convertFromOWL(new BufferedInputStream(
				new FileInputStream(biopaxFile)));
		if (bpModel.getLevel() != BioPAXLevel.L3)
		{
			throw new ConverterException ("Wrong BioPAX Level " + bpModel.getLevel() + "\n" +
					"This converter only understands " + BioPAXLevel.L3);
		}
		Logger.log.info ("Level : "+bpModel.getLevel());
		bpModel.getLevel().getDefaultFactory();
	}

	XrefMapper xrefMapper;
	
	/**
	 * Set the xref mappings
	 * @see XrefMapper
	 */
	public void setXrefMapper(XrefMapper xrefMapper) {
		this.xrefMapper = xrefMapper;
	}
	
	protected XrefMapper getXrefMapper() {
		if(xrefMapper == null) xrefMapper = new DefaultXrefMapper();
		return xrefMapper;
	}
	
	StyleMapper styleMapper;
	
	/**
	 * Set the style mappings
	 * @see StyleMapper
	 */
	public void setStyleMapper(StyleMapper styleMapper) {
		this.styleMapper = styleMapper;
	}
	
	protected StyleMapper getStyleMapper() {
		if(styleMapper == null) styleMapper = new DefaultStyleMapper();
		return styleMapper;
	}
	
	/**
	 * Creates a mapping between all RDF:ID properties and their corresponding
	 * JDom Element. This method will recursively create mappings for all children
	 * of the given element.
	 */
	private void mapRdfIds(Element e) {
		Attribute a = e.getAttribute("ID", GpmlFormat.RDF);
		if(a != null) {
			String base = 
				e.getDocument().getRootElement().getAttributeValue("base", Namespace.XML_NAMESPACE);
			String rdfId = a.getValue();

			if(base != null) rdfId = base + "#" + rdfId;
			rdfid2element.put(rdfId, e);
		}
		
		for(Object o : e.getChildren()) {
			if(o instanceof Element) {
				mapRdfIds((Element)o);
			}
		}
	}
	
	/**
	 * Get the GPML element that maps to the given BioPAX element.
	 * @return The GPML element, or null if the BioPAX element has not been
	 * converted, or the element doesn't have a direct mapping.
	 * @see BiopaxFormat#isConverted(BioPAXElement)
	 */
	protected PathwayElement getConverted(BioPAXElement e) {
		return converted.get(e);
	}
	
	/**
	 * Mark the BioPAX element as converted. This will add a BiopaxRef
	 * to the given GPML element. After this method is called, {@link #isConverted(BioPAXElement)}
	 * will return true for the given BioPAX element.
	 * @param e The BioPAX element that will be marked as converted.
	 * @param p The pathway element that maps to the BioPAX element.
	 */
	

	protected void markConverted(BioPAXElement e, PathwayElement p) {
//		if(p != null) {
//			bpRef= p.getBiopaxReferenceManager();
//		}
		converted.put(e, p);
	}
	
	/**
	 * Find out if a BioPAX element has already been converted to
	 * a GPML element.
	 */
	protected boolean isConverted(BioPAXElement e) {
		return converted.containsKey(e);
	}
	
	/**
	 * This method will be called for each element that doesn't have a
	 * mapping
	 * @param gpmlPathway
	 * @param o
	 */
	void noMapping(Pathway gpmlPathway, BioPAXElement o) {
		Logger.log.warn("No mapping found for " + o);
		String rdfId = o.getRDFId();
		markConverted(o, null);
		
		Element e = rdfid2element.get(rdfId);
		if(e != null) {
			try {
				gpmlPathway.getBiopax().addPassiveElement(e);
			} catch (Exception ex) {
				Logger.log.error("Unable to create BiopaxElement", ex);
			}
		} else {
			Logger.log.warn("No element found for RDF:ID " + rdfId);
		}
	}

	/**
	 * Get a properly formatted text label to put on the
	 * GPML element. This method prefers the SHORT_NAME
	 * property. If that doesn't exist, it takes the NAME
	 * attribute. Finally, if the length of the resulting String
	 * is > 20, it tries to find a shorter label in the
	 * SYNONYM properties.
	 * @return The text label, or null if no NAME, SHORT_NAME or
	 * SYNONYM is available
	 */
	protected String getTextLabel(Named e) {
		String label = e.getDisplayName();
		//Prefer short name
		if(e.getDisplayName() != null) {
			label = e.getDisplayName();
		}
		//Try to find a shorter synonym if
		//the label is long
		if(label == null || label.length() > 20) {
			for(String s : e.getName()) {
				if(label == null || s.length() < label.length()) {
					label = s;
				}
			}
		}
		return label;
	}
	
	public List<org.biopax.paxtools.model.level3.Pathway> getPathways(){
		List<org.biopax.paxtools.model.level3.Pathway> pathways = new ArrayList<org.biopax.paxtools.model.level3.Pathway>();
		for (BioPAXElement bpe : bpModel.getObjects(org.biopax.paxtools.model.level3.Pathway.class)) {
			pathways.add((org.biopax.paxtools.model.level3.Pathway)bpe);
		}
		return pathways;
	}

	/**
	 * Convert the whole BioPAX model to a single Pathway, regardless
	 * of how many Pathway objects are contained in it.
	 */
	public Pathway convertAll()
	{
		Pathway result = new Pathway();
		result.getMappInfo().setMapInfoName(FileUtils.removeExtension(biopaxFile.getName()));
		x = 80;
		y = 80;
		for (BioPAXElement bpElt : bpModel.getObjects())
		{
			if (bpElt instanceof Interaction)
			{
				mapInteraction(result, (Interaction)bpElt);
			} 
			else if (bpElt instanceof PhysicalEntity || bpElt instanceof Gene)
			{
				PathwayElement pv = mapEntity(result, (Entity)bpElt, false);
				result.add(pv);
			}
			
		}
//		for (BioPAXElement bpe : bpModel.getObjects())
//		{
//			Element e = rdfid2element.get(bpe.getRDFId());
//			if (e != null && e instanceof Entity) 
//				result.getBiopaxElementManager().addPassiveElement(e);
//		}
		return result;
	}
	
	/**
	 * Convert the BioPAX model to a set of GPML Pathways.
	 * This methods creates a GPML pathway for each BioPAX pathway entity,
	 * by iterating over the pathwayStep properties and converting
	 * all underlying interactions and physicalEntities to GPML
	 * elements.
	 * @return A list of converted GPML pathways.
	 */
	public List<Pathway> convert() {
		Logger.log.info("Starting conversion of " + bpModel);
		List<Pathway> pathways = new ArrayList<Pathway>();

		if (bpModel.getObjects(org.biopax.paxtools.model.level3.Pathway.class).size()==0){			
			newPathway();
			Pathway gpmlPathway = new Pathway();
			pathways.add(gpmlPathway);
			
			// Map the pathway components
			for (Interaction bpc : bpModel.getObjects(Interaction.class)) {
				
				Logger.log.info("Pathway component: " + bpc.getRDFId());
				if (bpc instanceof Interaction) {
					mapInteraction(gpmlPathway, bpc);
				} else {
					noMapping(gpmlPathway, bpc);
				}
			}	
		}
			
		else{
			for (BioPAXElement bpe : bpModel.getObjects(org.biopax.paxtools.model.level3.Pathway.class)) {
				x= 80;
				y = 80;
				Logger.log.info("Found pathway: " + bpe.getRDFId());
				newPathway();
				Pathway gpmlPathway = new Pathway();
				pathways.add(gpmlPathway);

				org.biopax.paxtools.model.level3.Pathway bpPathway = (org.biopax.paxtools.model.level3.Pathway) bpe;

				// Map general pathway information
				String pathwayName = bpPathway.getRDFId().substring(bpPathway.getRDFId().lastIndexOf('#') + 1);
				if (pathwayName != null) {
					if (pathwayName.length() > 50) {
						pathwayName = pathwayName.substring(0, 49);
					}
					gpmlPathway.getMappInfo().setMapInfoName(pathwayName);
				}

				//			File file = new File(gpmlPathway.getMappInfo()
				//					.getMapInfoName().replace(' ', '_')
				//					+ ".gpml"
				//			);
				//			System.out.println(" File : "+file.getAbsolutePath());
				//			gpmlPathway.setSourceFile(file);

				//			Organism organism = Organism.fromLatinName(bpPathway.getORGANISM().getNAME());
				//			if (organism != null) {
				//				gpmlPathway.getMappInfo().setOrganism(organism.latinName());
				//			}

				// Map the pathway components
				for (PathwayStep bpc : bpPathway.getPathwayOrder()) {

					Logger.log.info("Pathway component: " + bpc.getRDFId());
					if (bpc instanceof BiochemicalPathwayStep) {
						mapBiochemicalPathwayStep(gpmlPathway, (BiochemicalPathwayStep) bpc);
					} else {
						//					mapProcess(gpmlPathway, bp );
						noMapping(gpmlPathway, bpc);
					}
				}	
			}
		}
		return pathways;
	}

	public Pathway simple_convert(org.biopax.paxtools.model.level3.Pathway paw){
		x = 80;
		y = 80;
		Logger.log.info("Found pathway: " + paw.getRDFId());
		newPathway();
		Pathway gpmlPathway = new Pathway();
		
		org.biopax.paxtools.model.level3.Pathway bpPathway = (org.biopax.paxtools.model.level3.Pathway) paw;

		// Map general pathway information
		String pathwayName = bpPathway.getRDFId().substring(bpPathway.getRDFId().lastIndexOf('#') + 1);
		if (pathwayName != null) {
			if (pathwayName.length() > 50) {
				pathwayName = pathwayName.substring(0, 49);
			}
			gpmlPathway.getMappInfo().setMapInfoName(pathwayName);
		}
		
		// Map the pathway components
		for (Process bpc : paw.getPathwayComponent()) {

			Logger.log.info("Pathway component: " + bpc.getRDFId());
			if (bpc instanceof BiochemicalPathwayStep) {
				mapBiochemicalPathwayStep(gpmlPathway, (BiochemicalPathwayStep) bpc);
			} else {
				//					mapProcess(gpmlPathway, bp );
				noMapping(gpmlPathway, bpc);
			}
		}	
		return gpmlPathway;
	}

	
	/**
	 * Initializes variables that need to be refreshed every time a new pathway
	 * is encountered. This method is called from {@link #convert()} every time a
	 * new BioPAX pathway entity is found.
	 */
	protected void newPathway() {
		converted.clear();
	}

	/**
	 * Maps a BioPAX pathwayStep to GPML element(s) and marks
	 * it as converted.
	 * @param gpmlPathway The GPML pathway to add the elements to
	 * @param pws The BioPAX entity
	 */
	void mapBiochemicalPathwayStep(Pathway gpmlPathway, BiochemicalPathwayStep pws) {
		if(isConverted(pws)) return;
		Logger.log.info("Mapping pathwayStep: " + pws.getRDFId());
		for (Process p : pws.getStepProcess()) {
			mapProcess(gpmlPathway, p);
		}
		markConverted(pws, null);
	}

	/**
	 * Maps a BioPAX process to GPML element(s) and marks it as
	 * converted.
	 * @param gpmlPathway The GPML pathway to add the elements to
	 * @param pws The BioPAX entity
	 */
	void mapProcess(Pathway gpmlPathway, Process p) {
		if(isConverted(p)) return;
		Logger.log.info("\n Mapping process: " + p.getRDFId());
		if (p instanceof Interaction) {
			mapInteraction(gpmlPathway, (Interaction) p);
		} 
		else if (p instanceof org.biopax.paxtools.model.level3.Pathway){
			mapPathway(gpmlPathway, (org.biopax.paxtools.model.level3.Pathway) p);
		}
	}

	/**
	 * Maps a BioPAX pathway entity to GPML element(s) and marks
	 * it as converted. A pathway entity will be converted to 
	 * a label in this method (this should become a Link in the
	 * future).
	 * @param gpmlPathway The GPML pathway to add the elements to
	 * @param pws The BioPAX entity
	 */
	void mapPathway(Pathway gpmlPathway, org.biopax.paxtools.model.level3.Pathway p) {
		if(isConverted(p)) return;
		Logger.log.info("Mapping pathway " + p.getRDFId());
		
		PathwayElement link = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		link.setInitialSize();
		//String name = getTextLabel(p);
		String name = p.getDisplayName();
		link.setTextLabel(name);
		link.setInitialSize();
		link.setMCenterX(240);
		link.setMCenterY(80);
		gpmlPathway.add(link);
		markConverted(p, link);
	}
	
	/**
	 * Maps a BioPAX interaction to GPML element(s) and marks
	 * it as converted.
	 * @param gpmlPathway The GPML pathway to add the elements to
	 * @param pws The BioPAX entity
	 */
	void mapInteraction(Pathway gpmlPathway, Interaction i) {
		if(isConverted(i)) return;
		Logger.log.info("Mapping interaction " + i.getRDFId());
		
		if (i instanceof Conversion) {
			mapConversion(gpmlPathway, (Conversion) i);
		} else if (i instanceof Control){
			mapControl(gpmlPathway, (Control) i);
		}
		else 
		{
			mapOtherInteraction(gpmlPathway, i);
		}
	}

	void mapOtherInteraction(Pathway gpmlPathway, Interaction i){
		if(isConverted(i)) return;
		System.out.println("Mapping PhysicalInteraction " + i.getRDFId());
		for (Entity e : i.getParticipant()){
			mapInteractionParticipant(gpmlPathway, e);
		}
	}
	
	void mapCommentsAndId(PathwayElement elm, Level3Element bp)
	{
		elm.addComment("" + bp.getComment(), "owl");
		
		elm.addComment(
				(bp instanceof Named ? ((Named)bp).getName().toString() : "") + 
				" RDFId "+ bp.getRDFId(), "Biopax3GPML");
		elm.addBiopaxRef(bp.getRDFId());
	}
	
	/**
	 * Maps a BioPAX control to GPML element(s) and marks
	 * it as converted. Depending on the number of
	 * CONTROLLER and CONTROLLED objects, a control may result in multiple GPML
	 * line objects, e.g.:
	 * 
	 * CONTROLLER = a,b,c    CONTROLLED = x,y
	 * 
	 * a ------------------------|
	 *                           v
	 * b ----------------------> x
	 *                           ^
	 * c ------------------------|
	 * 
	 * a ------------------------|
	 *                           v
	 * b ----------------------> y
	 *                           ^
	 * c ------------------------|
	 * 
	 * results in a total of 6 GPML lines.
	 * 
	 * @param gpmlPathway The GPML pathway to add the elements to
	 * @param pws The BioPAX entity
	 */
	double anchor_pos = 0.1;
	protected void mapControl(Pathway gpmlPathway, Control c) {
		/*
		 * TODO: the CONTROLLER -> CONTROLLED lines should use an anchored line
		 * to connect to the CONTROLLED instance:
		 * a --------------
		 *                 \
		 * b ---------------o------> x
		 *                 /
		 * c --------------
		 */
		if(isConverted(c)) return;
		Logger.log.info("Mapping control " + c.getRDFId());
		Set<Controller> controller = c.getController();
		Set<Process> controlled = c.getControlled();
	
		for(Controller pe : controller) {
			PathwayElement pweController = mapEntity(gpmlPathway, pe, false);
			for(Process pr : controlled) {
				PathwayElement line = PathwayElement.createPathwayElement(ObjectType.LINE);
				
				getStyleMapper().mapControl(c, line);
				
				gpmlPathway.add(line);
				line.getMStart().linkTo(pweController, 0, 0);
				
				mapProcess(gpmlPathway, pr);
				PathwayElement prPwe = getConverted(pr);
				if (prPwe == null) continue;
				if(prPwe.getObjectType() == ObjectType.LINE) {
					MAnchor ma = prPwe.addMAnchor(anchor_pos);
					line.getMEnd().linkTo(ma, -1,0);
					anchor_pos = anchor_pos + 0.03;
					if (anchor_pos > 0.9) anchor_pos -= 0.8;
				} else {
					line.getMEnd().linkTo(prPwe, -1,0);
				}
				mapCommentsAndId(line, c);
				markConverted(c, line);
			}
		}
	}
	
	/**
	 * Maps a BioPAX conversion to GPML element(s) and marks
	 * it as converted. The conversion may result in multiple
	 * GPML objects, e.g.:
	 * 
	 * LEFT = a, b	RIGHT = x, y
	 * 
	 * a -----o-------o-----> x
	 *       /         \
	 * b ----           ----> y
	 * 
	 * This will result in a main conversion line, linking the first
	 * LEFT with the first RIGHT property. Additional properties will be
	 * linked to the main line using anchors.
	 * 
	 * @param gpmlPathway The GPML pathway to add the elements to
	 * @param pws The BioPAX entity
	 */
	protected void mapConversion(Pathway gpmlPathway, Conversion c) {
		if(isConverted(c)) return;
		Logger.log.info("Mapping conversion " + c.getRDFId());
		Iterator<PhysicalEntity> itLeft = c.getLeft().iterator();
		Iterator<PhysicalEntity> itRight = c.getRight().iterator();

		PhysicalEntity pLeft = c.getLeft().size() > 0 ? itLeft.next() : null;
		PhysicalEntity pRight = c.getRight().size() > 0 ? itRight.next() : null;
		
		PathwayElement pweLeft = null;
		PathwayElement pweRight = null;
		if(pLeft == null) {
			pweLeft = getStyleMapper().createUnknownParticipant();
			gpmlPathway.add(pweLeft);
		} else {
			pweLeft = mapEntity(gpmlPathway, pLeft, false);
		}
		if(pRight == null) {
			pweRight = getStyleMapper().createUnknownParticipant();
			gpmlPathway.add(pweRight);
		} else {
			pweRight = mapEntity(gpmlPathway, pRight, false);
		}
		//Create a line between the first input/output
		PathwayElement line = PathwayElement.createPathwayElement(ObjectType.LINE);
		getStyleMapper().mapConversion(c, line);
		gpmlPathway.add(line);

		//Add additional input/output to anchors of the first line
		if (pweLeft != null && pweRight != null) {
			line.getMStart().linkTo(pweLeft, 0, 0);
			line.getMEnd().linkTo(pweRight, -1,0);
			
			while(itLeft.hasNext()) {
				MAnchor anchorLeft = line.addMAnchor(0.3);
				PhysicalEntity pep = itLeft.next();
				PathwayElement pwe = mapEntity(gpmlPathway, pep, false);
				if(pep != pLeft && pwe != null) {
					PathwayElement l = PathwayElement.createPathwayElement(ObjectType.LINE);
					getStyleMapper().mapConversionLeft(c, l);
					gpmlPathway.add(l);
					l.getMStart().linkTo(pwe, 0, 0);
					l.getMEnd().linkTo(anchorLeft,  -1,0);
				}
			}
			
			while(itRight.hasNext()) {
				MAnchor anchorRight = line.addMAnchor(0.7);
				PhysicalEntity pep = itRight.next();
				PathwayElement pwe = mapEntity(gpmlPathway, pep, false);
				if(pep != pRight && pwe != null) {
					PathwayElement l = PathwayElement.createPathwayElement(ObjectType.LINE);
					getStyleMapper().mapConversionRight(c, l);
					gpmlPathway.add(l);
					l.getMStart().linkTo(anchorRight, 0, 0);
					l.getMEnd().linkTo(pwe, -1, 0);
				}
			}
		}
		mapCommentsAndId(line, c);
		
		markConverted(c, line);
	}

	/**
	 * Maps a BioPAX interactionParticipan to GPML element(s) and marks
	 * it as converted.
	 * @param gpmlPathway The GPML pathway to add the elements to
	 * @param pws The BioPAX entity
	 */
	PathwayElement mapInteractionParticipant(Pathway gpmlPathway,
			Entity p) {
		if(isConverted(p)) return converted.get(p);
		Logger.log.info("Mapping interaction participant: " + p.getRDFId());
		PathwayElement pwElm = null;

		if (p instanceof PhysicalEntity || p instanceof Gene) {
			pwElm = mapEntity(gpmlPathway,
					p, false);
		} else {
			noMapping(gpmlPathway, p);
		}

		if (pwElm != null) {
			for (String cmt : p.getComment()) {
				pwElm.addComment(cmt, COMMENT_SRC);
			}
		}
		markConverted(p, pwElm);
		return pwElm;
	}

	/**
	 * Maps a BioPAX physicalEntityParticipant to GPML element(s) and marks
	 * it as converted.
	 * @param gpmlPathway The GPML pathway to add the elements to
	 * @param pws The BioPAX entity
	 * @param forceCreate If true, this method will always create a new GPML element instead
	 * of reusing the converted element, even if the BioPAX entity has been converted to a GPML 
	 * element before.
	 */
//	PathwayElement mapPhysicalEntityParticipant(Pathway gpmlPathway,
//			physicalEntityParticipant p, boolean forceCreate) {
//		if(isConverted(p) && !forceCreate) return getConverted(p);
//		Logger.log.info("Mapping physical entity participant: " + p.getRDFId());
//		
//		PathwayElement pe = mapPhysicalEntity(gpmlPathway, p.getPHYSICAL_ENTITY(), forceCreate || shouldReplicate(p.getPHYSICAL_ENTITY()));
//		markConverted(p, pe);
//		return pe;
//	}
	
	/**
	 * Should this physicalEntity always create a replicate GPML element, even
	 * if it's already converted. If this method returns true, it will override the forceCreate
	 * parameter of {@link #mapPhysicalEntityParticipant(Pathway, physicalEntityParticipant, boolean)}.
	 * This can be used to specify that certain small molecules should be converted into a replicate GPML
	 * element for each reaction (e.g. ATP or ADP).
	 */
	protected boolean shouldReplicate(PhysicalEntity p) {
//		if("ATP".equals(p.getSHORT_NAME())) return true;
//		if("ADP".equals(p.getSHORT_NAME())) return true;
		return false;
	}
	
	public void setCoordinate (PathwayElement elt)
	{
		elt.setMCenterX(x);
		elt.setMCenterY(y);
		y = y + 40;
		x = x + 80;
		if (x > 640) {
			x = 80;
			y= y - 240;
		}		
	}
	
	PathwayElement mapGeneOrNonComplexPhysicalEntity(Pathway gpmlPathway, Entity entity)
	{
		Logger.log.trace("\tMapping gene or non-complex: " + entity.getRDFId());
		PathwayElement pwElm = PathwayElement.createPathwayElement(ObjectType.DATANODE);
		Logger.log.trace(" GraphRef for "+entity.getRDFId()+ " : " + pwElm.getGraphRef());
		//semi-automatic layout
		String name = getTextLabel(entity);
		if (name == null)
		{
			if (entity instanceof SimplePhysicalEntity)
			{
				name = getTextLabel(((SimplePhysicalEntity)entity).getEntityReference());
			}
		}
		if (name != null) {
			pwElm.setTextLabel(name);
		}
		pwElm.setInitialSize();
		pwElm.setMWidth(6 * (pwElm.getTextLabel().length()+2));
		
		setCoordinate(pwElm);			
		getStyleMapper().mapEntity(entity, pwElm);
		getXrefMapper().mapXref(entity, pwElm);
		gpmlPathway.add(pwElm);
		return pwElm;
	}
	
	
	/**
	 * Maps a BioPAX gene or physicalEntity to GPML element(s) and marks
	 * it as converted.
	 * @param gpmlPathway The GPML pathway to add the elements to
	 * @param pws The BioPAX entity
	 * @param forceCreate If true, this method will always create a new GPML element instead
	 * of reusing the converted element, even if the BioPAX entity has been converted to a GPML 
	 * element before.
	 */
	PathwayElement mapEntity(Pathway gpmlPathway, Entity entity, boolean forceCreate) {
		if(isConverted(entity) && !forceCreate) return getConverted(entity);
		Logger.log.info("Mapping physical entity: " + entity.getRDFId());
		PathwayElement pwElm = null;
		if (entity instanceof Complex) {
			Logger.log.trace("\tMapping complex: " + entity.getRDFId());
			pwElm = mapComplex(gpmlPathway, (Complex) entity, true);
		} 
		else if (entity instanceof Gene || entity instanceof PhysicalEntity)
		{
			pwElm = mapGeneOrNonComplexPhysicalEntity(gpmlPathway, entity);
		}
		
		mapCommentsAndId(pwElm, entity);
				
		markConverted(entity, pwElm);
		return pwElm;
	}
	
	/**
	 * Maps a BioPAX complex to GPML element(s) and marks
	 * it as converted.
	 * @param gpmlPathway The GPML pathway to add the elements to
	 * @param pws The BioPAX entity
	 * @param forceCreate If true, this method will always create a new GPML element instead
	 * of reusing the converted element, even if the BioPAX entity has been converted to a GPML 
	 * element before.
	 */
	PathwayElement mapComplex(Pathway gpmlPathway, Complex c, boolean forceCreate) {
		if(isConverted(c) && !forceCreate) return converted.get(c);
		Logger.log.info("Mapping complex: " + c.getRDFId());
		PathwayElement group = PathwayElement.createPathwayElement(ObjectType.GROUP);
		gpmlPathway.add(group);
		
		String name = getTextLabel(c);
		if (name != null) {
			group.setTextLabel(name);
		}
		group.setGroupStyle(GroupStyle.COMPLEX);
		String groupId = group.createGroupId();

		for(PhysicalEntity ep : c.getComponent()) {
			PathwayElement groupElm = mapEntity(gpmlPathway, ep, true);
			String currRef = groupElm.getGroupRef();
			if(currRef != null) {
				Logger.log.warn("Object already in group " + currRef + ", replacing with " + groupId);
			}
			groupElm.setGroupRef(groupId);
		}
		stackGroup(group);
		markConverted(c, group);
		return group;
	}
	
	/**
	 * Stacks all elements in a GPML group.
	 */
	protected void stackGroup(PathwayElement group) {
		Pathway p = group.getParent();
		if(p != null) {
			Set<PathwayElement> groupElements = p.getGroupElements(group.getGroupId());
			List<PathwayElement> sorted = new ArrayList<PathwayElement>(groupElements);
			Collections.sort(
					sorted,
					new Comparator<PathwayElement>() {
						public int compare(PathwayElement o1, PathwayElement o2) {
							ObjectType ot1 = o1.getObjectType();
							ObjectType ot2 = o2.getObjectType();
							if(ot1 == ot2 || (ot1 != ObjectType.GROUP && ot2 != ObjectType.GROUP)) {
								return o1.compareTo(o2);
							} else {
								return ot1 == ObjectType.GROUP ? 1 : -1;
							}
						}
					}
			);
			double mtop = group.getMTop();
			double cx = group.getMCenterX();
			
			for(PathwayElement ge : sorted) {
				ge.setMTop(mtop);
				ge.setMCenterX(cx);
				mtop += ge.getMHeight();
			}
		}
	}
	
	/**
	 * Subclasses may implement this method to provide additional layout for the 
	 * converted pathway.
	 * @param gpmlPathway
	 * @param bpPathway
	 */
	protected void layoutPathway(Pathway gpmlPathway, org.biopax.paxtools.model.level3.Pathway bpPathway) {
		// layout may be implemented by subclasses
	}

}
