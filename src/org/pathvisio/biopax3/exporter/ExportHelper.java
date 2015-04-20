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
package org.pathvisio.biopax3.exporter;

import java.awt.geom.Point2D;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.BiochemicalReaction;
import org.biopax.paxtools.model.level3.Catalysis;
import org.biopax.paxtools.model.level3.Complex;
import org.biopax.paxtools.model.level3.DnaRegion;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Level3Element;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.Rna;
import org.biopax.paxtools.model.level3.RnaReference;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.SmallMolecule;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.pathvisio.biopax3.BpStyleSheet;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.GraphLink.GraphRefContainer;
import org.pathvisio.core.model.GroupStyle;
import org.pathvisio.core.model.LineType;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElement.Comment;
import org.pathvisio.core.model.PathwayElement.MAnchor;
import org.pathvisio.core.model.PathwayElement.MPoint;
import org.pathvisio.core.util.FileUtils;

public class ExportHelper
{
	static void transferComments (Entity bpE, PathwayElement pwElm)
	{
		Set<String> comments = new HashSet<String>();
		for (Comment com : pwElm.getComments()){
			comments.add(com.toString());
		}
		for (String c : comments)
		{
			((Level3Element)bpE).addComment(c);
		}
	}

	/**
	 * transfer Publication Xrefs attached to a pathway element to an 
	 * biopax entity.
	 */
	void transferPublicationXref(Entity bpE, PathwayElement pwElm)
	{
		for (org.pathvisio.core.biopax.PublicationXref pvPub : pwElm.getBiopaxReferenceManager().getPublicationXRefs())
		{
			String id = pvPub.getId();
			PublicationXref bpPub = null;
			if (bpModel.getByID(id) == null)
			{
				bpPub = bpModel.addNew(PublicationXref.class, id);
				
				for (String author : pvPub.getAuthors()) bpPub.addAuthor(author);
				bpPub.addSource(pvPub.getSource());
				try
				{
					bpPub.setYear(Integer.parseInt(pvPub.getYear()));
				}
				catch (NumberFormatException ex) { /* ignore, not a valid year */ }
				bpPub.setId(pvPub.getPubmedId());
				bpPub.setDb("PubMed");
				bpPub.setTitle(pvPub.getTitle());
			}
			else
			{
				bpPub = (org.biopax.paxtools.model.level3.PublicationXref)bpModel.getByID(id);
			}			
			bpE.addXref(bpPub);
		}
	}

	private BioPAXFactory factory;
	private Model bpModel;
	private int nextId = 1;
	private SimpleIOHandler exporter = new SimpleIOHandler(BioPAXLevel.L3);
	private final Pathway pvPwy;
	private org.biopax.paxtools.model.level3.Pathway bpPwy = null; 

	private Map<PathwayElement, PhysicalEntity> uniqueDatanodes = new HashMap<PathwayElement,PhysicalEntity>();
	private Map<Object, EntityReference> uniqueEntityRef = new HashMap<Object, EntityReference>();
	private Map<Xref, UnificationXref> uniqueUnificationXrefs = new HashMap<Xref, UnificationXref>();
	private Map<Xref, RelationshipXref> uniqueRelationshipXrefs = new HashMap<Xref, RelationshipXref>();
	private BioSource organism = null;

	private final BpStyleSheet bpss = new BpStyleSheet();

	public ExportHelper(Pathway pvPwy)
	{
		this.pvPwy = pvPwy;
		System.out.println(" Saving from GPML to Biopax");

		
		factory = BioPAXLevel.L3.getDefaultFactory();
		bpModel = factory.createModel();

		mapPathway();
	}
	
	public String generateRdfId()
	{
		String result = "id" + nextId;
		nextId++;
		return result;
	}

