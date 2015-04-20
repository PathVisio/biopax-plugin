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

package org.pathvisio.biopax3.layout;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.Timer;

import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;

/**
 * This class is an implementation for representing a pathway in
 * Spring Embedded Layout
 * @author adem
 */
public class SpringLayout {
	
	List<Node> nodes;
	List<Edge> edges;
	
	public SpringLayout(List<Node> nodes,List<Edge> edges){
		this.nodes=nodes;
		this.edges=edges;
	}
	
	public SpringLayout(){
		this.nodes=nodes;
		this.edges=edges;
	}
	
	int ITERATIONS = 100;
	double SPRING_LENGTH = 50.0f;
	double SPRING_CONSTANT = 2.0;
	int ITERATION_MSEC = 500;
	
	public void doLayout()
	{
		for (int n = 0; n < ITERATIONS; ++n)
		{
			doIteration();
		}		
	}
	
	private static Random random = new Random();

	
	
	public void doIteration()
	{
		
		double K = Math.sqrt((300*300) / nodes.size());
		int temp = ITERATIONS + 1;
		
		for (Node i : nodes)
		{
			i.dx=0;
			i.dy=0;
			for (Node u : nodes){
				if (u != i){
					double dx = i.x - u.x;
					double dy = i.y - u.y;
					double nodeLen = Math.sqrt(dx * dx + dy * dy);
					
					double forceR= K * K / nodeLen; 
					
					i.dx += (nodeLen / Math.abs(nodeLen)) / forceR;
					i.dy += (nodeLen / Math.abs(nodeLen)) / forceR;
				}
			}
//			i.x += (i.dx / Math.abs(i.dx)) ;
//			i.y += (i.dy / Math.abs(i.dy)) ;
//			i.x += i.dx;
//			i.y += i.dy;
		}
		
		
		
//		for (Node i : nodes)
//		{
//			i.dx = 0;
//			i.dy = 0;
//		}
		
		
		// calculate force on each node
		for (Edge e : edges)
		{
			double dx = e.start.x - e.end.x;
			double dy = e.start.y - e.end.y;
			double len = Math.sqrt(dx * dx + dy * dy);
			System.out.println ("len : "+len);
			double delta = (len - SPRING_LENGTH);			
			System.out.println ("delta : "+delta);
			double force = -delta / SPRING_CONSTANT;
			System.out.println ("force : "+force);
			
			e.start.dx += (dx / len) * force; 
			e.start.dy += (dy / len) * force;
		}

		// generate noise
//		for (Node i : nodes)
//		{
//			i.dx += random.nextDouble() - 0.5;
//			i.dy += random.nextDouble() - 0.5;
//		}

		for(Node v: nodes){
			v.x = Math.min( 300/2 , Math.max(-300/2 , v.dx) );
			v.y = Math.min( 300/2 , Math.max(-300/2 , v.dy) );
		}
		
		
	}
	
	public static class Node{
		double x;
		double y;
		double dx;
		double dy;
		public Node(double x, double y){
			this.x=x;
			this.y=y;
		}
		
		public double getX(){
			return x;
		}
		
		public double getY(){
			return y;
		}
	}
	
	public static class Edge{
		Node start;
		Node end;
		public Edge(Node start, Node end){
			this.start=start;
			this.end=end;
		}
	}
	
	private class Canvas extends JComponent
	{
		@Override
		public void paint(Graphics g) 
		{	
			Graphics2D g2d = (Graphics2D)g;
			
			for (Node n : nodes)
			{
				g2d.drawOval((int)n.x, (int)n.y, 5, 5);
			}
			for (Edge e : edges)
			{
				g2d.drawLine((int)e.start.x, (int)e.start.y, (int)e.end.x, (int)e.end.y);
			}
		}
	}
	
	public void run()
	{
		JFrame frame = new JFrame();
		final Canvas canvas = new Canvas();
		canvas.setSize(300, 300);
		frame.add(canvas);
		frame.setSize(400, 400);
		frame.setVisible(true);
		final Timer t = new Timer(ITERATION_MSEC, new ActionListener()
		{
			public void actionPerformed(ActionEvent arg0) 
			{
				doIteration();
				canvas.repaint();
			}
		});
		t.start();
		
		frame.addWindowListener(new WindowAdapter()
		{

			@Override
			public void windowClosing(WindowEvent arg0) {
				t.stop();
			}

		});
	}
	
	public static void main (String [] args)
	{
		List<Node> nl = new ArrayList<Node>();
		List<Edge> el = new ArrayList<Edge>();
		
		for (int i = 0; i < 10; ++i)
		{
			nl.add(new Node(100 + (i % 3) * 20, 100 + (i / 3) * 20));
		}
		
		for (int i = 0; i < 25; ++i)
		{
			int start = i % 10;
			int end = (i + 2) % 10;
			
//			int start = i % 10;
//			int end = random.nextInt(10);
			Edge e = new Edge(nl.get(start), nl.get(end));
			el.add (e);
		}
		
		SpringLayout spl = new SpringLayout();
		spl.run();
	}
}
