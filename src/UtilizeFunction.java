

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

public class UtilizeFunction {
	/**
	 * Returns a psuedo-random number between min and max, inclusive.
	 * The difference between min and max can be at most
	 * <code>Integer.MAX_VALUE - 1</code>.
	 *
	 * @param min Minimim value
	 * @param max Maximim value.  Must be greater than min.
	 * @return Integer between min and max, inclusive.
	 * @see java.util.Random#nextInt(int)
	 */
	
	public static boolean isPositive(Vector<Double> k)
	{
		for (int i=0;i<k.size();i++)
			if(k.get(i)<=0)
				return false;
		return true;
	}
	public static Vector<Double> add(Vector<Double> a, Vector<Double> b)
	{
		Vector<Double> temp = new Vector<Double>();
		if(a.size()!=b.size()) return null;
		for (int i=0;i<a.size();i++)
			temp.addElement(a.get(i)+b.get(i));
		return temp;
	}
	public static Vector<Double> minus(Vector<Double> a, Vector<Double> b)
	{
		Vector<Double> temp = new Vector<Double>();
		if(a.size()!=b.size()) return null;
		for (int i=0;i<a.size();i++)
			temp.addElement(a.get(i)-b.get(i));
		return temp;
	}
	public static double multi(Vector<Double> a, Vector<Double> b)
	{
		double temp=0.0;
		if(a.size()!=b.size()) return -1;
		for (int i=0;i<a.size();i++)
			
			temp+=a.get(i)*b.get(i);
		return temp;
	}
	public static Vector<Double> multi(Vector<Double> a, double b)
	{
		Vector<Double> temp = new Vector<Double>();
		for (int i=0;i<a.size();i++)
			
			temp.addElement(b*a.get(i));
		return temp;
	}
	public static boolean isBig(Vector<Double> k1, Vector<Double> k2)
	{
		if(k1.get(0)>=k2.get(0)&& k1.get(1)>=k2.get(1)&& k1.get(2)>=k2.get(2))
			return true;
		else
			return false;
	}
	public static double value(Vector<Double> k)
	{
		double tam=0.0;
		for(int i=0;i<k.size();i++)
			tam+=k.get(i)*k.get(i);
		return Math.sqrt(tam);
	}
	public static int bigger(Vector<Double> k1, Vector<Double> k2)
	{
		// 1: k1 > k2
		// 2: k2 > k1
		// 0: k1 = k2
		//-1: k1 != k2
		if(k1.size() != k2.size()) return -1;
		if(k1.get(0)==k2.get(0))
			if(k1.get(1)==k2.get(1))
				if(k1.get(2)==k2.get(2))
					return 0;
				else
					if(k1.get(2)>k2.get(2))
						return 1;
					else
						return 2;
			else
				if(k1.get(1)>k2.get(1))
					return 1;
				else
					return 2;
		else
			if(k1.get(0)>k2.get(0))
				return 1;
			else
				return 2;
	}
	