	private void transferEntityReference(SimplePhysicalEntity bpPe, PathwayElement pwyElt)
	{
		// for pwyElms with a good Xref, we generate only one EntityReference per Xref.
		// otherwise, we create a new EntityReference each time.
		Object key;
		if (pwyElt.getObjectType() == ObjectType.DATANODE && pwyElt.getDataSource() != null &&
				pwyElt.getGeneID() != null) key = pwyElt.getXref(); 
		else key = pwyElt;
		
		EntityReference bpEr = null;
		if (!uniqueEntityRef.containsKey(key))
		{
			if (bpPe instanceof SmallMolecule)
			{
				bpEr = bpModel.addNew(SmallMoleculeReference.class, generateRdfId());
				// must set to unknown, default is 0.0 which makes no sense...
				((SmallMoleculeReference)bpEr).setMolecularWeight(Entity.UNKNOWN_FLOAT);
			}
			else if (bpPe instanceof Protein)
			{
				bpEr = bpModel.addNew(ProteinReference.class, generateRdfId());
				if (organism != null) ((ProteinReference)bpEr).setOrganism(organism);
			}
			else if (bpPe instanceof Rna)
			{
				bpEr = bpModel.addNew(RnaReference.class, generateRdfId());
			}
			else
			{
				// We can't create entity references for complexes 
				// or unknown or unspecified elements
				return;
			}
			uniqueEntityRef.put(key, bpEr);
			
			// if possible, add a UnificationXref to the entityRef
			if (key instanceof Xref) transferXref(pwyElt, bpEr);
		}
		else
		{
			bpEr = uniqueEntityRef.get (key);
		}

		bpPe.setEntityReference(bpEr);
	}

	enum BiopaxEntityType
	{
		SMALLMOLECULE,
		PROTEIN,
		GENE,
		RNA,
		COMPLEX,
		PHYSICALENTITY,
		PATHWAY;
		
		/** Determine the biopax entity type suitable for a given datanode */
		public static BiopaxEntityType getFromElement(PathwayElement pwyElt)
		{
			if (pwyElt.getObjectType() == ObjectType.DATANODE)
			{
				if ((pwyElt.getDataNodeType()).equals("Metabolite"))
				{
					return SMALLMOLECULE;
				}
				else if (pwyElt.getDataNodeType().equals("Protein") || 
						pwyElt.getDataNodeType().equals("GeneProduct") ||
						pwyElt.getDataNodeType().equals("Unknown"))
				{
					return PROTEIN;
				}
				else if ((pwyElt.getDataNodeType()).equals("Rna"))
				{
					return RNA;
				}			
				else if ((pwyElt.getDataNodeType()).equals("Complex"))
				{
					return COMPLEX;	
				}
				else if ((pwyElt.getDataNodeType()).equals("Pathway"))
				{
					return PATHWAY;
				}
			}
			else if (pwyElt.getObjectType() == ObjectType.GROUP &&
					pwyElt.getGroupStyle() == GroupStyle.COMPLEX)
			{
				return COMPLEX;
			}
			return PHYSICALENTITY;
		}
	}
	
	/**
	 * Add an xref of a given PathwayElement to a biopax EntityReference
	 * If the Xref DataSource is of the same type as the element, a UnificationXref is created.
	 * Otherwise a relationshipXref is created.
	 * <p>
	 * For example, if PathwayElement is of type metabolite, and the Xref is from HMDB, 
	 * a unificationXref is created.
	 * If the pathwayElement is of type protein, and the Xref is from Entrez Gene, then there is
	 * a mismatch (protein versus gene), and a relationShipXref is created instead.
	 */
	private void transferXref(PathwayElement pwyElt, EntityReference bpEr)
	{
		String xrefType = pwyElt.getDataSource().getType();
		BiopaxEntityType eltType = BiopaxEntityType.getFromElement(pwyElt);
		
		boolean unification = false;
			
		switch (eltType)
		{
		case PROTEIN:
			if (xrefType.equalsIgnoreCase("protein")) unification = true;
			break;
		case RNA:
			if (xrefType.equalsIgnoreCase("rna")) unification = true;
			break;
		case SMALLMOLECULE:
			if (xrefType.equalsIgnoreCase("metabolite")) unification = true;
			break;
		default:
			return; // nothing to do
		}
		
		if (unification)
			bpEr.addXref(createOrGetUnificationXref(pwyElt.getXref()));
		else
			bpEr.addXref(createOrGetRelationshipXref(pwyElt.getXref()));
	}
	
	static Map<DataSource, String> miriamNameOverrides = new HashMap<DataSource, String>();
	
	static 
	{
		miriamNameOverrides.put(BioDataSource.UNIPROT, "Uniprot");
	}
	
