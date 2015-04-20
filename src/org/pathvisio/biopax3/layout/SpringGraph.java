package org.pathvisio.biopax3.layout;

import java.util.ArrayList;
import java.util.List;

import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.Pathway;
import org.pathvisio.core.model.PathwayElement;

/**
 * To define the pathway which will be arranged
 * @author adem
 */
public class SpringGraph implements MutableGraphLayout{

	List<PathwayElement> nodes = new ArrayList<PathwayElement>();
	List<PathwayElement> edges = new ArrayList<PathwayElement>();
	double height;
	double width;
	Pathway pathway;
	
	public SpringGraph(Pathway pathway, int h, int w){
		this.pathway= pathway;
		for (PathwayElement pEl : pathway.getDataObjects()){
			if ((pEl.getObjectType()==ObjectType.DATANODE)||
					(pEl.getObjectType()==ObjectType.GROUP))
				nodes.add(pEl);
			else if (pEl.getObjectType()==ObjectType.LINE)
				edges.add(pEl);
		}
		
		List<String> groups = new ArrayList<String>();
		for (PathwayElement pt : nodes){
			if(pt.getObjectType()==ObjectType.GROUP){
				groups.add(pt.getGroupId());
			}
		}
		System.out.println(groups);	
		
		List<PathwayElement> toDelete = new ArrayList<PathwayElement>();
		for (PathwayElement pl : nodes){
			if (pl.getObjectType()==ObjectType.DATANODE){
				System.out.println("groupId "+pl.getGroupRef());
				if (groups.contains(pl.getGroupRef())){
					toDelete.add(pl);
				}
			}
		}
		
		nodes.removeAll(toDelete);
		
		System.out.println(nodes );
		this.height=h;
		this.width=w;
		System.out.println("height : "+h);
		System.out.println("width : "+w);
		System.out.println("nodes size : "+nodes.size()+" egdes size : "+edges.size());
	}
	
	public double getMaxHeight() {
		return height*20;
	}

	public double getMaxWidth() {
		return width*20;
	}

	public double getNodePosition(int nodeIndex, boolean position) {
//		return nodes.get(nodeIndex).getMCenterX();
		return 0;
	}

	public int getEdgeNodeIndex(int edgeIndex, boolean sourceNode) {
		PathwayElement line = edges.get(edgeIndex);
		String end  = line.getEndGraphRef();
		int ind =0;
		for (PathwayElement pl : nodes){
			if (nodes.contains(pl.getGraphRef())){
				ind = nodes.indexOf(pl.getGraphRef());
			}
			else
				return 0;
		}
		return ind;
	}

	public int getNumEdges() {
		return edges.size();
	}

	public int getNumNodes() {
		return nodes.size();
	}

	public boolean isDirectedEdge(int edgeIndex) {
		return true;
	}

	public boolean isMovableNode(int nodeIndex) {
		return true;
	}

	public void setNodePosition(int nodeIndex, double pos, double pos2) {
		nodes.get(nodeIndex).setMCenterX(pos);
		nodes.get(nodeIndex).setMCenterY(pos2);
		System.out.println("node : "+nodes.get(nodeIndex).getGraphId());
		System.out.println("pos : "+pos);
		System.out.println("pos 2 : "+pos2);
	}
}