	public static int randInt(int min, int max) {

	    // Usually this can be a field rather than a method variable
	    Random rand = new Random();

	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    int randomNum = rand.nextInt((max - min) + 1) + min;

	    return randomNum;
	}
	
	
	public static double randDouble(double min, double max) {
	    double random = new Random().nextDouble();
		double result = min + (random * (max - min));

	    return result;
	}
	public static double randDouble(int hso) {

	    // Usually this can be a field rather than a method variable
	    Random rand = new Random();

	    // nextInt is normally exclusive of the top value,
	    // so add 1 to make it inclusive
	    double randomNum = rand.nextDouble()*hso;

	    return randomNum;
	}
	public static void randomData_lib(String fileIn, String fileOut,int m,int s,int V, int E ,int d )
	{
		
		
		DecimalFormat df = new DecimalFormat("#.##");
		BufferedWriter out;
		Scanner salesDataFile;
		List<Double> x_list = new ArrayList<>();
		List<Double> y_list = new ArrayList<>();
		List<Double> cap_list=new ArrayList<>();
		List<Double> price_list=new ArrayList<>();
		
		
		
		SimpleWeightedGraph<String, DefaultWeightedEdge>  g_cap = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		SimpleWeightedGraph<String, DefaultWeightedEdge>  g_cost = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		SimpleWeightedGraph<String, DefaultWeightedEdge>  g_demand = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
       	
		try {
			salesDataFile = new Scanner(new File(fileIn));
	        String line=null;
			for (int i=0;i<8;i++)
				line = salesDataFile.nextLine();
			for (int i=0;i<V;i++)
			{
				//read x,y for node
				line= salesDataFile.nextLine();
				Scanner scanner = new Scanner(line);
	            scanner.useDelimiter(" ");
	            while (scanner.hasNext())
	            {
	            	String name = scanner.next();
	            	 // Wuerzburg ( 9.97 49.78 )
	            	name = scanner.next();
	            	g_cap.addVertex(name);
	            	g_cost.addVertex(name);
	            	g_demand.addVertex(name);
	            	scanner.next();
	            	double x= scanner.nextDouble();
	            	double y= scanner.nextDouble();
	            	x_list.add(x);
	            	y_list.add(y);
	            	cap_list.add(80 * Math.random()+2);
	            	price_list.add(10 * Math.random()+3);
	            	System.out.println(name+":"+x+":"+y);
	            	scanner.next();
	            }
	            scanner.close();
			}
			for (int i=0;i<7;i++)
				salesDataFile.nextLine();
			for (int i=0;i<E;i++)
			{
				line= salesDataFile.nextLine();
				
				//  L1 ( Duesseldorf Essen ) 0.00 0.00 0.00 0.00 ( 40.00 3290.00 )
				Scanner scanner = new Scanner(line);
	            scanner.useDelimiter(" ");
	            while (scanner.hasNext())
	            {
	            	scanner.next();
	            	scanner.next();
	            	String node1= scanner.next();
	            	String node2= scanner.next();
	            	for (int j=0;j<6;j++)
	            		scanner.next();            	
	            	double cap= scanner.nextDouble();
	            	double cost= scanner.nextDouble();
	            	DefaultWeightedEdge e_cap=g_cap.addEdge(node1,node2);	        			
	        		g_cap.setEdgeWeight(e_cap, cap);
	        		DefaultWeightedEdge e_cost=g_cost.addEdge(node1,node2);	        			
	        		g_cap.setEdgeWeight(e_cost, cost);
	            	scanner.next();
	            }
	            scanner.close();
			}
			for (int i=0;i<7;i++)
				salesDataFile.nextLine();
			
			for (int i=0;i<d;i++)
			{
				line= salesDataFile.nextLine();
			//  Essen_Duesseldorf ( Essen Duesseldorf ) 1 34.00 UNLIMITED
				Scanner scanner = new Scanner(line);
	            scanner.useDelimiter(" ");
	            while (scanner.hasNext())
	            {
	            	scanner.next();
	            	String name = scanner.next();
	            	scanner.next();
	            	String node1= scanner.next();
	            	String node2= scanner.next();
	            	for (int j=0;j<2;j++)
	            		scanner.next();            	
	            	double demand= scanner.nextDouble();
	            	DefaultWeightedEdge e_demand=g_demand.addEdge(node1,node2);	        			
	        		g_demand.setEdgeWeight(e_demand, demand);
	            	System.out.println(name+":"+node1+":"+node2 + ":"+ demand);
	            	scanner.next();
	            }
	            scanner.close();
			}
			
			
	    salesDataFile.close();

		    
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<String> list_vertex = new ArrayList<String>(g_cap.vertexSet());
		
		
		File file = new File(fileOut);
        try {
	    	out= new BufferedWriter(new FileWriter(file));
		    Function[] functionArr = new Function[m];
		    for (int i=0;i< m;i++)
		       functionArr[i]= new Function(i+1);     
		    Service[] serviceArr = new Service[s];
		    int k=0;
		    if(fileIn=="france.txt")
		    	k=2500;
		    else
		    	k=40;
		    for (int i=0;i<s;i++)
		    {
		    	serviceArr[i]= new Service(k,i+1,functionArr);
		    } 
		    
		    //ghi ra file
		    out.write(m+" "+s + " "+ g_cost.vertexSet().size() + " "+ g_cost.edgeSet().size());
		    out.newLine();
		    for (int i=0;i<m;i++)
	       	{	 
	               out.write(df.format(functionArr[i].bw())+" ");
	       	}
		    out.newLine();
	       	for (int i=0;i<s;i++)
	       	{ 
	       		out.write(df.format(serviceArr[i].bwS())+" ");
	       		for (int j=0;j<serviceArr[i].noF();j++)
	       			out.write(serviceArr[i].getFunctions()[j].id() +" ");
	       		out.newLine();
	       	}
	       	for (int i=0;i<list_vertex.size();i++)
	       	{
	       		out.write(df.format(x_list.get(i)) + " " + df.format(y_list.get(i)) + " " +df.format(cap_list.get(i)) + " " +df.format(price_list.get(i)));
	       		out.newLine();
	       	}
	       	double weit=0.0;
		    for (int i=0;i<list_vertex.size();i++)
		    {
		    	for(int j=0;j<list_vertex.size();j++)
		    	{
		    		weit=g_cap.getEdgeWeight(g_cap.getEdge(list_vertex.get(i), list_vertex.get(j)));
		    		if(weit<=1)
		    			weit=0.0;
		    		out.write(df.format(weit) + " ");
		    		System.out.print(weit + "   ");
		    	}
		    	out.newLine();
		    	System.out.println();
		    }
		    for (int i=0;i<list_vertex.size();i++)
		    {
		    	for(int j=0;j<list_vertex.size();j++)
		    	{
		    		weit=g_cost.getEdgeWeight(g_cost.getEdge(list_vertex.get(i), list_vertex.get(j)));
		    		if(weit<=1)
		    			weit=0.0;
		    		out.write(df.format(weit) + " ");
		    		System.out.print(weit + "   ");
		    	}
		    	out.newLine();
		    	System.out.println();
		    }
	       	out.close();
		} catch (IOException e) {
			// TODO Auto-generated catsch block
			e.printStackTrace();
		}
		
	}
    public static void randomNewest(String filePara)//edit model
	{
		DecimalFormat df = new DecimalFormat("#.##");
		BufferedWriter out;
		BufferedReader in;
		File file = new File(filePara);
        try {
			in = new BufferedReader(new FileReader(file));

			String strLine = in.readLine();
			int dataNo= Integer.parseInt(strLine);
			Vector<Vector<Double>> dataReal = new Vector<Vector<Double>>();
			for (int i=0;i<dataNo;i++)
			{
				//doc n hang tiep theo
				strLine = in.readLine();
				String[] _line = strLine.split(" ");
				Vector<Double> t= new Vector<Double>(4);
				for (int j=0;j <4;j++)
				{
					t.addElement(Double.parseDouble(_line[j]));
				}
				dataReal.addElement(t);
			}
			int temp=1;
			dataNo = dataNo *8;
			//Read File Line By Line			
			while ((strLine = in.readLine()) != null)   {
				String[] line= strLine.split(" ");			
				
				int n = Integer.parseInt(line[0]);
				double p = Double.parseDouble(line[1]);
				int noG= Integer.parseInt(line[2]);
				int m=Integer.parseInt(line[3]);
				MyGraph g = new MyGraph(n,p,dataReal);
				int E = g.E();
				Function[] functionArr = new Function[m];
				
			    for (int i=0;i< m;i++)
			       functionArr[i]= new Function(i+1);
			    for (int l=0;l<noG;l++)
			    {
			    	String fileName= "data\\in"+(l+temp)+".txt";
			    	out= new BufferedWriter(new FileWriter(fileName));
					int s = Integer.parseInt(line[4+l]);
				    
					Demand[] demandArr= new Demand[s];
					for (int i=0;i<s;i++)
					{
						demandArr[i]= new Demand(i+1,-1,-1,-1,13,-1,functionArr,n,g); 
					}
				    //ghi ra file
				    out.write(m+" "+s + " "+ n + " "+ E);
				    out.newLine();
				    for (int i=0;i<m;i++)
				    {
			               for (int j=0;j<3;j++)
			            	   out.write(df.format(functionArr[i].getLamda().get(j))+" ");
			               out.write(";");
			       	}
				    out.newLine();
			       	for (int i=0;i<s;i++)
			       	{ 
			       		System.out.println("bandwidth: "+demandArr[i].bwS() );
			       		out.write(demandArr[i].idS() +" " +demandArr[i].sourceS() +" "+ demandArr[i].destinationS()+" "+ demandArr[i].bwS()+" "+demandArr[i].arrivalTimeS()+" ");
			       		out.write(demandArr[i].getRate()+" ");
			       		for (int j=0;j<demandArr[i].noF();j++)
			       			out.write(demandArr[i].getFunctions()[j].id() +" ");
			       		
			       		out.newLine();
			       	}
			       	out.write(df.format(g.getPriceBandwidth())+"");
			       	out.newLine();
			       	for (int i=0;i<n;i++)
			       	{		            
			       		for (int j=0;j<3;j++)
			            	   out.write(df.format(g.getCap(i+1).get(j))+" ");
			            out.write(";");
			            out.write(df.format(g.getPriceNode(i+1))+" ");
//			       		for (int j=0;j<3;j++)
//			            	   out.write(df.format(g.getPriceNode(i+1).get(j))+" ");
			            
			       		out.newLine();
			       	}
			       	for (int i=0;i<n;i++)
			       	{
			       		for (int j=0;j<n;j++)
			       			out.write(df.format(g.getEdgeWeight(i+1, j+1)) + " ");
			       		out.newLine();
			       	}
			       	out.close();
			    }
			    temp+=noG;
			    
			}

			//Close the input stream
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catsch block
			e.printStackTrace();
		}
		
	}
    
    public static void randomTopology(String filePara)//edit model
	{
		DecimalFormat df = new DecimalFormat("#.##");
		BufferedWriter out;
		BufferedReader in;
		File file = new File(filePara);
        try {
			in = new BufferedReader(new FileReader(file));

			String strLine = in.readLine();
			int dataNo= Integer.parseInt(strLine);
			Vector<Vector<Double>> dataReal = new Vector<Vector<Double>>();
			for (int i=0;i<dataNo;i++)
			{
				//doc n hang tiep theo
				strLine = in.readLine();
				String[] _line = strLine.split(" ");
				Vector<Double> t= new Vector<Double>(4);
				for (int j=0;j <4;j++)
				{
					t.addElement(Double.parseDouble(_line[j]));
				}
				dataReal.addElement(t);
			}
			int temp=1;
			dataNo = dataNo *8;
			//Read File Line By Line			
			while ((strLine = in.readLine()) != null)   {
				String[] line= strLine.split(" ");			
				
				int n = Integer.parseInt(line[0]);
				int E = Integer.parseInt(line[1]);
				int noG= Integer.parseInt(line[2]);
				int m=Integer.parseInt(line[3]);
				MyGraph g = new MyGraph(n,dataReal);
				Function[] functionArr = new Function[m];
				Double[] hso_Func= {2.0,10.0,3.0};
			    for (int i=0;i< m;i++)
			       functionArr[i]= new Function(new Vector<Double>(Arrays.asList(hso_Func)),i+1);
			    for (int l=0;l<noG;l++)
			    {
			    	String fileName= "in"+(l+temp)+".txt";
			    	out= new BufferedWriter(new FileWriter(fileName));
					int s = Integer.parseInt(line[4+l]);
				    
					Demand[] demandArr= new Demand[s];
					for (int i=0;i<s;i++)
					{
						demandArr[i]= new Demand(i+1,-1,-1,-1,13,-1,functionArr,n,g); 
					}
				    //ghi ra file
				    out.write(m+" "+s + " "+ n + " "+ E);
				    out.newLine();
				    for (int i=0;i<m;i++)
				    {
			               for (int j=0;j<3;j++)
			            	   out.write(df.format(functionArr[i].getLamda().get(j))+" ");
			               out.write(";");
			       	}
				    out.newLine();
			       	for (int i=0;i<s;i++)
			       	{ 
			       		System.out.println("bandwidth: "+demandArr[i].bwS() );
			       		out.write(demandArr[i].idS() +" " +demandArr[i].sourceS() +" "+ demandArr[i].destinationS()+" "+ demandArr[i].bwS()+" "+demandArr[i].arrivalTimeS()+" ");
			       		out.write(demandArr[i].getRate()+" ");
			       		for (int j=0;j<demandArr[i].noF();j++)
			       			out.write(demandArr[i].getFunctions()[j].id() +" ");
			       		
			       		out.newLine();
			       	}
			       	out.write(df.format(g.getPriceBandwidth())+"");
			       	out.newLine();
			       	for (int i=0;i<n;i++)
			       	{		            
			       		for (int j=0;j<3;j++)
			            	   out.write(df.format(g.getCap(i+1).get(j))+" ");
			            out.write(";");
			            out.write(df.format(g.getPriceNode(i+1))+" ");
//			       		for (int j=0;j<3;j++)
//			            	   out.write(df.format(g.getPriceNode(i+1).get(j))+" ");
			            
			       		out.newLine();
			       	}
			       	for (int i=0;i<n;i++)
			       	{
			       		for (int j=0;j<n;j++)
			       			out.write(df.format(g.getEdgeWeight(i+1, j+1)) + " ");
			       		out.newLine();
			       	}
			       	out.close();
			    }
			    temp+=noG;
			    
			}

			//Close the input stream
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catsch block
			e.printStackTrace();
		}
		
	}
	
	public static void randomNewMode(String filePara)
	{
		DecimalFormat df = new DecimalFormat("#.##");
		BufferedWriter out;
		BufferedReader in;
		File file = new File(filePara);
        try {
			in = new BufferedReader(new FileReader(file));

			String strLine;
			int temp=1;
			//Read File Line By Line
			while ((strLine = in.readLine()) != null)   {
				String[] line= strLine.split(" ");			
				
				int n = Integer.parseInt(line[0]);
				int E = Integer.parseInt(line[1]);
				
				int noG= Integer.parseInt(line[2]);
				int m=Integer.parseInt(line[3]);
				double randomResource = Double.parseDouble(line[4]);
				MyGraph g = new MyGraph(n,E,randomResource);
				Function[] functionArr = new Function[m];
			    for (int i=0;i< m;i++)
			       functionArr[i]= new Function(i+1);
			    for (int l=0;l<noG;l++)
			    {
			    	String fileName= "in"+(l+temp)+".txt";
			    	out= new BufferedWriter(new FileWriter(fileName));
					int s = Integer.parseInt(line[5+l]);
				    
					Demand[] demandArr= new Demand[s];
					for (int i=0;i<s;i++)
					{
						demandArr[i]= new Demand(i+1,-1,-1,13,functionArr,-1,n); 
					}
				    //ghi ra file
				    out.write(m+" "+s + " "+ n + " "+ E);
				    out.newLine();
				    for (int i=0;i<m;i++)
			       	{	 
			               out.write(df.format(functionArr[i].bw())+" ");
			       	}
				    out.newLine();
			       	for (int i=0;i<s;i++)
			       	{ 
			       		System.out.println("bandwidth: "+demandArr[i].bwS() );
			       		out.write(demandArr[i].idS() +" " +demandArr[i].sourceS() +" "+ demandArr[i].destinationS()+" "+ demandArr[i].bwS()+" "+demandArr[i].arrivalTimeS()+" ");
			       		for (int j=0;j<demandArr[i].noF();j++)
			       			out.write(demandArr[i].getFunctions()[j].id() +" ");
			       		out.newLine();
			       	}
			       	out.write(df.format(g.getPriceBandwidth())+"");
			       	out.newLine();
			       	for (int i=0;i<n;i++)
			       	{
			       		out.write(df.format(g.getPriceForUnitNode(i+1)) + " " + df.format(g.getCapacity(i+1)));
			       		out.newLine();
			       	}
			       	for (int i=0;i<n;i++)
			       	{
			       		for (int j=0;j<n;j++)
			       			out.write(df.format(g.getEdgeWeight(i+1, j+1)) + " ");
			       		out.newLine();
			       	}
			       	out.close();
			    }
			    temp+=noG;
			    
			}

			//Close the input stream
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catsch block
			e.printStackTrace();
		}
		
	}
	public static void random3Topology(String filePara)//Fat tree, Bcube, VL2
	{

		int type = 0;
		int k0=4;
		int n0=2;
		int maxServer=0;
		int minServer=0;
		DecimalFormat df = new DecimalFormat("#.##");
		BufferedWriter out;
		BufferedReader in;
		File file = new File(filePara);
        try {
			in = new BufferedReader(new FileReader(file));

			String strLine = in.readLine();
			int dataNo= Integer.parseInt(strLine);
			Vector<Vector<Double>> dataReal = new Vector<Vector<Double>>();
			for (int i=0;i<dataNo;i++)
			{
				//doc n hang tiep theo
				strLine = in.readLine();
				String[] _line = strLine.split(" ");
				Vector<Double> t= new Vector<Double>(4);
				for (int j=0;j <4;j++)
				{
					t.addElement(Double.parseDouble(_line[j]));
				}
				dataReal.addElement(t);
			}
			int temp=1;
			dataNo = dataNo *8;
			//Read File Line By Line			
			while ((strLine = in.readLine()) != null)   {			

				String[] line= strLine.split(" ");			
				
				int n = Integer.parseInt(line[0]);
				int E = Integer.parseInt(line[1]);
				int noG= Integer.parseInt(line[2]);
				int m=Integer.parseInt(line[3]);
				MyGraph g = new MyGraph(type,k0,n0,dataReal);
				if(type==0|| type==1)//Fat tree
				{
					n= (k0*k0*k0+5*k0*k0)/4;
					minServer =5*k0*k0/4+1;
					maxServer= (k0*k0*k0+5*k0*k0)/4;
				}
				if(type==2)//Bcube
				{
					n=(int)Math.pow(n0, k0+1)+ (int)Math.pow(n0, k0)*(k0+1);
					minServer=1;
					maxServer =(int) Math.pow(n0, k0+1); 
				}
				
				Function[] functionArr = new Function[m];
				Double[] hso_Func= {6.0,50.0,20.0};
			    for (int i=0;i< m;i++)
			       functionArr[i]= new Function(new Vector<Double>(Arrays.asList(hso_Func)),i+1);
			    for (int l=0;l<noG;l++)
			    {
			    	String fileName= "data3TopoFat4\\in"+(l+temp)+".txt";
			    	out= new BufferedWriter(new FileWriter(fileName));
					int s = Integer.parseInt(line[4+l]);
				    
					Demand[] demandArr= new Demand[s];
					for (int i=0;i<s;i++)
					{
						demandArr[i]= new Demand(i+1,n,-1,functionArr,minServer,maxServer); 
					}
				    //ghi ra file
				    out.write(m+" "+s + " "+ n + " "+ E);
				    out.newLine();
				    for (int i=0;i<m;i++)
				    {
			               for (int j=0;j<3;j++)
			            	   out.write(df.format(functionArr[i].getLamda().get(j))+" ");
			               out.write(";");
			       	}
				    out.newLine();
			       	for (int i=0;i<s;i++)
			       	{ 
			       		System.out.println("bandwidth: "+demandArr[i].bwS() );
			       		out.write(demandArr[i].idS() +" " +demandArr[i].sourceS() +" "+ demandArr[i].destinationS()+" "+ demandArr[i].bwS()+" "+demandArr[i].arrivalTimeS()+" ");
			       		out.write(demandArr[i].getRate()+" ");
			       		for (int j=0;j<demandArr[i].noF();j++)
			       			out.write(demandArr[i].getFunctions()[j].id() +" ");
			       		
			       		out.newLine();
			       	}
			       	out.write(df.format(g.getPriceBandwidth())+"");
			       	out.newLine();
			       	for (int i=0;i<n;i++)
			       	{		            
			       		for (int j=0;j<3;j++)
			            	   out.write(df.format(g.getCap(i+1).get(j))+" ");
			            out.write(";");
			            out.write(df.format(g.getPriceNode(i+1))+" ");
//			       		for (int j=0;j<3;j++)
//			            	   out.write(df.format(g.getPriceNode(i+1).get(j))+" ");
			            
			       		out.newLine();
			       	}
			       	for (int i=0;i<n;i++)
			       	{
			       		for (int j=0;j<n;j++)
			       			out.write(df.format(g.getEdgeWeight(i+1, j+1)) + " ");
			       		out.newLine();
			       	}
			       	out.close();
			    }
			    temp+=noG;
			    
			}

			//Close the input stream
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	
	}
	public static void random1DemandNTopology(String filePara)
	{
		DecimalFormat df = new DecimalFormat("#.##");
		BufferedWriter out;
		BufferedReader in;
		File file = new File(filePara);
        try {
			in = new BufferedReader(new FileReader(file));

			String strLine = in.readLine();
			int dataNo= Integer.parseInt(strLine);
			Vector<Vector<Double>> dataReal = new Vector<Vector<Double>>();
			for (int i=0;i<dataNo;i++)
			{
				//doc n hang tiep theo
				strLine = in.readLine();
				String[] _line = strLine.split(" ");
				Vector<Double> t= new Vector<Double>(4);
				for (int j=0;j <4;j++)
				{
					t.addElement(Double.parseDouble(_line[j]));
				}
				dataReal.addElement(t);
			}
			int temp=1;
			dataNo = dataNo *8;
			//Read File Line By Line			
			while ((strLine = in.readLine()) != null)   {
				String[] line= strLine.split(" ");			
				
				int n = Integer.parseInt(line[0]);
				int s = Integer.parseInt(line[1]);
				int noG= Integer.parseInt(line[2]);
				int m=Integer.parseInt(line[3]);
				
				Function[] functionArr = new Function[m];
				Double[] hso_Func= {2.0,10.0,3.0};
			    for (int i=0;i< m;i++)
			       functionArr[i]= new Function(new Vector<Double>(Arrays.asList(hso_Func)),i+1);
			    Demand[] demandArr= new Demand[s];
			    int E=Integer.parseInt(line[4]);
			    MyGraph g = new MyGraph(n,E,dataReal);
				for (int i=0;i<s;i++)
				{
					demandArr[i]= new Demand(i+1,-1,-1,-1,13,-1,functionArr,n,g); 
				}
			    for (int l=0;l<noG;l++)
			    {
			    	String fileName= "in"+(l+temp)+".txt";
			    	out= new BufferedWriter(new FileWriter(fileName));
					int E1 = Integer.parseInt(line[5+l]);
					while (E1>0) {
			        	int v = UtilizeFunction.randInt(1, n);
			            int w = UtilizeFunction.randInt(1, n);            
			            if (v!=w)
			            {  	//random link_cost and link_bandwidth
			        		double b= UtilizeFunction.randomDouble(new Integer[] {430, 510,600,730,800,860, 852,900,1000,1600,2000 });
			            	if(g.addEdge1(v, w,b))
			            		E1--;
			            }
			        }
					
				    //ghi ra file
				    out.write(m+" "+s + " "+ n + " "+ E);
				    out.newLine();
				    for (int i=0;i<m;i++)
				    {
			               for (int j=0;j<3;j++)
			            	   out.write(df.format(functionArr[i].getLamda().get(j))+" ");
			               out.write(";");
			       	}
				    out.newLine();
			       	for (int i=0;i<s;i++)
			       	{ 
			       		System.out.println("bandwidth: "+demandArr[i].bwS() );
			       		out.write(demandArr[i].idS() +" " +demandArr[i].sourceS() +" "+ demandArr[i].destinationS()+" "+ demandArr[i].bwS()+" "+demandArr[i].arrivalTimeS()+" ");
			       		out.write(demandArr[i].getRate()+" ");
			       		for (int j=0;j<demandArr[i].noF();j++)
			       			out.write(demandArr[i].getFunctions()[j].id() +" ");
			       		
			       		out.newLine();
			       	}
			       	out.write(df.format(g.getPriceBandwidth())+"");
			       	out.newLine();
			       	for (int i=0;i<n;i++)
			       	{		            
			       		for (int j=0;j<3;j++)
			            	   out.write(df.format(g.getCap(i+1).get(j))+" ");
			            out.write(";");
			            out.write(df.format(g.getPriceNode(i+1))+" ");
//			       		for (int j=0;j<3;j++)
//			            	   out.write(df.format(g.getPriceNode(i+1).get(j))+" ");
			            
			       		out.newLine();
			       	}
			       	for (int i=0;i<n;i++)
			       	{
			       		for (int j=0;j<n;j++)
			       			out.write(df.format(g.getEdgeWeight(i+1, j+1)) + " ");
			       		out.newLine();
			       	}
			       	out.close();
			    }
			    temp+=noG;
			    
			}

			//Close the input stream
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catsch block
			e.printStackTrace();
		}
		
	}
	public static double randomDouble(Integer[] intArray)
	{
		//Integer[] intArray = new Integer[] { 100,150,200,400, 500 };
		
		ArrayList<Integer> asList = new ArrayList<Integer>(Arrays.asList(intArray));
		Collections.shuffle(asList);
		return Double.parseDouble(asList.get(0).toString());
	}
	public static void main()
	{
		//randomData("inputFile.txt");
		//randomData_lib("out.txt", 3, 5);
	}
}