	/** 
	 * get MIRIAM name for given DataSource. 
	 * //TODO: This should be functionality of BridgeDb.
	 */
	String getMiriamName (DataSource ds)
	{
		System.out.println (ds.getFullName());
		if (miriamNameOverrides.containsKey(ds)) 
			return miriamNameOverrides.get(ds);
		else 
			return ds.getFullName();
	}
	
	/**
	 * Creates a BioPAX UnificationXref for a given PathVisio Xref. 
	 * Equivalent UnificationXrefs are re-used, not re-created.
	 * However, a UnificationXref may co-exist with an equivalent RelationshipXref.
	 */
	private UnificationXref createOrGetUnificationXref(Xref pvXref)
	{
		if (!uniqueUnificationXrefs.containsKey(pvXref))
		{
			UnificationXref bpXref = bpModel.addNew(UnificationXref.class, generateRdfId());
			bpXref.setDb(getMiriamName(pvXref.getDataSource()));
			bpXref.setId(pvXref.getId());
			uniqueUnificationXrefs.put(pvXref, bpXref);
			return bpXref;
		}
		else
		{
			return uniqueUnificationXrefs.get(pvXref);
		}
	}
	
	/**
	 * Creates a BioPAX RelationshipXref for a given PathVisio Xref. 
	 * Equivalent RelationshipXrefs are re-used, not re-created. 
	 * However, a UnificationXref may co-exist with an equivalent RelationshipXref.
	 */
	private RelationshipXref createOrGetRelationshipXref(Xref pvXref)
	{
		if (!uniqueRelationshipXrefs.containsKey(pvXref))
		{
			RelationshipXref bpXref = bpModel.addNew(RelationshipXref.class, generateRdfId());
			bpXref.setDb(getMiriamName(pvXref.getDataSource()));
			bpXref.setId(pvXref.getId());
			uniqueRelationshipXrefs.put(pvXref, bpXref);
			return bpXref;
		}
		else
		{
			return uniqueRelationshipXrefs.get(pvXref);
		}
	}
	
	private Complex createComplex(PathwayElement pwyElt)
	{
		Complex bpPe = bpModel.addNew(Complex.class, generateRdfId());
		for (PathwayElement subElt : pwyElt.getParent().getGroupElements(pwyElt.getGroupId()))
		{
			PhysicalEntity bpSub = createOrGetPhysicalEntity(subElt);
			if (bpSub != null) bpPe.addComponent(bpSub);
		}		
		return bpPe;
	}
	
	private SimplePhysicalEntity createSimplePhysicalEntity(PathwayElement pwyElt)
	{
		SimplePhysicalEntity bpPe = null;
		
		// make sure every pathway element has a graph id.
		if (pwyElt.getGraphId() == null) pwyElt.setGeneratedGraphId();
		
		switch (BiopaxEntityType.getFromElement(pwyElt))
		{
		case SMALLMOLECULE:
			bpPe = bpModel.addNew(SmallMolecule.class, pwyElt.getGraphId());
			break;
		case PROTEIN:
			bpPe = bpModel.addNew(Protein.class, pwyElt.getGraphId());
			break;
		case RNA:
			bpPe = bpModel.addNew(Rna.class, pwyElt.getGraphId());
			break;
		case GENE:
			bpPe = bpModel.addNew(DnaRegion.class, pwyElt.getGraphId());
			break;
		default:
			System.out.println ("Ignoring " + pwyElt.getGraphId() + ", it is of type " + BiopaxEntityType.getFromElement(pwyElt)); 
			return null;
		}
		bpPe.setDisplayName(pwyElt.getTextLabel());

		// create entity reference
		transferEntityReference (bpPe, pwyElt);
		
		// add coordinates to style sheet
		bpss.add(bpPe.getRDFId(), new Point2D.Double(pwyElt.getMCenterX(), pwyElt.getMCenterY()));

		return bpPe;
	}
	
	/**
	 * Create objects related to a pathway element.
	 * <p>
	 * This usually means an instance of PhysicalEntity, and possibly
	 * corresponding PhysicalEntityReference and UnificationXref objects.
	 */
	private PhysicalEntity createOrGetPhysicalEntity(PathwayElement pwyElt)
	{
		PhysicalEntity bpPe = null;

		if (!uniqueDatanodes.containsKey(pwyElt))
		{
			if (pwyElt.getObjectType() == ObjectType.GROUP &&
					pwyElt.getGroupStyle() == GroupStyle.COMPLEX)
			{
				bpPe = createComplex(pwyElt);
			}
			else
			{
				bpPe = createSimplePhysicalEntity(pwyElt);
			}
			
			if (bpPe != null)
			{
				// map comments
				transferComments(bpPe, pwyElt);
				transferPublicationXref(bpPe, pwyElt);
	
				uniqueDatanodes.put(pwyElt, bpPe);
			}
		}
		else
		{
			bpPe = uniqueDatanodes.get(pwyElt);
		}
		
		return bpPe;
	}

	/**
	 * Map a relation, which is usually a biochemical reaction. 
	 */
	private void mapRelation(PathwayElement pwElm)
	{
		Relation r = new Relation(pwElm);

		Set<PhysicalEntity> leftPe = new HashSet<PhysicalEntity>() ;
		Set<PhysicalEntity> rightPe = new HashSet<PhysicalEntity>() ;

		for(PathwayElement elem : r.getLefts())
		{
			System.out.println("left: " + elem.getTextLabel() + " GraphID : " + elem.getGraphId());
			if (elem.getObjectType() == ObjectType.DATANODE)
			{
				PhysicalEntity pe = createOrGetPhysicalEntity(elem);
				if (pe != null) leftPe.add(pe);
			}
		}

		for(PathwayElement elem : r.getRights())
		{
			System.out.println("right: " + elem.getTextLabel() + " GraphID: " + elem.getGraphId());
			if (elem.getObjectType() == ObjectType.DATANODE)
			{
				PhysicalEntity pe = createOrGetPhysicalEntity(elem);
				if (pe != null) rightPe.add(pe);
			}
		}

		BiochemicalReaction reaction = bpModel.addNew (BiochemicalReaction.class, generateRdfId());
		reaction.setDisplayName(generateRdfId());
		
		for (PhysicalEntity i : leftPe) reaction.addLeft(i);
		for (PhysicalEntity i : rightPe) reaction.addRight(i);

		bpPwy.addPathwayComponent(reaction);

		for (PathwayElement elem : r.getMediators())
		{
			System.out.println("mediator: " + elem.getTextLabel() + " GraphID: " + elem.getGraphId());
			if (elem.getObjectType()==ObjectType.DATANODE)
			{
				PhysicalEntity pe = createOrGetPhysicalEntity(elem);
				if (pe != null) 
				{
					//In WikiPathways pathways, a reaction can have multiple enzymes,
					// but usually these are alternatives that are not required at the same time.
					// this constitutes an OR relationship, thus we need 
					// a separate Catalysis instance for each enzyme. 
					Catalysis catalysis = bpModel.addNew (Catalysis.class, generateRdfId());
					catalysis.addController(pe);
					catalysis.addControlled(reaction);
					bpPwy.addPathwayComponent(catalysis);
				}
			}
		}
	}

	private void mapPathway()
	{
		PathwayElement info = pvPwy.getMappInfo();

		bpPwy = bpModel.addNew (org.biopax.paxtools.model.level3.Pathway.class, generateRdfId());
		transferComments(bpPwy, info);
		bpPwy.setDisplayName(info.getMapInfoName());

		if (info.getOrganism() != null)
		{
			organism = bpModel.addNew (BioSource.class, generateRdfId());
			organism.setStandardName(info.getOrganism());
			bpPwy.setOrganism(organism);
		}

		for (PathwayElement pwElm : pvPwy.getDataObjects())
		{
			// is it a gene, protein or metabolite? 
			if (pwElm.getObjectType() == ObjectType.DATANODE)
			{
				createOrGetPhysicalEntity (pwElm);
			}

			// is it a complex (NB not every group automatically counts as a complex)
			if (pwElm.getObjectType() == ObjectType.GROUP &&
					pwElm.getGroupStyle() == GroupStyle.COMPLEX)
			{
				createOrGetPhysicalEntity (pwElm);	
			}

			// is it a biochemical reaction or other relation?
			if(isRelation(pvPwy, pwElm))
			{
				mapRelation(pwElm);					
			}

			// is it a line that can not be represented in BioPAX?
			if (pwElm.getObjectType() == ObjectType.LINE){ 
				if((pwElm.getStartGraphRef() == null) || (pwElm.getEndGraphRef() == null)){
					Logger.log.info("This pathway contains an incorrect arrow");
				}
			}
		}			
	}

	public void export(File file, boolean doBpSs) throws IOException
	{
		exporter.convertToOWL(bpModel, 
				new BufferedOutputStream(new FileOutputStream(file)));
		if (doBpSs)
		{
			File fnSs = FileUtils.replaceExtension(file, "bpss");
			FileOutputStream fos = new FileOutputStream (fnSs);
			bpss.write(fos);
			fos.close();
		}
	}

	// Test if a line is a relation
	static boolean isRelation(Pathway pv, PathwayElement pe) {
		if(pe.getObjectType() == ObjectType.LINE) {
			System.out.println(" LINE ");
			MPoint s = pe.getMStart();
			MPoint e = pe.getMEnd();
			if(s.isLinked() && e.isLinked()) {
				//Objects behind graphrefs should be PathwayElement
				//so not MAnchor
				if(pv.getElementById(s.getGraphRef()) != null &&
						pv.getElementById(e.getGraphRef()) != null)
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * The Relation class test each line and define the inputs, the outputs and
	 * the mediators for this reaction
	 * @author adem
	 *
	 */
	static class Relation 
	{
		private Set<PathwayElement> lefts = new HashSet<PathwayElement>();
		private Set<PathwayElement> rights = new HashSet<PathwayElement>();
		private Set<PathwayElement> mediators = new HashSet<PathwayElement>();

		public Relation(PathwayElement relationLine) {
			if(relationLine.getObjectType() != ObjectType.LINE) {
				throw new IllegalArgumentException("Object type should be line!");
			}
			Pathway pathway = relationLine.getParent();
			if(pathway == null) {
				throw new IllegalArgumentException("Object has no parent pathway");
			}
			//Add obvious left and right
			addLeft(pathway.getElementById(
					relationLine.getMStart().getGraphRef()
			));
			addRight(pathway.getElementById(
					relationLine.getMEnd().getGraphRef()
			));
			//Find all connecting lines (via anchors)
			for(MAnchor ma : relationLine.getMAnchors()) {
				for(GraphRefContainer grc : ma.getReferences()) {
					if(grc instanceof MPoint) {
						MPoint mp = (MPoint)grc;
						PathwayElement line = mp.getParent();
						if(line.getMStart() == mp) {
							//Start linked to anchor, make it a 'right'
							if(line.getMEnd().isLinked()) {
								addRight(pathway.getElementById(line.getMEnd().getGraphRef()));
							}
						} else {
							//End linked to anchor
							if(line.getEndLineType() == LineType.LINE) {
								//Add as 'left'
								addLeft(pathway.getElementById(line.getMStart().getGraphRef()));
							} else {
								//Add as 'mediator'
								addMediator(pathway.getElementById(line.getMStart().getGraphRef()));
							}
						}
					} else {
						Logger.log.warn("unsupported GraphRefContainer: " + grc);
					}
				}
			}
		}

		void addLeft(PathwayElement pwe) {
			addElement(pwe, lefts);
		}

		void addRight(PathwayElement pwe) {
			addElement(pwe, rights);
		}

		void addMediator(PathwayElement pwe) {
			addElement(pwe, mediators);
		}

		void addElement(PathwayElement pwe, Set<PathwayElement> set) {
			if(pwe != null) {
				//deal with groups...
				if(pwe.getObjectType() == ObjectType.GROUP) 
				{
					//If it's not a protein complex, add each element recursively
					if (pwe.getGroupStyle() != GroupStyle.COMPLEX)
					{
						for(PathwayElement ge : pwe.getParent().getGroupElements(pwe.getGroupId())) 
						{
							addElement(ge, set);
						}
					}
				}
				set.add(pwe);
			}
		}

		Set<PathwayElement> getLefts() { return lefts; }
		Set<PathwayElement> getRights() { return rights; }
		Set<PathwayElement> getMediators() { return mediators; }
	}
}


