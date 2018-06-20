import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

import org.apache.commons.io.FileUtils;

import gurobi.*;
import gurobi.GRB.IntParam;

import org.jgrapht.alg.*;
import org.jgrapht.graph.*;

import ilog.concert.*;
import ilog.cplex.*;

public class ExtendModel1 {
	static BufferedWriter out;
	static BufferedReader in;
	static int c,n,m,d,z,E,_no,noOldDemand;
	static double alpha, beta,gama,theta;
	static MyGraph g;
	static Function[] functionArr;
//	static Demand[] demandArr;
	static ArrayList<Demand> demandArray;
	static GRBVar[][][][] x;// d=1->d, a=1->n; b=0->m; c=1->position (n*m)
	static GRBVar[] y;
	static GRBVar[][][]x1;//function on node
	static GRBVar[][][] y1;//link 
	static GRBVar[][][] y2;//ancestor node
	static GRBVar[][][][] phi;
	static GRBVar[]z1;
	static GRBVar r_l,r_n;
	static long _duration=0;
	static double value_final=0.0;
	static double value_bandwidth=0.0;
	static double ultilize_resource =0.0;
	static double currentTime=0.0;
	static double maxNode =0;
	static Double[] zero ={0.0,0.0,0.0};
	static int prevNode;
	static double numberofCore=0;
	static double numberofEdge=0;
	static double numberofMidle=0;
	
	public static ArrayList<Integer> ShortestPath(int src, int dest, MyGraph _g,double maxBw)
	{
		ArrayList<Integer> _shortestPath = new ArrayList<Integer>();
		SimpleDirectedWeightedGraph<String, DefaultWeightedEdge>  g_i = new SimpleDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class); 
        
		for (int j=0;j<g.V();j++)
        {
        	g_i.addVertex("node"+(j+1));
        }
        //DefaultWeightedEdge[] e= new DefaultWeightedEdge[(g.getV()*(g.getV()-1))/2];
        //int id=0;        
        for (int j=0;j<g.V();j++)
        {	        	
        	for(int k=0;k<g.V();k++)
        	{
        		if(j!=k&&_g.getEdgeWeight(j+1, k+1)>maxBw)
        		{
        			DefaultWeightedEdge e=g_i.addEdge("node"+(j+1),"node"+(k+1));	        			
	        		g_i.setEdgeWeight(e, g.getEdgeWeight((j+1), (k+1)));
        		}
        	}
        }       
        List<DefaultWeightedEdge> _p =   DijkstraShortestPath.findPathBetween(g_i, "node"+src, "node"+dest);
        int source;
		if(_p!=null)
		{
			_shortestPath.add(src);
			source=src;
			while (_p.size()>0)
			{	
				int ix =0;
				for(int l=0;l<_p.size();l++)
				{
					int int_s =Integer.parseInt(g_i.getEdgeSource(_p.get(l)).replaceAll("[\\D]", ""));
					int int_t =Integer.parseInt(g_i.getEdgeTarget(_p.get(l)).replaceAll("[\\D]", ""));
					if( int_s == source )
					{
						_shortestPath.add(int_t);
						source = int_t;
						ix = l;
						//_g.setEdgeWeight(int_s, int_t, _g.getEdgeWeight(int_s, int_t)-maxBw);
						break;
					}
					if( int_t == source)
					{
						_shortestPath.add(int_s);
						source = int_s;
						ix = l;
						//_g.setEdgeWeight(int_s, int_t, _g.getEdgeWeight(int_s, int_t)-maxBw);
						break;
					}
				}
				_p.remove(ix);
			}
			for(int _i:_shortestPath)
				{
					System.out.print(_i+",");
				}						
		}
		else
		{
			System.out.print("khong tim duoc duong di giua "+src+" va "+ dest);
			return null;
			
		}
        
        
		return _shortestPath;
	}
	
	
	public static double getBwFunction(int id)
	{
		if(id==0) return 0;
		for(int i=0;i<m;i++)
			if (functionArr[i].id() ==id)
				return functionArr[i].bw();
		return -1;
	}
	public static Vector<Double> getLamdaF(int id)
	{
		if(id==0) return new Vector<Double>(Arrays.asList(zero));
		for(int i=0;i<m;i++)
			if (functionArr[i].id() ==id)
				return functionArr[i].getLamda();
		return null;
	}
	/**id is from 1 to m*/
	public static Function getFunction(int id)
	{
		if(id==0) return null;
		for(int i=0;i<m;i++)
			if (functionArr[i].id() ==id)
				return functionArr[i];
		return null;
	}
	
	public static double getBwService(int id)
	{
		if(id==0) return 0;
		for(int i=0;i<m;i++)
			if(demandArray.get(i).idS()==id)
				return demandArray.get(i).bwS();
		return -1;
	}
	public static double getRateService(int id)
	{
		if(id==0) return 0;
		for(int i=0;i<m;i++)
			if(demandArray.get(i).idS()==id)
				return demandArray.get(i).getRate();
		return -1;
	}
	/**id is from 1 to d*/
	public static Demand getDemand(int id)
	{
		for (int i=0;i<d;i++)
			if(demandArray.get(i).idS()==id)
				return demandArray.get(i);
		return null;
	}
	
	
	public static boolean IsCapacity()
	{
		Vector<Double> resourceRequirement = new Vector<Double>(Arrays.asList(zero));
		Vector<Double> resourceCapacity = new Vector<Double>(Arrays.asList(zero));
		for (int i=0;i<d;i++)
		{
			Function[] fArr = demandArray.get(i).getFunctions();
			for (int j=0;j<fArr.length;j++)
			{
				resourceRequirement = UtilizeFunction.add(resourceRequirement,fArr[j].getLamda());
			}
		}
		for (int i=0;i<n;i++)
			resourceCapacity = UtilizeFunction.add(resourceCapacity, g.getCap(i+1));
		if(UtilizeFunction.isBig(resourceRequirement, resourceCapacity))
			return false;
		return true;
	}
	
	public static void ReadInput(String fileName)
	{

		File file = new File(fileName);
		demandArray = new ArrayList<Demand>();
        try {
			in = new BufferedReader(new FileReader(file));
			String[] firstLine=in.readLine().split(" ");
			m= Integer.parseInt(firstLine[0]);
			d= Integer.parseInt(firstLine[1]);
			n= Integer.parseInt(firstLine[2]);
			E = Integer.parseInt(firstLine[3]);
			String[] line= new String[2*n+d+2];
			String thisLine=null;
			int k =0;
			while((thisLine = in.readLine()) !=null)
			{				
				line[k]=thisLine;
				k++;
			}
			functionArr= new Function[m];
			//demandArr = new Demand[d];
			
			//m function
			String[] lineFunc = line[0].split(";");
			for(int i = 0;i<m;i++)
			{ 
				Vector<Double> lamda= new Vector<>(3);
				for (int j=0;j<3;j++)
		        	lamda.addElement(Double.parseDouble(lineFunc[i].split(" ")[j]));
				functionArr[i]= new Function(i+1,lamda);
			}
			String[] tempLine;
			//d demand
			for (int i=0;i<d;i++)
			{
				tempLine = line[i+1].split(" ");
				Function[] f = new Function[tempLine.length-6];
				for (int j=0;j<f.length;j++)
					f[j]= getFunction(Integer.parseInt(tempLine[j+6]));
				Demand d_temp= new Demand(Integer.parseInt(tempLine[0]),Integer.parseInt(tempLine[1]),Integer.parseInt(tempLine[2]),Double.parseDouble(tempLine[3]),Double.parseDouble(tempLine[4]),Double.parseDouble(tempLine[5]),f,n,g);
				demandArray.add(d_temp);//				
			}
			//luu vao mang n+1 chieu
			Vector<Vector<Double>> cap = new Vector<Vector<Double>>(n+1);
			Vector<Double> pricePerNode = new Vector<Double>(n+1);
			double[][] w = new double[n+1][n+1];  			
			double price_bandwidth = Double.parseDouble(line[d+1]);		
			
			// virtual network
			Double[] zero ={0.0,0.0,0.0};
			cap.add(new Vector<Double>(Arrays.asList(zero)));
			pricePerNode.add(0.0);
			for (int i=0;i <n;i++)
			{
				tempLine =line[i+d+2].split(";");
				Vector<Double> t= new Vector<>(3);
	   	        for (int j=0;j<3;j++)
	   	        	t.addElement(Double.parseDouble(tempLine[0].split(" ")[j]));
	   	        cap.add(t);
	   	        pricePerNode.add(Double.parseDouble(tempLine[1]));
			}
			for (int i=0;i<n+1;i++)
			{
				w[i][0]=0.0;
				w[0][i]=0.0;
			}
			for (int i=1;i<n+1;i++)
			{
				tempLine = line[i+d+n+1].split(" ");
				for(int j=1;j<n+1;j++)
				{
					w[i][j] = Double.parseDouble(tempLine[j-1]);
				}
			}					
			g= new MyGraph(cap,pricePerNode,w,price_bandwidth);
			if (n*m <  m+4)
				_no=5;
			else
				_no = 5;
            in.close();  
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
	}
	public static void ReadInputFile(String fileName)
	{
		File file = new File(fileName);
		demandArray = new ArrayList<Demand>();
        try {
			in = new BufferedReader(new FileReader(file));
			String[] firstLine=in.readLine().split(" ");
			m= Integer.parseInt(firstLine[0]);
			d= Integer.parseInt(firstLine[1]);
			n= Integer.parseInt(firstLine[2]);
			E = Integer.parseInt(firstLine[3]);
			String[] line= new String[2*n+d+2];
			String thisLine=null;
			int k =0;
			while((thisLine = in.readLine()) !=null)
			{				
				line[k]=thisLine;
				k++;
			}
			functionArr= new Function[m];
			//demandArr = new Demand[d];
			
			//m function
			String[] lineFunc = line[0].split(";");
			for(int i = 0;i<m;i++)
			{ 
				Vector<Double> lamda= new Vector<>(3);
				for (int j=0;j<3;j++)
		        	lamda.addElement(Double.parseDouble(lineFunc[i].split(" ")[j]));
				functionArr[i]= new Function(i+1,lamda);
			}
			String[] tempLine;
			//d demand
			for (int i=0;i<d;i++)
			{
				tempLine = line[i+1].split(" ");
				Function[] f = new Function[tempLine.length-6];
				for (int j=0;j<f.length;j++)
					f[j]= getFunction(Integer.parseInt(tempLine[j+6]));
				Demand d_temp= new Demand(Integer.parseInt(tempLine[0]),Integer.parseInt(tempLine[1]),Integer.parseInt(tempLine[2]),Double.parseDouble(tempLine[3]),Double.parseDouble(tempLine[4]),Double.parseDouble(tempLine[5]),f,n,g);
				demandArray.add(d_temp);//				
			}
			//luu vao mang n+1 chieu
			Vector<Vector<Double>> cap = new Vector<Vector<Double>>(n+1);
			Vector<Double> pricePerNode = new Vector<Double>(n+1);
			double[][] w = new double[n+1][n+1];  			
			double price_bandwidth = Double.parseDouble(line[d+1]);		
			
			// virtual network
			Double[] zero ={0.0,0.0,0.0};
			cap.add(new Vector<Double>(Arrays.asList(zero)));
			pricePerNode.add(0.0);
			for (int i=0;i <n;i++)
			{
				tempLine =line[i+d+2].split(";");
				Vector<Double> t= new Vector<>(3);
	   	        for (int j=0;j<3;j++)
	   	        	t.addElement(Double.parseDouble(tempLine[0].split(" ")[j]));
	   	        cap.add(t);
	   	        pricePerNode.add(Double.parseDouble(tempLine[1]));
			}
			for (int i=0;i<n+1;i++)
			{
				w[i][0]=0.0;
				w[0][i]=0.0;
			}
			for (int i=1;i<n+1;i++)
			{
				tempLine = line[i+d+n+1].split(" ");
				for(int j=1;j<n+1;j++)
				{
					w[i][j] = Double.parseDouble(tempLine[j-1]);
				}
			}					
			g= new MyGraph(cap,pricePerNode,w,price_bandwidth);
			
//			for (int i=0;i<n;i++)
//				for(int j=0;j<n;j++)
//				{
//					ultilize_resource += g.getEdgeWeight(i+1, j+1) * g.getPriceBandwidth() ;
//				}
			if (n*m <  m+4)
				_no=5;
			else
				_no = 5;
			x= new GRBVar[d][n][m+1][_no]; 
			y= new GRBVar[n]; 
			x1= new GRBVar[d][m][n];//binary
			y1= new GRBVar[d][n][n];//float (0,1)
			y2= new GRBVar[d][n][n];//float (0,1)
			phi = new GRBVar[d][m+1][n][n];
			//z1= new GRBVar[d];//float (0,1)
			
			
            // Always close files.
            in.close();  
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	//heuristic
	static int[][] Dist;
	public static boolean _Dist()
	{
		SimpleWeightedGraph<String, DefaultWeightedEdge>  g_i = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class); 
		Dist = new int[g.V()+1][g.V()+1];
		for(int i=0;i<n+1;i++)
        	for (int j=0;j<n+1;j++)
        		Dist[i][j]=Integer.MAX_VALUE;
		for (int j=0;j<n;j++)
        {
        	g_i.addVertex("node"+(j+1));
        }
        DefaultWeightedEdge[] e= new DefaultWeightedEdge[(n*(n-1))/2];
        int id=0;
        
        for (int j=0;j<n-1;j++)
        {	        	
        	for(int k=j+1;k<n;k++)
        	{
        		e[id]=g_i.addEdge("node"+(j+1),"node"+(k+1));	        			
        		g_i.setEdgeWeight(e[id], g.getEdgeWeight((j +1), (k+1)));
        		id++;
        	}
        }
        for(int i=0;i<n-1;i++)
        	for (int j=i+1;j<n;j++)
        	{
        		List<DefaultWeightedEdge> _p =   DijkstraShortestPath.findPathBetween(g_i, "node"+(i+1), "node"+(j+1));
        		if(_p!=null)
        		{
        			Dist[i+1][j+1]=_p.size()+1;
        			Dist[j+1][i+1]=_p.size()+1;
        		}
        		else
        		{
        			Dist[i+1][j+1]=Integer.MAX_VALUE;
        			Dist[j+1][i+1]=Integer.MAX_VALUE;
        		}
        	} 
        return true;
        
	}
	
	static double functionCost=0.0;
	public static boolean heuristic(String outFile)//Max-Min algorithm
	{
		functionCost=0;
		numberofCore=0;
		numberofEdge=0;
		numberofMidle=0;
		value_final=0;
		value_bandwidth =0;
		ultilize_resource =0;
		_duration=0;
		for (int i=0;i<n;i++)
		{
			ultilize_resource +=UtilizeFunction.value(g.getCap(i+1)) * g.getPriceNode(i+1);				
		}
		final long startTime = System.currentTimeMillis();
		
		try {
			File file = new File(outFile);
			out = new BufferedWriter(new FileWriter(file));
		MyGraph g_temp=	new MyGraph(g.cap,g.pricePernode,g.link_bandwidth,g.getPriceBandwidth());
		//Graph g_temp= new Graph(g.r, g.K, g.link_bandwidth, g.getPriceBandwidth(),true);
		
		int[][] function_Loc = new int[m+1][n+1]; // if number of function f put on virtual node v
		
		double bw_min=getDemand(1).bwS();//gia tri nho nhat cua canh can duoc dap ung cho tat ca cac demand
		//double r_min =functionArr[0].bw();//gia tri nho nhat cua node can duoc dap ung cho tat ca cac function
		Vector<Double> r_min =functionArr[0].getLamda();
		int[] f_rank = new int[m];//rank function decrease
		double[] f_maxbw=new double[m+1];//max bandwidth of function in all services
		int[] no_Function= new int[m+1]; //number of function in all services
		
		List<Integer> nodeList;
		double weight_path=0;
		double min_w ;
		int source=0;
		int destination=0;		
		List<DefaultWeightedEdge> _p;
		
		ArrayList<Integer> srcArr = new ArrayList<Integer>();//chua tat ca cac source
		ArrayList<Integer> desArr= new ArrayList<Integer>();//chua tat ca cac destination
		for (int i=0;i<n;i++)
		{
			boolean _flagS=false;
			boolean _flagD=false;
			for (Demand d : demandArray) {
				
				if(!_flagS && d.sourceS()==(i+1))
				{
					srcArr.add(i+1);
					_flagS=true;
				}
				if(!_flagD && d.destinationS()==(i+1))
				{
					desArr.add(i+1);
					_flagD=true;
				}
			}
		}		
		//sap xep cac function theo thu tu giam dan tai nguyen yeu cau
		for (int i=0;i<m;i++)
			f_rank[i]= functionArr[i].id();
		for(int i=0;i<f_rank.length-1;i++)
		{
			int temp=i;
			for (int j=i+1;j<f_rank.length;j++)
				if(UtilizeFunction.isBig(functionArr[j].getLamda(), functionArr[temp].getLamda()))
					temp=j;
			int k= f_rank[i];
			f_rank[i]=f_rank[temp];
			f_rank[temp]=k;
		}
		
		//so luong function trong tat ca cac demand
		for (int i=0;i<m;i++)
		{
			no_Function[i+1]=0;
			f_maxbw[i+1]=0;
		}
		for (int i=0;i<m;i++)
			for (int j=0;j<d;j++)
			{
				Function[] arrF = getDemand(j+1).getFunctions();				
				for (int k=0;k<arrF.length;k++)
				{					
					if(arrF[k].id()==(i+1))
					{
						no_Function[i+1]+=1;
						if(getDemand(j+1).bwS()>f_maxbw[i+1])
							f_maxbw[i+1]=getDemand(j+1).bwS();
					}
				}
			}
		
		//gia tri nho nhat cua cac link tu cac demand, gia tri nho nhat cac requirement for function
		for (int i=0;i<d;i++)
			if(getDemand(i+1).bwS()< bw_min)
				bw_min = getDemand(i+1).bwS();
		for (int i=0;i<m;i++)
		{
			if(UtilizeFunction.bigger(r_min, functionArr[i].getLamda())==1)
				r_min =functionArr[i].getLamda();
		}
		//1. Remove node and edge is not feasible
		for (int i=0;i<n;i++)
		{
			if(UtilizeFunction.isBig(r_min, g_temp.getCap(i+1)))
				g_temp.removeNode(i+1);
			for (int j=0;j<n;j++)
				if(g_temp.getEdgeWeight(i+1, j+1)<bw_min)
					g_temp.removeLink(i+1, j+1);
		}
		
		//B1. Rank larger is put on virtual node smaller (price per resource unit)
		// Prior: same function put on 1 virtual if it has enough capacity
		for (int i=0;i<m;i++)
			for (int j=0;j<n;j++)
				function_Loc[i+1][j+1]=0;
		//sort virtual nodes increase depend on price per resource unit
		double[] x_add= new double[n+1];// gia tri thay doi sau moii lan gan
		for(int j=0;j<n;j++)
			x_add[j+1]=0.0;
		double y= 0.0;
		double sum_r =0.0;
		for (int k=0;k<n;k++)
			//sum_r+=UtilizeFunction.value(g_temp.getPriceNode(k+1));
			sum_r+=g_temp.getPriceNode(k+1);
		for(int i=0;i<m;i++)
		{
			
			Function f= getFunction(f_rank[i]);//kiem tra f co thuoc demand nao ko
			if(no_Function[f.id()]>0)
			{
				int[] indexFunction = new int[d];
				for (int j=0;j<d;j++)
				{
					//kiem tra function f co the thuoc demand nao? -1 neu thuoc day dau, 1 neu thuoc day sau. 0 neu ko thuoc
					Function[] arrFunction = getDemand(j+1).getFunctions();
					boolean flag=false;
					for (int k=0;k<arrFunction.length;k++)
					{
						if(arrFunction[k].id()==f.id())
						{
							if(k<arrFunction.length/2)
								indexFunction[j]=-1;
							else
								indexFunction[j]=1;
							flag=true;
							break;
						}
					}
					if(flag==false)
						indexFunction[j]=0;
				}
				int temp_noF = no_Function[f.id()];
				
				//Each function in arrayFunction
				double mauso=0;
				for (int j=0;j<d;j++)
				{
					if(indexFunction[j]!=0)
					{
					for (int k=0;k<n;k++)
					{
						mauso+=Dist[getDemand(j+1).sourceS()][k+1];
						mauso+=Dist[getDemand(j+1).destinationS()][k+1];
					}
					}
				}
				double[] rank_value = new double[n+1];// function to compute rank for each virtual node
				for (int k=0;k<n;k++)
					rank_value[k+1]=0.0;
				while (temp_noF>0)
				{	
					for (int k=0;k<n;k++)
					{
						if(function_Loc[f.id()][k+1]>0)
						{
							rank_value[k+1]=theta/function_Loc[f.id()][k+1];
						}
						else
							rank_value[k+1]=0.0;
					}
					double sum_w = 0.0;				
					for (int k=0;k<n-1;k++)
						for (int j=k+1;j<n;j++)
							sum_w+=g_temp.getEdgeWeight(k+1, j+1) ;
					sum_w= 2*sum_w -y;//tru di mot luong
					for (int k=0;k<n;k++)
					{
						rank_value[k+1]=0.0;
						if(g_temp.getExistNode(k+1))
						{
							//rank_node[i]= i+1;
							rank_value[k+1]+=alpha * g_temp.getPriceNode(k+1)/sum_r;
							double temp_bw= 0.0;
							for (int j=0;j<n;j++)
							{
								if(g_temp.getEdgeWeight(k+1, j+1)>0)
									temp_bw +=g_temp.getEdgeWeight(k+1, j+1) ;
							}
							temp_bw = temp_bw - x_add[k+1];
	//						if(sum_w >0)
	//						rank_value[k+1]+=beta*(1-temp_bw/(2*sum_w));
	//						else
	//						{
	//							rank_value[k+1]=Double.MAX_VALUE;
	//						}
							if(temp_bw >0)
								rank_value[k+1]+=beta*sum_w/temp_bw;
							else
							{
								rank_value[k+1]=Double.MAX_VALUE;
							}
							double tuso=0;
							for (int j=0;j<d;j++)
							{
								if (indexFunction[j]!=0)
								{
									tuso+=Dist[getDemand(j+1).sourceS()][k+1];
									tuso+=Dist[getDemand(j+1).destinationS()][k+1];
								}
							}
							if(mauso!=0)
								rank_value[k+1]+=gama*tuso/mauso;
							else
								rank_value[k+1]=Double.MAX_VALUE;
	//						for (int d : srcArr) {
	//							if(d==(k+1))
	//								rank_value[k+1]+=gama;
	//						}
	//						for (int d : desArr) {
	//							if(d==(k+1))
	//								rank_value[k+1]+=gama;
	//						}
							
						}
						else
							rank_value[k+1]=Double.MAX_VALUE;
					}
					// find node: rank_value min
					double min_rank=Double.MAX_VALUE;
					int v_min = -1;
					for (int k=0;k<n;k++)
					{
						//kiem tra them f1 và f2 có ton tai duong di ko
						if(UtilizeFunction.isBig(g_temp.getCap(k+1), f.getLamda()) && rank_value[k+1]<min_rank)
						{
							min_rank = rank_value[k+1];
							v_min = k+1;
						}
					}
					if(v_min==-1)
					{
						//khong the tim dc v_min -> Bai toan khong co loi giai
						out.write("khong tim dc v_min cho f"+f.id());
						out.newLine();
						return false;
					}
					function_Loc[f.id()][v_min]+=1;
					g_temp.setCap(v_min, UtilizeFunction.minus(g_temp.getCap(v_min), f.getLamda()));
					if(UtilizeFunction.isBig(r_min, g_temp.getCap(v_min)))
						g_temp.removeNode(v_min);
					temp_noF--;
					//x_add[v_min] += f_maxbw[f.id()]*g_temp.getPriceBandwidth() ;
					x_add[v_min] += f_maxbw[f.id()] ;
					y+= x_add[v_min];
				}
			}
		}
		//in ra file =======
		for (int i=0;i<m;i++)
			for (int j=0;j<n;j++)
			{
				if(function_Loc[i+1][j+1]>0)
				{
					value_final+= (1-function_Loc[i+1][j+1])*Gain(g.getPriceNode(j+1));
					functionCost+=g.getPriceNode(j+1)*function_Loc[i+1][j+1];
					//val_final1+=UtilizeFunction.value(g.getPriceNode(j+1))*UtilizeFunction.value(functionArr[i].getLamda());
					System.out.println("function:"+(i+1)+ " node: "+(j+1)+" quantity: "+ function_Loc[i+1][j+1]);
					out.write("function:"+(i+1)+ " node: "+(j+1)+" quantity: "+ function_Loc[i+1][j+1]);
					out.newLine();
				}
			}
		out.write("Cost for function: "+ functionCost);
		out.newLine();
//		for(int i=0;i<m;i++)
//			for(int j=0;j<5;j++)
//				if(function_Loc[i+1][j+1]>0)
//					numberofCore+=function_Loc[i+1][j+1];
//		for(int i=0;i<m;i++)
//			for(int j=5;j<15;j++)
//				if(function_Loc[i+1][j+1]>0)
//					numberofMidle+=function_Loc[i+1][j+1];
//		for(int i=0;i<m;i++)
//			for(int j=15;j<n;j++)
//				if(function_Loc[i+1][j+1]>0)
//					numberofEdge+=function_Loc[i+1][j+1];
		//value_final=val_final1;
		//B2. Find path for all demand
		//sort demand depend on decrease bandwidth
		int[] rank_service= new int[d];
		if(d>1)
		{
			for (int i=0;i<d;i++)
				rank_service[i]= i+1;
			for(int i=0;i<d-1;i++)
			{
				int temp=i;
				for (int j=i+1;j<d;j++)
					if(getDemand(rank_service[j]).bwS()<getDemand(rank_service[temp]).bwS())
						temp=j;
				int k= rank_service[i];
				rank_service[i]=rank_service[temp];
				rank_service[temp]=k;
			}
		}
		else
		{
			rank_service[0]=1;
		}
		
        int i=0;
		while(i<d)
		{
		
			Demand _d= getDemand(rank_service[i]);
			
			//remove edges which haven't enough capacity
			SimpleDirectedWeightedGraph<String, DefaultWeightedEdge>  g_i = new SimpleDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class); 
	        
			for (int j=0;j<n;j++)
	        {
	        	g_i.addVertex("node"+(j+1));
	        }
	        
	        
	        for (int j=0;j<n;j++)
	        {	        	
	        	for(int k=0;k<n;k++)
	        	{
	        		if(j!=k && g_temp.getEdgeWeight((j +1), (k+1))>_d.bwS())
	        		{
	        			DefaultWeightedEdge e=g_i.addEdge("node"+(j+1),"node"+(k+1));	        			
		        		g_i.setEdgeWeight(e, g.getEdgeWeight((j +1), (k+1)));
	        		}
	        		
	        	}
	        }
			
			
			Function[] fs= _d.getFunctions();//xet demand theo bandwidth
			
			List<List<Integer>> node = new ArrayList<List<Integer>>(m);
			for (int j=0;j<fs.length;j++)
			{
				List<Integer> innerList = new ArrayList<Integer>();
				for (int k=0;k<n;k++)
					for(int h=0;h<function_Loc[fs[j].id()][k+1];h++)
						innerList.add(k+1);
				node.add(innerList);				  
			}
			List<List<Integer>> shortest_tree = new ArrayList<List<Integer>>();// mang cac node cho moi duong di tu d den t
			List<Double> weight = new ArrayList<>();//gia tri cho moi duong di do
			
			System.out.println("demand: "+_d.idS());
			out.write("demand: "+ _d.idS());
			out.newLine();
			if(fs.length==1)
			{
				boolean fl=false;
				List<Integer> _node = node.get(0);
				if (_node.contains(_d.sourceS()) )
				{
					//giam trong node
					node.set(0,new ArrayList<Integer>(Arrays.asList(_d.sourceS())));
					function_Loc[fs[0].id()][_d.sourceS()]--;
					ultilize_resource-= g.getPriceNode(_d.sourceS())*UtilizeFunction.value(fs[0].getLamda());					
					//value_final+=_d.getRate()*g.getPriceNode(_d.sourceS());
					value_final+=g.getPriceNode(_d.sourceS());
					
					fl=true;
				}
				else
				{
					if( _node.contains(_d.destinationS()))
					{
						//giam trong node
						node.set(0,new ArrayList<Integer>(Arrays.asList(_d.destinationS())));
						function_Loc[fs[0].id()][_d.destinationS()]--;
						ultilize_resource-= g.getPriceNode(_d.destinationS())*UtilizeFunction.value(fs[0].getLamda());					
						value_final+=g.getPriceNode(_d.destinationS());
						//value_final+=_d.getRate()*g.getPriceNode(_d.destinationS());
						fl=true;
					}	
				}
				
				if(fl)
				{					//thuc hien tim duong di giua source va destinaion
					//DefaultWeightedEdge[] removed_edge = new DefaultWeightedEdge[id];
					ArrayList<DefaultWeightedEdge> removed_edge = new ArrayList<>();
					for( DefaultWeightedEdge v:g_i.edgeSet())
					{
						int int_s =Integer.parseInt(g_i.getEdgeSource(v).replaceAll("[\\D]", ""));
						int int_t =Integer.parseInt(g_i.getEdgeTarget(v).replaceAll("[\\D]", ""));
						if(g_temp.getEdgeWeight(int_s, int_t)<_d.bwS())
						//if(g_i.getEdgeWeight(v)<_d.bwS())
							removed_edge.add(v);
							//removed_edge[no_removed_edge++]=v;				
					}
					for (DefaultWeightedEdge v: removed_edge)
						g_i.removeEdge(v);
					_p =   DijkstraShortestPath.findPathBetween(g_i, "node"+_d.sourceS(), "node"+_d.destinationS());
					if(_p!=null)
					{
						for (DefaultWeightedEdge l:_p)
						{
							weight_path+=g_i.getEdgeWeight(l);
						}
						nodeList = new ArrayList<Integer>();
						nodeList.add(_d.sourceS());
						source=_d.sourceS();
						while (_p.size()>0)
						{	
							int ix =0;
							for(int l=0;l<_p.size();l++)
							{
								int int_s =Integer.parseInt(g_i.getEdgeSource(_p.get(l)).replaceAll("[\\D]", ""));
								int int_t =Integer.parseInt(g_i.getEdgeTarget(_p.get(l)).replaceAll("[\\D]", ""));
								value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;
								value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;	
								if( int_s == source )
								{
									nodeList.add(int_t);
									source = int_t;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
								if( int_t == source)
								{
									nodeList.add(int_s);
									source = int_s;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
							}
							_p.remove(ix);
						}
						for(int _i:nodeList)
							{
								System.out.print(_i+",");
								out.write(_i+", ");
							}	
						out.newLine();						
					}
					else
					{
						System.out.print("1....");
						out.write("khong tim duojc duong di giua:"+ _d.sourceS() +" va "+_d.destinationS());
						out.newLine();
						return false;
						
					}
				}
				else
				{
					//neu khong thi tim duong di tu source -> node, và từ node->destination
					int n_max= -1;
					double n_price_max = 0.0;
					for (int _intMax: _node)
						if(n_price_max < g_temp.getPriceNode(_intMax))
						{
							n_price_max = g_temp.getPriceNode(_intMax);
							n_max= _intMax;
						}
					function_Loc[fs[0].id()][n_max]--;
					node.set(0,new ArrayList<Integer>(Arrays.asList(n_max)));
					ultilize_resource-= g.getPriceNode(n_max)*UtilizeFunction.value(fs[0].getLamda());					
					
					value_final+=g.getPriceNode(n_max);
					//value_final+=_d.getRate()*g.getPriceNode(n_max);
					ArrayList<DefaultWeightedEdge> removed_edge = new ArrayList<>();
					for( DefaultWeightedEdge v:g_i.edgeSet())
					{
						int int_s =Integer.parseInt(g_i.getEdgeSource(v).replaceAll("[\\D]", ""));
						int int_t =Integer.parseInt(g_i.getEdgeTarget(v).replaceAll("[\\D]", ""));
						if(g_temp.getEdgeWeight(int_s, int_t)<_d.bwS())
						//if(g_i.getEdgeWeight(v)<_d.bwS())
							removed_edge.add(v);
							//removed_edge[no_removed_edge++]=v;				
					}
					for (DefaultWeightedEdge v: removed_edge)
						g_i.removeEdge(v);
					_p =   DijkstraShortestPath.findPathBetween(g_i, "node"+_d.sourceS(), "node"+n_max);
					if(_p!=null)
					{
						for (DefaultWeightedEdge l:_p)
						{
							weight_path+=g_i.getEdgeWeight(l);
						}
						nodeList = new ArrayList<Integer>();
						nodeList.add(_d.sourceS());
						source=_d.sourceS();
						while (_p.size()>0)
						{	
							int ix =0;
							for(int l=0;l<_p.size();l++)
							{
								int int_s =Integer.parseInt(g_i.getEdgeSource(_p.get(l)).replaceAll("[\\D]", ""));
								int int_t =Integer.parseInt(g_i.getEdgeTarget(_p.get(l)).replaceAll("[\\D]", ""));
								value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth()*0.0001 ;
								value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;	
								if( int_s == source )
								{
									nodeList.add(int_t);
									source = int_t;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
								if( int_t == source)
								{
									nodeList.add(int_s);
									source = int_s;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
							}
							_p.remove(ix);
						}
						for(int _i:nodeList)
							{
								System.out.print(_i+",");
								out.write(_i+", ");
							}	
						out.newLine();
					}
					else
					{
						out.write("khong tim duojc duong di giua:"+ _d.sourceS() +" va "+n_max);
						out.newLine();
						return false;
						
					}
					removed_edge = new ArrayList<>();
					for( DefaultWeightedEdge v:g_i.edgeSet())
					{
						int int_s =Integer.parseInt(g_i.getEdgeSource(v).replaceAll("[\\D]", ""));
						int int_t =Integer.parseInt(g_i.getEdgeTarget(v).replaceAll("[\\D]", ""));
						if(g_temp.getEdgeWeight(int_s, int_t)<_d.bwS())
						//if(g_i.getEdgeWeight(v)<_d.bwS())
							removed_edge.add(v);
							//removed_edge[no_removed_edge++]=v;				
					}
					for (DefaultWeightedEdge v: removed_edge)
						g_i.removeEdge(v);
					_p =   DijkstraShortestPath.findPathBetween(g_i, "node"+n_max, "node"+_d.destinationS());
					if(_p!=null)
					{
						for (DefaultWeightedEdge l:_p)
						{
							weight_path+=g_i.getEdgeWeight(l);
						}
						nodeList = new ArrayList<Integer>();
						nodeList.add(n_max);
						source=n_max;
						while (_p.size()>0)
						{	
							int ix =0;
							for(int l=0;l<_p.size();l++)
							{
								int int_s =Integer.parseInt(g_i.getEdgeSource(_p.get(l)).replaceAll("[\\D]", ""));
								int int_t =Integer.parseInt(g_i.getEdgeTarget(_p.get(l)).replaceAll("[\\D]", ""));
								value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;
								value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth()*0.0001 ;	
								if( int_s == source )
								{
									nodeList.add(int_t);
									source = int_t;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
								if( int_t == source)
								{
									nodeList.add(int_s);
									source = int_s;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
							}
							_p.remove(ix);
						}
					
						for(int _i:nodeList)
						{
							System.out.print(_i+",");
							out.write(_i+", ");
						}	
					out.newLine();	
				
				}
					else
					{
						out.write("khong tim duojc duong di giua:"+ _d.destinationS() +" va "+n_max);
						out.newLine();
						return false;
						
					}

			}
			}
			else
			{
				//xet truong hop source + nguon + thanh phan thu nhat
				
				ArrayList<DefaultWeightedEdge> removed_edge = new ArrayList<>();
				for( DefaultWeightedEdge v:g_i.edgeSet())
				{
					int int_s =Integer.parseInt(g_i.getEdgeSource(v).replaceAll("[\\D]", ""));
					int int_t =Integer.parseInt(g_i.getEdgeTarget(v).replaceAll("[\\D]", ""));
					if(g_temp.getEdgeWeight(int_s, int_t)<_d.bwS())
					//if(g_i.getEdgeWeight(v)<_d.bwS())
						removed_edge.add(v);
						//removed_edge[no_removed_edge++]=v;				
				}
				for (DefaultWeightedEdge v: removed_edge)
					g_i.removeEdge(v);
				
				List<Integer> n_s = node.get(0);
				min_w = Double.MAX_VALUE;
				List<DefaultWeightedEdge> sht_path_temp=null;
				boolean _isSrc=false;
				for (int k=0;k<n_s.size();k++)
				{
					if(n_s.get(k)==_d.sourceS())
					{
						node.set(0,new ArrayList<Integer>(Arrays.asList(_d.sourceS())));
						//value_final+=UtilizeFunction.value(g.getPriceNode(_d.sourceS()))*UtilizeFunction.value(fs[0].getLamda());
						//function_Loc[d][_d.sourceS()]--;
						_isSrc =true;
						break;
					}
				}
				if(!_isSrc)
				{
					for (int k=0;k<n_s.size();k++)
					{
					//tinh duong di tu  (source->n_s.get(k))
						weight_path=0;
						_p =   DijkstraShortestPath.findPathBetween(g_i, "node"+_d.sourceS(), "node"+n_s.get(k));
						if(_p!=null)
						{
							for (DefaultWeightedEdge l:_p)
							{
								weight_path+=g_i.getEdgeWeight(l);
							}
							if(weight_path < min_w)
							{
								min_w = weight_path;
								sht_path_temp = new ArrayList<DefaultWeightedEdge>();
								for (DefaultWeightedEdge l:_p)
								{
									sht_path_temp.add(l);
								}
								source=_d.sourceS();
								destination = n_s.get(k);
							}
						}
					}
					
					nodeList = new ArrayList<Integer>();
					// sau do chon duong ngan nhat 
					if(sht_path_temp!=null)
					{
						prevNode=source;
						nodeList.add(source);
						while (sht_path_temp.size()>0)
						{	
							int ix =0;
							for(int l=0;l<sht_path_temp.size();l++)
							{
								int int_s =Integer.parseInt(g_i.getEdgeSource(sht_path_temp.get(l)).replaceAll("[\\D]", ""));
								int int_t =Integer.parseInt(g_i.getEdgeTarget(sht_path_temp.get(l)).replaceAll("[\\D]", ""));
								value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth()*0.0001 ;
								value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;
								if( int_s == source )
								{
									nodeList.add(int_t);
									source = int_t;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
								if( int_t == source)
								{
									nodeList.add(int_s);
									source = int_s;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
							}
							sht_path_temp.remove(ix);
						}
					}
					else
					{
						out.write("khong tim duojc duong di giua:"+ _d.sourceS() +" va f"+fs[0].id());
						out.newLine();
						return false;
						
					}
					weight.add(min_w);
					shortest_tree.add(nodeList);
					node.set(0,new ArrayList<Integer>(Arrays.asList(destination)));	
					//value_final+=UtilizeFunction.value(g.getPriceNode(destination))*UtilizeFunction.value(fs[0].getLamda());
					//function_Loc[fs[0].id()][destination]--;
					}				
				
					for(int j=0;j<fs.length-1;j++)
					{
						//Tim duong di ngan nhat giua tung cap (i, i+1) trong fs
						//xet xem cap nao co duong di ngan nhat
						removed_edge = new ArrayList<>();
						for( DefaultWeightedEdge v:g_i.edgeSet())
						{
							int int_s =Integer.parseInt(g_i.getEdgeSource(v).replaceAll("[\\D]", ""));
							int int_t =Integer.parseInt(g_i.getEdgeTarget(v).replaceAll("[\\D]", ""));
							if(g_temp.getEdgeWeight(int_s, int_t)<_d.bwS())
							//if(g_i.getEdgeWeight(v)<_d.bwS())
								removed_edge.add(v);
								//removed_edge[no_removed_edge++]=v;				
						}
						for (DefaultWeightedEdge v: removed_edge)
							g_i.removeEdge(v);
						int t1=fs[j].id();
						int t2=fs[j+1].id();
						List<Integer> node_s = node.get(j);
						List<Integer> node_t = node.get(j+1);
						double min_weight = Double.MAX_VALUE;
						List<DefaultWeightedEdge> shortest_path_temp=null;
						boolean temflag=false;
						for (int k=0;k<node_s.size();k++)
						{
							int temp_s=node_s.get(k);
							for(int h=0;h<node_t.size();h++)
							{
								int temp_t=node_t.get(h);
								if(temp_t==temp_s)
								{
								//truong hop 2 function nằm trong 1 node
									min_weight=0;
									source=temp_s;
									destination = temp_t;
									break;
								}
								else
								{
									if(temp_t!=prevNode)
									{
										temflag=true;
										prevNode=temp_t;
									weight_path=0;
									_p =   DijkstraShortestPath.findPathBetween(g_i, "node"+temp_s, "node"+temp_t);
									if(_p!=null)
									{
									for (DefaultWeightedEdge l:_p)
									{
										weight_path+=g_i.getEdgeWeight(l);
									}
									if(weight_path < min_weight)
									{
										//chon duong nay
										min_weight = weight_path;
										shortest_path_temp = new ArrayList<DefaultWeightedEdge>();
										for (DefaultWeightedEdge l:_p)
										{
											shortest_path_temp.add(l);
										}
										source=temp_s;
										destination = temp_t;
									}
									}
									}
								}
							}
						}
						if(shortest_path_temp==null && !temflag)
						{
							for (int k=0;k<node_s.size();k++)
							{
								int temp_s=node_s.get(k);
								for(int h=0;h<node_t.size();h++)
								{
									int temp_t=node_t.get(h);
									weight_path=0;
									_p =   DijkstraShortestPath.findPathBetween(g_i, "node"+temp_s, "node"+temp_t);
									if(_p!=null)
									{
									for (DefaultWeightedEdge l:_p)
									{
										weight_path+=g_i.getEdgeWeight(l);
									}
									if(weight_path < min_weight)
									{
										//chon duong nay
										min_weight = weight_path;
										shortest_path_temp = new ArrayList<DefaultWeightedEdge>();
										for (DefaultWeightedEdge l:_p)
										{
											shortest_path_temp.add(l);
										}
										source=temp_s;
										destination = temp_t;
									}
								}
							}
						}
					}
					nodeList = new ArrayList<Integer>();
					if(min_weight==0)
					{
						nodeList.add(source);
						nodeList.add(destination);
						weight.add(min_weight);
						shortest_tree.add(nodeList);
						node.set(j+1, new ArrayList<Integer>(Arrays.asList(destination)));
						//value_final+=UtilizeFunction.value(g.getPriceNode(destination))*UtilizeFunction.value(fs[j+1].getLamda());
					}
					else
					{
						if(shortest_path_temp!=null)
						{
							nodeList.add(source);
							while (shortest_path_temp.size()>0)
							{	
								int ix =0;
								for(int l=0;l<shortest_path_temp.size();l++)
								{
									
									int int_s =Integer.parseInt(g_i.getEdgeSource(shortest_path_temp.get(l)).replaceAll("[\\D]", ""));
									int int_t =Integer.parseInt(g_i.getEdgeTarget(shortest_path_temp.get(l)).replaceAll("[\\D]", ""));
									value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth()*0.0001 ;
									value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth()*0.0001;
									if( int_s == source )
									{
										nodeList.add(int_t);
										source = int_t;
										ix = l;
										//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
										//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
										g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
										break;
									}
									if( int_t == source)
									{
										nodeList.add(int_s);
										source = int_s;
										ix = l;
										//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
										//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
										g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
										break;
									}
								}
								shortest_path_temp.remove(ix);
							}
						}
							else
							{
								out.write("khong tim duojc duong di giua:f"+ t1 +" va f"+t2);
								out.newLine();
								return false;
								
							}
					//nodeList.add(destination);
					weight.add(min_weight);
					shortest_tree.add(nodeList);
					node.set(j+1, new ArrayList<Integer>(Arrays.asList(destination)));
					
					}
					source= nodeList.get(0);
					function_Loc[t1][source]--;
					//value_final+=_d.getRate()*g.getPriceNode(source);
					value_final+=g.getPriceNode(source);
					
					ultilize_resource-= g.getPriceNode(source);
					//ultilize_resource-= g.getPriceNode(source)*UtilizeFunction.value(fs[j].getLamda());
					if(j==fs.length-2)
					{
						destination = nodeList.get(nodeList.size()-1);
						function_Loc[t2][destination]--;
						//value_final+=_d.getRate()*g.getPriceNode(destination);
						value_final+=g.getPriceNode(destination);
						//value_final+=_d.getRate()*g.getPriceNode(destination)*UtilizeFunction.value(fs[j+1].getLamda());
						ultilize_resource-= g.getPriceNode(destination)*UtilizeFunction.value(fs[j+1].getLamda());
					}
				}
				
				removed_edge = new ArrayList<>();
					for( DefaultWeightedEdge v:g_i.edgeSet())
					{
						int int_s =Integer.parseInt(g_i.getEdgeSource(v).replaceAll("[\\D]", ""));
						int int_t =Integer.parseInt(g_i.getEdgeTarget(v).replaceAll("[\\D]", ""));
						if(g_temp.getEdgeWeight(int_s, int_t)<_d.bwS())
						//if(g_i.getEdgeWeight(v)<_d.bwS())
							removed_edge.add(v);
							//removed_edge[no_removed_edge++]=v;				
					}
					for (DefaultWeightedEdge v: removed_edge)
						g_i.removeEdge(v);
				
				n_s = node.get(fs.length-1);
				min_w = Double.MAX_VALUE;
				sht_path_temp=null;
				boolean _isDes=false;
				for (int k=0;k<n_s.size();k++)
				{
					if(n_s.get(k)==_d.destinationS())
					{
						_isDes =true;
						break;
					}
					else
					{
						//tinh duong di tu  (n_s.get(k)->destination)
						weight_path=0;
						_p =   DijkstraShortestPath.findPathBetween(g_i,"node"+n_s.get(k), "node"+_d.destinationS());
						if(_p!=null)
						{
						for (DefaultWeightedEdge l:_p)
						{
							weight_path+=g_i.getEdgeWeight(l);
						}
						if(weight_path < min_w)
						{
							//chon duong nay
							min_w = weight_path;
							sht_path_temp = new ArrayList<DefaultWeightedEdge>();
							for (DefaultWeightedEdge l:_p)
							{
								sht_path_temp.add(l);
							}
							source=n_s.get(k);
							destination = _d.destinationS();
						}
						}
					}
					
				}
				if(!_isDes)
				{
					nodeList = new ArrayList<Integer>();
					// sau do chon duong ngan nhat 
					if(sht_path_temp!=null)
					{
						nodeList.add(source);
						while (sht_path_temp.size()>0)
						{	
							int ix =0;
							for(int l=0;l<sht_path_temp.size();l++)
							{
								//tinh gia tri cua duong di o day
								
								int int_s =Integer.parseInt(g_i.getEdgeSource(sht_path_temp.get(l)).replaceAll("[\\D]", ""));
								int int_t =Integer.parseInt(g_i.getEdgeTarget(sht_path_temp.get(l)).replaceAll("[\\D]", ""));
								value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;
								value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth()*0.0001 ;
								if( int_s == source )
								{
									nodeList.add(int_t);
									source = int_t;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
								if( int_t == source)
								{
									nodeList.add(int_s);
									source = int_s;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
							}
							sht_path_temp.remove(ix);
						}
					}
					else
					{
						out.write("khong tim duojc duong di giua:f"+ fs[fs.length-1].id() +" va destination"+_d.destinationS());
						out.newLine();
						return false;
					}
					weight.add(min_w);
					shortest_tree.add(nodeList);
					node.set(0,new ArrayList<Integer>(Arrays.asList(destination)));				
					//function_Loc[d][destination]--;
				}	
			
				for(List<Integer> _list:shortest_tree)
				{
					out.write("[");
					for(int _i:_list)
					{
						System.out.print(_i+",");
						out.write(_i+", ");
					}
					System.out.println();
					out.write("], ");
				}			
				out.newLine();
//				double _min=0.0;
//				for(double _w:weight)
//					_min+=_w;
//				value_bandwidth +=_min * g.getPriceBandwidth() ;
//				value_final +=_min * g.getPriceBandwidth() ;
				//value_final +=_min;
			}
			i++;
		}
		for (int j=0;j<n;j++)
			for (int k=0;k<n;k++)
			{
				ultilize_resource += g_temp.getEdgeWeight(j+1, k+1)*g_temp.getPriceBandwidth() ;
			}
		_duration = System.currentTimeMillis() - startTime;
		System.out.println(_duration);
		out.write("Value solution: "+ value_final);
		out.newLine();
		out.write("Runtime (mS): "+ _duration);
		out.write("Value Bandwidth: "+ value_bandwidth);
		out.write("Resource ultilization: "+ ultilize_resource);
		
		} catch ( IOException e1 ) {
			e1.printStackTrace();
			} finally {
				if ( out != null )
					try {
						out.close();
						} catch (IOException e) {
							e.printStackTrace();}
				}    
	try {
  		out.close();
  		} catch (IOException e2) {
  			e2.printStackTrace();
  			}
	return true;
	}
	
	public static boolean nonNFV(String outFile)
	{
		value_final=0;
		value_bandwidth=0;
		ultilize_resource=0;
		_duration=0;
		for (int i=0;i<n;i++)
		{
			ultilize_resource +=UtilizeFunction.value(g.getCap(i+1)) * g.getPriceNode(i+1);				
		}
		List<DefaultWeightedEdge> _p;
		List<Integer> nodeList;
		final long startTime = System.currentTimeMillis();
		
		try {
			File file = new File(outFile);
			out = new BufferedWriter(new FileWriter(file));
			int[] rank_service= new int[d];
			if(d>1)
			{
				for (int i=0;i<d;i++)
					rank_service[i]= i+1;
				for(int i=0;i<d-1;i++)
				{
					int temp=i;
					for (int j=i+1;j<d;j++)
						if(getDemand(rank_service[j]).bwS()>getDemand(rank_service[temp]).bwS())
							temp=j;
					int k= rank_service[i];
					rank_service[i]=rank_service[temp];
					rank_service[temp]=k;
				}
			}
			else
			{
				rank_service[0]=1;
			}
			SimpleWeightedGraph<String, DefaultWeightedEdge>  g_i = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class); 
	        for (int j=0;j<n;j++)
	        {
	        	g_i.addVertex("node"+(j+1));
	        }
	        DefaultWeightedEdge[] e= new DefaultWeightedEdge[(n*(n-1))/2];
	        int id=0;
	        
	        for (int j=0;j<n-1;j++)
	        {	        	
	        	for(int k=j+1;k<n;k++)
	        	{
	        		e[id]=g_i.addEdge("node"+(j+1),"node"+(k+1));	        			
	        		g_i.setEdgeWeight(e[id], g.getEdgeWeight((j +1), (k+1)));
	        		id++;
	        	}
	        }
	        int i=0;
			while(i<d)
			{
				//tim duong di cho moi demand
				//tuy thuoc vao bandwidth
				Demand _d= getDemand(rank_service[i]);
				if(_d.sourceS()==_d.destinationS())
					
				{
					out.write("["+_d.sourceS()+"]");
					out.newLine();
				}
				else
				{
				//remove edges which haven't enough capacity
				DefaultWeightedEdge[] removed_edge = new DefaultWeightedEdge[id];
				int no_removed_edge =0;
				for( DefaultWeightedEdge v:g_i.edgeSet())
				{
					if(g_i.getEdgeWeight(v)<_d.bwS())
						removed_edge[no_removed_edge++]=v;				
				}
				for (int j=0;j<no_removed_edge;j++)
					g_i.removeEdge(removed_edge[j]);
				_p =  DijkstraShortestPath.findPathBetween(g_i, "node"+_d.sourceS(), "node"+_d.destinationS());
				
				int source = _d.sourceS();
				if(_p!=null)
				{
					nodeList = new ArrayList<Integer>();
					// sau do chon duong ngan nhat 
					nodeList.add(source);
					while (_p.size()>0)
					{	
						int ix =0;
						for(int l=0;l<_p.size();l++)
						{
							int int_s =Integer.parseInt(g_i.getEdgeSource(_p.get(l)).replaceAll("[\\D]", ""));
							int int_t =Integer.parseInt(g_i.getEdgeTarget(_p.get(l)).replaceAll("[\\D]", ""));
							value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;
							value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth()*0.0001 ;	
							if( int_s == source )
							{
								nodeList.add(int_t);
								source = int_t;
								ix = l;
								g.setEdgeWeight(int_s, int_t, g.getEdgeWeight(int_s, int_t)-_d.bwS());
								break;
							}
							if( int_t == source)
							{
								nodeList.add(int_s);
								source = int_s;
								ix = l;
								g.setEdgeWeight(int_s, int_t, g.getEdgeWeight(int_s, int_t)-_d.bwS());
								break;
							}
						}
						_p.remove(ix);	
					}
					//in ra file
					out.write("[");
					for(int _i:nodeList)
					{
						System.out.print(_i+",");
						out.write(_i+", ");
					}
					System.out.println();
					out.write("]");
					out.newLine();
				}
				else
				{
					//khong tim duoc duong di -> khong tim ra giai phap
					return false;
				}
				}
				//value_bandwidth +=weight_path * g.getPriceBandwidth() *_d.bwS();
				i++;			
			
			}
			_duration = System.currentTimeMillis() - startTime;
			System.out.println(_duration);
			out.write("Runtime (mS): "+ _duration);
			out.newLine();
			out.write("Value bandwidth: "+ value_bandwidth);
			
		} catch ( IOException e1 ) {
			e1.printStackTrace();
			} finally {
				if ( out != null )
					try {
						out.close();
						} catch (IOException e) {
							e.printStackTrace();}
				}    
	try {
  		out.close();
  		} catch (IOException e2) {
  			e2.printStackTrace();
  			}
			return true;
	}
	
	public static boolean heuristic_2(String outFile)//Min-Min algorithn
	{
		functionCost=0;
		numberofCore=0;
		numberofEdge=0;
		numberofMidle=0;
		value_final=0;
		value_bandwidth =0;
		ultilize_resource=0;
		_duration=0;
		for (int i=0;i<n;i++)
		{
			ultilize_resource +=UtilizeFunction.value(g.getCap(i+1)) * g.getPriceNode(i+1);				
		}
		final long startTime = System.currentTimeMillis();
		
		try {
			File file = new File(outFile);
			out = new BufferedWriter(new FileWriter(file));
		MyGraph g_temp=	new MyGraph(g.cap,g.pricePernode,g.link_bandwidth,g.getPriceBandwidth());
		//Graph g_temp= new Graph(g.r, g.K, g.link_bandwidth, g.getPriceBandwidth(),true);
		
		int[][] function_Loc = new int[m+1][n+1]; // if number of function f put on virtual node v
		
		double bw_min=getDemand(1).bwS();//gia tri nho nhat cua canh can duoc dap ung cho tat ca cac demand
		//double r_min =functionArr[0].bw();//gia tri nho nhat cua node can duoc dap ung cho tat ca cac function
		Vector<Double> r_min =functionArr[0].getLamda();
		int[] f_rank = new int[m];//rank function decrease
		double[] f_maxbw=new double[m+1];//max bandwidth of function in all services
		int[] no_Function= new int[m+1]; //number of function in all services
		
		List<Integer> nodeList;
		double weight_path=0;
		double min_w ;
		int source=0;
		int destination=0;		
		List<DefaultWeightedEdge> _p;
		
		ArrayList<Integer> srcArr = new ArrayList<Integer>();//chua tat ca cac source
		ArrayList<Integer> desArr= new ArrayList<Integer>();//chua tat ca cac destination
		for (int i=0;i<n;i++)
		{
			boolean _flagS=false;
			boolean _flagD=false;
			for (Demand d : demandArray) {
				
				if(!_flagS && d.sourceS()==(i+1))
				{
					srcArr.add(i+1);
					_flagS=true;
				}
				if(!_flagD && d.destinationS()==(i+1))
				{
					desArr.add(i+1);
					_flagD=true;
				}
			}
		}
		
		
		//sap xep cac function theo thu tu tang dan tai nguyen yeu cau
		for (int i=0;i<m;i++)
			f_rank[i]= functionArr[i].id();
		for(int i=0;i<f_rank.length-1;i++)
		{
			int temp=i;
			for (int j=i+1;j<f_rank.length;j++)
				if(UtilizeFunction.isBig(functionArr[temp].getLamda(),functionArr[j].getLamda()))
					temp=j;
			int k= f_rank[i];
			f_rank[i]=f_rank[temp];
			f_rank[temp]=k;
		}
		
		//so luong function trong tat ca cac demand
		for (int i=0;i<m;i++)
		{
			no_Function[i+1]=0;
			f_maxbw[i+1]=0;
		}
		for (int i=0;i<m;i++)
			for (int j=0;j<d;j++)
			{
				Function[] arrF = getDemand(j+1).getFunctions();				
				for (int k=0;k<arrF.length;k++)
				{					
					if(arrF[k].id()==(i+1))
					{
						no_Function[i+1]+=1;
						if(getDemand(j+1).bwS()>f_maxbw[i+1])
							f_maxbw[i+1]=getDemand(j+1).bwS();
					}
				}
			}
		
		//gia tri nho nhat cua cac link tu cac demand, gia tri nho nhat cac requirement for function
		for (int i=0;i<d;i++)
			if(getDemand(i+1).bwS()< bw_min)
				bw_min = getDemand(i+1).bwS();
		for (int i=0;i<m;i++)
		{
			if(UtilizeFunction.bigger(r_min, functionArr[i].getLamda())==1)
				r_min =functionArr[i].getLamda();
		}
		
		
		//1. Remove node and edge is not feasible
		for (int i=0;i<n;i++)
		{
			if(UtilizeFunction.isBig(r_min, g_temp.getCap(i+1)))
				g_temp.removeNode(i+1);
			for (int j=0;j<n;j++)
				if(g_temp.getEdgeWeight(i+1, j+1)<bw_min)
					g_temp.removeLink(i+1, j+1);
		}
		
		//B1. Rank larger is put on virtual node smaller (price per resource unit)
		// Prior: same function put on 1 virtual if it has enough capacity
		for (int i=0;i<m;i++)
			for (int j=0;j<n;j++)
				function_Loc[i+1][j+1]=0;
		//sort virtual nodes increase depend on price per resource unit
		double[] x_add= new double[n+1];// gia tri thay doi sau moii lan gan
		for(int j=0;j<n;j++)
			x_add[j+1]=0.0;
		double y= 0.0;
		double sum_r =0.0;
		for (int k=0;k<n;k++)
			sum_r+=g_temp.getPriceNode(k+1);
		for(int i=0;i<m;i++)
		{
			Function f= getFunction(f_rank[i]);
			if(no_Function[f.id()]>0)
			{
				int[] indexFunction = new int[d];
				for (int j=0;j<d;j++)
				{
					//kiem tra function f co the thuoc demand nao? -1 neu thuoc day dau, 1 neu thuoc day sau. 0 neu ko thuoc
					Function[] arrFunction = getDemand(j+1).getFunctions();
					boolean flag=false;
					for (int k=0;k<arrFunction.length;k++)
					{
						if(arrFunction[k].id()==f.id())
						{
							if(k<arrFunction.length/2)
								indexFunction[j]=-1;
							else
								indexFunction[j]=1;
							flag=true;
							break;
						}
					}
					if(flag==false)
						indexFunction[j]=0;
				}
				int temp_noF = no_Function[f.id()];
				
				double mauso=0;
				for (int j=0;j<d;j++)
				{
					if(indexFunction[j]!=0)
					{
					for (int k=0;k<n;k++)
					{
						mauso+=Dist[getDemand(j+1).sourceS()][k+1];
						mauso+=Dist[getDemand(j+1).destinationS()][k+1];
					}
					}
				}
				//Each function in arrayFunction
				double[] rank_value = new double[n+1];// function to compute rank for each virtual node
				for (int k=0;k<n;k++)
					rank_value[k+1]=0.0;
				while (temp_noF>0)
				{
					for (int k=0;k<n;k++)
					{
						if(function_Loc[f.id()][k+1]>0)
						{
							rank_value[k+1]=theta/function_Loc[f.id()][k+1];
						}
						else
							rank_value[k+1]=0.0;
					}				
					double sum_w = 0.0;				
					for (int k=0;k<n-1;k++)
						for (int j=k+1;j<n;j++)
							sum_w+=g_temp.getEdgeWeight(k+1, j+1) ;
					sum_w= 2*sum_w -y;//tru di mot luong
					for (int k=0;k<n;k++)
					{
						rank_value[k+1]=0.0;
						if(g_temp.getExistNode(k+1))
						{
							//rank_node[i]= i+1;
							rank_value[k+1]+=alpha * g_temp.getPriceNode(k+1)/sum_r;
							double temp_bw= 0.0;
							for (int j=0;j<n;j++)
							{
								if(g_temp.getEdgeWeight(k+1, j+1)>0)
									temp_bw +=g_temp.getEdgeWeight(k+1, j+1) ;
							}
							temp_bw = temp_bw - x_add[k+1];
	//						if(sum_w >0)
	//							rank_value[k+1]+=beta*(1-temp_bw/(2*sum_w));
	//						else
	//						{
	//							rank_value[k+1]=Double.MAX_VALUE;
	//						}
							if(temp_bw >0)
								rank_value[k+1]+=beta*sum_w/temp_bw;
							else
							{
								rank_value[k+1]=Double.MAX_VALUE;
							}
							
							double tuso=0;
							for (int j=0;j<d;j++)
							{
								if (indexFunction[j]==-1)
								{
									tuso+=Dist[getDemand(j+1).sourceS()][k+1];
									tuso+=Dist[getDemand(j+1).destinationS()][k+1];
								}
							}
							if(mauso!=0)
								rank_value[k+1]+=gama*tuso/mauso;
							else
								rank_value[k+1]=Double.MAX_VALUE;
	//						for (int d : srcArr) {
	//							if(d==(k+1))
	//								rank_value[k+1]+=gama;
	//						}
	//						for (int d : desArr) {
	//							if(d==(k+1))
	//								rank_value[k+1]+=gama;
	//						}
							
						}
						else
							rank_value[k+1]=Double.MAX_VALUE;
					}
					// find node: rank_value min
					double min_rank=Double.MAX_VALUE;
					int v_min = -1;
					for (int k=0;k<n;k++)
					{
						//kiem tra them f1 và f2 có ton tai duong di ko
						if(UtilizeFunction.isBig(g_temp.getCap(k+1), f.getLamda()) && rank_value[k+1]<min_rank)
						{
							min_rank = rank_value[k+1];
							v_min = k+1;
						}
					}
					if(v_min==-1)
					{
						//khong the tim dc v_min -> Bai toan khong co loi giai
						out.write("khong tim dc v_min cho f"+f.id());
						out.newLine();
						return false;
					}
					function_Loc[f.id()][v_min]+=1;
					g_temp.setCap(v_min, UtilizeFunction.minus(g_temp.getCap(v_min), f.getLamda()));
					if(UtilizeFunction.isBig(r_min, g_temp.getCap(v_min)))
						g_temp.removeNode(v_min);
					temp_noF--;
					//x_add[v_min] += f_maxbw[f.id()]*g_temp.getPriceBandwidth() ;
					x_add[v_min] += f_maxbw[f.id()];
					y+= x_add[v_min];
					
				}
			}	
		}
		//in ra file =======
		for (int i=0;i<m;i++)
			for (int j=0;j<n;j++)
				if(function_Loc[i+1][j+1]>0)
				{
					value_final+= (1-function_Loc[i+1][j+1])*Gain(g.getPriceNode(j+1));
					functionCost+=function_Loc[i+1][j+1]*g.getPriceNode(j+1);
					//val_final1+=UtilizeFunction.value(g.getPriceNode(j+1))*UtilizeFunction.value(functionArr[i].getLamda());
					System.out.println("function:"+(i+1)+ " node: "+(j+1)+" quantity: "+ function_Loc[i+1][j+1]);
					out.write("function:"+(i+1)+ " node: "+(j+1)+" quantity: "+ function_Loc[i+1][j+1]);
					out.newLine();
				}
		out.write("Cost for function: "+ functionCost);
		out.newLine();
//		for(int i=0;i<m;i++)
//			for(int j=0;j<5;j++)
//				if(function_Loc[i+1][j+1]>0)
//					numberofCore+=function_Loc[i+1][j+1];
//		for(int i=0;i<m;i++)
//			for(int j=5;j<15;j++)
//				if(function_Loc[i+1][j+1]>0)
//					numberofMidle+=function_Loc[i+1][j+1];
//		for(int i=0;i<m;i++)
//			for(int j=15;j<n;j++)
//				if(function_Loc[i+1][j+1]>0)
//					numberofEdge+=function_Loc[i+1][j+1];
		
		//value_final=val_final1;
		//B2. Find path for all demand
		//sort demand depend on increase bandwidth
		int[] rank_service= new int[d];
		if(d>1)
		{
			for (int i=0;i<d;i++)
				rank_service[i]= i+1;
			for(int i=0;i<d-1;i++)
			{
				int temp=i;
				for (int j=i+1;j<d;j++)
					if(getDemand(rank_service[j]).bwS()<getDemand(rank_service[temp]).bwS())
						temp=j;
				int k= rank_service[i];
				rank_service[i]=rank_service[temp];
				rank_service[temp]=k;
			}
		}
		else
		{
			rank_service[0]=1;
		}
		
        int i=0;
		while(i<d)
		{
			Demand _d= getDemand(rank_service[i]);
			SimpleDirectedWeightedGraph<String, DefaultWeightedEdge>  g_i = new SimpleDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class); 
	        
			for (int j=0;j<n;j++)
	        {
	        	g_i.addVertex("node"+(j+1));
	        }
	        
	        for (int j=0;j<n;j++)
	        {	        	
	        	for(int k=0;k<n;k++)
	        	{
	        		if (j!=k && g_temp.getEdgeWeight((j +1), (k+1))>=_d.bwS())
	        		{
	        			DefaultWeightedEdge e=g_i.addEdge("node"+(j+1),"node"+(k+1));	        			
	        		g_i.setEdgeWeight(e, g.getEdgeWeight((j +1), (k+1)));
	        		}
	        	}
	        }
			//remove edges which haven't enough capacity
			
			Function[] fs= _d.getFunctions();//xet demand theo bandwidth
			List<List<Integer>> node = new ArrayList<List<Integer>>(m);
			for (int j=0;j<fs.length;j++)
			{
				List<Integer> innerList = new ArrayList<Integer>();
				for (int k=0;k<n;k++)
					for(int h=0;h<function_Loc[fs[j].id()][k+1];h++)
						innerList.add(k+1);
				node.add(innerList);				  
			}
			List<List<Integer>> shortest_tree = new ArrayList<List<Integer>>();// mang cac node cho moi duong di tu d den t
			List<Double> weight = new ArrayList<>();//gia tri cho moi duong di do
			
			
			//xet truong hop source + nguon + thanh phan thu nhat
			System.out.println("demand: "+_d.idS());
			out.write("demand: "+ _d.idS());
			out.newLine();
			if(fs.length==1)
			{
				boolean fl=false;
				List<Integer> _node = node.get(0);
				if (_node.contains(_d.sourceS()) )
				{
					//giam trong node
					node.set(0,new ArrayList<Integer>(Arrays.asList(_d.sourceS())));
					function_Loc[fs[0].id()][_d.sourceS()]--;
					ultilize_resource-= g.getPriceNode(_d.sourceS())*UtilizeFunction.value(fs[0].getLamda());					
					//value_final+=_d.getRate()*g.getPriceNode(_d.sourceS());
					value_final+=g.getPriceNode(_d.sourceS());
					fl=true;
				}
				else
				{
					if( _node.contains(_d.destinationS()))
					{
						//giam trong node
						node.set(0,new ArrayList<Integer>(Arrays.asList(_d.destinationS())));
						function_Loc[fs[0].id()][_d.destinationS()]--;
						ultilize_resource-= g.getPriceNode(_d.destinationS())*UtilizeFunction.value(fs[0].getLamda());					
						//value_final+=_d.getRate()*g.getPriceNode(_d.destinationS());
						value_final+=g.getPriceNode(_d.destinationS());
						fl=true;
					}	
				}
				
				if(fl)
				{
					//thuc hien tim duong di giua source va destinaion
					ArrayList<DefaultWeightedEdge> removed_edge = new ArrayList<>();
					for( DefaultWeightedEdge v:g_i.edgeSet())
					{
						int int_s =Integer.parseInt(g_i.getEdgeSource(v).replaceAll("[\\D]", ""));
						int int_t =Integer.parseInt(g_i.getEdgeTarget(v).replaceAll("[\\D]", ""));
						if(g_temp.getEdgeWeight(int_s, int_t)<_d.bwS())
						//if(g_i.getEdgeWeight(v)<_d.bwS())
							removed_edge.add(v);
							//removed_edge[no_removed_edge++]=v;				
					}
					for (DefaultWeightedEdge v: removed_edge)
						g_i.removeEdge(v);
					_p =   DijkstraShortestPath.findPathBetween(g_i, "node"+_d.sourceS(), "node"+_d.destinationS());
					if(_p!=null)
					{
						for (DefaultWeightedEdge l:_p)
						{
							weight_path+=g_i.getEdgeWeight(l);
						}
						nodeList = new ArrayList<Integer>();
						nodeList.add(_d.sourceS());
						source=_d.sourceS();
						while (_p.size()>0)
						{	
							int ix =0;
							for(int l=0;l<_p.size();l++)
							{
								int int_s =Integer.parseInt(g_i.getEdgeSource(_p.get(l)).replaceAll("[\\D]", ""));
								int int_t =Integer.parseInt(g_i.getEdgeTarget(_p.get(l)).replaceAll("[\\D]", ""));
								value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;
								value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth()*0.0001 ;	
								if( int_s == source )
								{
									nodeList.add(int_t);
									source = int_t;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
								if( int_t == source)
								{
									nodeList.add(int_s);
									source = int_s;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
							}
							_p.remove(ix);
						}
						for(int _i:nodeList)
							{
								System.out.print(_i+",");
								out.write(_i+", ");
							}	
						out.newLine();						
					}
					else 
					{
						out.write("khong tim duojc duong di giua:"+ _d.sourceS() +" va "+_d.destinationS());
						out.newLine();
						return false;
					}
				}
				else
				{
					//neu khong thi tim duong di tu source -> node, và từ node->destination
					int n_max= -1;
					double n_price_max = 0.0;
					for (int _intMax: _node)
						if(n_price_max < g_temp.getPriceNode(_intMax))
						{
							n_price_max = g_temp.getPriceNode(_intMax);
							n_max= _intMax;
						}
					function_Loc[fs[0].id()][n_max]--;
					node.set(0,new ArrayList<Integer>(Arrays.asList(n_max)));
					ultilize_resource-= g.getPriceNode(n_max)*UtilizeFunction.value(fs[0].getLamda());					
					//value_final+=_d.getRate()*g.getPriceNode(n_max);
					value_final+=g.getPriceNode(n_max);
					
					ArrayList<DefaultWeightedEdge> removed_edge = new ArrayList<>();
					for( DefaultWeightedEdge v:g_i.edgeSet())
					{
						int int_s =Integer.parseInt(g_i.getEdgeSource(v).replaceAll("[\\D]", ""));
						int int_t =Integer.parseInt(g_i.getEdgeTarget(v).replaceAll("[\\D]", ""));
						if(g_temp.getEdgeWeight(int_s, int_t)<_d.bwS())
						//if(g_i.getEdgeWeight(v)<_d.bwS())
							removed_edge.add(v);
							//removed_edge[no_removed_edge++]=v;				
					}
					for (DefaultWeightedEdge v: removed_edge)
						g_i.removeEdge(v);
					_p =   DijkstraShortestPath.findPathBetween(g_i, "node"+_d.sourceS(), "node"+n_max);
					if(_p!=null)
					{
						for (DefaultWeightedEdge l:_p)
						{
							weight_path+=g_i.getEdgeWeight(l);
						}
						nodeList = new ArrayList<Integer>();
						nodeList.add(_d.sourceS());
						source=_d.sourceS();
						while (_p.size()>0)
						{	
							int ix =0;
							for(int l=0;l<_p.size();l++)
							{
								int int_s =Integer.parseInt(g_i.getEdgeSource(_p.get(l)).replaceAll("[\\D]", ""));
								int int_t =Integer.parseInt(g_i.getEdgeTarget(_p.get(l)).replaceAll("[\\D]", ""));
								value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth()*0.0001 ;
								value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;	
								if( int_s == source )
								{
									nodeList.add(int_t);
									source = int_t;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
								if( int_t == source)
								{
									nodeList.add(int_s);
									source = int_s;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
							}
							_p.remove(ix);
						}
						for(int _i:nodeList)
							{
								System.out.print(_i+",");
								out.write(_i+", ");
							}	
						out.newLine();
					}
					else
					{
						out.write("khong tim duojc duong di giua:"+ _d.sourceS() +" va "+n_max);
						out.newLine();
						return false;
					}
					removed_edge = new ArrayList<>();
					for( DefaultWeightedEdge v:g_i.edgeSet())
					{
						int int_s =Integer.parseInt(g_i.getEdgeSource(v).replaceAll("[\\D]", ""));
						int int_t =Integer.parseInt(g_i.getEdgeTarget(v).replaceAll("[\\D]", ""));
						if(g_temp.getEdgeWeight(int_s, int_t)<_d.bwS())
						//if(g_i.getEdgeWeight(v)<_d.bwS())
							removed_edge.add(v);
							//removed_edge[no_removed_edge++]=v;				
					}
					for (DefaultWeightedEdge v: removed_edge)
						g_i.removeEdge(v);
					_p =   DijkstraShortestPath.findPathBetween(g_i, "node"+n_max, "node"+_d.destinationS());
					if(_p!=null)
					{
						for (DefaultWeightedEdge l:_p)
						{
							weight_path+=g_i.getEdgeWeight(l);
						}
						nodeList = new ArrayList<Integer>();
						nodeList.add(n_max);
						source=n_max;
						while (_p.size()>0)
						{	
							int ix =0;
							for(int l=0;l<_p.size();l++)
							{
								int int_s =Integer.parseInt(g_i.getEdgeSource(_p.get(l)).replaceAll("[\\D]", ""));
								int int_t =Integer.parseInt(g_i.getEdgeTarget(_p.get(l)).replaceAll("[\\D]", ""));
								value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;
								value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth()*0.0001 ;	
								if( int_s == source )
								{
									nodeList.add(int_t);
									source = int_t;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
								if( int_t == source)
								{
									nodeList.add(int_s);
									source = int_s;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
							}
							_p.remove(ix);
						}
					
						for(int _i:nodeList)
						{
							System.out.print(_i+",");
							out.write(_i+", ");
						}	
					out.newLine();	
				
				}
				else
				{
					out.write("khong tim duojc duong di giua:"+ _d.destinationS() +" va "+n_max);
					out.newLine();
					return false;
				}
					
			}
			}
			else
			{
			
				ArrayList<DefaultWeightedEdge> removed_edge = new ArrayList<>();
				for( DefaultWeightedEdge v:g_i.edgeSet())
				{
					int int_s =Integer.parseInt(g_i.getEdgeSource(v).replaceAll("[\\D]", ""));
					int int_t =Integer.parseInt(g_i.getEdgeTarget(v).replaceAll("[\\D]", ""));
					if(g_temp.getEdgeWeight(int_s, int_t)<_d.bwS())
					//if(g_i.getEdgeWeight(v)<_d.bwS())
						removed_edge.add(v);
						//removed_edge[no_removed_edge++]=v;				
				}
				for (DefaultWeightedEdge v: removed_edge)
					g_i.removeEdge(v);
				
				List<Integer> n_s = node.get(0);
				min_w = Double.MAX_VALUE;
				List<DefaultWeightedEdge> sht_path_temp=null;
				boolean _isSrc=false;
				for (int k=0;k<n_s.size();k++)
				{
					if(n_s.get(k)==_d.sourceS())
					{
						node.set(0,new ArrayList<Integer>(Arrays.asList(_d.sourceS())));
						//value_final+=UtilizeFunction.value(g.getPriceNode(_d.sourceS()))*UtilizeFunction.value(fs[0].getLamda());
						//function_Loc[d][_d.sourceS()]--;
						_isSrc =true;
						break;
					}					
				}
				if(!_isSrc)
				{
					for (int k=0;k<n_s.size();k++)
					{

						//tinh duong di tu  (source->n_s.get(k))
						weight_path=0;
						_p =   DijkstraShortestPath.findPathBetween(g_i, "node"+_d.sourceS(), "node"+n_s.get(k));
						if(_p!=null)
						{
							for (DefaultWeightedEdge l:_p)
							{
								weight_path+=g_i.getEdgeWeight(l);
							}
							if(weight_path < min_w)
							{
								//chon duong nay
								min_w = weight_path;
								sht_path_temp = new ArrayList<DefaultWeightedEdge>();
								for (DefaultWeightedEdge l:_p)
								{
									sht_path_temp.add(l);
								}
								source=_d.sourceS();
								destination = n_s.get(k);
							}
						}
						}
					nodeList = new ArrayList<Integer>();
					// sau do chon duong ngan nhat 
					if(sht_path_temp!=null)
					{
						prevNode=source;
						nodeList.add(source);
						while (sht_path_temp.size()>0)
						{	
							int ix =0;
							for(int l=0;l<sht_path_temp.size();l++)
							{
								int int_s =Integer.parseInt(g_i.getEdgeSource(sht_path_temp.get(l)).replaceAll("[\\D]", ""));
								int int_t =Integer.parseInt(g_i.getEdgeTarget(sht_path_temp.get(l)).replaceAll("[\\D]", ""));
								value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth()*0.0001 ;
								value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth()*0.0001 ;	
								if( int_s == source )
								{
									nodeList.add(int_t);
									source = int_t;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
								if( int_t == source)
								{
									nodeList.add(int_s);
									source = int_s;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
							}
							sht_path_temp.remove(ix);
						}
					}
					else{
						out.write("khong tim duojc duong di giua:"+ _d.sourceS() +" va f"+fs[0].id());
						out.newLine();
						return false;
						
					}
					weight.add(min_w);
					shortest_tree.add(nodeList);
					node.set(0,new ArrayList<Integer>(Arrays.asList(destination)));	
					//value_final+=UtilizeFunction.value(g.getPriceNode(destination))*UtilizeFunction.value(fs[0].getLamda());
					//function_Loc[d][destination]--;
				}	
				
				for(int j=0;j<fs.length-1;j++)
				{
					//Tim duong di ngan nhat giua tung cap (i, i+1) trong fs
					//xet xem cap nao co duong di ngan nhat
					removed_edge = new ArrayList<>();
					for( DefaultWeightedEdge v:g_i.edgeSet())
					{
						int int_s =Integer.parseInt(g_i.getEdgeSource(v).replaceAll("[\\D]", ""));
						int int_t =Integer.parseInt(g_i.getEdgeTarget(v).replaceAll("[\\D]", ""));
						if(g_temp.getEdgeWeight(int_s, int_t)<_d.bwS())
						//if(g_i.getEdgeWeight(v)<_d.bwS())
							removed_edge.add(v);
							//removed_edge[no_removed_edge++]=v;				
					}
					for (DefaultWeightedEdge v: removed_edge)
						g_i.removeEdge(v);
					int t1=fs[j].id();
					int t2=fs[j+1].id();
					
					List<Integer> node_s = node.get(j);
					List<Integer> node_t = node.get(j+1);
					double min_weight = Double.MAX_VALUE;
					List<DefaultWeightedEdge> shortest_path_temp=null;
					boolean temflag=false;
					for (int k=0;k<node_s.size();k++)
					{
						int temp_s=node_s.get(k);
						for(int h=0;h<node_t.size();h++)
						{
							int temp_t=node_t.get(h);
							if(temp_t==temp_s)
							{
							//truong hop 2 function nằm trong 1 node
								min_weight=0;
								source=temp_s;
								destination = temp_t;
								break;
							}
							else
							{
								if(temp_t!=prevNode)
								{
									temflag=true;
									prevNode=temp_t;
								weight_path=0;
								_p =   DijkstraShortestPath.findPathBetween(g_i, "node"+temp_s, "node"+temp_t);
								if(_p!=null)
								{
								for (DefaultWeightedEdge l:_p)
								{
									weight_path+=g_i.getEdgeWeight(l);
								}
								if(weight_path < min_weight)
								{
									//chon duong nay
									min_weight = weight_path;
									shortest_path_temp = new ArrayList<DefaultWeightedEdge>();
									for (DefaultWeightedEdge l:_p)
									{
										shortest_path_temp.add(l);
									}
									source=temp_s;
									destination = temp_t;
								}
								}
							}
//								else //can kiem tra truong hop neu ko quay lai thi ko tim ra duong di.haizzz
//								{
//									//tranh quay lai
//									System.out.print("aaaa");
//									if(min_weight==Integer.MAX_VALUE)
//									{
//										
//									}
//								}
							}
						}
					}
					if(shortest_path_temp==null && !temflag)
					{
						for (int k=0;k<node_s.size();k++)
						{
							int temp_s=node_s.get(k);
							for(int h=0;h<node_t.size();h++)
							{
								int temp_t=node_t.get(h);
								weight_path=0;
								_p =   DijkstraShortestPath.findPathBetween(g_i, "node"+temp_s, "node"+temp_t);
								if(_p!=null)
								{
								for (DefaultWeightedEdge l:_p)
								{
									weight_path+=g_i.getEdgeWeight(l);
								}
								if(weight_path < min_weight)
								{
									//chon duong nay
									min_weight = weight_path;
									shortest_path_temp = new ArrayList<DefaultWeightedEdge>();
									for (DefaultWeightedEdge l:_p)
									{
										shortest_path_temp.add(l);
									}
									source=temp_s;
									destination = temp_t;
								}
							}
						}
					}
					}
					nodeList = new ArrayList<Integer>();
					if(min_weight==0)
					{
						nodeList.add(source);
						nodeList.add(destination);
						weight.add(min_weight);
						shortest_tree.add(nodeList);
						node.set(j+1, new ArrayList<Integer>(Arrays.asList(destination)));
						//value_final+=UtilizeFunction.value(g.getPriceNode(destination))*UtilizeFunction.value(fs[j+1].getLamda());
					}
					else
					{
						if(shortest_path_temp!=null)
						{
							nodeList.add(source);
							while (shortest_path_temp.size()>0)
							{	
								int ix =0;
								for(int l=0;l<shortest_path_temp.size();l++)
								{
									int int_s =Integer.parseInt(g_i.getEdgeSource(shortest_path_temp.get(l)).replaceAll("[\\D]", ""));
									int int_t =Integer.parseInt(g_i.getEdgeTarget(shortest_path_temp.get(l)).replaceAll("[\\D]", ""));
									value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;
									value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth()*0.0001 ;	
									if( int_s == source )
									{
										nodeList.add(int_t);
										source = int_t;
										ix = l;
										//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
										//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
										g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
										break;
									}
									if( int_t == source)
									{
										nodeList.add(int_s);
										source = int_s;
										ix = l;
										//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
										//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
										g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
										break;
									}
								}
								shortest_path_temp.remove(ix);
							}
						}
						else
						{
							out.write("khong tim duojc duong di giua:f"+ t1 +" va f"+t2);
							out.newLine();
							return false;
							
						}
					//nodeList.add(destination);
					weight.add(min_weight);
					shortest_tree.add(nodeList);
					node.set(j+1, new ArrayList<Integer>(Arrays.asList(destination)));
					
					}
					source= nodeList.get(0);
					function_Loc[t1][source]--;
					//value_final+=_d.getRate()*g.getPriceNode(source);
					value_final+=g.getPriceNode(source);
					ultilize_resource -=g.getPriceNode(source)*UtilizeFunction.value(fs[j].getLamda());
					if(j==fs.length-2)
					{
						destination= nodeList.get(nodeList.size()-1);
						function_Loc[t2][destination]--;
						//value_final+=_d.getRate()*g.getPriceNode(destination);
						value_final+=g.getPriceNode(destination);
						ultilize_resource-=g.getPriceNode(destination)*UtilizeFunction.value(fs[j+1].getLamda());
						//value_final+=_d.getRate()*UtilizeFunction.value(g.getPriceNode(destination))*UtilizeFunction.value(fs[j+1].getLamda());
					}
				}
				
				removed_edge = new ArrayList<>();
				for( DefaultWeightedEdge v:g_i.edgeSet())
				{
					int int_s =Integer.parseInt(g_i.getEdgeSource(v).replaceAll("[\\D]", ""));
					int int_t =Integer.parseInt(g_i.getEdgeTarget(v).replaceAll("[\\D]", ""));
					if(g_temp.getEdgeWeight(int_s, int_t)<_d.bwS())
					//if(g_i.getEdgeWeight(v)<_d.bwS())
						removed_edge.add(v);
						//removed_edge[no_removed_edge++]=v;				
				}
				for (DefaultWeightedEdge v: removed_edge)
					g_i.removeEdge(v);
				
				n_s = node.get(fs.length-1);
				min_w = Double.MAX_VALUE;
				sht_path_temp=null;
				boolean _isDes=false;
				for (int k=0;k<n_s.size();k++)
				{
					if(n_s.get(k)==_d.destinationS())
					{
						_isDes =true;
						break;
					}
					
				}
				if(!_isDes)
				{
					for (int k=0;k<n_s.size();k++)
					{
					//tinh duong di tu  (n_s.get(k)->destination)
					weight_path=0;
					_p =   DijkstraShortestPath.findPathBetween(g_i,"node"+n_s.get(k), "node"+_d.destinationS());
					if(_p!=null)
					{
					for (DefaultWeightedEdge l:_p)
					{
						weight_path+=g_i.getEdgeWeight(l);
					}
					if(weight_path < min_w)
					{
						//chon duong nay
						min_w = weight_path;
						sht_path_temp = new ArrayList<DefaultWeightedEdge>();
						for (DefaultWeightedEdge l:_p)
						{
							sht_path_temp.add(l);
						}
						source=n_s.get(k);
						destination = _d.destinationS();
					}
					}
					}
					nodeList = new ArrayList<Integer>();
					// sau do chon duong ngan nhat 
					if(sht_path_temp!=null)
					{
						nodeList.add(source);
						while (sht_path_temp.size()>0)
						{	
							int ix =0;
							for(int l=0;l<sht_path_temp.size();l++)
							{
								int int_s =Integer.parseInt(g_i.getEdgeSource(sht_path_temp.get(l)).replaceAll("[\\D]", ""));
								int int_t =Integer.parseInt(g_i.getEdgeTarget(sht_path_temp.get(l)).replaceAll("[\\D]", ""));
								value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;
								value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth()*0.0001 ;	
								if( int_s == source )
								{
									nodeList.add(int_t);
									source = int_t;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
								if( int_t == source)
								{
									nodeList.add(int_s);
									source = int_s;
									ix = l;
									//double eg_w= g_i.getEdgeWeight(shortest_path_temp.get(l));
									//g_i.setEdgeWeight(shortest_path_temp.get(l), _d.bwS()-eg_w);
									g_temp.setEdgeWeight(int_s, int_t, g_temp.getEdgeWeight(int_s, int_t)-_d.bwS());
									break;
								}
							}
							sht_path_temp.remove(ix);
						}
					}
					else
					{
						out.write("khong tim duojc duong di giua:f"+ fs[fs.length-1].id() +" va destination"+_d.destinationS());
						out.newLine();
						return false;
					}
					weight.add(min_w);
					shortest_tree.add(nodeList);
					node.set(0,new ArrayList<Integer>(Arrays.asList(destination)));				
					//function_Loc[d][destination]--;
				}	
			
				for(List<Integer> _list:shortest_tree)
				{
					out.write("[");
					for(int _i:_list)
					{
						System.out.print(_i+",");
						out.write(_i+", ");
					}
					System.out.println();
					out.write("], ");
				}			
				out.newLine();
//				double _min=0.0;
//				for(double _w:weight)
//					_min+=_w;
//				value_bandwidth +=_min * g.getPriceBandwidth() ;
//				value_final +=_min * g.getPriceBandwidth() ;
//				//value_final +=_min;
//				System.out.println("min value: "+_min);
			}
			i++;
		}
		for (int j=0;j<n;j++)
			for (int k=0;k<n;k++)
			{
				ultilize_resource += g_temp.getEdgeWeight(j+1, k+1)*g_temp.getPriceBandwidth() ;
			}
		_duration = System.currentTimeMillis() - startTime;
		System.out.println(_duration);
		out.write("Value solution: "+ value_final);
		out.newLine();
		out.write("Runtime (mS): "+ _duration);
		out.write("Value Bandwidth: "+ value_bandwidth);
		out.write("Resource ultilization: "+ ultilize_resource);
		} catch ( IOException e1 ) {
			e1.printStackTrace();
			} finally {
				if ( out != null )
					try {
						out.close();
						} catch (IOException e) {
							e.printStackTrace();}
				}    
	try {
  		out.close();
  		} catch (IOException e2) {
  			e2.printStackTrace();
  			}
	return true;
	
	}
	private static double Gain(double u) {
		double temp= u;
		if(temp>0)
			return Math.log(temp);
		else
			return 0;
		}
	
	public static void Model_cplex(String outFile)
	{
		IloNumVar[][][] x_val = new IloNumVar[d][m][n];
		IloNumVar[][][] y_val = new IloNumVar[d][n][n];
		IloNumVar[][][][] phi_val = new IloNumVar[d][m+1][n][n];
		try {

			File file = new File(outFile);
			out = new BufferedWriter(new FileWriter(file));
			out.write("number of function:" + m);
			out.newLine();
			for (int i=0;i<m;i++)
	       	{	 
	               out.write(functionArr[i].toString());
	               out.newLine();
	       	}
	   		out.write("number of Demand:" + d);
	   		out.newLine();
	       	for (int i=0;i<d;i++)
	       	{    		
	       		out.write(demandArray.get(i).toString());
	       		out.newLine();
	       	}
	       	out.write("virtual node:"+ n);
	       	out.newLine();
	       	for (int i=0;i<n;i++)
	       	{
	       		for (int j=0;j<n;j++)
	       			out.write(g.getEdgeWeight(i+1, j+1) + " ");
	       		out.newLine();
	       	}
	       	
	       	try {
				IloCplex cplex = new IloCplex();
				for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
				    		{
								
				    			x_val[i][k][j] = cplex.boolVar();				    		}
				for(int i = 0; i < demandArray.size(); i++) 
				    for(int j = 0; j < n; j++)
				    	for(int k = 0; k < n; k++)
				    		{
				    			if(g.getEdgeWeight(j+1, k+1)>0)
				    			{
				    				y_val[i][j][k] = cplex.boolVar();
				    			}
				    		}
				
				for (int i=0;i<demandArray.size();i++)
					for(int k=0;k<demandArray.get(i).getFunctions().length;k++)
						for(int j1=0;j1<n;j1++)
							for(int j2=0;j2<n;j2++)
							{
								if(g.getEdgeWeight(j1+1, j2+1)>0)
								{
				    				phi_val[i][k][j1][j2] = cplex.boolVar();
								}
							}
				IloLinearNumExpr expr = cplex.linearNumExpr();
				//ham muc tieu
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						if(g.getEdgeWeight(j1+1, j2+1)>0)
						{
							for(int i = 0; i < demandArray.size(); i++) 
							{
								expr.addTerm(g.getEdgeWeight(j1+1, j2+1)*g.getPriceBandwidth()*0.0001 , y_val[i][j1][j2]);
							}
						}
					}
				for(int j = 0; j < n; j++)
					for(int i = 0; i < demandArray.size(); i++)
						for(int k = 0; k < m; k++)
				    		{
				    			expr.addTerm(g.getPriceNode(j+1), x_val[i][k][j]);
				    		}
				IloObjective obj = cplex.minimize(expr);
				cplex.add(obj); 
	//Constraints:
				//Eq (1) ->Ok
				for(int compo=0;compo<3;compo++)
				{
					for(int j = 0; j < n; j++) //node
			    	{
						IloLinearNumExpr expr_temp = cplex.linearNumExpr();
						for(int i = 0; i < demandArray.size(); i++) //demand
							for(int k = 0; k < m; k++) //function
							{
								expr_temp.addTerm(getFunction(k+1).getLamda().get(compo),x_val[i][k][j]);
							}
						cplex.addLe(expr_temp, g.getCap(j+1).get(compo));
			    	}
				}
				
				//Eq (2)->OK
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						if(g.getEdgeWeight(j1+1, j2+1)>0)
						{
							IloLinearNumExpr expr_temp = cplex.linearNumExpr();
							for (int i =0;i<demandArray.size();i++) //demand
							{
								expr_temp.addTerm(demandArray.get(i).bwS(),y_val[i][j1][j2]);
							}
							cplex.addLe(expr_temp, g.getEdgeWeight(j1+1, j2+1));
						}
						
					}
				
				//Eq (3)
				for (int i =0;i<d;i++) //demand
					for (int k = 0;k<m;k++)//k is a function
					{
						IloLinearNumExpr expr_temp = cplex.linearNumExpr();
						for (int j=0;j<n;j++)// j is a node
						{
							expr_temp.addTerm(1, x_val[i][k][j]);
						}
						int id = getDemand(i+1).getOrderFunction(k+1);
						if (id!=0)//truong hop function in demand =1
						{
							cplex.addEq(expr_temp, 1);
						}
						else
							cplex.addEq(expr_temp, 0);
					}
				
				//Eq 4
				for (int i =0;i<d;i++) //demand
					for (int j1=0;j1<n;j1++)
						for(int j2=0;j2<n;j2++)
						{
							if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0&& g.getEdgeWeight(j2+1, j1+1)>0)
							{
								IloLinearNumExpr expr_temp = cplex.linearNumExpr();
								expr_temp.addTerm(1, y_val[i][j1][j2]);
								expr_temp.addTerm(1, y_val[i][j2][j1]);
								cplex.addLe(expr_temp, 1.0);
							}
							
						}
				//Eq (5) ->ok
				for (int i=0;i<demandArray.size();i++)
				{
					int desti = demandArray.get(i).destinationS();
					int source = demandArray.get(i).sourceS();
					for (int j1=0;j1<n;j1++)
					{
						IloLinearNumExpr expr_temp = cplex.linearNumExpr();
						for (int j2=0;j2<n;j2++)
						{
							if(g.getEdgeWeight(j1+1, j2+1)>0)
							{
								
								expr_temp.addTerm(1, y_val[i][j1][j2]);
							}
							if(g.getEdgeWeight(j2+1, j1+1)>0)
								expr_temp.addTerm(-1, y_val[i][j2][j1]);
								
						}
						
						if(j1 !=source-1 && j1 !=desti-1)
						{
							cplex.addEq(expr_temp, 0.0);
						}
						else
						{
							if(j1==source-1)
							{
								cplex.addEq(expr_temp, 1.0);
							}
							if(j1==desti-1)
							{
								cplex.addEq(expr_temp, -1.0);
							}
						}
					}
					
				}
				
				//Eq 6
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						if(j1!=j2 && (g.getEdgeWeight(j1+1, j2+1)>0))
						{
							for(int i=0;i<demandArray.size();i++)
								for(int k=0;k<demandArray.get(i).getFunctions().length;k++)
								{
									IloLinearNumExpr expr_temp = cplex.linearNumExpr();			
									expr_temp.addTerm(1, phi_val[i][k][j1][j2]);
									expr_temp.addTerm(-1,y_val[i][j1][j2]);
									cplex.addLe(expr_temp, 0.0);
									
								}
						}
					}
					//Eq 7
				
				for (int i=0;i<demandArray.size();i++)
					for(int k=0;k<demandArray.get(i).getFunctions().length;k++)
						for (int j=0;j<n;j++)
						{
							int source = demandArray.get(i).sourceS();
							int destination = demandArray.get(i).destinationS();
							IloLinearNumExpr expr_temp = cplex.linearNumExpr();	
							for (int j1=0;j1<n;j1++)
								for(int j2=0;j2<n;j2++)
								{
									if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0)
									{
										if(j==j1)
										{
											expr_temp.addTerm(-1, phi_val[i][k][j1][j2]);
										}
										if(j==j2)
										{
											expr_temp.addTerm(1, phi_val[i][k][j1][j2]);
										}
									}
								}
							if(k==0)
							{
								int f2 = demandArray.get(i).getFunctions()[k].id()-1;							
								expr_temp.addTerm(-1, x_val[i][f2][j]);
								if(j==source-1)
									cplex.addEq(expr_temp, -1.0);
								else
									cplex.addEq(expr_temp, 0.0);
								
							}
							else
							{
								int f1 = demandArray.get(i).getFunctions()[k-1].id()-1;
								expr_temp.addTerm(1, x_val[i][f1][j]);
								int f2 = demandArray.get(i).getFunctions()[k].id()-1;							
								expr_temp.addTerm(-1, x_val[i][f2][j]);
								cplex.addEq(expr_temp,0.0);								
								
							}
							
						}
					//Eq 8
				
				for (int i=0;i<demandArray.size();i++)
					for(int j=0;j<n;j++)
					{
						for (int k=0;k<demandArray.get(i).getFunctions().length-1;k++)
						{
							int f1 = demandArray.get(i).getFunctions()[k].id()-1;
							int f2= demandArray.get(i).getFunctions()[k+1].id()-1;
							IloLinearNumExpr expr_temp = cplex.linearNumExpr();								
							expr_temp.addTerm(1, x_val[i][f1][j]);
							expr_temp.addTerm(1, x_val[i][f2][j]);
							cplex.addLe(expr_temp, 1.0);
						}
					}
				
				//Eq (9)
				for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
						{
							int source = demandArray.get(i).sourceS();
							if(source != j+1)
							{
								IloLinearNumExpr expr_temp = cplex.linearNumExpr();				
								expr_temp.addTerm(1, x_val[i][k][j]);
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j1+1, j+1)>0)
										expr_temp.addTerm(-1, y_val[i][j1][j]);
								cplex.addLe(expr_temp, 0);
								IloLinearNumExpr expr_temp1 = cplex.linearNumExpr();
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j1+1, j+1)>0)
										expr_temp1.addTerm(1, y_val[i][j1][j]);
								cplex.addLe(expr_temp1, 1);
							}	
							else
							{
								IloLinearNumExpr expr_temp = cplex.linearNumExpr();				
								expr_temp.addTerm(1, x_val[i][k][j]);
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j+1, j1+1)>0)
										expr_temp.addTerm(-1, y_val[i][j][j1]);
								cplex.addLe(expr_temp, 0);
								IloLinearNumExpr expr_temp1 = cplex.linearNumExpr();
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j+1, j1+1)>0)
										expr_temp1.addTerm(1, y_val[i][j][j1]);
								cplex.addEq(expr_temp1, 1);
							}
							
				
						}
				cplex.solve();
				if ( cplex.getStatus() == IloCplex.Status.Infeasible||
				           cplex.getStatus() == IloCplex.Status.InfeasibleOrUnbounded ) {
					System.out.println("No solution");
				}
				else
				{

					value_final = cplex.getObjValue();
					out.newLine();
					for(int i = 0; i < demandArray.size(); i++) 
						for(int k = 0; k < m; k++)
							for(int j = 0; j < n; j++)
					    		{
									if(cplex.getValue(x_val[i][k][j])>0)
					    			{
					    			out.write("x_val["+i+"]["+k+"]["+j+"]="+cplex.getValue(x_val[i][k][j]));
					    			out.newLine();
					    			}
					    		}
					for(int i = 0; i < demandArray.size(); i++) 
					    for(int j1 = 0; j1 < n; j1++)
					    	for(int j2 = 0; j2 < n; j2++)
					    		{	
					    			if(g.getEdgeWeight(j1+1, j2+1)>0&&cplex.getValue(y_val[i][j1][j2])>0)
					    			{
					    				out.write("y_val["+i+"]["+j1+"]["+j2+"]="+cplex.getValue(y_val[i][j1][j2]));
					    			
					    				out.newLine();
					    			}
					    		}
					for (int i=0;i<demandArray.size();i++)
						for(int k=0;k<demandArray.get(i).getFunctions().length;k++)
							for(int j1=0;j1<n;j1++)
								for(int j2=0;j2<n;j2++)
					    		{	
					    			if(g.getEdgeWeight(j1+1, j2+1)>0 && cplex.getValue(phi_val[i][k][j1][j2])>0)
					    			{
					    				out.write("phi_val["+i+"]["+k+"]["+j1+"]["+j2+"]="+cplex.getValue(phi_val[i][k][j1][j2]));
					    				out.newLine();
					    			}
					    		}
				
				}

				
			} catch (IloException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	       	
			
			} catch ( IOException e1 ) {
					e1.printStackTrace();
					} finally {
						if ( out != null )
							try {
								out.close();
								} catch (IOException e) {
									e.printStackTrace();}
						}    
			try {
		  		out.close();
		  		} catch (IOException e2) {
		  			e2.printStackTrace();
		  			}
	
	
	
	}
	
	public static ArrayList<Integer> minimalCover(ArrayList<Double> a,int C,double w)
	{
		ArrayList<Integer> indexLst = new ArrayList<>();
		GRBEnv env1;
		GRBVar[] xCover = new GRBVar[a.size()];
		try {
			env1 = new GRBEnv("qp1.log");
			env1.set(GRB.DoubleParam.MIPGap, 0.000000001);
			env1.set(GRB.DoubleParam.FeasibilityTol, 0.000000001);
			GRBModel model1 = new GRBModel(env1);			
			GRBLinExpr obj = new GRBLinExpr();
			
			for (int i=0;i<a.size();i++)
			{
				String st = "xCover["+(i+1)+ "]";
				xCover[i] = model1.addVar(0, 1, 0, GRB.BINARY, st);
			}			
					
			
			model1.update();

			//ham muc tieu
			for (int i=0;i<a.size();i++)
			{
				obj.addTerm(a.get(i) , xCover[i]);
				
			}
			model1.setObjective(obj,GRB.MINIMIZE);		
			//add constraints
			GRBLinExpr expr1= new GRBLinExpr();
			for (int i=0;i<a.size();i++)
			{
				expr1.addTerm(1,xCover[i]);
			}
			String st = "c[1]";
			model1.addConstr(expr1, GRB.EQUAL, C , st);
			expr1 = null;
			
			expr1= new GRBLinExpr();
			for (int i=0;i<a.size();i++)
			{
				expr1.addTerm(a.get(i),xCover[i]);
			}
			st = "d[1]";
			model1.addConstr(expr1, GRB.GREATER_EQUAL, w , st);
			expr1 = null;
				
			System.gc();
			
			
				try{
				model1.optimize();
				//model.write("model1.lp");
			
				int optimstatus = model1.get(GRB.IntAttr.Status); 
				if (optimstatus == GRB.Status.OPTIMAL) 
				{ 
					//r_min= r.get(GRB.DoubleAttr.X);
					value_final = obj.getValue();
					for (int i=0;i<a.size();i++)
					    		{	
					    			if(xCover[i].get(GRB.DoubleAttr.X)>0)
					    			{
					    				indexLst.add(i);
					    			}
					    		}
			
				 } else if (optimstatus == GRB.Status.INF_OR_UNBD) 
				 	{ 
				        System.out.println("Model is infeasible or unbounded"); 
				        return null;
				 	} else if (optimstatus == GRB.Status.INFEASIBLE) 
				        	{ 
						        System.out.println("Model is infeasible AAAAAAAAAAAAAA"); 
						        return null; 
				        	}
				}catch (Exception e) {
					e.printStackTrace();
				}
				  
		} catch (GRBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return indexLst;
	}
	
	public static ArrayList<Integer> sortDecreasing()
	{
		ArrayList<Integer> temp = new ArrayList<>();
		ArrayList<Demand> dArrtemp = new ArrayList<>();
		for (int i=0;i<demandArray.size();i++)
		{
			dArrtemp.add(new Demand(demandArray.get(i).idS(),demandArray.get(i).bwS()));
		}
		int dem=0;
		while (dem<dArrtemp.size())
		{
			double max=0.0;
			int id=-1;
			for (int i=0;i< dArrtemp.size();i++)
			{
				Demand dtemp= dArrtemp.get(i);
				if(dtemp.bwS()>max)
				{
					max = dtemp.bwS();
					id=i;
				}
			
			}
			dem++;
			if(id==-1)
			{
				System.out.println("Het chua "+ dem);
				continue;
			}
			
			Demand dmax= dArrtemp.get(id);
			dmax.set_bwS(0.0);
			dArrtemp.set(id, dmax);
			temp.add(id);
		}
		return temp;
	}
	public static ArrayList<Integer> sortFuncDecreasing(int termNo)
	{
		ArrayList<Integer> temp = new ArrayList<>();
		//ArrayList<Demand> fArrtemp = new ArrayList<>();
		ArrayList<Integer> fArrtemp= new ArrayList<>();
		for (int i=0;i<functionArr.length;i++)
		{
			fArrtemp.add(functionArr[i].id());
		}
		while (fArrtemp.size()>0)
		{
			double max=0.0;
			int maxID = 0;
			int id=-1;
			for (int i=0;i< fArrtemp.size();i++)
			{
				Function ftemp=getFunction(fArrtemp.get(i));
				if(ftemp.getLamda().get(termNo)>max)
				{
					max = ftemp.getLamda().get(termNo);
					maxID=ftemp.id();
					id=i;
				}
			
			}
			temp.add(maxID);
			fArrtemp.remove(id);
		}
		return temp;
	}
	
	public static void Model_Minh(String outFile)
	{

		double Const_No = 28.0;
		try {

			File file = new File(outFile);
			out = new BufferedWriter(new FileWriter(file));
			out.write("number of function:" + m);
			out.newLine();
			for (int i=0;i<m;i++)
	       	{	 
	               out.write(functionArr[i].toString());
	               out.newLine();
	       	}
	   		out.write("number of Demand:" + d);
	   		out.newLine();
	       	for (int i=0;i<d;i++)
	       	{    		
	       		out.write(demandArray.get(i).toString());
	       		out.newLine();
	       	}
	       	out.write("virtual node:"+ n);
	       	out.newLine();
	       	for (int i=0;i<n;i++)
	       	{
	       		for (int j=0;j<n;j++)
	       			out.write(g.getEdgeWeight(i+1, j+1) + " ");
	       		out.newLine();
	       	}
	       	
	       	
	       	
			try{
				GRBEnv env = new GRBEnv("qp.log");
				env.set(GRB.DoubleParam.MIPGap, 0.000000001);
				env.set(GRB.DoubleParam.FeasibilityTol, 0.000000001);
				env.set(GRB.IntParam.Threads,8);
				env.set(GRB.DoubleParam.TimeLimit,4000);
				GRBModel model = new GRBModel(env);
				
				GRBLinExpr obj = new GRBLinExpr();
				int constant=1;
	
				
				//variable declaire
				
			
				for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
				    		{
				    			String st = "x1["+(i+1)+ "]["+(k+1)+ "]["+(j+1)+ "]";
				    			x1[i][k][j] = model.addVar(0, 1, 0, GRB.BINARY, st);
				    		}
				for(int i = 0; i < demandArray.size(); i++) 
				    for(int j = 0; j < n; j++)
				    	for(int k = 0; k < n; k++)
				    		{
				    			if(g.getEdgeWeight(j+1, k+1)>0)
				    			{
				    				String st = "y1["+(i+1)+ "]["+(j+1)+ "]["+(k+1)+ "]";
				    				y1[i][j][k] = model.addVar(0, 1, 0, GRB.BINARY, st);
				    			}
				    		}
				
				for (int i=0;i<demandArray.size();i++)
					for(int k=0;k<demandArray.get(i).getFunctions().length;k++)
						for(int j1=0;j1<n;j1++)
							for(int j2=0;j2<n;j2++)
							{
								if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0)
								{
									String st = "phi["+(i+1)+ "]["+(k+1)+ "]["+(j1+1)+ "]["+(j2+1)+ "]";
				    				phi[i][k][j1][j2] = model.addVar(0, 1, 0, GRB.CONTINUOUS, st);
								}
							}
						
				
				model.update();

				//ham muc tieu
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						if(g.getEdgeWeight(j1+1, j2+1)>0)
						{
							for(int i = 0; i < demandArray.size(); i++) 
							{
								obj.addTerm(g.getEdgeWeight(j1+1, j2+1)*g.getPriceBandwidth()*0.0001 , y1[i][j1][j2]);
								//obj.addTerm(g.getEdgeWeight(j1+1, j2+1)*g.getPriceBandwidth()*0.0001 , y1[i][j2][j1]);
							}
						}
					}
				for(int j = 0; j < n; j++)
					for(int i = 0; i < demandArray.size(); i++)
						for(int k = 0; k < m; k++)
				    		{
				    			obj.addTerm(g.getPriceNode(j+1), x1[i][k][j]);
				    		}
				model.setObjective(obj,GRB.MINIMIZE);		
				//add constraints
				
					
//				
				//Eq (5) ->Ok
				for(int compo=0;compo<3;compo++)
				{
					for(int j = 0; j < n; j++) //node
			    	{
						GRBLinExpr expr1= new GRBLinExpr();
						for(int i = 0; i < demandArray.size(); i++) //demand
							for(int k = 0; k < m; k++) //function
							{
									expr1.addTerm(getFunction(k+1).getLamda().get(compo),x1[i][k][j]);
							}
						String st = "c["+(j)+ "]["+compo+"]";
						model.addConstr(expr1, GRB.LESS_EQUAL, g.getCap(j+1).get(compo) , st);
						expr1 = null;
			    	}
				}
				System.gc();
				
				//Eq (6)->OK
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						if(g.getEdgeWeight(j1+1, j2+1)>0)
						{
							GRBLinExpr expr2= new GRBLinExpr();
							for (int i =0;i<demandArray.size();i++) //demand
							{
								expr2.addTerm(demandArray.get(i).bwS(),y1[i][j1][j2]);
								//expr2.addTerm(demandArray.get(i).bwS(),y1[i][j2][j1]);
							}
							//expr2.addTerm(-g.getEdgeWeight(j1+1, j2+1),1);
							String st = "d["+(j1+1)+ "]["+(j2+1)+ "]";
							model.addConstr(expr2, GRB.LESS_EQUAL,g.getEdgeWeight(j1+1, j2+1), st);
							expr2 = null;	
						}
						
					}
				
			
				System.gc();
				
				//Eq (8)
				for (int i =0;i<d;i++) //demand
					for (int k = 0;k<m;k++)//k is a function
					{
						GRBLinExpr expr3 = new GRBLinExpr();
						for (int j=0;j<n;j++)// j is a node
						{
							expr3.addTerm(1, x1[i][k][j]);
						}
						int id = getDemand(i+1).getOrderFunction(k+1);
						String st = "f["+(i)+ "]["+(k)+ "]";
						if (id!=0)//truong hop function in demand =1
						{
							//expr3.addTerm(-1, z1[i]);
							model.addConstr(expr3, GRB.EQUAL, 1, st);
						}
						else
							model.addConstr(expr3, GRB.EQUAL, 0, st);
						
						
						expr3 = null;
					}
				//Eq 9
				for (int i =0;i<d;i++) //demand
					for (int j1=0;j1<n;j1++)
						for(int j2=0;j2<n;j2++)
						{
							if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0&& g.getEdgeWeight(j2+1, j1+1)>0)
							{
								GRBLinExpr expr3 = new GRBLinExpr();
								expr3.addTerm(1, y1[i][j1][j2]);
								expr3.addTerm(1, y1[i][j2][j1]);
								String st = "g["+(i)+ "]["+(j1)+ "]["+(j2)+"]";
								model.addConstr(expr3, GRB.LESS_EQUAL, 1, st);
								expr3 = null;
							}
							
						}
				System.gc();
				//Eq (10) ->ok
				for (int i=0;i<demandArray.size();i++)
				{
					int desti = demandArray.get(i).destinationS();
					int source = demandArray.get(i).sourceS();
					for (int j1=0;j1<n;j1++)
					{
						GRBLinExpr expr7= new GRBLinExpr();
						String st = "h1["+(i)+ "]["+(j1+1)+  "s]";
						for (int j2=0;j2<n;j2++)
						{
							if(g.getEdgeWeight(j1+1, j2+1)>0)
							{
								
								expr7.addTerm(1, y1[i][j1][j2]);
							}
							if(g.getEdgeWeight(j2+1, j1+1)>0)
								expr7.addTerm(-1, y1[i][j2][j1]);
								
						}
						
						if(j1 !=source-1 && j1 !=desti-1)
						{
							
							
							model.addConstr(expr7, GRB.EQUAL, 0, st);
							expr7 = null;
						}
						else
						{
							if(j1==source-1)
							{
								model.addConstr(expr7, GRB.EQUAL, 1, st);
								expr7 = null;
							}
							if(j1==desti-1)
							{
								model.addConstr(expr7, GRB.EQUAL, -1, st);
								expr7 = null;
							}
						}
					}
					
				}
				
				//Eq 11 new
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						if(j1!=j2 && (g.getEdgeWeight(j1+1, j2+1)>0))
						{
							for(int i=0;i<demandArray.size();i++)
								for(int k=0;k<demandArray.get(i).getFunctions().length;k++)
								{
									GRBLinExpr exp = new GRBLinExpr();								
									exp.addTerm(1, phi[i][k][j1][j2]);
									exp.addTerm(-1,y1[i][j1][j2]);
									String st = "i1["+(i)+ "]["+(k)+ "]["+(j1+1)+"]["+(j2+1)+  "]";
									model.addConstr(exp, GRB.LESS_EQUAL, 0, st);
									
								}
						}
					}
				
				//Eq 11b new
				
				for (int i=0;i<demandArray.size();i++)
					for(int k=0;k<demandArray.get(i).getFunctions().length;k++)
						for (int j=0;j<n;j++)
						{
							int source = demandArray.get(i).sourceS();
							int destination = demandArray.get(i).destinationS();
							GRBLinExpr exp = new GRBLinExpr();
							for (int j1=0;j1<n;j1++)
								for(int j2=0;j2<n;j2++)
								{
									if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0)
									{
										if(j==j1)
										{
											exp.addTerm(-1, phi[i][k][j1][j2]);
										}
										if(j==j2)
										{
											exp.addTerm(1, phi[i][k][j1][j2]);
										}
									}
								}
							if(k==0)
							{
								int f2 = demandArray.get(i).getFunctions()[k].id()-1;							
								exp.addTerm(-1, x1[i][f2][j]);
								String st = "i2["+(i)+ "]["+(k)+ "]["+(j+1)+ "]";
								if(j==source-1)
									model.addConstr(exp, GRB.EQUAL, -1, st);
								else
									model.addConstr(exp, GRB.EQUAL, 0, st);
								
							}
							else
							{
								int f1 = demandArray.get(i).getFunctions()[k-1].id()-1;
								exp.addTerm(1, x1[i][f1][j]);
								int f2 = demandArray.get(i).getFunctions()[k].id()-1;							
								exp.addTerm(-1, x1[i][f2][j]);
								String st = "i2["+(i)+ "]["+(k)+ "]["+(j+1)+ "]";
								model.addConstr(exp, GRB.EQUAL, 0, st);
								
								
							}
							
						}
				
				//Eq 11 new
				
				for (int i=0;i<demandArray.size();i++)
					for(int j=0;j<n;j++)
					{
						for (int k=0;k<demandArray.get(i).getFunctions().length-1;k++)
						{
							int f1 = demandArray.get(i).getFunctions()[k].id()-1;
							int f2= demandArray.get(i).getFunctions()[k+1].id()-1;
							GRBLinExpr exp = new GRBLinExpr();								
							exp.addTerm(1, x1[i][f1][j]);
							exp.addTerm(1, x1[i][f2][j]);							
							String st = "i3["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
							model.addConstr(exp, GRB.LESS_EQUAL, 1, st);
						}
					}
	
				
//				for (int i=0;i<demandArray.size();i++)
//					for(int k=0;k<demandArray.get(i).getFunctions().length;k++)
//						for(int j1=0;j1<n;j1++)
//							for(int j2=0;j2<n;j2++)
//							{
//								if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0)
//								{
//									GRBLinExpr exp = new GRBLinExpr();								
//									exp.addTerm(1, phi[i][k][j1][j2]);
//									String st = "i4["+(i)+ "]["+(k)+ "]["+(j1+1)+"]["+(j2+1)+  "]";
//									model.addConstr(exp, GRB.GREATER_EQUAL, 0, st);
//								}
//								
//							}
//				for (int i=0;i<demandArray.size();i++)
//				{
//					GRBLinExpr exp = new GRBLinExpr();	
//					int source = demandArray.get(i).sourceS()-1;
//					for(int j2=0;j2<n;j2++)
//					{
//						
//						if(source!=j2 && g.getEdgeWeight(source+1, j2+1)>0)
//						{
//														
//							exp.addTerm(1, phi[i][0][source][j2]);
//							
//						}
//						
//					}
//					String st = "i5["+(i)+ "][0]["+(source+1)+  "]";
//					model.addConstr(exp, GRB.EQUAL, 1, st);
//				}
						
				
//			
				//Eq (12)
				for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
						{
							int source = demandArray.get(i).sourceS();
							if(source != j+1)
							{
								GRBLinExpr exp = new GRBLinExpr();								
								exp.addTerm(1, x1[i][k][j]);
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j1+1, j+1)>0)
										exp.addTerm(-1, y1[i][j1][j]);
								String st = "k1["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp, GRB.LESS_EQUAL, 0, st);
								exp=null;
								GRBLinExpr exp1 = new GRBLinExpr();		
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j1+1, j+1)>0)
										exp1.addTerm(1, y1[i][j1][j]);
								st = "k2["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp1, GRB.LESS_EQUAL, 1, st);
								exp1=null;
							}	
							else
							{
								GRBLinExpr exp = new GRBLinExpr();								
								exp.addTerm(1, x1[i][k][j]);
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j+1, j1+1)>0)
										exp.addTerm(-1, y1[i][j][j1]);
								String st = "k3["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp, GRB.LESS_EQUAL, 0, st);
								exp=null;
								GRBLinExpr exp1 = new GRBLinExpr();		
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j+1, j1+1)>0)
										exp1.addTerm(1, y1[i][j][j1]);
								st = "k4["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp1, GRB.EQUAL, 1, st);
								exp1=null;
							}
							
						}
			

				ArrayList<Integer> demandLst = sortDecreasing();
				
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						double w= g.getEdgeWeight(j1+1, j2+1);
						if(w>0)
						{
							double sum = 0.0;
							int id=-1;
							for(int i=0;i<demandLst.size();i++)
							{
								sum+=getDemand(demandLst.get(i)).bwS();
								if(sum>w)
								{
									id=i+1;
									break;
								}
							}
							double lambda = sum-w;
							
							GRBLinExpr exp = new GRBLinExpr();	
							String st = "cover["+(j1)+ "]["+(j2)+ "]";
							for(int i=0;i<id;i++)
							{
								int idDemand = demandLst.get(i);
								double bwD = getDemand(demandLst.get(i)).bwS();
								
								
								exp.addTerm(getDemand(demandLst.get(i)).bwS(), y1[idDemand][j1][j2]);
								
								
								if(bwD>lambda)
								{
									exp.addConstant(bwD-lambda);
									exp.addTerm(lambda-bwD,y1[idDemand][j1][j2] );
								}
							}
							model.addConstr(exp, GRB.LESS_EQUAL, w, st);
							exp=null;
						}						
						
					}
			
				System.gc();
				
			
				
				// Optimize model
				try {
					
					model.optimize();
					//model.write("model1.lp");
					out.write("Solution for the problem:");
					out.newLine();
				
					int optimstatus = model.get(GRB.IntAttr.Status); 
					if (optimstatus == GRB.Status.OPTIMAL) 
					{ 
						//r_min= r.get(GRB.DoubleAttr.X);
						value_final = obj.getValue();
						out.write("Objective optimal Value: "+obj.getValue());
						out.newLine();
						for(int i = 0; i < demandArray.size(); i++) 
							for(int k = 0; k < m; k++)
								for(int j = 0; j < n; j++)
						    		{	
						    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
						    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
						for(int i = 0; i < demandArray.size(); i++) 
						    for(int j1 = 0; j1 < n; j1++)
						    	for(int j2 = 0; j2 < n; j2++)
						    		{	
						    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
						    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
						for (int i=0;i<demandArray.size();i++)
							for(int k=0;k<demandArray.get(i).getFunctions().length;k++)
								for(int j1=0;j1<n;j1++)
									for(int j2=0;j2<n;j2++)
						    		{	
						    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
						    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
//						for(int i = 0; i < demandArray.size(); i++)
//							if(z1[i].get(GRB.DoubleAttr.X)>0)
//			    			{
//								//a_min++;
//			    			out.write(z1[i].get(GRB.StringAttr.VarName)
//			    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//			    			out.newLine();
//			    			}
////						out.write(r.get(GRB.StringAttr.VarName)
////		    					+ " : " +r.get(GRB.DoubleAttr.X));
		    			out.newLine();
				
					 } else if (optimstatus == GRB.Status.INF_OR_UNBD) 
					 	{ 
					        System.out.println("Model is infeasible or unbounded"); 
					        return;
					 	} else if (optimstatus == GRB.Status.INFEASIBLE) 
					        	{ 
							        System.out.println("Model is infeasible AAAAAAAAAAAAAA"); 
							        return; 
					        	} else if (optimstatus == GRB.Status.INTERRUPTED)
					        	{
					        		//r_min= r.get(GRB.DoubleAttr.X);
					        		value_final = obj.getValue();
					        		out.write("Objective interrupt Value: "+obj.getValue());
									out.newLine();
									for(int i = 0; i < demandArray.size(); i++) 
									for(int k = 0; k < m; k++)
										for(int j = 0; j < n; j++)
								    		{	
								    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
								    			{
								    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
								    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
								    			out.newLine();
								    			}
								    		}
									for (int i=0;i<demandArray.size();i++)
										for(int k=0;k<demandArray.get(i).getFunctions().length;k++)
											for(int j1=0;j1<n;j1++)
												for(int j2=0;j2<n;j2++)
									    		{	
									    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
									    			{
									    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
									    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
									    			out.newLine();
									    			}
									    		}
									for(int i = 0; i < demandArray.size(); i++) 
									    for(int j1 = 0; j1 < n; j1++)
									    	for(int j2 = 0; j2 < n; j2++)
									    		{	
									    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
									    			{
									    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
									    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
									    			out.newLine();
									    			}
									    		}
//									for(int i = 0; i < demandArray.size(); i++)
//										if(z1[i].get(GRB.DoubleAttr.X)>0)
//						    			{
//											//a_min++;
//						    			out.write(z1[i].get(GRB.StringAttr.VarName)
//						    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//						    			out.newLine();
//						    			}
////									out.write(r.get(GRB.StringAttr.VarName)
////					    					+ " : " +r.get(GRB.DoubleAttr.X));
					    			out.newLine();
					        		
					        	}
					
					 else
					 {
						 //r_min= r.get(GRB.DoubleAttr.X);
						 value_final = obj.getValue();
						 out.write("Objective feasible Value: "+obj.getValue());
						 out.newLine();
							for(int i = 0; i < demandArray.size(); i++) 
							for(int k = 0; k < m; k++)
								for(int j = 0; j < n; j++)
						    		{	
						    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
						    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
							for (int i=0;i<demandArray.size();i++)
								for(int k=0;k<demandArray.get(i).getFunctions().length;k++)
									for(int j1=0;j1<n;j1++)
										for(int j2=0;j2<n;j2++)
							    		{	
							    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
							    			{
							    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
							    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
							    			out.newLine();
							    			}
							    		}
							for(int i = 0; i < demandArray.size(); i++) 
							    for(int j1 = 0; j1 < n; j1++)
							    	for(int j2 = 0; j2 < n; j2++)
							    		{	
							    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
							    			{
							    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
							    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
							    			out.newLine();
							    			}
							    		}
//							for(int i = 0; i < demandArray.size(); i++)
//								if(z1[i].get(GRB.DoubleAttr.X)>0)
//				    			{
//									//a_min++;
//				    			out.write(z1[i].get(GRB.StringAttr.VarName)
//				    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//				    			out.newLine();
//				    			}
////							out.write(r.get(GRB.StringAttr.VarName)
////			    					+ " : " +r.get(GRB.DoubleAttr.X));
			    			out.newLine();
							
					  }
				
					
				} catch (Exception e) {
					//r_min= r.get(GRB.DoubleAttr.X);
					value_final = obj.getValue();
					out.write("Objective interrupt Value: "+obj.getValue());
					out.newLine();
					for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
				    		{	
				    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
				    			{
				    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
				    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
				    			out.newLine();
				    			}
				    		}
					for (int i=0;i<demandArray.size();i++)
						for(int k=0;k<demandArray.get(i).getFunctions().length;k++)
							for(int j1=0;j1<n;j1++)
								for(int j2=0;j2<n;j2++)
					    		{	
					    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
					    			{
					    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
					    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
					    			out.newLine();
					    			}
					    		}
					for(int i = 0; i < demandArray.size(); i++) 
					    for(int j1 = 0; j1 < n; j1++)
					    	for(int j2 = 0; j2 < n; j2++)
					    		{	
					    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
					    			{
					    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
					    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
					    			out.newLine();
					    			}
					    		}
//					for(int i = 0; i < demandArray.size(); i++)
//						if(z1[i].get(GRB.DoubleAttr.X)>0)
//		    			{
//							//a_min++;
//		    			out.write(z1[i].get(GRB.StringAttr.VarName)
//		    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//		    			out.newLine();
//		    			}
////					out.write(r.get(GRB.StringAttr.VarName)
////	    					+ " : " +r.get(GRB.DoubleAttr.X));
	    			out.newLine();
					
	
				}
					model.dispose();
				env.dispose();
				System.gc();
			
				} catch(GRBException e3){			
					System.out.println("Error code1: " + e3.getErrorCode() + ". " +
							e3.getMessage());
					System.out.print("This problem can't be solved");
					
					
					}
			} catch ( IOException e1 ) {
					e1.printStackTrace();
					} finally {
						if ( out != null )
							try {
								out.close();
								} catch (IOException e) {
									e.printStackTrace();}
						}    
			try {
		  		out.close();
		  		} catch (IOException e2) {
		  			e2.printStackTrace();
		  			}
	
	
	}
	 public static int[] sortVal(double[] srcLst)
	  {
		  int[] temp= new int[d];
			int dem=0;
			
			while (dem<d)
			{
				double max=-1.0;
				int id=-1;
				for (int i=0;i< srcLst.length;i++)
				{
					double dtemp= srcLst[i];
					if(dtemp>max)
					{
						max = dtemp;
						id=i;
					}
				
				}			
				if(id==-1)
				{
					System.out.println("Het chua "+ dem);
					continue;
				}
				srcLst[id] = -1.0;
				temp[dem]=id;
				dem++;
			}
			return temp;
		
		  
	  }
	public static void preSolveProblem(String outFile)//goi callback
	{


		try {

			File file = new File(outFile);
			out = new BufferedWriter(new FileWriter(file));
			out.write("number of function:" + m);
			out.newLine();
			for (int i=0;i<m;i++)
	       	{	 
	               out.write(functionArr[i].toString());
	               out.newLine();
	       	}
	   		out.write("number of Demand:" + d);
	   		out.newLine();
	       	for (int i=0;i<d;i++)
	       	{    		
	       		out.write(demandArray.get(i).toString());
	       		out.newLine();
	       	}
	       	out.write("virtual node:"+ n);
	       	out.newLine();
	       	for (int i=0;i<n;i++)
	       	{
	       		for (int j=0;j<n;j++)
	       			out.write(g.getEdgeWeight(i+1, j+1) + " ");
	       		out.newLine();
	       	}
	       	
	       	
	       	
			try{
				GRBEnv env = new GRBEnv("qp.log");
				env.set(GRB.DoubleParam.MIPGap, 0.000000001);
				env.set(GRB.DoubleParam.FeasibilityTol, 0.000000001);
				env.set(GRB.IntParam.Threads,24);
				env.set(GRB.DoubleParam.TimeLimit,4000);
				
				//env.set(GRB.IntParam.DualReductions, 0);
				GRBModel model = new GRBModel(env);
				//model.getEnv().set(GRB.IntParam.DualReductions, 0);//add lazy
				model.getEnv().set(GRB.IntParam.PreCrush,1);//add cut
				  //model.getEnv().set(GRB.IntParam.OutputFlag, 0);
			    //model.getEnv().set(GRB.DoubleParam.Heuristics, 0.0);
				//model.getEnv().set(GRB.IntParam.Presolve, 0);
				model.getEnv().set(GRB.IntParam.FlowCoverCuts, 0);
				model.getEnv().set(GRB.IntParam.Cuts, 0);
				GRBLinExpr obj = new GRBLinExpr();
	
				
				//variable declaire
				
			
				for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
				    		{
				    			String st = "x1["+(i+1)+ "]["+(k+1)+ "]["+(j+1)+ "]";
				    			x1[i][k][j] = model.addVar(0, 1, 0, GRB.INTEGER, st);
				    		}
				for(int i = 0; i < demandArray.size(); i++) 
				    for(int j = 0; j < n; j++)
				    	for(int k = 0; k < n; k++)
				    		{
				    			//if(g.getEdgeWeight(j+1, k+1)>0)
				    			//{
				    				String st = "y1["+(i+1)+ "]["+(j+1)+ "]["+(k+1)+ "]";
				    				y1[i][j][k] = model.addVar(0, 1, 0, GRB.INTEGER, st);
				    			//}
				    		}
				
				for (int i=0;i<demandArray.size();i++)
					for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
						for(int j1=0;j1<n;j1++)
							for(int j2=0;j2<n;j2++)
							{
								if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0)
								{
									String st = "phi["+(i+1)+ "]["+(k+1)+ "]["+(j1+1)+ "]["+(j2+1)+ "]";
				    				phi[i][k][j1][j2] = model.addVar(0, 1, 0, GRB.CONTINUOUS, st);
								}
							}
						
				
				model.update();
				String st1 = "r_l";
				r_l = model.addVar(0, 1, 0, GRB.CONTINUOUS, st1);
				st1 = "r_n";
				r_n = model.addVar(0, 1, 0, GRB.CONTINUOUS, st1);
				model.update();

				obj.addTerm(1, r_l);
				obj.addTerm(1, r_n);
				//ham muc tieu
//				for (int j1=0;j1<n;j1++)
//					for(int j2=0;j2<n;j2++)
//					{
//						if(g.getEdgeWeight(j1+1, j2+1)>0)
//						{
//							for(int i = 0; i < demandArray.size(); i++) 
//							{
//								obj.addTerm(g.getEdgeWeight(j1+1, j2+1)*g.getPriceBandwidth()*0.0001 , y1[i][j1][j2]);
//								//obj.addTerm(g.getEdgeWeight(j1+1, j2+1)*g.getPriceBandwidth()*0.0001 , y1[i][j2][j1]);
//							}
//						}
//					}
//				for(int j = 0; j < n; j++)
//					for(int i = 0; i < demandArray.size(); i++)
//						for(int k = 0; k < m; k++)
//				    		{
//				    			obj.addTerm(g.getPriceNode(j+1), x1[i][k][j]);
//				    		}
				model.setObjective(obj,GRB.MINIMIZE);		
				//add constraints
				
					
//				
				//Eq (5) ->Ok
				for(int compo=0;compo<3;compo++)
				{
					for(int j = 0; j < n; j++) //node
			    	{
						GRBLinExpr expr1= new GRBLinExpr();
						for(int i = 0; i < demandArray.size(); i++) //demand
							for(int k = 0; k < m; k++) //function
							{
									expr1.addTerm(getFunction(k+1).getLamda().get(compo),x1[i][k][j]);
							}
						expr1.addTerm(-g.getCap(j+1).get(compo),r_n);
						String st = "c["+(j)+ "]["+compo+"]";
						model.addConstr(expr1, GRB.LESS_EQUAL, 0 , st);
						expr1 = null;
			    	}
				}
				System.gc();
				
				//Eq (6)->OK
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						if(g.getEdgeWeight(j1+1, j2+1)>0)
						{
							GRBLinExpr expr2= new GRBLinExpr();
							for (int i =0;i<demandArray.size();i++) //demand
							{
								expr2.addTerm(demandArray.get(i).bwS(),y1[i][j1][j2]);
								//expr2.addTerm(demandArray.get(i).bwS(),y1[i][j2][j1]);
							}
							//expr2.addTerm(-g.getEdgeWeight(j1+1, j2+1),1);
							expr2.addTerm(-g.getEdgeWeight(j1+1, j2+1),r_l);
							String st = "d["+(j1+1)+ "]["+(j2+1)+ "]";
							model.addConstr(expr2, GRB.LESS_EQUAL,0, st);
							expr2 = null;	
						}
						
					}
				
			
				System.gc();
				
				//Eq (8)
				for (int i =0;i<d;i++) //demand
					for (int k = 0;k<m;k++)//k is a function
					{
						GRBLinExpr expr3 = new GRBLinExpr();
						for (int j=0;j<n;j++)// j is a node
						{
							expr3.addTerm(1, x1[i][k][j]);
						}
						int id = getDemand(i+1).getOrderFunction(k+1);
						String st = "f["+(i)+ "]["+(k)+ "]";
						if (id!=0)//truong hop function in demand =1
						{
							//expr3.addTerm(-1, z1[i]);
							model.addConstr(expr3, GRB.EQUAL, 1, st);
						}
						else
							model.addConstr(expr3, GRB.EQUAL, 0, st);
						
						
						expr3 = null;
					}
				//Eq 9
				for (int i =0;i<d;i++) //demand
					for (int j1=0;j1<n;j1++)
						for(int j2=0;j2<n;j2++)
						{
							if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0&& g.getEdgeWeight(j2+1, j1+1)>0)
							{
								GRBLinExpr expr3 = new GRBLinExpr();
								expr3.addTerm(1, y1[i][j1][j2]);
								expr3.addTerm(1, y1[i][j2][j1]);
								String st = "g["+(i)+ "]["+(j1)+ "]["+(j2)+"]";
								model.addConstr(expr3, GRB.LESS_EQUAL, 1, st);
								expr3 = null;
							}
							
						}
				System.gc();
				//Eq (10) ->ok
				for (int i=0;i<demandArray.size();i++)
				{
					int desti = demandArray.get(i).destinationS();
					int source = demandArray.get(i).sourceS();
					for (int j1=0;j1<n;j1++)
					{
						GRBLinExpr expr7= new GRBLinExpr();
						String st = "h1["+(i)+ "]["+(j1+1)+  "s]";
						for (int j2=0;j2<n;j2++)
						{
							if(g.getEdgeWeight(j1+1, j2+1)>0)
							{
								
								expr7.addTerm(1, y1[i][j1][j2]);
							}
							if(g.getEdgeWeight(j2+1, j1+1)>0)
								expr7.addTerm(-1, y1[i][j2][j1]);
								
						}
						
						if(j1 !=source-1 && j1 !=desti-1)
						{
							
							
							model.addConstr(expr7, GRB.EQUAL, 0, st);
							expr7 = null;
						}
						else
						{
							if(j1==source-1)
							{
								model.addConstr(expr7, GRB.EQUAL, 1, st);
								expr7 = null;
							}
							if(j1==desti-1)
							{
								model.addConstr(expr7, GRB.EQUAL, -1, st);
								expr7 = null;
							}
						}
					}
					
				}
				
				//Eq 11 new
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						if(j1!=j2 && (g.getEdgeWeight(j1+1, j2+1)>0))
						{
							for(int i=0;i<demandArray.size();i++)
								for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
								{
									GRBLinExpr exp = new GRBLinExpr();								
									exp.addTerm(1, phi[i][k][j1][j2]);
									exp.addTerm(-1,y1[i][j1][j2]);
									String st = "i1["+(i)+ "]["+(k)+ "]["+(j1+1)+"]["+(j2+1)+  "]";
									model.addConstr(exp, GRB.LESS_EQUAL, 0, st);
									
								}
						}
					}
				
				//Eq 11b new
				
				for (int i=0;i<demandArray.size();i++)
					for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
						for (int j=0;j<n;j++)
						{
							int source = demandArray.get(i).sourceS();
							int destination = demandArray.get(i).destinationS();
							GRBLinExpr exp = new GRBLinExpr();
							for (int j1=0;j1<n;j1++)
								for(int j2=0;j2<n;j2++)
								{
									if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0)
									{
										if(j==j1)
										{
											exp.addTerm(-1, phi[i][k][j1][j2]);
										}
										if(j==j2)
										{
											exp.addTerm(1, phi[i][k][j1][j2]);
										}
									}
								}
							if(k==0)
							{
								int f2 = demandArray.get(i).getFunctions()[k].id()-1;							
								exp.addTerm(-1, x1[i][f2][j]);
								String st = "i2["+(i)+ "]["+(k)+ "]["+(j+1)+ "]";
								if(j==source-1)
									model.addConstr(exp, GRB.EQUAL, -1, st);
								else
									model.addConstr(exp, GRB.EQUAL, 0, st);
								
							}
							else
							{
								if(k==demandArray.get(i).getFunctions().length)
								{
									int f1 = demandArray.get(i).getFunctions()[k-1].id()-1;
									exp.addTerm(1, x1[i][f1][j]);
									String st = "i2["+(i)+ "]["+(k)+ "]["+(j+1)+ "]";
									if(j==destination-1)
										model.addConstr(exp, GRB.EQUAL, 1, st);
									else
										model.addConstr(exp, GRB.EQUAL, 0, st);
								}
								else
								{
									int f1 = demandArray.get(i).getFunctions()[k-1].id()-1;
									exp.addTerm(1, x1[i][f1][j]);
									int f2 = demandArray.get(i).getFunctions()[k].id()-1;							
									exp.addTerm(-1, x1[i][f2][j]);
									String st = "i2["+(i)+ "]["+(k)+ "]["+(j+1)+ "]";
									model.addConstr(exp, GRB.EQUAL, 0, st);
								}
								
							}
							
						}
				
				//Eq 11 new
				
				for (int i=0;i<demandArray.size();i++)
					for(int j=0;j<n;j++)
					{
						for (int k=0;k<demandArray.get(i).getFunctions().length-1;k++)
						{
							int f1 = demandArray.get(i).getFunctions()[k].id()-1;
							int f2= demandArray.get(i).getFunctions()[k+1].id()-1;
							GRBLinExpr exp = new GRBLinExpr();								
							exp.addTerm(1, x1[i][f1][j]);
							exp.addTerm(1, x1[i][f2][j]);							
							String st = "i3["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
							model.addConstr(exp, GRB.LESS_EQUAL, 1, st);
						}
					}

				
//			
				//Eq (12)
				for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
						{
							int source = demandArray.get(i).sourceS();
							if(source != j+1)
							{
								GRBLinExpr exp = new GRBLinExpr();								
								exp.addTerm(1, x1[i][k][j]);
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j1+1, j+1)>0)
										exp.addTerm(-1, y1[i][j1][j]);
								String st = "k1["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp, GRB.LESS_EQUAL, 0, st);
								exp=null;
								GRBLinExpr exp1 = new GRBLinExpr();		
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j1+1, j+1)>0)
										exp1.addTerm(1, y1[i][j1][j]);
								st = "k2["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp1, GRB.LESS_EQUAL, 1, st);
								exp1=null;
							}	
							else
							{
								GRBLinExpr exp = new GRBLinExpr();								
								exp.addTerm(1, x1[i][k][j]);
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j+1, j1+1)>0)
										exp.addTerm(-1, y1[i][j][j1]);
								String st = "k3["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp, GRB.LESS_EQUAL, 0, st);
								exp=null;
								GRBLinExpr exp1 = new GRBLinExpr();		
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j+1, j1+1)>0)
										exp1.addTerm(1, y1[i][j][j1]);
								st = "k4["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp1, GRB.EQUAL, 1, st);
								exp1=null;
							}
							
						}

			
				model.update();
				// Optimize model
				try {
				      //GRBVar[] vars = model.getVars();
					
					
						//model.getEnv().set(GRB.DoubleParam.NodeLimit, 1);
					
						//model.optimize();
						GRBVar[] yFracSol = new GRBVar[n*n*d];
						int dem=0;
						double[][] w= new double[n][n];
						double[] b_d= new double[d];
						for (int j1=0;j1<n;j1++)
							for(int j2=0;j2<n;j2++)
							{
								w[j1][j2]=g.getEdgeWeight(j1+1, j2+1);
									for (int i=0;i<demandArray.size();i++)
									{
										
										yFracSol[dem]= y1[i][j1][j2];
										dem++;
									}
								
							}
						dem=0;
						for (int i=0;i<demandArray.size();i++)
						{
							
							b_d[dem]= demandArray.get(i).bwS();
							dem++;
						}
						
						GRBVar[] xFracSol = new GRBVar[d*m*n];
						dem=0;
						double[][] capLst = new double[n][3];
						double[][] lambdaLst = new double[m][3];
						for(int i=0;i<m;i++)
							for(int j=0;j<n;j++)
								for(int k=0;k<d;k++)
								{
									xFracSol[dem]=x1[k][i][j];
									dem++;
								}
						for(int i=0;i<3;i++)
						{
							for(int j=0;j<n;j++)
								capLst[j][i] = g.getCap(j+1).get(i);
							for(int j=0;j<m;j++)
								lambdaLst[j][i]= getFunction(j+1).getLamda().get(i);
						}
						//Callback cb   = new Callback(yFracSol,d,n,w,b_d);
						Callback1 cb   = new Callback1(xFracSol,yFracSol,d,n,m,w,b_d,capLst,lambdaLst,100000);
						model.setCallback(cb); 
						
						
		
//					double[] ySol = cb.getVar();
				     
//				     for (int i=0;i<ySol.length;i++)
//						{
//				    	 if(ySol[i]>0)
//			    			{
//				    		 System.out.println("Kqua: "+ ySol[i]);
//			    			}
//				    	 		
//						}
					
					//model.getEnv().set(GRB.DoubleParam.NodeLimit, GRB.INFINITY);
				   // model.optimize();
						model.getEnv().set(GRB.IntParam.FlowCoverCuts, 1);
						model.getEnv().set(GRB.IntParam.CoverCuts, 1);
				    model.update();
				    model.optimize();
					
					model.write("model1.lp");
					out.write("Solution for the problem:");
					out.newLine();
					
					int optimstatus = model.get(GRB.IntAttr.Status); 
					
						    if (optimstatus == GRB.Status.INF_OR_UNBD)
						    {
						      model.getEnv().set(GRB.IntParam.Presolve, 0);
						      model.optimize();
						      optimstatus = model.get(GRB.IntAttr.Status); 
						    }
					
					if (optimstatus == GRB.Status.OPTIMAL) 
					{ 
						//r_min= r.get(GRB.DoubleAttr.X);
						value_final = obj.getValue();
						out.write("Objective optimal Value: "+obj.getValue());
						out.newLine();
						for(int i = 0; i < demandArray.size(); i++) 
							for(int k = 0; k < m; k++)
								for(int j = 0; j < n; j++)
						    		{	
						    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
						    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
						for(int i = 0; i < demandArray.size(); i++) 
						    for(int j1 = 0; j1 < n; j1++)
						    	for(int j2 = 0; j2 < n; j2++)
						    		{	
						    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
						    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
						for (int i=0;i<demandArray.size();i++)
							for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
								for(int j1=0;j1<n;j1++)
									for(int j2=0;j2<n;j2++)
						    		{	
						    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
						    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
//						for(int i = 0; i < demandArray.size(); i++)
//							if(z1[i].get(GRB.DoubleAttr.X)>0)
//			    			{
//								//a_min++;
//			    			out.write(z1[i].get(GRB.StringAttr.VarName)
//			    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//			    			out.newLine();
//			    			}
////						out.write(r.get(GRB.StringAttr.VarName)
////		    					+ " : " +r.get(GRB.DoubleAttr.X));
		    			out.newLine();
				
					 } else if (optimstatus == GRB.Status.INF_OR_UNBD) 
					 	{ 
					        System.out.println("Model is infeasible or unbounded"); 
					        return;
					 	} else if (optimstatus == GRB.Status.INFEASIBLE) 
					        	{ 
							        System.out.println("Model is infeasible AAAAAAAAAAAAAA"); 
							        return; 
					        	} else if (optimstatus == GRB.Status.INTERRUPTED)
					        	{
					        		//r_min= r.get(GRB.DoubleAttr.X);
					        		value_final = obj.getValue();
					        		out.write("Objective interrupt Value: "+obj.getValue());
									out.newLine();
									for(int i = 0; i < demandArray.size(); i++) 
									for(int k = 0; k < m; k++)
										for(int j = 0; j < n; j++)
								    		{	
								    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
								    			{
								    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
								    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
								    			out.newLine();
								    			}
								    		}
									for (int i=0;i<demandArray.size();i++)
										for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
											for(int j1=0;j1<n;j1++)
												for(int j2=0;j2<n;j2++)
									    		{	
									    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
									    			{
									    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
									    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
									    			out.newLine();
									    			}
									    		}
									for(int i = 0; i < demandArray.size(); i++) 
									    for(int j1 = 0; j1 < n; j1++)
									    	for(int j2 = 0; j2 < n; j2++)
									    		{	
									    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
									    			{
									    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
									    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
									    			out.newLine();
									    			}
									    		}
//									for(int i = 0; i < demandArray.size(); i++)
//										if(z1[i].get(GRB.DoubleAttr.X)>0)
//						    			{
//											//a_min++;
//						    			out.write(z1[i].get(GRB.StringAttr.VarName)
//						    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//						    			out.newLine();
//						    			}
////									out.write(r.get(GRB.StringAttr.VarName)
////					    					+ " : " +r.get(GRB.DoubleAttr.X));
					    			out.newLine();
					        		
					        	}
					
					 else
					 {
						 //r_min= r.get(GRB.DoubleAttr.X);
						 value_final = obj.getValue();
						 out.write("Objective feasible Value: "+obj.getValue());
						 out.newLine();
							for(int i = 0; i < demandArray.size(); i++) 
							for(int k = 0; k < m; k++)
								for(int j = 0; j < n; j++)
						    		{	
						    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
						    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
							for (int i=0;i<demandArray.size();i++)
								for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
									for(int j1=0;j1<n;j1++)
										for(int j2=0;j2<n;j2++)
							    		{	
							    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
							    			{
							    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
							    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
							    			out.newLine();
							    			}
							    		}
							for(int i = 0; i < demandArray.size(); i++) 
							    for(int j1 = 0; j1 < n; j1++)
							    	for(int j2 = 0; j2 < n; j2++)
							    		{	
							    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
							    			{
							    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
							    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
							    			out.newLine();
							    			}
							    		}
//							for(int i = 0; i < demandArray.size(); i++)
//								if(z1[i].get(GRB.DoubleAttr.X)>0)
//				    			{
//									//a_min++;
//				    			out.write(z1[i].get(GRB.StringAttr.VarName)
//				    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//				    			out.newLine();
//				    			}
////							out.write(r.get(GRB.StringAttr.VarName)
////			    					+ " : " +r.get(GRB.DoubleAttr.X));
			    			out.newLine();
							
					  }
				
					
				} catch (Exception e) {
					//r_min= r.get(GRB.DoubleAttr.X);
					value_final = obj.getValue();
					out.write("Objective interrupt Value: "+obj.getValue());
					out.newLine();
					for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
				    		{	
				    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
				    			{
				    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
				    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
				    			out.newLine();
				    			}
				    		}
					for (int i=0;i<demandArray.size();i++)
						for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
							for(int j1=0;j1<n;j1++)
								for(int j2=0;j2<n;j2++)
					    		{	
					    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
					    			{
					    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
					    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
					    			out.newLine();
					    			}
					    		}
					for(int i = 0; i < demandArray.size(); i++) 
					    for(int j1 = 0; j1 < n; j1++)
					    	for(int j2 = 0; j2 < n; j2++)
					    		{	
					    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
					    			{
					    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
					    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
					    			out.newLine();
					    			}
					    		}
//					for(int i = 0; i < demandArray.size(); i++)
//						if(z1[i].get(GRB.DoubleAttr.X)>0)
//		    			{
//							//a_min++;
//		    			out.write(z1[i].get(GRB.StringAttr.VarName)
//		    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//		    			out.newLine();
//		    			}
////					out.write(r.get(GRB.StringAttr.VarName)
////	    					+ " : " +r.get(GRB.DoubleAttr.X));
	    			out.newLine();
					
	
				}
					model.dispose();
				env.dispose();
				System.gc();
			
				} catch(GRBException e3){			
					System.out.println("Error code1: " + e3.getErrorCode() + ". " +
							e3.getMessage());
					System.out.print("This problem can't be solved");
					
					
					}
			} catch ( IOException e1 ) {
					e1.printStackTrace();
					} finally {
						if ( out != null )
							try {
								out.close();
								} catch (IOException e) {
									e.printStackTrace();}
						}    
			try {
		  		out.close();
		  		} catch (IOException e2) {
		  			e2.printStackTrace();
		  			}
	
	
	
	
	}
	public static void Model_cover(String outFile)
	{

		try {

			File file = new File(outFile);
			out = new BufferedWriter(new FileWriter(file));
			out.write("number of function:" + m);
			out.newLine();
			for (int i=0;i<m;i++)
	       	{	 
	               out.write(functionArr[i].toString());
	               out.newLine();
	       	}
	   		out.write("number of Demand:" + d);
	   		out.newLine();
	       	for (int i=0;i<d;i++)
	       	{    		
	       		out.write(demandArray.get(i).toString());
	       		out.newLine();
	       	}
	       	out.write("virtual node:"+ n);
	       	out.newLine();
	       	for (int i=0;i<n;i++)
	       	{
	       		for (int j=0;j<n;j++)
	       			out.write(g.getEdgeWeight(i+1, j+1) + " ");
	       		out.newLine();
	       	}
	       	
	       	
	       	
			try{
				GRBEnv env = new GRBEnv("qp.log");
				env.set(GRB.DoubleParam.MIPGap, 0.000000001);
				env.set(GRB.DoubleParam.FeasibilityTol, 0.000000001);
				env.set(GRB.IntParam.Threads,24);
				env.set(GRB.DoubleParam.TimeLimit,4000);
				GRBModel model = new GRBModel(env);
				
				GRBLinExpr obj = new GRBLinExpr();
	
				
				//variable declaire
				
			
				for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
				    		{
				    			String st = "x1["+(i+1)+ "]["+(k+1)+ "]["+(j+1)+ "]";
				    			x1[i][k][j] = model.addVar(0, 1, 0, GRB.BINARY, st);
				    		}
				for(int i = 0; i < demandArray.size(); i++) 
				    for(int j = 0; j < n; j++)
				    	for(int k = 0; k < n; k++)
				    		{
				    			if(g.getEdgeWeight(j+1, k+1)>0)
				    			{
				    				String st = "y1["+(i+1)+ "]["+(j+1)+ "]["+(k+1)+ "]";
				    				y1[i][j][k] = model.addVar(0, 1, 0, GRB.BINARY, st);
				    			}
				    		}
				
				for (int i=0;i<demandArray.size();i++)
					for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
						for(int j1=0;j1<n;j1++)
							for(int j2=0;j2<n;j2++)
							{
								if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0)
								{
									String st = "phi["+(i+1)+ "]["+(k+1)+ "]["+(j1+1)+ "]["+(j2+1)+ "]";
				    				phi[i][k][j1][j2] = model.addVar(0, 1, 0, GRB.CONTINUOUS, st);
								}
							}
						
				
				model.update();
				String st1 = "r_l";
				r_l = model.addVar(0, 1, 0, GRB.CONTINUOUS, st1);
				st1 = "r_n";
				r_n = model.addVar(0, 1, 0, GRB.CONTINUOUS, st1);
				model.update();

				obj.addTerm(1, r_l);
				obj.addTerm(1, r_n);
				//ham muc tieu
//				for (int j1=0;j1<n;j1++)
//					for(int j2=0;j2<n;j2++)
//					{
//						if(g.getEdgeWeight(j1+1, j2+1)>0)
//						{
//							for(int i = 0; i < demandArray.size(); i++) 
//							{
//								obj.addTerm(g.getEdgeWeight(j1+1, j2+1)*g.getPriceBandwidth()*0.0001 , y1[i][j1][j2]);
//								//obj.addTerm(g.getEdgeWeight(j1+1, j2+1)*g.getPriceBandwidth()*0.0001 , y1[i][j2][j1]);
//							}
//						}
//					}
//				for(int j = 0; j < n; j++)
//					for(int i = 0; i < demandArray.size(); i++)
//						for(int k = 0; k < m; k++)
//				    		{
//				    			obj.addTerm(g.getPriceNode(j+1), x1[i][k][j]);
//				    		}
				model.setObjective(obj,GRB.MINIMIZE);		
				//add constraints
				
					
//				
				//Eq (5) ->Ok
				for(int compo=0;compo<3;compo++)
				{
					for(int j = 0; j < n; j++) //node
			    	{
						GRBLinExpr expr1= new GRBLinExpr();
						for(int i = 0; i < demandArray.size(); i++) //demand
							for(int k = 0; k < m; k++) //function
							{
									expr1.addTerm(getFunction(k+1).getLamda().get(compo),x1[i][k][j]);
							}
						String st = "c["+(j)+ "]["+compo+"]";
						model.addConstr(expr1, GRB.LESS_EQUAL, g.getCap(j+1).get(compo) , st);
						expr1 = null;
			    	}
				}
				System.gc();
				
				//Eq (6)->OK
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						if(g.getEdgeWeight(j1+1, j2+1)>0)
						{
							GRBLinExpr expr2= new GRBLinExpr();
							for (int i =0;i<demandArray.size();i++) //demand
							{
								expr2.addTerm(demandArray.get(i).bwS(),y1[i][j1][j2]);
								//expr2.addTerm(demandArray.get(i).bwS(),y1[i][j2][j1]);
							}
							//expr2.addTerm(-g.getEdgeWeight(j1+1, j2+1),1);
							String st = "d["+(j1+1)+ "]["+(j2+1)+ "]";
							model.addConstr(expr2, GRB.LESS_EQUAL,g.getEdgeWeight(j1+1, j2+1), st);
							expr2 = null;	
						}
						
					}
				
			
				System.gc();
				
				//Eq (8)
				for (int i =0;i<d;i++) //demand
					for (int k = 0;k<m;k++)//k is a function
					{
						GRBLinExpr expr3 = new GRBLinExpr();
						for (int j=0;j<n;j++)// j is a node
						{
							expr3.addTerm(1, x1[i][k][j]);
						}
						int id = getDemand(i+1).getOrderFunction(k+1);
						String st = "f["+(i)+ "]["+(k)+ "]";
						if (id!=0)//truong hop function in demand =1
						{
							//expr3.addTerm(-1, z1[i]);
							model.addConstr(expr3, GRB.EQUAL, 1, st);
						}
						else
							model.addConstr(expr3, GRB.EQUAL, 0, st);
						
						
						expr3 = null;
					}
				//Eq 9
				for (int i =0;i<d;i++) //demand
					for (int j1=0;j1<n;j1++)
						for(int j2=0;j2<n;j2++)
						{
							if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0&& g.getEdgeWeight(j2+1, j1+1)>0)
							{
								GRBLinExpr expr3 = new GRBLinExpr();
								expr3.addTerm(1, y1[i][j1][j2]);
								expr3.addTerm(1, y1[i][j2][j1]);
								String st = "g["+(i)+ "]["+(j1)+ "]["+(j2)+"]";
								model.addConstr(expr3, GRB.LESS_EQUAL, 1, st);
								expr3 = null;
							}
							
						}
				System.gc();
				//Eq (10) ->ok
				for (int i=0;i<demandArray.size();i++)
				{
					int desti = demandArray.get(i).destinationS();
					int source = demandArray.get(i).sourceS();
					for (int j1=0;j1<n;j1++)
					{
						GRBLinExpr expr7= new GRBLinExpr();
						String st = "h1["+(i)+ "]["+(j1+1)+  "s]";
						for (int j2=0;j2<n;j2++)
						{
							if(g.getEdgeWeight(j1+1, j2+1)>0)
							{
								
								expr7.addTerm(1, y1[i][j1][j2]);
							}
							if(g.getEdgeWeight(j2+1, j1+1)>0)
								expr7.addTerm(-1, y1[i][j2][j1]);
								
						}
						
						if(j1 !=source-1 && j1 !=desti-1)
						{
							
							
							model.addConstr(expr7, GRB.EQUAL, 0, st);
							expr7 = null;
						}
						else
						{
							if(j1==source-1)
							{
								model.addConstr(expr7, GRB.EQUAL, 1, st);
								expr7 = null;
							}
							if(j1==desti-1)
							{
								model.addConstr(expr7, GRB.EQUAL, -1, st);
								expr7 = null;
							}
						}
					}
					
				}
				
				//Eq 11 new
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						if(j1!=j2 && (g.getEdgeWeight(j1+1, j2+1)>0))
						{
							for(int i=0;i<demandArray.size();i++)
								for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
								{
									GRBLinExpr exp = new GRBLinExpr();								
									exp.addTerm(1, phi[i][k][j1][j2]);
									exp.addTerm(-1,y1[i][j1][j2]);
									String st = "i1["+(i)+ "]["+(k)+ "]["+(j1+1)+"]["+(j2+1)+  "]";
									model.addConstr(exp, GRB.LESS_EQUAL, 0, st);
									
								}
						}
					}
				
				//Eq 11b new
				
				for (int i=0;i<demandArray.size();i++)
					for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
						for (int j=0;j<n;j++)
						{
							int source = demandArray.get(i).sourceS();
							int destination = demandArray.get(i).destinationS();
							GRBLinExpr exp = new GRBLinExpr();
							for (int j1=0;j1<n;j1++)
								for(int j2=0;j2<n;j2++)
								{
									if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0)
									{
										if(j==j1)
										{
											exp.addTerm(-1, phi[i][k][j1][j2]);
										}
										if(j==j2)
										{
											exp.addTerm(1, phi[i][k][j1][j2]);
										}
									}
								}
							if(k==0)
							{
								int f2 = demandArray.get(i).getFunctions()[k].id()-1;							
								exp.addTerm(-1, x1[i][f2][j]);
								String st = "i2["+(i)+ "]["+(k)+ "]["+(j+1)+ "]";
								if(j==source-1)
									model.addConstr(exp, GRB.EQUAL, -1, st);
								else
									model.addConstr(exp, GRB.EQUAL, 0, st);
								
							}
							else
							{
								if(k==demandArray.get(i).getFunctions().length)
								{
									int f1 = demandArray.get(i).getFunctions()[k-1].id()-1;
									exp.addTerm(1, x1[i][f1][j]);
									String st = "i2["+(i)+ "]["+(k)+ "]["+(j+1)+ "]";
									if(j==destination-1)
										model.addConstr(exp, GRB.EQUAL, 1, st);
									else
										model.addConstr(exp, GRB.EQUAL, 0, st);
								}
								else
								{
									int f1 = demandArray.get(i).getFunctions()[k-1].id()-1;
									exp.addTerm(1, x1[i][f1][j]);
									int f2 = demandArray.get(i).getFunctions()[k].id()-1;							
									exp.addTerm(-1, x1[i][f2][j]);
									String st = "i2["+(i)+ "]["+(k)+ "]["+(j+1)+ "]";
									model.addConstr(exp, GRB.EQUAL, 0, st);
								}
								
							}
							
						}
				
				//Eq 11 new
				
				for (int i=0;i<demandArray.size();i++)
					for(int j=0;j<n;j++)
					{
						for (int k=0;k<demandArray.get(i).getFunctions().length-1;k++)
						{
							int f1 = demandArray.get(i).getFunctions()[k].id()-1;
							int f2= demandArray.get(i).getFunctions()[k+1].id()-1;
							GRBLinExpr exp = new GRBLinExpr();								
							exp.addTerm(1, x1[i][f1][j]);
							exp.addTerm(1, x1[i][f2][j]);							
							String st = "i3["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
							model.addConstr(exp, GRB.LESS_EQUAL, 1, st);
						}
					}

				
//			
				//Eq (12)
				for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
						{
							int source = demandArray.get(i).sourceS();
							if(source != j+1)
							{
								GRBLinExpr exp = new GRBLinExpr();								
								exp.addTerm(1, x1[i][k][j]);
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j1+1, j+1)>0)
										exp.addTerm(-1, y1[i][j1][j]);
								String st = "k1["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp, GRB.LESS_EQUAL, 0, st);
								exp=null;
								GRBLinExpr exp1 = new GRBLinExpr();		
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j1+1, j+1)>0)
										exp1.addTerm(1, y1[i][j1][j]);
								st = "k2["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp1, GRB.LESS_EQUAL, 1, st);
								exp1=null;
							}	
							else
							{
								GRBLinExpr exp = new GRBLinExpr();								
								exp.addTerm(1, x1[i][k][j]);
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j+1, j1+1)>0)
										exp.addTerm(-1, y1[i][j][j1]);
								String st = "k3["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp, GRB.LESS_EQUAL, 0, st);
								exp=null;
								GRBLinExpr exp1 = new GRBLinExpr();		
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j+1, j1+1)>0)
										exp1.addTerm(1, y1[i][j][j1]);
								st = "k4["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp1, GRB.EQUAL, 1, st);
								exp1=null;
							}
							
						}

				
			
				System.gc();
				
			
				
				// Optimize model
				try {
					model.getEnv().set(GRB.DoubleParam.NodeLimit, 1);
					model.optimize();
					
					ArrayList<Integer> demandLst = sortDecreasing();
					//index in demandArrr
					
					for (int j1=0;j1<n;j1++)
						for(int j2=0;j2<n;j2++)
						{
							double w= g.getEdgeWeight(j1+1, j2+1);
							if(w>0)
							{
								double sum = 0.0;
								int id=-1;
								ArrayList<Double> bwLst = new ArrayList<>();
								for(int i=0;i<demandLst.size();i++)
								{
									sum+=demandArray.get(demandLst.get(i)).bwS();
									
									if(sum>w)
									{
										id=i+1;
										break;
									}
								}
								if (id == -1)
									continue;
								for(int i=0;i<demandLst.size();i++)
								{
									bwLst.add(demandArray.get(demandLst.get(i)).bwS());
								}
								ArrayList<Integer> indexCover = minimalCover(bwLst, id,w);
								//index in demandLst
								
								
								GRBLinExpr exp = new GRBLinExpr();	
								String st = "cover["+(j1)+ "]["+(j2)+ "]";
								sum =0.0;
								for(int i=0;i<indexCover.size();i++)
								{
									int idDemand = demandLst.get(indexCover.get(i));
									double bwD = demandArray.get(idDemand).bwS();
									sum+=bwD;
								}
								double lambda = sum-w;
								for(int i=0;i<indexCover.size();i++)
								{
									int idDemand = demandLst.get(indexCover.get(i));
									double bwD = demandArray.get(idDemand).bwS();
									
									
									exp.addTerm(bwD, y1[idDemand][j1][j2]);
									
									
									if(bwD>lambda)
									{
										exp.addConstant(bwD-lambda);
										exp.addTerm(lambda-bwD,y1[idDemand][j1][j2] );
									}
								}
								model.addConstr(exp, GRB.LESS_EQUAL, w, st);
								exp=null;
							}						
							
						}
					
					for(int compo=0;compo<3;compo++)
					{
						for(int j = 0; j < n; j++) //node
				    	{
							GRBLinExpr expr1= new GRBLinExpr();
							ArrayList<Integer> fLst = sortFuncDecreasing(compo);
							double cap = g.getCap(j+1).get(compo);
							double sum = 0.0;
							int dem=0;
							int idDemand=-1;
							int idFuncLst =-1;
							for(int i1=0;i1<fLst.size();i1++)
							{
								for(int i2=0;i2<demandArray.size();i2++)
								{
									dem++;
									sum+=getFunction(fLst.get(i1)).getLamda().get(compo);
									if(sum>cap)
									{									
										idDemand=i2+1;
										idFuncLst=i1+1;
										break;
									}
								}
							}
							if (idDemand == -1)
								continue;
							ArrayList<Double> bwLst = new ArrayList<>();
							for(int i1=0;i1<fLst.size();i1++)
							{
								for(int i2=0;i2<demandArray.size();i2++)
								{
									bwLst.add(getFunction(fLst.get(i1)).getLamda().get(compo));
								}
							}
							ArrayList<Integer> indexCover = minimalCover(bwLst, dem,cap);
							for(int i=0;i<indexCover.size();i++)
							{
								int fID = fLst.get(indexCover.get(i)/demandArray.size());
								int dID = indexCover.get(i)%demandArray.size();
								expr1.addTerm(1, x1[dID][fID-1][j]);							
								
							}
//							for(int i1=0;i1<idFuncLst;i1++)
//							{
//								int fID = fLst.get(i1)-1;
//								for(int i2=0;i2<idDemand;i2++)
//								{
//									int dID = demandArray.get(i2).idS()-1;								
//									expr1.addTerm(1, x1[dID][fID][j]);
//								}
//							}
							String st = "IC["+(j)+ "]["+compo+"]";
							model.addConstr(expr1, GRB.LESS_EQUAL, dem-1 , st);
							expr1 = null;
				    	}
					}
					model.getEnv().set(GRB.DoubleParam.NodeLimit, GRB.INFINITY);
					model.optimize();
					//model.write("model1.lp");
					out.write("Solution for the problem:");
					out.newLine();
				
					int optimstatus = model.get(GRB.IntAttr.Status); 
					if (optimstatus == GRB.Status.OPTIMAL) 
					{ 
						//r_min= r.get(GRB.DoubleAttr.X);
						value_final = obj.getValue();
						out.write("Objective optimal Value: "+obj.getValue());
						out.newLine();
						for(int i = 0; i < demandArray.size(); i++) 
							for(int k = 0; k < m; k++)
								for(int j = 0; j < n; j++)
						    		{	
						    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
						    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
						for(int i = 0; i < demandArray.size(); i++) 
						    for(int j1 = 0; j1 < n; j1++)
						    	for(int j2 = 0; j2 < n; j2++)
						    		{	
						    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
						    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
						for (int i=0;i<demandArray.size();i++)
							for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
								for(int j1=0;j1<n;j1++)
									for(int j2=0;j2<n;j2++)
						    		{	
						    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
						    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
//						for(int i = 0; i < demandArray.size(); i++)
//							if(z1[i].get(GRB.DoubleAttr.X)>0)
//			    			{
//								//a_min++;
//			    			out.write(z1[i].get(GRB.StringAttr.VarName)
//			    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//			    			out.newLine();
//			    			}
////						out.write(r.get(GRB.StringAttr.VarName)
////		    					+ " : " +r.get(GRB.DoubleAttr.X));
		    			out.newLine();
				
					 } else if (optimstatus == GRB.Status.INF_OR_UNBD) 
					 	{ 
					        System.out.println("Model is infeasible or unbounded"); 
					        return;
					 	} else if (optimstatus == GRB.Status.INFEASIBLE) 
					        	{ 
							        System.out.println("Model is infeasible AAAAAAAAAAAAAA"); 
							        return; 
					        	} else if (optimstatus == GRB.Status.INTERRUPTED)
					        	{
					        		//r_min= r.get(GRB.DoubleAttr.X);
					        		value_final = obj.getValue();
					        		out.write("Objective interrupt Value: "+obj.getValue());
									out.newLine();
									for(int i = 0; i < demandArray.size(); i++) 
									for(int k = 0; k < m; k++)
										for(int j = 0; j < n; j++)
								    		{	
								    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
								    			{
								    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
								    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
								    			out.newLine();
								    			}
								    		}
									for (int i=0;i<demandArray.size();i++)
										for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
											for(int j1=0;j1<n;j1++)
												for(int j2=0;j2<n;j2++)
									    		{	
									    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
									    			{
									    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
									    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
									    			out.newLine();
									    			}
									    		}
									for(int i = 0; i < demandArray.size(); i++) 
									    for(int j1 = 0; j1 < n; j1++)
									    	for(int j2 = 0; j2 < n; j2++)
									    		{	
									    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
									    			{
									    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
									    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
									    			out.newLine();
									    			}
									    		}
//									for(int i = 0; i < demandArray.size(); i++)
//										if(z1[i].get(GRB.DoubleAttr.X)>0)
//						    			{
//											//a_min++;
//						    			out.write(z1[i].get(GRB.StringAttr.VarName)
//						    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//						    			out.newLine();
//						    			}
////									out.write(r.get(GRB.StringAttr.VarName)
////					    					+ " : " +r.get(GRB.DoubleAttr.X));
					    			out.newLine();
					        		
					        	}
					
					 else
					 {
						 //r_min= r.get(GRB.DoubleAttr.X);
						 value_final = obj.getValue();
						 out.write("Objective feasible Value: "+obj.getValue());
						 out.newLine();
							for(int i = 0; i < demandArray.size(); i++) 
							for(int k = 0; k < m; k++)
								for(int j = 0; j < n; j++)
						    		{	
						    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
						    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
							for (int i=0;i<demandArray.size();i++)
								for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
									for(int j1=0;j1<n;j1++)
										for(int j2=0;j2<n;j2++)
							    		{	
							    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
							    			{
							    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
							    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
							    			out.newLine();
							    			}
							    		}
							for(int i = 0; i < demandArray.size(); i++) 
							    for(int j1 = 0; j1 < n; j1++)
							    	for(int j2 = 0; j2 < n; j2++)
							    		{	
							    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
							    			{
							    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
							    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
							    			out.newLine();
							    			}
							    		}
//							for(int i = 0; i < demandArray.size(); i++)
//								if(z1[i].get(GRB.DoubleAttr.X)>0)
//				    			{
//									//a_min++;
//				    			out.write(z1[i].get(GRB.StringAttr.VarName)
//				    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//				    			out.newLine();
//				    			}
////							out.write(r.get(GRB.StringAttr.VarName)
////			    					+ " : " +r.get(GRB.DoubleAttr.X));
			    			out.newLine();
							
					  }
				
					
				} catch (Exception e) {
					//r_min= r.get(GRB.DoubleAttr.X);
					value_final = obj.getValue();
					out.write("Objective interrupt Value: "+obj.getValue());
					out.newLine();
					for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
				    		{	
				    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
				    			{
				    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
				    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
				    			out.newLine();
				    			}
				    		}
					for (int i=0;i<demandArray.size();i++)
						for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
							for(int j1=0;j1<n;j1++)
								for(int j2=0;j2<n;j2++)
					    		{	
					    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
					    			{
					    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
					    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
					    			out.newLine();
					    			}
					    		}
					for(int i = 0; i < demandArray.size(); i++) 
					    for(int j1 = 0; j1 < n; j1++)
					    	for(int j2 = 0; j2 < n; j2++)
					    		{	
					    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
					    			{
					    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
					    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
					    			out.newLine();
					    			}
					    		}
//					for(int i = 0; i < demandArray.size(); i++)
//						if(z1[i].get(GRB.DoubleAttr.X)>0)
//		    			{
//							//a_min++;
//		    			out.write(z1[i].get(GRB.StringAttr.VarName)
//		    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//		    			out.newLine();
//		    			}
////					out.write(r.get(GRB.StringAttr.VarName)
////	    					+ " : " +r.get(GRB.DoubleAttr.X));
	    			out.newLine();
					
	
				}
					model.dispose();
				env.dispose();
				System.gc();
			
				} catch(GRBException e3){			
					System.out.println("Error code1: " + e3.getErrorCode() + ". " +
							e3.getMessage());
					System.out.print("This problem can't be solved");
					
					
					}
			} catch ( IOException e1 ) {
					e1.printStackTrace();
					} finally {
						if ( out != null )
							try {
								out.close();
								} catch (IOException e) {
									e.printStackTrace();}
						}    
			try {
		  		out.close();
		  		} catch (IOException e2) {
		  			e2.printStackTrace();
		  			}
	
	
	
	}
	
	public static void Model_MM(String outFile)
	{
		try {

			File file = new File(outFile);
			out = new BufferedWriter(new FileWriter(file));
			out.write("number of function:" + m);
			out.newLine();
			for (int i=0;i<m;i++)
	       	{	 
	               out.write(functionArr[i].toString());
	               out.newLine();
	       	}
	   		out.write("number of Demand:" + d);
	   		out.newLine();
	       	for (int i=0;i<d;i++)
	       	{    		
	       		out.write(demandArray.get(i).toString());
	       		out.newLine();
	       	}
	       	out.write("virtual node:"+ n);
	       	out.newLine();
	       	for (int i=0;i<n;i++)
	       	{
	       		for (int j=0;j<n;j++)
	       			out.write(g.getEdgeWeight(i+1, j+1) + " ");
	       		out.newLine();
	       	}
	       	
	       	
	       	
			try{
				GRBEnv env = new GRBEnv("qp.log");
				env.set(GRB.DoubleParam.MIPGap, 0.000000001);
				env.set(GRB.DoubleParam.FeasibilityTol, 0.000000001);
				env.set(GRB.IntParam.Threads,24);
				env.set(GRB.DoubleParam.TimeLimit,4000);
				GRBModel model = new GRBModel(env);
				
				GRBLinExpr obj = new GRBLinExpr();
	
				
				//variable declaire
				
			
				for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
				    		{
				    			String st = "x1["+(i+1)+ "]["+(k+1)+ "]["+(j+1)+ "]";
				    			x1[i][k][j] = model.addVar(0, 1, 0, GRB.INTEGER, st);
				    		}
				for(int i = 0; i < demandArray.size(); i++) 
				    for(int j = 0; j < n; j++)
				    	for(int k = 0; k < n; k++)
				    		{
				    			if(g.getEdgeWeight(j+1, k+1)>0)
				    			{
				    				String st = "y1["+(i+1)+ "]["+(j+1)+ "]["+(k+1)+ "]";
				    				y1[i][j][k] = model.addVar(0, 1, 0, GRB.INTEGER, st);
				    			}
				    		}
				
				for (int i=0;i<demandArray.size();i++)
					for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
						for(int j1=0;j1<n;j1++)
							for(int j2=0;j2<n;j2++)
							{
								if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0)
								{
									String st = "phi["+(i+1)+ "]["+(k+1)+ "]["+(j1+1)+ "]["+(j2+1)+ "]";
				    				phi[i][k][j1][j2] = model.addVar(0, 1, 0, GRB.CONTINUOUS, st);
								}
							}
						
				String st1 = "r_l";
				r_l = model.addVar(0, 1, 0, GRB.CONTINUOUS, st1);
				st1 = "r_n";
				r_n = model.addVar(0, 1, 0, GRB.CONTINUOUS, st1);
				model.update();

				obj.addTerm(1, r_l);
				obj.addTerm(1, r_n);
				//ham muc tieu
//				for (int j1=0;j1<n;j1++)
//					for(int j2=0;j2<n;j2++)
//					{
//						if(g.getEdgeWeight(j1+1, j2+1)>0)
//						{
//							for(int i = 0; i < demandArray.size(); i++) 
//							{
//								obj.addTerm(g.getEdgeWeight(j1+1, j2+1)*g.getPriceBandwidth()*0.0001 , y1[i][j1][j2]);
//								//obj.addTerm(g.getEdgeWeight(j1+1, j2+1)*g.getPriceBandwidth()*0.0001 , y1[i][j2][j1]);
//							}
//						}
//					}
//				for(int j = 0; j < n; j++)
//					for(int i = 0; i < demandArray.size(); i++)
//						for(int k = 0; k < m; k++)
//				    		{
//				    			obj.addTerm(g.getPriceNode(j+1), x1[i][k][j]);
//				    		}
				model.setObjective(obj,GRB.MINIMIZE);		
				//add constraints
				
					
//				
				//Eq (5) ->Ok
				for(int compo=0;compo<3;compo++)
				{
					for(int j = 0; j < n; j++) //node
			    	{
						GRBLinExpr expr1= new GRBLinExpr();
						for(int i = 0; i < demandArray.size(); i++) //demand
							for(int k = 0; k < m; k++) //function
							{
									expr1.addTerm(getFunction(k+1).getLamda().get(compo),x1[i][k][j]);
							}
						expr1.addTerm(-g.getCap(j+1).get(compo),r_n);
						String st = "c["+(j)+ "]["+compo+"]";
						model.addConstr(expr1, GRB.LESS_EQUAL, 0 , st);
						expr1 = null;
			    	}
				}
				System.gc();
				
				//Eq (6)->OK
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						if(g.getEdgeWeight(j1+1, j2+1)>0)
						{
							GRBLinExpr expr2= new GRBLinExpr();
							for (int i =0;i<demandArray.size();i++) //demand
							{
								expr2.addTerm(demandArray.get(i).bwS(),y1[i][j1][j2]);
								//expr2.addTerm(demandArray.get(i).bwS(),y1[i][j2][j1]);
							}
							expr2.addTerm(-g.getEdgeWeight(j1+1, j2+1),r_l);
							String st = "d["+(j1+1)+ "]["+(j2+1)+ "]";
							model.addConstr(expr2, GRB.LESS_EQUAL,0, st);
							expr2 = null;	
						}
						
					}
				
			
				System.gc();
				
				//Eq (8)
				for (int i =0;i<d;i++) //demand
					for (int k = 0;k<m;k++)//k is a function
					{
						GRBLinExpr expr3 = new GRBLinExpr();
						for (int j=0;j<n;j++)// j is a node
						{
							expr3.addTerm(1, x1[i][k][j]);
						}
						int id = getDemand(i+1).getOrderFunction(k+1);
						String st = "f["+(i)+ "]["+(k)+ "]";
						if (id!=0)//truong hop function in demand =1
						{
							//expr3.addTerm(-1, z1[i]);
							model.addConstr(expr3, GRB.EQUAL, 1, st);
						}
						else
							model.addConstr(expr3, GRB.EQUAL, 0, st);
						
						
						expr3 = null;
					}
				//Eq 9
				for (int i =0;i<d;i++) //demand
					for (int j1=0;j1<n;j1++)
						for(int j2=0;j2<n;j2++)
						{
							if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0&& g.getEdgeWeight(j2+1, j1+1)>0)
							{
								GRBLinExpr expr3 = new GRBLinExpr();
								expr3.addTerm(1, y1[i][j1][j2]);
								expr3.addTerm(1, y1[i][j2][j1]);
								String st = "g["+(i)+ "]["+(j1)+ "]["+(j2)+"]";
								model.addConstr(expr3, GRB.LESS_EQUAL, 1, st);
								expr3 = null;
							}
							
						}
				System.gc();
				//Eq (10) ->ok
				for (int i=0;i<demandArray.size();i++)
				{
					int desti = demandArray.get(i).destinationS();
					int source = demandArray.get(i).sourceS();
					for (int j1=0;j1<n;j1++)
					{
						GRBLinExpr expr7= new GRBLinExpr();
						String st = "h1["+(i)+ "]["+(j1+1)+  "s]";
						for (int j2=0;j2<n;j2++)
						{
							if(g.getEdgeWeight(j1+1, j2+1)>0)
							{
								
								expr7.addTerm(1, y1[i][j1][j2]);
							}
							if(g.getEdgeWeight(j2+1, j1+1)>0)
								expr7.addTerm(-1, y1[i][j2][j1]);
								
						}
						
						if(j1 !=source-1 && j1 !=desti-1)
						{
							
							
							model.addConstr(expr7, GRB.EQUAL, 0, st);
							expr7 = null;
						}
						else
						{
							if(j1==source-1)
							{
								model.addConstr(expr7, GRB.EQUAL, 1, st);
								expr7 = null;
							}
							if(j1==desti-1)
							{
								model.addConstr(expr7, GRB.EQUAL, -1, st);
								expr7 = null;
							}
						}
					}
					
				}
				
				//Eq 11 new
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						if(j1!=j2 && (g.getEdgeWeight(j1+1, j2+1)>0))
						{
							for(int i=0;i<demandArray.size();i++)
								for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
								{
									GRBLinExpr exp = new GRBLinExpr();								
									exp.addTerm(1, phi[i][k][j1][j2]);
									exp.addTerm(-1,y1[i][j1][j2]);
									String st = "i1["+(i)+ "]["+(k)+ "]["+(j1+1)+"]["+(j2+1)+  "]";
									model.addConstr(exp, GRB.LESS_EQUAL, 0, st);
									
								}
						}
					}
				
				//Eq 11b new
				
				for (int i=0;i<demandArray.size();i++)
					for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
						for (int j=0;j<n;j++)
						{
							int source = demandArray.get(i).sourceS();
							int destination = demandArray.get(i).destinationS();
							GRBLinExpr exp = new GRBLinExpr();
							for (int j1=0;j1<n;j1++)
								for(int j2=0;j2<n;j2++)
								{
									if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0)
									{
										if(j==j1)
										{
											exp.addTerm(-1, phi[i][k][j1][j2]);
										}
										if(j==j2)
										{
											exp.addTerm(1, phi[i][k][j1][j2]);
										}
									}
								}
							if(k==0)
							{
								int f2 = demandArray.get(i).getFunctions()[k].id()-1;							
								exp.addTerm(-1, x1[i][f2][j]);
								String st = "i2["+(i)+ "]["+(k)+ "]["+(j+1)+ "]";
								if(j==source-1)
									model.addConstr(exp, GRB.EQUAL, -1, st);
								else
									model.addConstr(exp, GRB.EQUAL, 0, st);
								
							}
							else
							{
								if(k==demandArray.get(i).getFunctions().length)
								{
									int f1 = demandArray.get(i).getFunctions()[k-1].id()-1;
									exp.addTerm(1, x1[i][f1][j]);
									String st = "i2["+(i)+ "]["+(k)+ "]["+(j+1)+ "]";
									if(j==destination-1)
										model.addConstr(exp, GRB.EQUAL, 1, st);
									else
										model.addConstr(exp, GRB.EQUAL, 0, st);
								}
								else
								{
									int f1 = demandArray.get(i).getFunctions()[k-1].id()-1;
									exp.addTerm(1, x1[i][f1][j]);
									int f2 = demandArray.get(i).getFunctions()[k].id()-1;							
									exp.addTerm(-1, x1[i][f2][j]);
									String st = "i2["+(i)+ "]["+(k)+ "]["+(j+1)+ "]";
									model.addConstr(exp, GRB.EQUAL, 0, st);
								}
								
							}
							
						}
				
				//Eq 11 new
				
				for (int i=0;i<demandArray.size();i++)
					for(int j=0;j<n;j++)
					{
						for (int k=0;k<demandArray.get(i).getFunctions().length-1;k++)
						{
							int f1 = demandArray.get(i).getFunctions()[k].id()-1;
							int f2= demandArray.get(i).getFunctions()[k+1].id()-1;
							GRBLinExpr exp = new GRBLinExpr();								
							exp.addTerm(1, x1[i][f1][j]);
							exp.addTerm(1, x1[i][f2][j]);							
							String st = "i3["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
							model.addConstr(exp, GRB.LESS_EQUAL, 1, st);
						}
					}

				
//			
				//Eq (12)
				for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
						{
							int source = demandArray.get(i).sourceS();
							if(source != j+1)
							{
								GRBLinExpr exp = new GRBLinExpr();								
								exp.addTerm(1, x1[i][k][j]);
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j1+1, j+1)>0)
										exp.addTerm(-1, y1[i][j1][j]);
								String st = "k1["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp, GRB.LESS_EQUAL, 0, st);
								exp=null;
								GRBLinExpr exp1 = new GRBLinExpr();		
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j1+1, j+1)>0)
										exp1.addTerm(1, y1[i][j1][j]);
								st = "k2["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp1, GRB.LESS_EQUAL, 1, st);
								exp1=null;
							}	
							else
							{
								GRBLinExpr exp = new GRBLinExpr();								
								exp.addTerm(1, x1[i][k][j]);
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j+1, j1+1)>0)
										exp.addTerm(-1, y1[i][j][j1]);
								String st = "k3["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp, GRB.LESS_EQUAL, 0, st);
								exp=null;
								GRBLinExpr exp1 = new GRBLinExpr();		
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j+1, j1+1)>0)
										exp1.addTerm(1, y1[i][j][j1]);
								st = "k4["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp1, GRB.EQUAL, 1, st);
								exp1=null;
							}
							
						}


			
				System.gc();
				
			
				
				// Optimize model
				try {
					
					model.optimize();
					//model.write("model1.lp");
					out.write("Solution for the problem:");
					out.newLine();
				
					int optimstatus = model.get(GRB.IntAttr.Status); 
					if (optimstatus == GRB.Status.OPTIMAL) 
					{ 
						//r_min= r.get(GRB.DoubleAttr.X);
						value_final = obj.getValue();
						out.write("Objective optimal Value: "+obj.getValue());
						out.newLine();
						for(int i = 0; i < demandArray.size(); i++) 
							for(int k = 0; k < m; k++)
								for(int j = 0; j < n; j++)
						    		{	
						    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
						    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
						for(int i = 0; i < demandArray.size(); i++) 
						    for(int j1 = 0; j1 < n; j1++)
						    	for(int j2 = 0; j2 < n; j2++)
						    		{	
						    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
						    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
						for (int i=0;i<demandArray.size();i++)
							for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
								for(int j1=0;j1<n;j1++)
									for(int j2=0;j2<n;j2++)
						    		{	
						    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
						    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
//						for(int i = 0; i < demandArray.size(); i++)
//							if(z1[i].get(GRB.DoubleAttr.X)>0)
//			    			{
//								//a_min++;
//			    			out.write(z1[i].get(GRB.StringAttr.VarName)
//			    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//			    			out.newLine();
//			    			}
////						out.write(r.get(GRB.StringAttr.VarName)
////		    					+ " : " +r.get(GRB.DoubleAttr.X));
		    			out.newLine();
				
					 } else if (optimstatus == GRB.Status.INF_OR_UNBD) 
					 	{ 
					        System.out.println("Model is infeasible or unbounded"); 
					        return;
					 	} else if (optimstatus == GRB.Status.INFEASIBLE) 
					        	{ 
							        System.out.println("Model is infeasible AAAAAAAAAAAAAA"); 
							        return; 
					        	} else if (optimstatus == GRB.Status.INTERRUPTED)
					        	{
					        		//r_min= r.get(GRB.DoubleAttr.X);
					        		value_final = obj.getValue();
					        		out.write("Objective interrupt Value: "+obj.getValue());
									out.newLine();
									for(int i = 0; i < demandArray.size(); i++) 
									for(int k = 0; k < m; k++)
										for(int j = 0; j < n; j++)
								    		{	
								    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
								    			{
								    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
								    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
								    			out.newLine();
								    			}
								    		}
									for (int i=0;i<demandArray.size();i++)
										for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
											for(int j1=0;j1<n;j1++)
												for(int j2=0;j2<n;j2++)
									    		{	
									    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
									    			{
									    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
									    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
									    			out.newLine();
									    			}
									    		}
									for(int i = 0; i < demandArray.size(); i++) 
									    for(int j1 = 0; j1 < n; j1++)
									    	for(int j2 = 0; j2 < n; j2++)
									    		{	
									    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
									    			{
									    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
									    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
									    			out.newLine();
									    			}
									    		}
//									for(int i = 0; i < demandArray.size(); i++)
//										if(z1[i].get(GRB.DoubleAttr.X)>0)
//						    			{
//											//a_min++;
//						    			out.write(z1[i].get(GRB.StringAttr.VarName)
//						    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//						    			out.newLine();
//						    			}
////									out.write(r.get(GRB.StringAttr.VarName)
////					    					+ " : " +r.get(GRB.DoubleAttr.X));
					    			out.newLine();
					        		
					        	}
					
					 else
					 {
						 //r_min= r.get(GRB.DoubleAttr.X);
						 value_final = obj.getValue();
						 out.write("Objective feasible Value: "+obj.getValue());
						 out.newLine();
							for(int i = 0; i < demandArray.size(); i++) 
							for(int k = 0; k < m; k++)
								for(int j = 0; j < n; j++)
						    		{	
						    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
						    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
							for (int i=0;i<demandArray.size();i++)
								for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
									for(int j1=0;j1<n;j1++)
										for(int j2=0;j2<n;j2++)
							    		{	
							    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
							    			{
							    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
							    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
							    			out.newLine();
							    			}
							    		}
							for(int i = 0; i < demandArray.size(); i++) 
							    for(int j1 = 0; j1 < n; j1++)
							    	for(int j2 = 0; j2 < n; j2++)
							    		{	
							    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
							    			{
							    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
							    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
							    			out.newLine();
							    			}
							    		}
//							for(int i = 0; i < demandArray.size(); i++)
//								if(z1[i].get(GRB.DoubleAttr.X)>0)
//				    			{
//									//a_min++;
//				    			out.write(z1[i].get(GRB.StringAttr.VarName)
//				    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//				    			out.newLine();
//				    			}
////							out.write(r.get(GRB.StringAttr.VarName)
////			    					+ " : " +r.get(GRB.DoubleAttr.X));
			    			out.newLine();
							
					  }
				
					
				} catch (Exception e) {
					//r_min= r.get(GRB.DoubleAttr.X);
					value_final = obj.getValue();
					out.write("Objective interrupt Value: "+obj.getValue());
					out.newLine();
					for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
				    		{	
				    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
				    			{
				    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
				    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
				    			out.newLine();
				    			}
				    		}
					for (int i=0;i<demandArray.size();i++)
						for(int k=0;k<demandArray.get(i).getFunctions().length+1;k++)
							for(int j1=0;j1<n;j1++)
								for(int j2=0;j2<n;j2++)
					    		{	
					    			if(g.getEdgeWeight(j1+1, j2+1)>0&&phi[i][k][j1][j2].get(GRB.DoubleAttr.X)>0)
					    			{
					    			out.write(phi[i][k][j1][j2].get(GRB.StringAttr.VarName)
					    					+ " : " +phi[i][k][j1][j2].get(GRB.DoubleAttr.X));
					    			out.newLine();
					    			}
					    		}
					for(int i = 0; i < demandArray.size(); i++) 
					    for(int j1 = 0; j1 < n; j1++)
					    	for(int j2 = 0; j2 < n; j2++)
					    		{	
					    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
					    			{
					    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
					    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
					    			out.newLine();
					    			}
					    		}
//					for(int i = 0; i < demandArray.size(); i++)
//						if(z1[i].get(GRB.DoubleAttr.X)>0)
//		    			{
//							//a_min++;
//		    			out.write(z1[i].get(GRB.StringAttr.VarName)
//		    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//		    			out.newLine();
//		    			}
////					out.write(r.get(GRB.StringAttr.VarName)
////	    					+ " : " +r.get(GRB.DoubleAttr.X));
	    			out.newLine();
					
	
				}
					model.dispose();
				env.dispose();
				System.gc();
			
				} catch(GRBException e3){			
					System.out.println("Error code1: " + e3.getErrorCode() + ". " +
							e3.getMessage());
					System.out.print("This problem can't be solved");
					
					
					}
			} catch ( IOException e1 ) {
					e1.printStackTrace();
					} finally {
						if ( out != null )
							try {
								out.close();
								} catch (IOException e) {
									e.printStackTrace();}
						}    
			try {
		  		out.close();
		  		} catch (IOException e2) {
		  			e2.printStackTrace();
		  			}
	
	
	}
	public static void newModel (String outFile)
	{
		double Const_No = 28.0;
		try {

			File file = new File(outFile);
			out = new BufferedWriter(new FileWriter(file));
			out.write("number of function:" + m);
			out.newLine();
			for (int i=0;i<m;i++)
	       	{	 
	               out.write(functionArr[i].toString());
	               out.newLine();
	       	}
	   		out.write("number of Demand:" + d);
	   		out.newLine();
	       	for (int i=0;i<d;i++)
	       	{    		
	       		out.write(demandArray.get(i).toString());
	       		out.newLine();
	       	}
	       	out.write("virtual node:"+ n);
	       	out.newLine();
	       	for (int i=0;i<n;i++)
	       	{
	       		for (int j=0;j<n;j++)
	       			out.write(g.getEdgeWeight(i+1, j+1) + " ");
	       		out.newLine();
	       	}
	       	
	       	
	       	
			try{
				GRBEnv env = new GRBEnv("qp.log");
				env.set(GRB.DoubleParam.MIPGap, 0.000000001);
				env.set(GRB.DoubleParam.FeasibilityTol, 0.000000001);
				env.set(GRB.IntParam.Threads,24);
				env.set(GRB.DoubleParam.TimeLimit,20000);
				GRBModel model = new GRBModel(env);
				GRBQuadExpr obj = new GRBQuadExpr();
				int constant=1;
				//r_min=0;
				//a_min=0;
				
//				ArrayList<newDemand> demandArray = new ArrayList<newDemand>();
//				for(int i=0;i<newDemandArray.size();i++)
//					demandArray.add(newDemandArray.get(i));
//				for(int i=0;i<oldDemandArray.size();i++)
//				{
//					oldDemand _old= oldDemandArray.get(i);
//					demandArray.add(new newDemand(_old.GetID(), _old.GetSrc(), _old.GetDest(), _old.GetArrivalTime(), _old.GetProcessTime(), _old.GetBandwidth(), _old.GetRate(), _old.GetSetFunc()));
//					ArrayList<Integer> f_sol=_old.Get_f_sol();
//					ArrayList<Integer> v_sol=_old.Get_v_sol();
//					for(int j=0;j<f_sol.size();j++)
//					{
//						if(f_sol.get(j)>0)
//						{
//							//update capacity for node
//							g.addCap(v_sol.get(j), getLamdaF(f_sol.get(j)));
//						}
//					}
//					for(int j=0;j<v_sol.size()-1;j++)
//						for(int k=j+1;k<v_sol.size();k++)
//						{
//							g.addEdgeWeight(v_sol.get(j), v_sol.get(k), _old.GetBandwidth());
//						}
//				}
				
				
				//variable declaire
				
			
				for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
				    		{
				    			String st = "x1["+(i+1)+ "]["+(k+1)+ "]["+(j+1)+ "]";
				    			x1[i][k][j] = model.addVar(0, 1, 0, GRB.BINARY, st);
				    		}
				for(int i = 0; i < demandArray.size(); i++) 
				    for(int j = 0; j < n; j++)
				    	for(int k = 0; k < n; k++)
				    		{
				    			if(g.getEdgeWeight(j+1, k+1)>0)
				    			{
				    				String st = "y1["+(i+1)+ "]["+(j+1)+ "]["+(k+1)+ "]";
				    				y1[i][j][k] = model.addVar(0, 1, 0, GRB.BINARY, st);
				    			}
				    		}
				for(int i = 0; i < demandArray.size(); i++) 
				    for(int j = 0; j < n; j++)
				    	for(int k = 0; k < n; k++)
				    		{
					    			String st = "y2["+(i+1)+ "]["+(j+1)+ "]["+(k+1)+ "]";
					    			y2[i][j][k] = model.addVar(0, 1, 0, GRB.BINARY, st);
				    		}
				//r=model.addVar(0, 1, 0, GRB.CONTINUOUS, "r");
//				for(int i = 0; i < demandArray.size(); i++) 
//				{
//	    			String st = "z1["+(i)+ "]";
//	    			z1[i] = model.addVar(0, 1, 0, GRB.BINARY, st);
//				}
				
				model.update();
				//obj.addTerm(0.5, r);
//				for(int i = 0; i < demandArray.size(); i++) 
//					obj.addTerm(-1, z1[i]);
				//ham muc tieu
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						if(g.getEdgeWeight(j1+1, j2+1)>0)
						{
							for(int i = 0; i < demandArray.size(); i++) 
							{
								obj.addTerm(g.getEdgeWeight(j1+1, j2+1)*g.getPriceBandwidth()*0.0001 , y1[i][j1][j2]);
								//obj.addTerm(g.getEdgeWeight(j1+1, j2+1)*g.getPriceBandwidth()*0.0001 , y1[i][j2][j1]);
							}
						}
					}
				for(int j = 0; j < n; j++)
					for(int i = 0; i < demandArray.size(); i++)
						for(int k = 0; k < m; k++)
				    		{
				    			obj.addTerm(g.getPriceNode(j+1), x1[i][k][j]);
				    		}
//				for(int j = 0; j < n; j++)
//					for(int k = 0; k < m; k++)
//					{
//						GRBLinExpr expr1= new GRBLinExpr();
//							for(int i = 0; i < demandArray.size(); i++)
//				    		{
//								expr1.addTerm(-Const_No, x1[i][k][j]);
//				    		}
//							expr1.addConstant(Const_No);
//							obj.add(expr1);
//					}
				model.setObjective(obj,GRB.MINIMIZE);		
				//add constraints
				
//				Rang buoc them

				for (int i=0;i<demandArray.size();i++)
					for(int j=0;j<n;j++)
					{
						for (int k=0;k<demandArray.get(i).getFunctions().length-1;k++)
						{
							int f1 = demandArray.get(i).getFunctions()[k].id()-1;
							int f2= demandArray.get(i).getFunctions()[k+1].id()-1;
							GRBLinExpr exp = new GRBLinExpr();								
							exp.addTerm(1, x1[i][f1][j]);
							exp.addTerm(1, x1[i][f2][j]);							
							String st = "i3["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
							model.addConstr(exp, GRB.LESS_EQUAL, 1, st);
						}
					}
				
					
//				
				//Eq (5) ->Ok
				for(int compo=0;compo<3;compo++)
				{
					for(int j = 0; j < n; j++) //node
			    	{
						GRBLinExpr expr1= new GRBLinExpr();
						for(int i = 0; i < demandArray.size(); i++) //demand
							for(int k = 0; k < m; k++) //function
									expr1.addTerm(getFunction(k+1).getLamda().get(compo),x1[i][k][j]);
						String st = "c["+(j)+ "]["+compo+"]";
						model.addConstr(expr1, GRB.LESS_EQUAL, g.getCap(j+1).get(compo) , st);
						expr1 = null;
			    	}
				}
				System.gc();
				
				//Eq (6)->OK
				for (int j1=0;j1<n;j1++)
					for(int j2=0;j2<n;j2++)
					{
						if(g.getEdgeWeight(j1+1, j2+1)>0)
						{
							GRBLinExpr expr2= new GRBLinExpr();
							for (int i =0;i<demandArray.size();i++) //demand
							{
								expr2.addTerm(demandArray.get(i).bwS(),y1[i][j1][j2]);
								//expr2.addTerm(demandArray.get(i).bwS(),y1[i][j2][j1]);
							}
							//expr2.addTerm(-g.getEdgeWeight(j1+1, j2+1),1);
							String st = "d["+(j1+1)+ "]["+(j2+1)+ "]";
							model.addConstr(expr2, GRB.LESS_EQUAL,g.getEdgeWeight(j1+1, j2+1), st);
							expr2 = null;	
						}
						
					}
				//Eq (7)
				for (int i =0;i<d;i++) //demand
					for (int k = 0;k<m;k++)//k is a function
						for (int b=0;b<m;b++)//b is a function
						{
							int id1 = getDemand(i+1).getOrderFunction(k+1);
							int id2 = getDemand(i+1).getOrderFunction(b+1);
							if (id1!=0 && id2!=0 && id1 < id2)//truong hop k < b
							{
							for (int j=0;j<n;j++)// j is a node
								for (int a=0;a<n;a++)//a is a node
								{
									GRBLinExpr expr3 = new GRBLinExpr();
									expr3.addTerm(1, x1[i][k][j]);
									expr3.addTerm(1, x1[i][b][a]);
									expr3.addTerm(-1, y2[i][j][a]);
									String st = "e1["+(i)+ "]["+(k)+ "]["+(b)+"]["+(j)+ "]["+(a)+  "]";
									model.addConstr(expr3, GRB.LESS_EQUAL, 1, st);
									expr3 = null;
								}
							}
							else if(id1!=0 && id2!=0 && id1 > id2) //truong hop b > k
							{
								for (int j=0;j<n;j++)// j is a node
									for (int a=0;a<n;a++)//a is a node
									{
										GRBLinExpr expr3 = new GRBLinExpr();
										expr3.addTerm(1, x1[i][k][j]);
										expr3.addTerm(1, x1[i][b][a]);
										expr3.addTerm(-1, y2[i][a][j]);
										String st = "e2["+(i)+ "]["+(k)+ "]["+(j)+ "]["+(b)+"]["+(a)+  "]";
										model.addConstr(expr3, GRB.LESS_EQUAL, 1, st);
										expr3 = null;
									}
							}
						}
			
				System.gc();
				
				//Eq (8)
				for (int i =0;i<d;i++) //demand
					for (int k = 0;k<m;k++)//k is a function
					{
						GRBLinExpr expr3 = new GRBLinExpr();
						for (int j=0;j<n;j++)// j is a node
						{
							expr3.addTerm(1, x1[i][k][j]);
						}
						int id = getDemand(i+1).getOrderFunction(k+1);
						String st = "f["+(i)+ "]["+(k)+ "]";
						if (id!=0)//truong hop function in demand =1
						{
							//expr3.addTerm(-1, z1[i]);
							model.addConstr(expr3, GRB.EQUAL, 1, st);
						}
						else
							model.addConstr(expr3, GRB.EQUAL, 0, st);
						
						
						expr3 = null;
					}
				//Eq 9
				for (int i =0;i<d;i++) //demand
					for (int j1=0;j1<n;j1++)
						for(int j2=0;j2<n;j2++)
						{
							if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0&& g.getEdgeWeight(j2+1, j1+1)>0)
							{
								GRBLinExpr expr3 = new GRBLinExpr();
								expr3.addTerm(1, y1[i][j1][j2]);
								expr3.addTerm(1, y1[i][j2][j1]);
								String st = "g["+(i)+ "]["+(j1)+ "]["+(j2)+"]";
								model.addConstr(expr3, GRB.LESS_EQUAL, 1, st);
								expr3 = null;
							}
							
						}
				System.gc();
				//Eq (10) ->ok
				for (int i=0;i<demandArray.size();i++)
				{
					int desti = demandArray.get(i).destinationS();
					int source = demandArray.get(i).sourceS();
					for (int j1=0;j1<n;j1++)
					{
						GRBLinExpr expr7= new GRBLinExpr();
						
						
						if(j1 !=source-1 && j1 !=desti-1)
						{
							
							String st = "h1["+(i)+ "]["+(j1+1)+  "s]";
							for (int j2=0;j2<n;j2++)
							{
								if(g.getEdgeWeight(j1+1, j2+1)>0)
								{
									
									expr7.addTerm(1, y1[i][j1][j2]);
								}
								if(g.getEdgeWeight(j2+1, j1+1)>0)
									expr7.addTerm(-1, y1[i][j2][j1]);
									
							}
							model.addConstr(expr7, GRB.EQUAL, 0, st);
							expr7 = null;
						}
						else
						{
							if(j1==source-1)
							{
								String st = "h2["+(i)+ "]["+(j1+1)+  "s]";
								for (int j2=0;j2<n;j2++)
								{
									if(j1!=j2)
										if(g.getEdgeWeight(j1+1, j2+1)>0)
											expr7.addTerm(1, y1[i][j1][j2]);
								}
								//expr7.addTerm(-1, z1[i]);
								model.addConstr(expr7, GRB.EQUAL, 1, st);
								expr7 = null;
								st = "h3["+(i)+ "]["+(j1+1)+  "s]";
								GRBLinExpr expr= new GRBLinExpr();
								for (int j2=0;j2<n;j2++)
								{
									if(j1!=j2 && g.getEdgeWeight(j2+1, j1+1)>0)
										expr.addTerm(1, y1[i][j2][j1]);
								}
								model.addConstr(expr, GRB.EQUAL, 0, st);
								expr = null;
							}
							if(j1==desti-1)
							{
								String st = "h4["+(i)+ "]["+(j1+1)+  "s]";
								//expr7.addTerm(1, z1[i]);
								for (int j2=0;j2<n;j2++)
								{
									if(j1!=j2 && g.getEdgeWeight(j2+1, j1+1)>0)
										expr7.addTerm(1, y1[i][j2][j1]);
								}
								model.addConstr(expr7, GRB.EQUAL, 1, st);
								expr7 = null;
								GRBLinExpr expr= new GRBLinExpr();
								st = "h5["+(i)+ "]["+(j1+1)+  "s]";
								for (int j2=0;j2<n;j2++)
								{
									if(j1!=j2 && g.getEdgeWeight(j1+1, j2+1)>0)
										expr.addTerm(1, y1[i][j1][j2]);
								}
								model.addConstr(expr, GRB.EQUAL, 0, st);
								expr = null;
							}
						}
					}
					
				}
				
				System.gc();
//				//Eq 11
//				for (int i =0;i<d;i++) //demand
//					for (int j1=0;j1<n;j1++)
//						for(int j2=0;j2<n;j2++)
//						{
//							if(j1!=j2)
//							{
//								GRBQuadExpr exp= new GRBQuadExpr();
//								exp.addTerm(1, y2[i][j1][j2]);;
//								for (int j=0;j<n;j++)
//								{
//									if(j!=j2)
//										exp.addTerm(-1, y2[i][j1][j], y1[i][j][j2]);
//								}
//								String st = "i["+(i)+ "]["+(j1+1)+ "]["+(j2+1)+  "s]";
//								model.addQConstr(exp, GRB.EQUAL, 0, st);
//								exp=null;
//							}
//							
//						}
				
				//Eq 11
				for (int i =0;i<d;i++) //demand
					for (int j1=0;j1<n;j1++)
						for(int j2=0;j2<n;j2++)
						{
							if(j1!=j2)
							{
								for (int j=0;j<n;j++)
								{
									
									if(j!=j2 && g.getEdgeWeight(j+1, j2+1)>0)
									{
										GRBLinExpr exp = new GRBLinExpr();
										exp.addTerm(1, y2[i][j1][j2]);

										exp.addTerm(-1, y2[i][j1][j]);
										exp.addTerm(-1, y1[i][j][j2]);
										String st = "i["+(i)+ "]["+(j1+1)+ "]["+(j2+1)+ "]["+(j+1)+ "s]";
										model.addConstr(exp, GRB.GREATER_EQUAL, -1, st);
										exp=null;
									}
										
								}
								
							}
							
						}
				//Eq (12)
				
				for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
						{
							int source = demandArray.get(i).sourceS();
							if(source != j+1)
							{
								GRBLinExpr exp = new GRBLinExpr();								
								exp.addTerm(1, x1[i][k][j]);
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j1+1, j+1)>0)
										exp.addTerm(-1, y1[i][j1][j]);
								String st = "k1["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp, GRB.LESS_EQUAL, 0, st);
								exp=null;
								GRBLinExpr exp1 = new GRBLinExpr();		
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j1+1, j+1)>0)
										exp1.addTerm(1, y1[i][j1][j]);
								st = "k2["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp1, GRB.LESS_EQUAL, 1, st);
								exp1=null;
							}	
							else
							{
								GRBLinExpr exp = new GRBLinExpr();								
								exp.addTerm(1, x1[i][k][j]);
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j+1, j1+1)>0)
										exp.addTerm(-1, y1[i][j][j1]);
								String st = "k3["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp, GRB.LESS_EQUAL, 0, st);
								exp=null;
								GRBLinExpr exp1 = new GRBLinExpr();		
								for(int j1=0;j1<n;j1++)
									if(g.getEdgeWeight(j+1, j1+1)>0)
										exp1.addTerm(1, y1[i][j][j1]);
								st = "k4["+(i)+ "]["+(k)+ "]["+(j+1)+  "]";
								model.addConstr(exp1, GRB.EQUAL, 1, st);
								exp1=null;
							}
							
						}

//				for (int i =0;i<d;i++) //demand
//					for (int j1=0;j1<n;j1++)
//					{
//						int source = demandArray.get(i).sourceS();
//						GRBLinExpr exp = new GRBLinExpr();
//						if(source == j1+1)
//						{
//							for(int j2=0;j2<n;j2++)
//								exp.addTerm(1, y1[i][j1][j2]);
//						}
//						else
//						{
//							for(int j2=0;j2<n;j2++)
//								exp.addTerm(1, y1[i][j2][j1]);
//						}
//						
//						String st = "l1["+(i)+ "]["+(j1)+  "]";
//						model.addConstr(exp, GRB.LESS_EQUAL, 1, st);
//						exp=null;
//					}
				
				//Eq 13
				
				for (int i =0;i<d;i++) //demand
					for (int j1=0;j1<n;j1++)
						for(int j2=0;j2<n;j2++)
						{
							if(g.getEdgeWeight(j1+1, j2+1)>0)
							{
								GRBLinExpr exp = new GRBLinExpr();
								exp.addTerm(1, y2[i][j1][j2]);
								exp.addTerm(-1, y1[i][j1][j2]);
								String st = "m["+(i)+ "]["+(j1)+ "]["+(j2)+  "]";
								model.addConstr(exp, GRB.GREATER_EQUAL, 0, st);
								exp=null;
							}
						}
				//Eq 14
				
				for (int i =0;i<d;i++) //demand
					for (int j1=0;j1<n;j1++)
					{
						GRBLinExpr exp = new GRBLinExpr();
						exp.addTerm(1, y2[i][j1][j1]);
						String st = "n["+(i)+ "]["+(j1)+  "]";
						model.addConstr(exp, GRB.EQUAL, 1, st);
						exp=null;
					}
				
				//Eq 15
				for (int i =0;i<d;i++) //demand
				{
					GRBLinExpr exp = new GRBLinExpr();
					for (int j1=0;j1<n;j1++)
						for(int j2=0;j2<n;j2++)
						{
							if(j1!=j2&&g.getEdgeWeight(j1+1, j2+1)>0)
								exp.addTerm(1, y1[i][j1][j2]);
							
						}
					//exp.addTerm(-1, z1[i]);
					String st = "o["+(i)+  "]";
					model.addConstr(exp, GRB.GREATER_EQUAL, 1, st);
					exp=null;
				}
				
				//Eq 16
				for (int i =0;i<d;i++) //demand
				{
					for (int j1=0;j1<n;j1++)
					{
						
						for(int j2=0;j2<n;j2++)
						{
							if(j2!=j1)
							{
								GRBLinExpr exp = new GRBLinExpr();
								exp.addTerm(1, y2[i][j1][j2]);
								exp.addTerm(1, y2[i][j2][j1]);
								String st = "p["+(i)+ "]["+(j1)+ "]["+(j2)+ "]";
								model.addConstr(exp, GRB.LESS_EQUAL, 1, st);
								exp=null;
							}
						}
						
						
					}
					
				}
				
				//Eq 17
				for (int i =0;i<d;i++) //demand
				{
					for (int j1=0;j1<n;j1++)
					{
						
						for(int j2=0;j2<n;j2++)
						{
							for(int j3=0;j3<n;j3++)
							{
								if(j1!=j2 && j2!=j3)
								{
									GRBLinExpr exp = new GRBLinExpr();
									exp.addTerm(1, y2[i][j1][j2]);
									exp.addTerm(1, y2[i][j2][j3]);
									exp.addTerm(-1, y2[i][j1][j3]);
									String st = "q["+(i)+ "]["+(j1)+ "]["+(j2)+ "]["+(j3)+ "]";
									model.addConstr(exp, GRB.LESS_EQUAL, 1, st);
									exp=null;
								}
								
							}
						}
						
						
					}
					
				}
				
				//Eq 18
				for (int i =0;i<d;i++) //demand
				{
					int source = demandArray.get(i).sourceS();
					GRBLinExpr exp1 = new GRBLinExpr();
					for (int j1=0;j1<n;j1++)
					{	
						if(j1!=source-1)
							exp1.addTerm(1, y2[i][source-1][j1]);	
					}
					String st = "s1["+(i)+ "]";
					model.addConstr(exp1, GRB.GREATER_EQUAL, 1, st);
					exp1=null;
					
				}
				//Eq 19
				for (int i =0;i<d;i++) //demand
				{
					int desti = demandArray.get(i).destinationS();
					GRBLinExpr exp1 = new GRBLinExpr();
					for (int j1=0;j1<n;j1++)
					{
						if(j1!=desti-1)
							exp1.addTerm(1, y2[i][j1][desti-1]);	
					}
					String st = "s2["+(i)+ "]";
					model.addConstr(exp1, GRB.GREATER_EQUAL, 1, st);
					exp1=null;
					
				}
				System.gc();
				
			
				
				// Optimize model
				try {
					
					model.optimize();
					model.write("model.lp");
					out.write("Solution for the problem:");
					out.newLine();
				
					int optimstatus = model.get(GRB.IntAttr.Status); 
					if (optimstatus == GRB.Status.OPTIMAL) 
					{ 
						//r_min= r.get(GRB.DoubleAttr.X);
						value_final = obj.getValue();
						out.write("Objective optimal Value: "+obj.getValue());
						out.newLine();
						for(int i = 0; i < demandArray.size(); i++) 
							for(int k = 0; k < m; k++)
								for(int j = 0; j < n; j++)
						    		{	
						    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
						    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
						for(int i = 0; i < demandArray.size(); i++) 
						    for(int j1 = 0; j1 < n; j1++)
						    	for(int j2 = 0; j2 < n; j2++)
						    		{	
						    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
						    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
						for(int i = 0; i < demandArray.size(); i++) 
						    for(int j1 = 0; j1 < n; j1++)
						    	for(int j2 = 0; j2 < n; j2++)
						    		{	
						    			if(y2[i][j1][j2].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(y2[i][j1][j2].get(GRB.StringAttr.VarName)
						    					+ " : " +y2[i][j1][j2].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
//						for(int i = 0; i < demandArray.size(); i++)
//							if(z1[i].get(GRB.DoubleAttr.X)>0)
//			    			{
//								//a_min++;
//			    			out.write(z1[i].get(GRB.StringAttr.VarName)
//			    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//			    			out.newLine();
//			    			}
////						out.write(r.get(GRB.StringAttr.VarName)
////		    					+ " : " +r.get(GRB.DoubleAttr.X));
		    			out.newLine();
				
					 } else if (optimstatus == GRB.Status.INF_OR_UNBD) 
					 	{ 
					        System.out.println("Model is infeasible or unbounded"); 
					        return;
					 	} else if (optimstatus == GRB.Status.INFEASIBLE) 
					        	{ 
							        System.out.println("Model is infeasible AAAAAAAAAAAAAA"); 
							        return; 
					        	} else if (optimstatus == GRB.Status.INTERRUPTED)
					        	{
					        		//r_min= r.get(GRB.DoubleAttr.X);
					        		value_final = obj.getValue();
					        		out.write("Objective interrupt Value: "+obj.getValue());
									out.newLine();
									for(int i = 0; i < demandArray.size(); i++) 
									for(int k = 0; k < m; k++)
										for(int j = 0; j < n; j++)
								    		{	
								    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
								    			{
								    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
								    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
								    			out.newLine();
								    			}
								    		}
									for(int i = 0; i < demandArray.size(); i++) 
									    for(int j1 = 0; j1 < n; j1++)
									    	for(int j2 = 0; j2 < n; j2++)
									    		{	
									    			if(y2[i][j1][j2].get(GRB.DoubleAttr.X)>0)
									    			{
									    			out.write(y2[i][j1][j2].get(GRB.StringAttr.VarName)
									    					+ " : " +y2[i][j1][j2].get(GRB.DoubleAttr.X));
									    			out.newLine();
									    			}
									    		}
									for(int i = 0; i < demandArray.size(); i++) 
									    for(int j1 = 0; j1 < n; j1++)
									    	for(int j2 = 0; j2 < n; j2++)
									    		{	
									    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
									    			{
									    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
									    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
									    			out.newLine();
									    			}
									    		}
//									for(int i = 0; i < demandArray.size(); i++)
//										if(z1[i].get(GRB.DoubleAttr.X)>0)
//						    			{
//											//a_min++;
//						    			out.write(z1[i].get(GRB.StringAttr.VarName)
//						    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//						    			out.newLine();
//						    			}
////									out.write(r.get(GRB.StringAttr.VarName)
////					    					+ " : " +r.get(GRB.DoubleAttr.X));
					    			out.newLine();
					        		
					        	}
					
					 else
					 {
						 //r_min= r.get(GRB.DoubleAttr.X);
						 value_final = obj.getValue();
						 out.write("Objective feasible Value: "+obj.getValue());
						 out.newLine();
							for(int i = 0; i < demandArray.size(); i++) 
							for(int k = 0; k < m; k++)
								for(int j = 0; j < n; j++)
						    		{	
						    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
						    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}
							for(int i = 0; i < demandArray.size(); i++) 
							    for(int j1 = 0; j1 < n; j1++)
							    	for(int j2 = 0; j2 < n; j2++)
							    		{	
							    			if(y2[i][j1][j2].get(GRB.DoubleAttr.X)>0)
							    			{
							    			out.write(y2[i][j1][j2].get(GRB.StringAttr.VarName)
							    					+ " : " +y2[i][j1][j2].get(GRB.DoubleAttr.X));
							    			out.newLine();
							    			}
							    		}
							for(int i = 0; i < demandArray.size(); i++) 
							    for(int j1 = 0; j1 < n; j1++)
							    	for(int j2 = 0; j2 < n; j2++)
							    		{	
							    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
							    			{
							    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
							    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
							    			out.newLine();
							    			}
							    		}
//							for(int i = 0; i < demandArray.size(); i++)
//								if(z1[i].get(GRB.DoubleAttr.X)>0)
//				    			{
//									//a_min++;
//				    			out.write(z1[i].get(GRB.StringAttr.VarName)
//				    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//				    			out.newLine();
//				    			}
////							out.write(r.get(GRB.StringAttr.VarName)
////			    					+ " : " +r.get(GRB.DoubleAttr.X));
			    			out.newLine();
							
					  }
				
					
				} catch (Exception e) {
					//r_min= r.get(GRB.DoubleAttr.X);
					value_final = obj.getValue();
					out.write("Objective interrupt Value: "+obj.getValue());
					out.newLine();
					for(int i = 0; i < demandArray.size(); i++) 
					for(int k = 0; k < m; k++)
						for(int j = 0; j < n; j++)
				    		{	
				    			if(x1[i][k][j].get(GRB.DoubleAttr.X)>0)
				    			{
				    			out.write(x1[i][k][j].get(GRB.StringAttr.VarName)
				    					+ " : " +x1[i][k][j].get(GRB.DoubleAttr.X));
				    			out.newLine();
				    			}
				    		}
					for(int i = 0; i < demandArray.size(); i++) 
					    for(int j1 = 0; j1 < n; j1++)
					    	for(int j2 = 0; j2 < n; j2++)
					    		{	
					    			if(y2[i][j1][j2].get(GRB.DoubleAttr.X)>0)
					    			{
					    			out.write(y2[i][j1][j2].get(GRB.StringAttr.VarName)
					    					+ " : " +y2[i][j1][j2].get(GRB.DoubleAttr.X));
					    			out.newLine();
					    			}
					    		}
					for(int i = 0; i < demandArray.size(); i++) 
					    for(int j1 = 0; j1 < n; j1++)
					    	for(int j2 = 0; j2 < n; j2++)
					    		{	
					    			if(g.getEdgeWeight(j1+1, j2+1)>0&&y1[i][j1][j2].get(GRB.DoubleAttr.X)>0)
					    			{
					    			out.write(y1[i][j1][j2].get(GRB.StringAttr.VarName)
					    					+ " : " +y1[i][j1][j2].get(GRB.DoubleAttr.X));
					    			out.newLine();
					    			}
					    		}
//					for(int i = 0; i < demandArray.size(); i++)
//						if(z1[i].get(GRB.DoubleAttr.X)>0)
//		    			{
//							//a_min++;
//		    			out.write(z1[i].get(GRB.StringAttr.VarName)
//		    					+ " : " +z1[i].get(GRB.DoubleAttr.X));
//		    			out.newLine();
//		    			}
////					out.write(r.get(GRB.StringAttr.VarName)
////	    					+ " : " +r.get(GRB.DoubleAttr.X));
	    			out.newLine();
					
	
				}
					model.dispose();
				env.dispose();
				System.gc();
			
				} catch(GRBException e3){			
					System.out.println("Error code1: " + e3.getErrorCode() + ". " +
							e3.getMessage());
					System.out.print("This problem can't be solved");
					
					
					}
			} catch ( IOException e1 ) {
					e1.printStackTrace();
					} finally {
						if ( out != null )
							try {
								out.close();
								} catch (IOException e) {
									e.printStackTrace();}
						}    
			try {
		  		out.close();
		  		} catch (IOException e2) {
		  			e2.printStackTrace();
		  			}
	}
	public static void model2( String outFile)//GUROBI
	{
		try {
			File file = new File(outFile);
			out = new BufferedWriter(new FileWriter(file));
			out.write("number of function:" + m);
			out.newLine();
			for (int i=0;i<m;i++)
	       	{	 
	               out.write(functionArr[i].toString());
	               out.newLine();
	       	}
	   		out.write("number of Demand:" + d);
	   		out.newLine();
	       	for (int i=0;i<d;i++)
	       	{    		
	       		out.write(demandArray.get(i).toString());
	       		out.newLine();
	       	}
	       	out.write("virtual node:"+ n);
	       	out.newLine();
	       	for (int i=0;i<n;i++)
	       	{
	       		for (int j=0;j<n;j++)
	       			out.write(g.getEdgeWeight(i+1, j+1) + " ");
	       		out.newLine();
	       	}
	       	
	       	
			try{
				GRBEnv env = new GRBEnv("qp.log");
				env.set(GRB.DoubleParam.MIPGap, 0.000000001);
				env.set(GRB.DoubleParam.FeasibilityTol, 0.000000001);
				env.set(GRB.IntParam.Threads,4);
				env.set(GRB.DoubleParam.TimeLimit,1000);
				GRBModel model = new GRBModel(env);
				GRBQuadExpr obj = new GRBQuadExpr();
				
				GRBQuadExpr[][] expr2 = new GRBQuadExpr[n][n];
				for (int i=0;i<n;i++)
					for (int j=0;j<n;j++)
						expr2[i][j]= new GRBQuadExpr();
				//variable declaire
				for(int i = 0; i < d; i++) 
				    for(int j = 0; j < n; j++)
				    	for(int k = 0; k < m+1; k++)
				    		for (int l=0;l<_no;l++)
				    		{
				    			String st = "x["+(i)+ "]["+(j)+ "]["+(k)+ "]["+(l)+ "]";
				    			x[i][j][k][l] = model.addVar(0, 1, 0, GRB.BINARY, st);
				    		}
				for (int j=0;j<n;j++)
				{
					String st = "x["+(j)+ "]";
	    			y[j] = model.addVar(0, 1, 0, GRB.BINARY, st);
				}
			
				model.update();
				
				//ham muc tieu
				for(int i = 0; i < d; i++) //for each demand
				{
					for(int j = 0; j < n; j++)
						for(int k = 1; k <= m; k++)
							for(int l=0;l<_no;l++)
							{
								if( getDemand(i+1).getOrderFunction(k)!=0)
									obj.addTerm(g.getPriceNode(j+1),x[i][j][k][l]);
								//obj.addTerm(g.getPriceNode(j+1)*getDemand(i+1).getRate(),x[i][j][k][l]);
							}
					for(int j = 0; j < n; j++)
						for(int k = 0; k <= m; k++)
							for(int l=0;l<_no-1;l++)
								for(int a=0;a<n;a++)
									for(int b=0;b<=m;b++)
										obj.addTerm(g.getEdgeWeight(j+1, a+1)*g.getPriceBandwidth()*0.0001 , 
										x[i][j][k][l], 
										x[i][a][b][l+1]);
				}
				
				for(int j = 0; j < n; j++)
					for(int k = 1; k <= m; k++)
					{	
						for(int i = 0; i < d; i++) 
							for(int l=0;l<_no;l++)
							{
								obj.addTerm(-Gain(g.getPriceNode(j+1)), x[i][j][k][l]);
							}
						obj.addConstant(Gain(g.getPriceNode(j+1)));	
					}
				model.setObjective(obj);		
				//add constraints
				
				//rang buoc 1
				for(int compo=0;compo<3;compo++)
				{
					for(int j = 0; j < n; j++) //node
			    	{
						GRBLinExpr expr1= new GRBLinExpr();
						for(int i = 0; i < d; i++) //demand
							for(int k = 1; k <= m; k++) //function
								for(int l=0;l<_no;l++) //possition
									expr1.addTerm(getFunction(k).getLamda().get(compo),x[i][j][k][l]);
									//expr1.addTerm(g.getPriceNode(j+1).get(compo),x[i][j][k][l]);
						String st = "c["+(j)+ ","+compo+"]";
						model.addConstr(expr1, GRB.LESS_EQUAL, g.getCap(j+1).get(compo) , st);
						expr1 = null;
			    	}
				}
				System.gc();
				
				//rang buoc 2
				for(int j = 0; j < n; j++) //node a1
					for(int a=0;a<n;a++) //node a2
						for (int i =0;i<d;i++) //demand
							for(int k = 0; k <= m; k++) //function b1
								for(int l=0;l<_no-1;l++)
									for(int b=0;b<=m;b++) //function b2
										if(a!=j)
											expr2[j][a].addTerm(getDemand(i+1).bwS(),
																x[i][j][k][l],
																x[i][a][b][l+1]);
				for(int j = 0; j < n-1; j++) //node a1
					for(int a=j+1;a<n;a++) //node a2
					{
						if(a!=j)
						{
							GRBQuadExpr expr4 = new GRBQuadExpr();
							expr4.add(expr2[j][a]);
							expr4.add(expr2[a][j]);
							String st = "d["+(j+1)+ "]";
							model.addQConstr(expr4, GRB.LESS_EQUAL,g.getEdgeWeight(j+1, a+1), st);
							expr4 = null;
						}
					}
				
				
				for(int j = 0; j < n; j++) //node a1
					for(int a=j;a<n;a++) //node a2
						expr2[j][a]=null;
				System.gc();
				
				//rang buoc 3
				//khong co truong hop b==0, khong xet cac nut khong gian
				//thu tu cac function trong cung mot demand
				for (int i =0;i<d;i++) //demand
					for (int k = 1;k<=m-1;k++)
						for (int b=k+1;b<=m;b++)
						{
							int id1 = getDemand(i+1).getOrderFunction(k);
							int id2 = getDemand(i+1).getOrderFunction(b);
							if (id1!=0 && id2!=0 && id1 < id2)//truong hop k < b
							{
							for (int j=0;j<n;j++)
								for(int l=0;l<_no;l++)
									for (int a=0;a<n;a++)
										for(int c=0;c<_no;c++)
											{
												GRBQuadExpr expr3= new GRBQuadExpr();	
												expr3.addTerm((c-l), x[i][j][k][l], x[i][a][b][c]);
												String st = "e["+(i)+ "]["+(j)+ "]["+(k)+ "]["+(l)+ "]";
												model.addQConstr(expr3, GRB.GREATER_EQUAL, 0, st);
												expr3 = null;
											}
							}
							else if(id1!=0 && id2!=0 && id1 > id2) //truong hop b1 > b2
							{
								for (int j=0;j<n;j++)
									for(int l=0;l<_no;l++)
										for (int a=0;a<n;a++)
											for(int c=0;c<_no;c++)
											{	
												GRBQuadExpr expr3= new GRBQuadExpr();	
												expr3.addTerm((c-l), x[i][j][k][l], x[i][a][b][c]);
												String st = "e["+(i)+ "]["+(j)+ "]["+(k)+ "]["+(l)+ "]";
												model.addQConstr(expr3, GRB.LESS_EQUAL, 0, st);
												expr3 = null;
											}
							}
						}
			
				System.gc();
				
				// rang buoc 4
				//moi function chi duoc xuat hien mot thu tu c trong demand
				for (int i =0;i<d;i++) //demand
					for (int k = 1;k<=m;k++)
					{
						int id = getDemand(i+1).getOrderFunction(k);
						if (id!=0)//truong hop function in demand =1
						{
							GRBLinExpr expr5= new GRBLinExpr();	
							for (int j=0;j<n;j++)
								for (int l =0;l<_no;l++)								
									expr5.addTerm(1, x[i][j][k][l]);
							String st = "f["+(i)+ "]["+(k)+ "t]";
							model.addConstr(expr5, GRB.EQUAL, 1, st);
							expr5 = null;
						}
						else
						{ //truong hop function outside demand -> =0
							GRBLinExpr expr5= new GRBLinExpr();
							for (int j=0;j<n;j++)
								for (int l =0;l<_no;l++)
									expr5.addTerm(1, x[i][j][k][l]);
							String st = "f["+(i)+ "]["+(k)+ "j]";
							model.addConstr(expr5, GRB.EQUAL, 0, st);
							expr5 = null;
						}
					}
				System.gc();
				
				//rang buoc 5
				
				for (int i=0;i<d;i++)
					for (int l=_no-1;l>0;l--)
					{
						GRBQuadExpr expr6= new GRBQuadExpr();
						for (int j=0;j<n;j++)
							for (int k=0;k<=m;k++)
							{							
								expr6.addTerm(-1, x[i][j][k][l]);
								for (int a=0;a<n;a++)
									for (int b=0;b <=m;b++)
										expr6.addTerm(1, x[i][j][k][l], x[i][a][b][l-1]);
							}
						String st = "g["+(i)+ "]["+(l)+ "]["+(c)+ "]";
						model.addQConstr(expr6, GRB.GREATER_EQUAL, 0, st);
						expr6 = null;
					}
				System.gc();
			
				
				//rang buoc 6 :: cung mot thu tu trong 1 demand chi co the =1 hoac nho hon
				for (int i=0;i<d;i++)
					for (int l=0;l<_no;l++)
					{
						GRBLinExpr expr7= new GRBLinExpr();
						for (int j=0;j<n;j++)
							for (int k=0;k<=m;k++)
								expr7.addTerm(1, x[i][j][k][l]);
						String st = "h["+(i)+ "]["+(l)+ "]";
						model.addConstr(expr7, GRB.LESS_EQUAL, 1, st);
						expr7 = null;
					}
				System.gc();
				
				//rang buoc 7 :: function k truoc function 0 tren cung 1 node
				for (int i=0;i<d;i++)
					for (int j=0;j<n;j++)
						for (int k=0;k<=m;k++)
							for (int l=0;l<_no-1;l++)
							{
								GRBQuadExpr expr8= new GRBQuadExpr();
								expr8.addTerm(1, x[i][j][k][l],x[i][j][0][l+1]);
								String st = "k["+(i)+ "]["+(l)+ "]";
								model.addQConstr(expr8, GRB.EQUAL, 0, st);
								expr8 = null;
							}
				System.gc();
				//rang buoc 8 :: function 0 truoc function k tren cung 1 node
				for (int i=0;i<d;i++)
					for (int j=0;j<n;j++)
						for (int k=0;k<=m;k++)
							for (int l=1;l<_no;l++)
							{
								GRBQuadExpr expr9= new GRBQuadExpr();
								expr9.addTerm(1, x[i][j][k][l],x[i][j][0][l-1]);
								String st = "k["+(i)+ "]["+(l)+ "g]";
								model.addQConstr(expr9, GRB.EQUAL, 0, st);
								expr9 = null;
							}
				System.gc();
				
				
//rang buoc 9; tranh truong hop x(d,v,0,k)=1 va tat ca x sau k deu bang 0 
				
				for (int i=0;i<d;i++)
					for (int l=1;l<_no;l++)
					{
						GRBLinExpr expr10= new GRBLinExpr();
						for (int j=0;j<n;j++)
							expr10.addTerm(1, x[i][j][0][l-1]);
						for (int a=0;a<n;a++)
							for (int b=0;b <=m;b++)
								expr10.addTerm(-1, x[i][a][b][l]);
						String st = "h["+(i)+ "]["+(l)+ "g]";
						model.addConstr(expr10, GRB.LESS_EQUAL, 0, st);
						expr10 = null;
					}
				System.gc();
				
//				//rang buoc 10 : source la diem dau
//				
				for (int i=0;i<d;i++)
				{
					GRBLinExpr expr11= new GRBLinExpr();
					int source = getDemand(i+1).sourceS();
					for (int k=0;k<=m;k++)
						expr11.addTerm(1, x[i][source-1][k][0]);
					String st = "k["+(i)+ "]["+source + "s]";
					model.addConstr(expr11, GRB.EQUAL, 1, st);
					expr11 = null;
				}
				System.gc();
//				//rang buoc 10 : des la diem dau
//				
				for (int i=0;i<d;i++)
				{
					GRBLinExpr expr12= new GRBLinExpr();
					int destination = getDemand(i+1).destinationS();
					for (int k=0;k<=m;k++)
						for(int l=0;l<_no;l++)
							expr12.addTerm(1, x[i][destination-1][k][l]);
					String st = "k["+(i)+ "]["+destination + "dg]";
					model.addConstr(expr12, GRB.GREATER_EQUAL, 1, st);
					expr12 = null;
				}
				System.gc();
				
//rang buoc 10 : destination la diem cuoi
				for (int i=0;i<d;i++)
					
					{
						int destination = getDemand(i+1).destinationS();
						GRBQuadExpr expr11= new GRBQuadExpr();						
						for (int k=0;k<=m;k++)
							for (int a=0;a<n;a++)
								for (int b=0;b<=m;b++)	
									for (int l=0;l<_no-1;l++)
										expr11.addTerm(1, x[i][destination-1][k][l],x[i][a][b][l+1]);
						String st = "k["+(i)+ "]["+destination + "d]";
						model.addQConstr(expr11, GRB.EQUAL, 0, st);
						expr11 = null;
					}
				System.gc();
				
				//rang buoc x(d,v1,0,k-1), x(d,v2,0,k), x(d,v1,0,k+1) khong dong thoi bang 1
				for(int i=0;i<d;i++)
					for(int j=0;j<n;j++)
						for(int a=0;a<n;a++)
							for(int l=0;l<_no-2;l++)
							{
								GRBLinExpr expr13= new GRBLinExpr();
								expr13.addTerm(1, x[i][j][0][l]);
								expr13.addTerm(1, x[i][a][0][l+1]);
								expr13.addTerm(1, x[i][j][0][l+2]);
								String st = "k["+(i)+ "]["+(l)+ "g]";
								model.addConstr(expr13, GRB.LESS_EQUAL, 2, st);
								expr13 = null;
							}
				
				//rang buoc function chi duoc tap trung o N node
				///TODO
				//int K=7;
				GRBLinExpr expr14= new GRBLinExpr();
					for(int j = 0; j < n; j++) //node
			    	{
						expr14.addTerm(1,y[j]);
			    	}
					String st = "p["+(n)+ ","+"]";
					model.addConstr(expr14, GRB.LESS_EQUAL, maxNode , st);
					expr14 = null;
					
				System.gc();
				
				for(int j=0;j<n;j++)
				{
					GRBQuadExpr expr15= new GRBQuadExpr();
					for (int l=_no-1;l>0;l--)
					{
						for (int i=0;i<d;i++)
							for (int k=1;k<=m;k++)
							{							
								expr15.addTerm(1, x[i][j][k][l],y[j]);
								expr15.addTerm(-1, x[i][j][k][l]);
							}
						
					}
					String st1 = "q["+(j)+ "]";
					model.addQConstr(expr15, GRB.EQUAL, 0, st1);
					expr15 = null;
				}
				
				System.gc();
				for(int j=0;j<n;j++)
				{
					GRBLinExpr expr16= new GRBLinExpr();
					for(int i=0;i<d;i++)
						for(int k=1;k<=m;k++)
							for(int l=0;l<_no-1;l++)
							{								
								expr16.addTerm(-1, x[i][j][k][l]);
								
							}
					expr16.addTerm(1, y[j]);
					String st1 = "z["+(j)+  "]";
					model.addConstr(expr16, GRB.LESS_EQUAL, 0, st1);
					expr16 = null;
				}
				System.gc();
				// Optimize model
				try {
					model.optimize();
					
					out.write("Solution for the problem:");
					out.newLine();
				
					int optimstatus = model.get(GRB.IntAttr.Status); 
					if (optimstatus == GRB.Status.OPTIMAL) 
					{ 
						value_final = obj.getValue();
						out.write("Objective optimal Value: "+obj.getValue());
						out.newLine();
						for(int i = 0; i < d; i++) 
						    for(int j = 0; j < n; j++)
						    	for(int k = 0; k < m+1; k++)
						    		for (int l=0;l<_no;l++)
						    		{	
						    			if(x[i][j][k][l].get(GRB.DoubleAttr.X)>0)
						    			{
						    			out.write(x[i][j][k][l].get(GRB.StringAttr.VarName)
						    					+ " : " +x[i][j][k][l].get(GRB.DoubleAttr.X));
						    			out.newLine();
						    			}
						    		}	
					 } else if (optimstatus == GRB.Status.INF_OR_UNBD) 
					 	{ 
					        System.out.println("Model is infeasible or unbounded"); 
					        return;
					 	} else if (optimstatus == GRB.Status.INFEASIBLE) 
					        	{ 
							        System.out.println("Model is infeasible AAAAAAAAAAAAAA"); 
							        return; 
					        	} else if (optimstatus == GRB.Status.INTERRUPTED)
					        	{
					        		value_final = obj.getValue();
					        		out.write("Objective interrupt Value: "+obj.getValue());
									out.newLine();
									for(int i = 0; i < d; i++) 
									    for(int j = 0; j < n; j++)
									    	for(int k = 0; k < m+1; k++)
									    		for (int l=0;l<_no;l++)
									    		{	
									    			if(x[i][j][k][l].get(GRB.DoubleAttr.X)>0)
									    			{
									    			out.write(x[i][j][k][l].get(GRB.StringAttr.VarName)
									    					+ " : " +x[i][j][k][l].get(GRB.DoubleAttr.X));
									    			out.newLine();
									    			}
									    		}
					        		
					        	}
					
					 else
					 {
						 value_final = obj.getValue();
						 out.write("Objective feasible Value: "+obj.getValue());
						 out.newLine();
						 for(int i = 0; i < d; i++) 
							 for(int j = 0; j < n; j++)
								 for(int k = 0; k < m+1; k++)
									 for (int l=0;l<_no;l++)
									 {
										 if(x[i][j][k][l].get(GRB.DoubleAttr.X)>0)
							    			{
											 System.out.println(x[i][j][k][l].get(GRB.StringAttr.VarName)
								    					+ " " +x[i][j][k][l].get(GRB.DoubleAttr.X));
												 out.write(x[i][j][k][l].get(GRB.StringAttr.VarName)
								    					+ " : " +x[i][j][k][l].get(GRB.DoubleAttr.X));
												 out.newLine();
							    			}
										 
						    		}
					  }
				
					
				} catch (Exception e) {
					value_final = obj.getValue();
					out.write("Objective interrupt Value: "+obj.getValue());
					out.newLine();
					for(int i = 0; i < d; i++) 
					    for(int j = 0; j < n; j++)
					    	for(int k = 0; k < m+1; k++)
					    		for (int l=0;l<_no;l++)
					    		{	
					    			if(x[i][j][k][l].get(GRB.DoubleAttr.X)>0)
					    			{
					    			out.write(x[i][j][k][l].get(GRB.StringAttr.VarName)
					    					+ " : " +x[i][j][k][l].get(GRB.DoubleAttr.X));
					    			out.newLine();
					    			}
					    		}
				}
					model.dispose();
				env.dispose();
				System.gc();
			
				} catch(GRBException e3){			
					System.out.println("Error code1: " + e3.getErrorCode() + ". " +
							e3.getMessage());
					System.out.print("This problem can't be solved");
					
					
					}
			} catch ( IOException e1 ) {
					e1.printStackTrace();
					} finally {
						if ( out != null )
							try {
								out.close();
								} catch (IOException e) {
									e.printStackTrace();}
						}    
			try {
		  		out.close();
		  		} catch (IOException e2) {
		  			e2.printStackTrace();
		  			}
	}

	static List<List<Integer>> v_solution;
	static List<List<Integer>> f_solution;
	static List<Integer> list_v = new ArrayList<Integer>();
	static List<Integer> list_f = new ArrayList<Integer>();
	static int f_id=0;
	static boolean _finished=false;
	public static boolean BFS(MyGraph _g_tam,int V,int start, int finish, double bwMax)
	{
		int color[]= new int[V+1];
		int back[]= new int[V+1];
		Queue<Integer> qList= new LinkedList<Integer>();
		for (int i=0;i<V;i++)
		{
			//color = 0-> chua di qua lan nao
			//color = 1 -> da di qua 1 lan
			//color = 2 -> tat ca dinh ke da duoc danh dau
			color[i+1]=0;
			back[i+1]=-1;//mang luu cac dinh cha cua i
		}
		color[start]=1;
		qList.add(start);
		while(!qList.isEmpty())
		{
			//lay gia tri dau tien trong hang doi
			int u=qList.poll();
			if(u==finish)
			{
				//tim duoc duong roi
				return_path(start, finish, back);
				break;
			}
			else
			{
				//tim dinh ke chua di qua lan nao
				for(int v=0;v<V;v++)
				{
					if(_g_tam.getEdgeWeight(u, v+1)>=bwMax && color[v+1]==0)
					{
						color[u]=1;
						//luu lai nut cha cua v
						back[v+1]=u;
						qList.add(v+1);
					}
				}
				//da duyet het dinh ke cua dinh u
				color[u]=2;				
			}
		}
		return true;
		
	}
	
	
	public static boolean return_path(int u, int v,int back[])
	{
		
		if(u==v)
			list_v.add(v);
		else
		{
			if(back[v]==-1)
				return false;
			else
			{
				return_path(u, back[v], back);
				list_v.add(v);
			}
		}
		return true;
			
	}
	public static boolean Viterbi(String outFile)
	{
		functionCost=0;
		numberofCore=0;
		numberofEdge=0;
		numberofMidle=0;
		_duration=0;
		value_final=0;
		value_bandwidth =0;
		int[][] function_Loc = new int[m+1][n+1]; 
		for (int i=0;i<m;i++)
			for (int j=0;j<n;j++)
				function_Loc[i+1][j+1]=0;
		List<DefaultWeightedEdge> _p = new ArrayList<>();
		List<DefaultWeightedEdge> _p_Min = new ArrayList<>();
		List<Integer> nodeList;
		final long startTime = System.currentTimeMillis();
		//Tim duong cho tung demand d
		try {
			File file = new File(outFile);
			out = new BufferedWriter(new FileWriter(file));
	
		for (int i=0;i<n;i++)
		{
			ultilize_resource +=UtilizeFunction.value(g.getCap(i+1)) * g.getPriceNode(i+1);				
		}
		MyGraph _g_tam=	new MyGraph(g.cap,g.pricePernode,g.link_bandwidth,g.getPriceBandwidth());
		//loai cac canh ko thoa man sua lai doan nay. Co the la luc minh loai cac canh sau moi lan co d. g_i se thay doi sau moi lan d. g_i cung duoc xay dung lai
		for(int i=0;i<d;i++)
		{
			Demand _d=getDemand(i+1);
//			Graph g_i = new Graph(g.cap,g.pricePernode,g.link_bandwidth,g.getPriceBandwidth());
//			// tim 1 duong cho lan luot demand.
//			//xet tung doan 1, xet voi moi nut ktra voi cac nut con lai voi gia tri nho nhat (su dung dijsktra)
			
			
			SimpleWeightedGraph<String, DefaultWeightedEdge>  g_i = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class); 
	        
			for (int j=0;j<n;j++)
	        {
	        	g_i.addVertex("node"+(j+1));
	        }
	        DefaultWeightedEdge[] e= new DefaultWeightedEdge[(n*(n-1))/2];
	        int id=0;
	        
	        for (int j=0;j<n-1;j++)
	        {	        	
	        	for(int k=j+1;k<n;k++)
	        	{
	        		if(_g_tam.getEdgeWeight((j +1), (k+1))>_d.bwS())
	        		{
	        		e[id]=g_i.addEdge("node"+(j+1),"node"+(k+1));	        			
	        		g_i.setEdgeWeight(e[id], _g_tam.getEdgeWeight((j +1), (k+1)));
	        		id++;
	        		}
	        	}
	        }
	        DefaultWeightedEdge[] removed_edge = new DefaultWeightedEdge[id];
			int no_removed_edge =0;
			for( DefaultWeightedEdge v:g_i.edgeSet())
			{
				if(g_i.getEdgeWeight(v)<_d.bwS())
					removed_edge[no_removed_edge++]=v;				
			}
			for (int j=0;j<no_removed_edge;j++)
				g_i.removeEdge(removed_edge[j]);
			double cost_temp=0.0;
			double _min=Double.MAX_VALUE;
			int preNode=_d.sourceS();
			int lastNode=preNode;
			int currentNode=-1;
			int source;
			boolean flag=false;
			boolean flag1=false;
			
			for (int j=0;j<_d.getFunctions().length;j++)
			{
				removed_edge = new DefaultWeightedEdge[id];
				no_removed_edge =0;
				for( DefaultWeightedEdge v:g_i.edgeSet())
				{
					int int_s =Integer.parseInt(g_i.getEdgeSource(v).replaceAll("[\\D]", ""));
					int int_t =Integer.parseInt(g_i.getEdgeTarget(v).replaceAll("[\\D]", ""));
					if(_g_tam.getEdgeWeight(int_s, int_t)<_d.bwS())
					//if(g_i.getEdgeWeight(v)<_d.bwS())
						removed_edge[no_removed_edge++]=v;				
				}
				for (int l=0;l<no_removed_edge;l++)
					g_i.removeEdge(removed_edge[l]);
				flag=false;
				flag1=true;
				_min=Double.MAX_VALUE;
				Function vnf=_d.getFunctions()[j];
				//xet tung function 1 xem dat o dau thi 
				nodeList = new ArrayList<Integer>();
				nodeList.add(preNode);
				for (int j1=0;j1<_g_tam.V();j1++)
				{
					flag=false;
					if(UtilizeFunction.isBig(_g_tam.getCap(j1+1), vnf.getLamda()))
					{
						int functionOnNode=0;
						for (int j2=0;j2<m;j2++)
							functionOnNode+=function_Loc[j2+1][j1+1];
						if(preNode==j1+1)
						{
							//penalty cost for delay on this node
							cost_temp+=functionOnNode*g.getPriceBandwidth();
							cost_temp+= g.getPriceNode(j1+1)*100;						
							flag=true;
						}
						else
						{
							flag=false;
							_p =   DijkstraShortestPath.findPathBetween(g_i, "node"+preNode, "node"+(j1+1));
							if(_p!=null)
							{
								for (DefaultWeightedEdge l:_p)
								{
									cost_temp+=g_i.getEdgeWeight(l);
								}							
								cost_temp=cost_temp * g.getPriceBandwidth()*0.0001;
								cost_temp+= g.getPriceNode(j1+1)*100;						
							}
							else
							{
								cost_temp=Double.MAX_VALUE;
							}
							if(lastNode==j1+1)
								cost_temp+=g.getPriceBandwidth()*200;
						}
						if(cost_temp<_min)
						{
							flag1=flag;
							//luu lai duong di
							_p_Min=_p;
							_min=cost_temp;
							currentNode=j1+1;
						}
					}
				}
				if(!flag1)
				{
					if(_p_Min!=null)
					{
						source = preNode;
						
						while (_p_Min.size()>0)
							{	
								int ix =0;
								for(int l=0;l<_p_Min.size();l++)
								{
									int int_s =Integer.parseInt(g_i.getEdgeSource(_p_Min.get(l)).replaceAll("[\\D]", ""));
									int int_t =Integer.parseInt(g_i.getEdgeTarget(_p_Min.get(l)).replaceAll("[\\D]", ""));
									value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;
									value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;	
									if( int_s == source )
									{
										nodeList.add(int_t);
										source = int_t;
										ix = l;
										_g_tam.setEdgeWeight(int_s, int_t, _g_tam.getEdgeWeight(int_s, int_t)-_d.bwS());
										break;
									}
									if( int_t == source)
									{
										nodeList.add(int_s);
										source = int_s;
										ix = l;
										_g_tam.setEdgeWeight(int_s, int_t, _g_tam.getEdgeWeight(int_s, int_t)-_d.bwS());
										break;
									}
								}
								_p_Min.remove(ix);
							}
						System.out.println("current Node:::" + currentNode);
							_g_tam.setCap(currentNode,UtilizeFunction.minus(_g_tam.getCap(currentNode),vnf.getLamda()));
							
							function_Loc[vnf.id()][currentNode]++;
							value_final += g.getPriceNode(currentNode);
												
					}
					else
						return false;
				}
				else
				{
					nodeList.add(preNode);
					_g_tam.setCap(preNode,UtilizeFunction.minus(_g_tam.getCap(preNode),vnf.getLamda()));
					function_Loc[vnf.id()][preNode]++;
					value_final += g.getPriceNode(preNode);
				}
				for(int _i:nodeList)
				{
					System.out.print(_i+",");
					out.write(_i+", ");
				}	
				lastNode=preNode;
				if(nodeList.size()>0)
					preNode=nodeList.get(nodeList.size()-1);
				nodeList=null;
			}
			//tu node cuoi cung den destination
			
			nodeList = new ArrayList<Integer>();
			if (preNode!=_d.destinationS())
			{
				_p =   DijkstraShortestPath.findPathBetween(g_i, "node"+preNode, "node"+_d.destinationS());
				if(_p!=null)
				{
					source = preNode;
					while (_p.size()>0)
					{	
						int ix =0;
						for(int l=0;l<_p.size();l++)
						{
							int int_s =Integer.parseInt(g_i.getEdgeSource(_p.get(l)).replaceAll("[\\D]", ""));
							int int_t =Integer.parseInt(g_i.getEdgeTarget(_p.get(l)).replaceAll("[\\D]", ""));
							value_bandwidth +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;
							value_final +=g.getEdgeWeight(int_s, int_t) * g.getPriceBandwidth() *0.0001;	
							if( int_s == source )
							{
								nodeList.add(int_t);
								source = int_t;
								ix = l;
								_g_tam.setEdgeWeight(int_s, int_t, _g_tam.getEdgeWeight(int_s, int_t)-_d.bwS());
								break;
							}
							if( int_t == source)
							{
								nodeList.add(int_s);
								source = int_s;
								ix = l;
								_g_tam.setEdgeWeight(int_s, int_t, _g_tam.getEdgeWeight(int_s, int_t)-_d.bwS());
								break;
							}
						}
						_p.remove(ix);
					}
				}
				else
				{
					cost_temp=Double.MAX_VALUE;
				}
				for(int _i:nodeList)
				{
					System.out.print(_i+",");
					out.write(_i+", ");
				}	
					
				nodeList=null;
			}
			out.newLine();
			
		}
		for(int i=0;i<m;i++)
			for(int j=0;j<5;j++)
				if(function_Loc[i+1][j+1]>0)
					numberofCore+=function_Loc[i+1][j+1];
		for(int i=0;i<m;i++)
			for(int j=5;j<15;j++)
				if(function_Loc[i+1][j+1]>0)
					numberofMidle+=function_Loc[i+1][j+1];
		for(int i=0;i<m;i++)
			for(int j=15;j<n;j++)
				if(function_Loc[i+1][j+1]>0)
					numberofEdge+=function_Loc[i+1][j+1];
		for (int i=0;i<m;i++)
			for (int j=0;j<n;j++)
			{
				if(function_Loc[i+1][j+1]>0)
				{
					value_final+= (1-function_Loc[i+1][j+1])*Gain(g.getPriceNode(j+1));
					functionCost +=function_Loc[i+1][j+1]*g.getPriceNode(j+1);
					//val_final1+=UtilizeFunction.value(g.getPriceNode(j+1))*UtilizeFunction.value(functionArr[i].getLamda());
					System.out.println("function:"+(i+1)+ " node: "+(j+1)+" quantity: "+ function_Loc[i+1][j+1]);
					out.write("function:"+(i+1)+ " node: "+(j+1)+" quantity: "+ function_Loc[i+1][j+1]);
					out.newLine();
				}
			}
		out.write("cost for function:"+functionCost);
		out.newLine();
			_duration = System.currentTimeMillis() - startTime;
			System.out.println(_duration);
			out.write("Value solution: "+ value_final);
			out.newLine();
			out.write("Value bandwidth: "+ value_bandwidth);
			out.newLine();
			out.write("Runtime (mS): "+ _duration);
		} catch ( IOException e1 ) {
			e1.printStackTrace();
			} finally {
				if ( out != null )
					try {
						out.close();
						} catch (IOException e) {
							e.printStackTrace();}
				}    
	try {
  		out.close();
  		} catch (IOException e2) {
  			e2.printStackTrace();
  			}		
		return true;
	}
	public static boolean Random(String outFile)
	{

		numberofCore=0;
		numberofEdge=0;
		numberofMidle=0;
		_duration=0;
		value_final=0;
		value_bandwidth=0;
		functionCost =0;
		int[][] function_Loc = new int[m+1][n+1]; 
		final long startTime = System.currentTimeMillis();
		boolean flag=false;
		int dem=0;
		for (int i=0;i<n;i++)
		{
			ultilize_resource +=UtilizeFunction.value(g.getCap(i+1)) * g.getPriceNode(i+1);				
		}

		for (int k=0;k<m;k++)
			for (int j=0;j<n;j++)
				function_Loc[k+1][j+1]=0;
		MyGraph _g_tam=	new MyGraph(g.cap,g.pricePernode,g.link_bandwidth,g.getPriceBandwidth());
		File file = new File(outFile);
		try {
			out = new BufferedWriter(new FileWriter(file));


		//loai cac canh ko thoa man sua lai doan nay. Co the la luc minh loai cac canh sau moi lan co d. g_i se thay doi sau moi lan d. g_i cung duoc xay dung lai
		for(int i=0;i<d;i++)
		{
			Demand _d=getDemand(i+1);
			int src = _d.sourceS();
			int dest = _d.destinationS();
			int prev=src;
			for (int j=0;j<_d.getFunctions().length;j++)
			{
				Function _f = _d.getFunctions()[j];
				int next=-1;
				int c = g.V();
				boolean okFlag = false;
				while (c>0)
				{
					next = UtilizeFunction.randInt(1, g.V());
					if(UtilizeFunction.isBig(_g_tam.getCap(next), _f.getLamda()))
					{
						okFlag = true;
						value_final+=g.getPriceNode(_d.sourceS());
						function_Loc[_f.id()][next]++;
						_g_tam.setCap(next, UtilizeFunction.minus(_g_tam.getCap(next), _f.getLamda()));
						break;
					}
					c--;
				}
				if(!okFlag)
					return false;
				ArrayList<Integer> sp = ShortestPath(prev, next, _g_tam, _d.bwS());
				if(sp!=null)
				{
					for (int k=0;k<sp.size()-1;k++)
					{
						double w= _g_tam.getEdgeWeight(sp.get(k), sp.get(k+1));
						_g_tam.setEdgeWeight(sp.get(k), sp.get(k+1), w-_d.bwS());
						value_bandwidth +=g.getEdgeWeight(sp.get(k), sp.get(k+1)) * g.getPriceBandwidth() *0.0001;
						value_final +=g.getEdgeWeight(sp.get(k), sp.get(k+1)) * g.getPriceBandwidth() *0.0001;
					}
					for(int _i:sp)
					{
						System.out.print(_i+",");
						out.write(_i+", ");
					}	
				out.newLine();
				prev=next;
				}				
				else
				{
					return false;
				}
				
			}
			ArrayList<Integer> sp = ShortestPath(prev, dest, _g_tam, _d.bwS());
			if(sp!=null)
			{
				for (int k=0;k<sp.size()-1;k++)
				{
					double w= _g_tam.getEdgeWeight(sp.get(k), sp.get(k+1));
					_g_tam.setEdgeWeight(sp.get(k), sp.get(k+1), w-_d.bwS());
					value_bandwidth +=g.getEdgeWeight(sp.get(k), sp.get(k+1)) * g.getPriceBandwidth() *0.0001;
					value_final +=g.getEdgeWeight(sp.get(k), sp.get(k+1)) * g.getPriceBandwidth() *0.0001;
				}
				for(int _i:sp)
				{
					System.out.print(_i+",");
					out.write(_i+", ");
				}	
			out.newLine();
			
			}
			else
				return false;
		}
			//in ra file =======
					for (int i=0;i<m;i++)
						for (int j=0;j<n;j++)
						{
							if(function_Loc[i+1][j+1]>0)
							{
								value_final+= (1-function_Loc[i+1][j+1])*Gain(g.getPriceNode(j+1));
								functionCost+=g.getPriceNode(j+1)*function_Loc[i+1][j+1];
								//val_final1+=UtilizeFunction.value(g.getPriceNode(j+1))*UtilizeFunction.value(functionArr[i].getLamda());
								System.out.println("function:"+(i+1)+ " node: "+(j+1)+" quantity: "+ function_Loc[i+1][j+1]);
								out.write("function:"+(i+1)+ " node: "+(j+1)+" quantity: "+ function_Loc[i+1][j+1]);
								out.newLine();
							}
						}
					out.write("Cost for function: "+ functionCost);
					out.newLine();
			
			for(int i=0;i<n;i++)
				for (int j=0;j<n;j++)
				{
					ultilize_resource +=_g_tam.getEdgeWeight(i+1, j+1)*_g_tam.getPriceBandwidth();
				}
			_duration = System.currentTimeMillis() - startTime;
			System.out.println(_duration);
			out.write("Value solution: "+ value_final);
			out.newLine();
			out.write("Value bandwidth: "+ value_bandwidth);
			out.newLine();
			out.write("Runtime (mS): "+ _duration);
		} catch ( IOException e1 ) {
			e1.printStackTrace();
			} finally {
				if ( out != null )
					try {
						out.close();
						} catch (IOException e) {
							e.printStackTrace();}
				}    
	try {
  		out.close();
  		} catch (IOException e2) {
  			e2.printStackTrace();
  			}		
	
		return true;
	
	}
	public static boolean GreadyAlg(String outFile)//dang co van de o day
	{
		numberofCore=0;
		numberofEdge=0;
		numberofMidle=0;
		_duration=0;
		final long startTime = System.currentTimeMillis();
		v_solution = new ArrayList<List<Integer>>();
		f_solution=new ArrayList<List<Integer>>();
		boolean flag=false;
		int dem=0;
		for (int i=0;i<n;i++)
		{
			ultilize_resource +=UtilizeFunction.value(g.getCap(i+1)) * g.getPriceNode(i+1);				
		}
		MyGraph _g_tam=	new MyGraph(g.cap,g.pricePernode,g.link_bandwidth,g.getPriceBandwidth());
		//loai cac canh ko thoa man sua lai doan nay. Co the la luc minh loai cac canh sau moi lan co d. g_i se thay doi sau moi lan d. g_i cung duoc xay dung lai
		for(int i=0;i<d;i++)
		{
			Demand _d=getDemand(i+1);
			MyGraph g_i = new MyGraph(g.cap,g.pricePernode,g.link_bandwidth,g.getPriceBandwidth());
			for (int j=0;j<n;j++)
				for(int k=0;k<n;k++)
				{
					if(_g_tam.getEdgeWeight(j+1, k+1)<_d.bwS())
						g_i.removeLink(j+1, k+1);
				}
			//Tim duong cho tung demand d
			list_v = new ArrayList<Integer>();
			list_f = new ArrayList<Integer>();
			
			BFS(g_i, g_i.V(), _d.sourceS(), _d.destinationS(), _d.bwS());
			if(list_v==null)
				return false;
			else
			{
				dem=0;
				for (int j=0;j<_d.getFunctions().length;j++)
				{
					flag=false;
					for (int h=0;h<list_v.size();h++)
					{
						if(UtilizeFunction.isBig(_g_tam.getCap(list_v.get(h)), _d.getFunctions()[j].getLamda()))
						{
							list_f.add(_d.getFunctions()[j].id());
							_g_tam.setCap(list_v.get(h), UtilizeFunction.minus(_g_tam.getCap(list_v.get(h)), _d.getFunctions()[j].getLamda()));
							//_g_tam.setCapacity(list_v.get(h), _g_tam.getCapacity(list_v.get(h))-getBwFunction(_d.getFunctions()[j].id()));
							flag=true;
							dem++;
							break;
						}
					}
					if(!flag)
						list_f.add(0);
				}
				for(int j=0;j<list_v.size()-dem;j++)
					list_f.add(0);
			}
			v_solution.add(list_v);
			f_solution.add(list_f);
			list_v= null;
			list_f=null;
		}
		if(v_solution.size()==d)
			flag=true;
		File file = new File(outFile);
		try {
			out = new BufferedWriter(new FileWriter(file));
			int[][] function_Loc = new int[m+1][n+1];
			for (int i=0;i<m;i++)
				for (int j=0;j<n;j++)
					function_Loc[i+1][j+1]=0;
			for (int i=0;i<m;i++)
			{
				for(int j=0;j<d;j++)
				{
					int _lengList= v_solution.get(j).size();
					for (int k=0;k<_lengList;k++)
					{
						if(f_solution.get(j).get(k)==(i+1))
							function_Loc[i+1][v_solution.get(j).get(k)]++;
					}
				}
			}
			for (int i=0;i<m;i++)
				for (int j=0;j<n;j++)
					if(function_Loc[i+1][j+1]>1)
						value_final+= (1-function_Loc[i+1][j+1])*Gain(g.getPriceNode(j+1));
//			
//			for(int i=0;i<m;i++)
//				for(int j=0;j<5;j++)
//					if(function_Loc[i+1][j+1]>0)
//						numberofCore+=function_Loc[i+1][j+1];
//			for(int i=0;i<m;i++)
//				for(int j=5;j<15;j++)
//					if(function_Loc[i+1][j+1]>0)
//						numberofMidle+=function_Loc[i+1][j+1];
//			for(int i=0;i<m;i++)
//				for(int j=15;j<n;j++)
//					if(function_Loc[i+1][j+1]>0)
//						numberofEdge+=function_Loc[i+1][j+1];
			for (int i=0;i<d;i++)
			{
				//in ra tung demand
				out.write ("demand: "+(i+1));
				out.newLine();
				int _lengList= v_solution.get(i).size();
				if(_lengList >0)
				{
					for (int j=0;j<_lengList;j++)
					{
						if(f_solution.get(i).get(j)!=0)
						{
							out.write("function:"+f_solution.get(i).get(j)+ " node: "+(v_solution.get(i).get(j)));
							out.newLine();
							
							//value_final+=getDemand(i+1).getRate()*g.getPriceNode(v_solution.get(i).get(j));
							value_final+=g.getPriceNode(v_solution.get(i).get(j));
							ultilize_resource -= g.getPriceNode(v_solution.get(i).get(j))*UtilizeFunction.value(getFunction(f_solution.get(i).get(j)).getLamda());
						}
					}
					out.write("path for demand:");
					for (int j=0;j<_lengList-1;j++)
					{
						out.write(" "+v_solution.get(i).get(j));
						for(int k=j+1;k<_lengList;k++)
						{
							value_bandwidth+=g.getEdgeWeight(v_solution.get(i).get(j), v_solution.get(i).get(k))*g.getPriceBandwidth() *0.0001;
							value_final+=g.getEdgeWeight(v_solution.get(i).get(j), v_solution.get(i).get(k))*g.getPriceBandwidth()*0.0001 ;
						}
					}
					out.write(" "+v_solution.get(i).get(_lengList-1));
					out.newLine();
				}
				else
					return false;
				
				
			}
			for(int i=0;i<n;i++)
				for (int j=0;j<n;j++)
				{
					ultilize_resource +=_g_tam.getEdgeWeight(i+1, j+1)*_g_tam.getPriceBandwidth();
				}
			_duration = System.currentTimeMillis() - startTime;
			System.out.println(_duration);
			out.write("Value solution: "+ value_final);
			out.newLine();
			out.write("Value bandwidth: "+ value_bandwidth);
			out.newLine();
			out.write("Runtime (mS): "+ _duration);
		} catch ( IOException e1 ) {
			e1.printStackTrace();
			} finally {
				if ( out != null )
					try {
						out.close();
						} catch (IOException e) {
							e.printStackTrace();}
				}    
	try {
  		out.close();
  		} catch (IOException e2) {
  			e2.printStackTrace();
  			}		
	
		return true;
	}
	
//	
//	public static void GreadyAlgorithm(String outFile)
//	{
//		_duration=0;
//		final long startTime = System.currentTimeMillis();
//		boolean flag=false;
//		while (!flag)
//		{
//			v_solution = new ArrayList<List<Integer>>();
//			f_solution=new ArrayList<List<Integer>>();
//			_g_tam=	new Graph(g.cap,g.pricePernode,g.link_bandwidth,g.getPriceBandwidth());
//			//_g_tam= new Graph(g.r, g.K, g.link_bandwidth, g.getPriceBandwidth(),true);
//			for(int i=0;i<d;i++)
//			{
//				//Tim duong cho tung demand d
//				list_v = new ArrayList<Integer>();
//				list_f = new ArrayList<Integer>();
//				f_id=0;
//				_finished=false;
//				Demand _d=getDemand(i+1);
//				service_backTrack(0, 0, _d);
//				list_v= null;
//				list_f=null;
//			}
//			if(v_solution.size()==d)
//				flag=true;
//		}
//		value_final=0.0;
//		File file = new File(outFile);
//		try {
//			out = new BufferedWriter(new FileWriter(file));
//			for (int i=0;i<d;i++)
//			{
//				//in ra tung demand
//				out.write ("demand: "+(i+1));
//				out.newLine();
//				int _lengList= v_solution.get(i).size();
//				for (int j=0;j<_lengList;j++)
//				{
//					if(f_solution.get(i).get(j)!=0)
//					{
//						out.write("function:"+f_solution.get(i).get(j)+ " node: "+(v_solution.get(i).get(j)));
//						out.newLine();
//						//value_final+=getDemand(i).getRate()*UtilizeFunction.value(g.getPriceNode(v_solution.get(i).get(j)))*UtilizeFunction.value(functionArr[f_solution.get(i).get(j)].getLamda());
//						value_final+=getDemand(i).getRate()*v_solution.get(i).get(j)*UtilizeFunction.value(functionArr[f_solution.get(i).get(j)].getLamda());
//					}
//				}
//				out.write("path for demand:");
//				for (int j=0;j<_lengList-1;j++)
//				{
//					out.write(" "+v_solution.get(i).get(j));
//					for(int k=j+1;k<_lengList;k++)
//						value_final+=g.getEdgeWeight(v_solution.get(i).get(j), v_solution.get(i).get(k))*g.getPriceBandwidth() ;
//				}
//				out.write(" "+v_solution.get(i).get(_lengList-1));
//				out.newLine();
//				
//			}
//			_duration = System.currentTimeMillis() - startTime;
//			System.out.println(_duration);
//			out.write("Value solution: "+ value_final);
//			out.newLine();
//			out.write("Runtime (mS): "+ _duration);
//		} catch ( IOException e1 ) {
//			e1.printStackTrace();
//			} finally {
//				if ( out != null )
//					try {
//						out.close();
//						} catch (IOException e) {
//							e.printStackTrace();}
//				}    
//	try {
//  		out.close();
//  		} catch (IOException e2) {
//  			e2.printStackTrace();
//  			}
//		
//	}
//	static int specNo=-1;
//	public static boolean service_backTrack(int k,int h, Demand _d)//k la chi so cua list_v,; h la chi so cua fucntion
//	{
//		List<Integer> Adj;
//		boolean[] visit = new boolean[n+1];
//		if(h==_d.getFunctions().length)
//		{
//			//tim ra duoc
//			if(list_v.get(k-1)==_d.destinationS())
//			{
//				//ket thuc
//				_finished=true;
//				v_solution.add(list_v);
//				f_solution.add(list_f);
//				return true;
//			}
//			else
//			{
//				//can tim duong den destination	
//
//				for(int i=0;i<n+1;i++)
//					visit[i]=false;
//				int noH=-1;
//				int preV=-1;
//				if(list_v.size()>0)
//					preV=list_v.get(list_v.size()-1);
//				else
//					preV=-1;
//				if(list_v.size()>1)
//					noH=list_v.get(list_v.size()-2);
//				else
//					noH=-1;
//				Adj = new ArrayList<Integer>();
//				//truong hop la node trung gian
//				for (int i=0;i<n;i++)
//					if ((specNo!=(i+1))&&(preV!=(i+1))&&(noH!=(i+1))&&(_g_tam.getEdgeWeight(i+1, list_v.get(k-1))>_d.bwS()))
//					{
//						if(_d.destinationS()==(i+1))
//						{
//							//ket thuc o day
//							list_f.add(0);
//							list_v.add(i+1);
//							_finished=true;
//							v_solution.add(list_v);
//							f_solution.add(list_f);
//							return true;
//						}
//						else
//						{
//							Adj.add(i+1);
//							visit[i+1]=false;
//						}
//						
//					}
//			if(specNo==-1)
//				specNo=preV;
//			Collections.shuffle(Adj);
//			for (int i : Adj) 
//			{					
//				if(!visit[i])
//				{
//					list_v.add(i);
//					visit[i]=true;
//					list_f.add(0);
//					_g_tam.setEdgeWeight(i, _d.destinationS(), _g_tam.getEdgeWeight(i,_d.destinationS())-_d.bwS());
//					if(f_id==_d.getFunctions().length-1)
//						service_backTrack(k+1, f_id+1, _d);
//					if(_finished)
//						break;
//					else
//					{
//						list_v.remove(list_v.size()-1);
//						list_f.remove(list_f.size()-1);
//						_g_tam.setEdgeWeight(i, _d.destinationS(), _g_tam.getEdgeWeight(i,_d.destinationS())+_d.bwS());
//						visit[i]=false;
//					}
//					//Cap nhat lai do thi g
//				}
//			}
//			Adj=null;
//			visit=null;
//			}	
//		}
//		else
//		{			
//			if(!_finished)
//			{
//				for(int i=0;i<n+1;i++)
//					visit[i]=false;
//				if(k>0)
//				{
//					int isInterNode= UtilizeFunction.randInt(0, 1);
//					if(isInterNode==0)
//					{
//						f_id++;
//					}
//					int noH=-1;
//					int preV=-1;
//					if(list_v.size()>0)
//						preV=list_v.get(list_v.size()-1);
//					else
//						preV=-1;
//					if(list_v.size()>1)
//						noH=list_v.get(list_v.size()-2);
//					else
//						noH=-1;
//					Adj = new ArrayList<Integer>();
//					if(h>=0)
//					{				
//						//truog hop la node binh thuong
//						
//						for(int i=0;i<n;i++)
//						{
//							if((preV==i+1) &&(_g_tam.getCapacity(i+1)>_d.getFunctions()[h].bw()) )
//							{
//								if(list_f.get(list_f.size()-1)!=0)
//								{
//								Adj.add(i+1);
//								visit[i+1]=false;
//								}
//							}
//							else						
//								if((noH!=(i+1))&&(_g_tam.getCapacity(i+1)>_d.getFunctions()[h].bw())&& (_g_tam.getEdgeWeight(i+1, list_v.get(k-1))>_d.bwS()))
//								{
//									Adj.add(i+1);
//									visit[i+1]=false;
//								}
//						}
//					}
//					else
//					{
//						//truong hop la node trung gian
//						for (int i=0;i<n;i++)
//							if ((specNo!=(i+1))&&(preV!=(i+1))&&(noH!=(i+1))&&(_g_tam.getEdgeWeight(i+1, list_v.get(k-1))>_d.bwS()))
//							{
//								Adj.add(i+1);
//								visit[i+1]=false;
//							}
//						if(isInterNode==1)
//							if(specNo==-1)
//								specNo=preV;
//						else
//							specNo=-1;
//					}
//			
//					Collections.shuffle(Adj);
//				
//				for (int i : Adj) 
//				{					
//					if(!visit[i])
//					{
//						list_v.add(i);
//						visit[i]=true;
//						
//						//Cap nhat lai do thi g
//						if(h>0)
//						{
//							list_f.add(_d.getFunctions()[h].id());
//							_g_tam.setCapacity(i, _g_tam.getCapacity(i)-_d.getFunctions()[h].bw());
//						}
//						else
//							list_f.add(0);
//						_g_tam.setEdgeWeight(i, list_v.get(k-1), _g_tam.getEdgeWeight(i, list_v.get(k-1))-_d.bwS());
//						
//						if(isInterNode==0)
//						{							//f_id++;
//							service_backTrack(k+1,f_id,_d);
//						}
//						else
//							if(f_id==_d.getFunctions().length-1)
//								service_backTrack(k+1, f_id+1, _d);
//							else
//								service_backTrack(k+1,0,_d);
//						if(_finished)
//							break;
//						else
//						{
//							list_v.remove(list_v.size()-1);
//							list_f.remove(list_f.size()-1);
//							_g_tam.setEdgeWeight(i, list_v.get(k-1), _g_tam.getEdgeWeight(i, list_v.get(k-1))+_d.bwS());
//							if(h>0)
//							{
//								f_id--;
//								_g_tam.setCapacity(i, _g_tam.getCapacity(i)+_d.getFunctions()[h].bw());
//							}
//							visit[i]=false;
//						}
//						//Cap nhat lai do thi g
//					}
//				}	
//				Adj=null;
//				visit=null;
//				}
//				else
//				{
//					int isInterNode= UtilizeFunction.randInt(0, 1);
//					Adj = new ArrayList<Integer>();
//					
//					//truong hop la node dau tien-> neu source co the dap ung -> lay source luon
//					if(_g_tam.getCapacity(_d.sourceS())>_d.getFunctions()[h].bw())
//					{
//						list_v.add(_d.sourceS());
//						list_f.add(_d.getFunctions()[h].id());
//						_g_tam.setCapacity(_d.sourceS(), _g_tam.getCapacity(_d.sourceS())-_d.getFunctions()[h].bw());
//						if(isInterNode==0)
//						{
//							f_id++;
//							service_backTrack(k+1,f_id,_d);
//						}
//						else
//							service_backTrack(k+1,-1,_d);
//					}
//					else
//					{
//						for(int i=0;i<n;i++)
//						{
//							if(_g_tam.getEdgeWeight(i+1, _d.sourceS())>_d.bwS())
//							{
//								Adj.add(i+1);
//								visit[i+1]=false;
//							}
//						}
//						Collections.shuffle(Adj);
//						for (int i : Adj) {
//							if(!visit[i])
//							{
//								list_v.add(i);
//								visit[i]=true;
//								
//								//Cap nhat lai do thi g
//								list_f.add(0);
//								_g_tam.setCapacity(i, _g_tam.getCapacity(i)-_d.getFunctions()[h].bw());
//								
//								if(isInterNode==0)
//								{
//									//f_id++;
//									service_backTrack(k+1,f_id,_d);
//								}
//								else
//									service_backTrack(k+1,-1,_d);
//								if(_finished)
//									break;
//								else
//								{
//									list_v.remove(list_v.size()-1);
//									list_f.remove(list_f.size()-1);
//									_g_tam.setCapacity(i, _g_tam.getCapacity(i)+_d.getFunctions()[h].bw());
//									visit[i]=false;
//								}
//								//Cap nhat lai do thi g
//							}
//						}
//						Adj =null;
//						visit =null;
//					}
//				}
//			}
//		}
//		return true;
//		
//	}
//	public static void RandomAlgorithm(String outFile)
//	{
//		_duration=0;
//		final long startTime = System.currentTimeMillis();
//		boolean flag;
//		List<List<Integer>> v_solution ;
//		List<List<Integer>> f_solution;
//		while(true)
//		{
//			v_solution = new ArrayList<List<Integer>>();
//			f_solution = new ArrayList<List<Integer>>();
//			for (int i=0;i<d;i++)
//			{
//				Demand _d = getDemand(i+1);
//				int numberOfFunction =_d.getFunctions().length;
//				int maxNUM=0;
//				if((n*m/2)<(numberOfFunction+2)) 
//					maxNUM=(int)n*m/2;
//				else
//					maxNUM = numberOfFunction +2;
//				int noNode= UtilizeFunction.randInt(numberOfFunction, maxNUM);
//				List<Integer> list = new ArrayList<Integer>();
//				for( int j = 0 ; j < noNode ; j++ ) { 
//					int v_random;
//					do 
//					{
//						flag=true;
//						v_random= UtilizeFunction.randInt(1,g.V());
//						if((j>1) &&(list.get(j-2)==v_random))
//						{
//							flag=false;
//							break;
//						}
//					}while(!flag);
//	                list.add(v_random);
//	            }
//				v_solution.add(list);//ham v
//				//ham function
//				List<Integer> f0_random = new ArrayList<Integer>();
//				for (int j=0;j<noNode-numberOfFunction;j++)
//				{					
//					int r;
//					do 
//					{
//						flag=true;
//						r=UtilizeFunction.randInt(1,noNode-2);
//						for (Integer integer : f0_random) {
//							if(r==integer)
//							{
//								flag=false;
//								break;
//							}
//							
//						}
//					}while(!flag);
//					f0_random.add(r);
//				}
//				Collections.sort(f0_random);
//				int temp=0;
//				int idFunction=0;
//				List<Integer> list2 = new ArrayList<Integer>();
//				for (Integer integer : f0_random) {
//					for (int j=temp;j<integer;j++)
//					{
//						list2.add(_d.getFunctions()[idFunction++].id());
//						temp++;
//					}
//					list2.add(0);
//					temp++;					
//				}
//				for (int j=temp;j<noNode;j++)
//					list2.add(_d.getFunctions()[idFunction++].id());
//				f_solution.add(list2);				
//			}
//			if (isFeasible(v_solution,f_solution))
//				break;
//		}
//		//In ra ket qua, Tinh cost, time
//		value_final=0.0;
//		File file = new File(outFile);
//		try {
//			out = new BufferedWriter(new FileWriter(file));
//			for (int i=0;i<d;i++)
//			{
//				//in ra tung demand
//				out.write ("demand: "+(i+1));
//				out.newLine();
//				int _lengList= v_solution.get(i).size();
//				for (int j=0;j<_lengList;j++)
//				{
//					if(f_solution.get(i).get(j)!=0)
//					{
//						out.write("function:"+f_solution.get(i).get(j)+ " node: "+(v_solution.get(i).get(j)));
//						out.newLine();
//						value_final+=getBwFunction(f_solution.get(i).get(j))*g.getPriceForUnitNode(v_solution.get(i).get(j));
//					}
//				}
//				out.write("path for demand:");
//				for (int j=0;j<_lengList-1;j++)
//				{
//					out.write(" "+v_solution.get(i).get(j));
//					for(int k=j+1;k<_lengList;k++)
//						value_final+=g.getEdgeWeight(v_solution.get(i).get(j), v_solution.get(i).get(k))*getBwService(i+1)*g.getPriceBandwidth() ;
//				}
//				out.write(" "+v_solution.get(i).get(_lengList-1));
//				out.newLine();
//				
//			}
//			_duration = System.currentTimeMillis() - startTime;
//			System.out.println(_duration);
//			out.write("Value solution: "+ value_final);
//			out.newLine();
//			out.write("Runtime (mS): "+ _duration);
//		} catch ( IOException e1 ) {
//			e1.printStackTrace();
//			} finally {
//				if ( out != null )
//					try {
//						out.close();
//						} catch (IOException e) {
//							e.printStackTrace();}
//				}    
//	try {
//  		out.close();
//  		} catch (IOException e2) {
//  			e2.printStackTrace();
//  			}
//		
//		
//	}
	
	public static boolean isFeasible(List<List<Integer>> v_solution,List<List<Integer>> f_solution)
	{
		double[] usedResource=new double[g.V()+1];//tinh so luong resource da su dung cua cac node
		for (int i=0;i<g.V();i++)
			usedResource[i]=0.0;
		for(int i=0;i<d;i++)
		{
			int _lengList= v_solution.get(i).size();
			if (_lengList >1 && (v_solution.get(i).get(_lengList-2)==v_solution.get(i).get(_lengList-1)) && f_solution.get(i).get(_lengList-2)==0)
				return false;
			if (_lengList>0 && (v_solution.get(i).get(0)==v_solution.get(i).get(1)) && f_solution.get(i).get(1)==0)
				return false;
			for (int j=0;j<_lengList;j++)
			{
				if(f_solution.get(i).get(j)!=0)
					usedResource[v_solution.get(i).get(j)]+=getBwFunction(f_solution.get(i).get(j));
			}
		}
		for (int i=0;i<g.V();i++)
		{
			if(usedResource[i+1]>g.getCapacity(i+1))
				return false;
		}
		
		double[][] usedBandwidth= new double[g.V()+1][g.V()+1];
		for(int i=0;i<d;i++)
		{
			int _lengList= v_solution.get(i).size();
			for (int j=0;j<_lengList-1;j++)
				for(int k=j+1;k<_lengList;k++)
				{
					if(g.getEdgeWeight(v_solution.get(i).get(j), v_solution.get(i).get(k))>0)
					{
						usedBandwidth[v_solution.get(i).get(j)][v_solution.get(i).get(k)]+=getBwService(i+1);
						usedBandwidth[v_solution.get(i).get(k)][v_solution.get(i).get(j)]+=getBwService(i+1);
					}
					else {
						return false;
					}
				}
		}
		for (int i=0;i<g.V()-1 ;i++)
			for (int j=i+1;j<g.V();j++)
				if(usedBandwidth[i+1][j+1]>g.getEdgeWeight(i+1, j+1))
					return false;
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public static void mainG(String[] args) {//giai voi gurobi
		BufferedWriter out1 = null;
		//currentTime=Double.parseDouble(args[0]);
		//maxNode = Double.parseDouble(args[0]);
		String folderName = args[0];
		File dir = new File(folderName);
		String[] extensions = new String[] { "txt" };
		try {
			System.out.println("Getting all .txt in " + dir.getCanonicalPath()
					+ " including those in subdirectories");
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);
		String chuoi1= "output_withoutCover.txt";
		File _f = new File(chuoi1 );
		String str="";
		try {
			out1 = new BufferedWriter(new FileWriter(_f,true));
			for (File file : files) {
				try {
					System.out.println("file: " + file.getCanonicalPath());
					ReadInputFile(file.getPath());
					//ReadInput(file.getPath());
					str=file.getName(); 
					///TODO
					String chuoi2= "withoutCover";
					str = str.replace("in",chuoi2 );
					out1.write(str);
					_duration=0;
					value_final=0.0;
					ultilize_resource=0;
					value_bandwidth =0;
					Model_MM(str);
						//Model_Minh(str);
					//Model_cover(str);
						out1.write(" "+m + " " +d +" "+n+" "+E +" "+ value_final+" "+_duration);
						out1.newLine();
					
					
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		} catch ( IOException e1 ) {
			e1.printStackTrace();
			} 
		try {
			out1.close();
		} catch (IOException e) {
			// TODO Auto-generated scatch block
			e.printStackTrace();
		}	
		
    }
	public static void mainCO(String[] args) {//Cover
		BufferedWriter out1 = null;
		//currentTime=Double.parseDouble(args[0]);
		//maxNode = Double.parseDouble(args[0]);
		String folderName = args[0];
		File dir = new File(folderName);
		String[] extensions = new String[] { "txt" };
		try {
			System.out.println("Getting all .txt in " + dir.getCanonicalPath()
					+ " including those in subdirectories");
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);
		String chuoi1= "output_Callback3.txt";
		File _f = new File(chuoi1 );
		String str="";
		try {
			out1 = new BufferedWriter(new FileWriter(_f,true));
			for (File file : files) {
				try {
					System.out.println("file: " + file.getCanonicalPath());
					ReadInputFile(file.getPath());
					//ReadInput(file.getPath());
					str=file.getName(); 
					///TODO
					String chuoi2= "Callback3";
					str = str.replace("in",chuoi2 );
					out1.write(str);
					_duration=0;
					value_final=0.0;
					ultilize_resource=0;
					value_bandwidth =0;
					
					//Model_cover(str);
					preSolveProblem(str);
						out1.write(" "+m + " " +d +" "+n+" "+E +" "+ value_final+" "+_duration);
						out1.newLine();
					
					
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		} catch ( IOException e1 ) {
			e1.printStackTrace();
			} 
		try {
			out1.close();
		} catch (IOException e) {
			// TODO Auto-generated scatch block
			e.printStackTrace();
		}	
		
    }
	public static void mainAAAA(String[] args)// so sanh ket qua doi voi heuristic cho truong hop alpha/beta tang dan
	{
		
		alpha=1;
		beta=1;
		gama=alpha;
		theta=beta;
		currentTime=2;
		BufferedWriter out1 = null;
		File dir = new File("data3TopoFat4");
		//File dir = new File("test_BCube");
		String[] extensions = new String[] { "txt" };
		try {
			System.out.println("Getting all .txt in " + dir.getCanonicalPath()
					+ " including those in subdirectories");
		} catch (IOException e) {
			e.printStackTrace();
		}
		@SuppressWarnings("unchecked")
		List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);

		File _f = new File("output_albe_max.txt");
		//File _f = new File("output_albe_min.txt");
		String str="";
		try {
			out1 = new BufferedWriter(new FileWriter(_f,true));
			for (File file : files) {
				try {
					System.out.println("file: " + file.getCanonicalPath());
					ReadInputFile(file.getPath());
					str=file.getName();
					
					//str = str.replace("in", "min");
					str = str.replace("in", "max");
					out1.write(str+ " "+m + " " +d +" "+n+" "+E);
					if (IsCapacity())
					{					
						_Dist();
						alpha=1;
						beta=0;
						gama=alpha;
						theta=alpha;
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						String str1= "0_"+str;
						
						str1= "resultTest//"+str1;
						//if(heuristic_2(str1))
						if(heuristic(str1))
						{
							out1.write(" "+ value_final);
							
						}
						else
						{
							//out1.write("MinMin ");
							out1.write("MaxMin ");
							
						}
						for(int i=0;i<10;i=i+1)
						{
							alpha-=0.1;
							beta+=0.1;
							gama=alpha;
							theta=alpha;
							_duration=0;
							value_final=0.0;
							ultilize_resource=0;
							value_bandwidth =0;
							str1= (i+1)+"_"+str;
							str1= "resultTest//"+str1;
							//if(heuristic_2(str1))
							if(heuristic(str1))
							{
								out1.write(" "+ value_final);
								
							}
							else
							{
								//out1.write("MinMin ");
								out1.write("MaxMin ");
								
							}
						}
						
						out1.newLine();
					}
					else
					{
						out1.write(" Khong du capacity ");
						out1.newLine();
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		} catch ( IOException e1 ) {
			e1.printStackTrace();
			} 
		try {
			out1.close();
		} catch (IOException e) {
			// TODO Auto-generated scatch block
			e.printStackTrace();
		}

	}
	public static void mainV(String[] args)// Viterbi
	{
		
		alpha=1;
		beta=1;
		gama=alpha;
		theta=beta;
		currentTime=2;
		BufferedWriter out1 = null;
		File dir = new File("test");
		String[] extensions = new String[] { "txt" };
		try {
			System.out.println("Getting all .txt in " + dir.getCanonicalPath()
					+ " including those in subdirectories");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		@SuppressWarnings("unchecked")
		List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);

		//File _f = new File("output_albe_min.txt");
		File _f = new File("output_Viterbi.txt");
		String str="";
		try {
			out1 = new BufferedWriter(new FileWriter(_f,true));
			for (File file : files) {
				try {
					System.out.println("file: " + file.getCanonicalPath());
					ReadInputFile(file.getPath());
					str=file.getName(); 
					//str = str.replace("in", "min");
					str = str.replace("test", "Viterbi");
					out1.write(str+ " "+m + " " +d +" "+n+" "+E);
					if (IsCapacity())
					{					
						_Dist();
						alpha=1;
						beta=0;
						gama=alpha;
						theta=alpha;
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						String str1= str;
						//if(heuristic_2(str1))
						if(Viterbi(str1))
						{
							out1.write(" "+ value_final+ " "+ _duration);
							
						}
						else
						{
							//out1.write("MinMin ");
							out1.write("Viterbi ");
							
						}
						
						out1.newLine();
					}
					else
					{
						out1.write(" Khong du capacity ");
						out1.newLine();
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		} catch ( IOException e1 ) {
			e1.printStackTrace();
			} 
		try {
			out1.close();
		} catch (IOException e) {
			// TODO Auto-generated scatch block
			e.printStackTrace();
		}

	}
	
	public static void mainAB(String[] args)
	{
		
		alpha=1;
		beta=0;
		gama=alpha;
		theta=alpha;
		currentTime=2;
		BufferedWriter out1 = null;
		double sum=0;
		File dir = new File("test_newtopology");
		String[] extensions = new String[] { "txt" };
		try {
			System.out.println("Getting all .txt in " + dir.getCanonicalPath()
					+ " including those in subdirectories");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		@SuppressWarnings("unchecked")
		List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);

		File _f = new File("output_newTopo.txt");
		String str="";
		try {
			out1 = new BufferedWriter(new FileWriter(_f,true));
			for (File file : files) {
				try {
					System.out.println("file: " + file.getCanonicalPath());
					ReadInputFile(file.getPath());
					str=file.getName(); 
					str = str.replace("in", "out");
					out1.write(str);
					if (IsCapacity())
					{					
						_Dist();
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						str = str.replace("out", "out_MaxMin");
						if(heuristic(str))
						{
							sum= numberofCore+numberofEdge+numberofMidle;
							out1.write(" "+m + " " +d +" "+n+" "+E +" "+ value_final+" "+ value_bandwidth+" "+numberofCore/sum+" "+ numberofMidle/sum+" "+numberofEdge/sum);
							//out1.write(" "+m + " " +d +" "+n+" "+E +" "+ value_final+" "+ value_bandwidth);
							
						}
						else
						{
							out1.write("MaxMin ");
							
						}
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						str = str.replace("MaxMin", "MinMin");
						if(heuristic_2(str))
						{
							sum= numberofCore+numberofEdge+numberofMidle;
							out1.write(" "+ value_final+" "+ value_bandwidth+" "+numberofCore/sum+" "+ numberofMidle/sum+" "+numberofEdge/sum);
							
						}
						else
						{
							out1.write(" MinMin ");
							
						}
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						alpha=0.88;
						beta=0.12;
						gama=alpha;
						theta=alpha;
						str = str.replace("MinMin", "1MaxMin");
						if(heuristic(str))
						{
							sum= numberofCore+numberofEdge+numberofMidle;
							out1.write(" "+value_final+" "+ value_bandwidth+" "+numberofCore/sum+" "+ numberofMidle/sum+" "+numberofEdge/sum);
							
						}
						else
						{
							out1.write("1MaxMin ");
							
						}
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						str = str.replace("1MaxMin", "1MinMin");
						if(heuristic_2(str))
						{
							sum= numberofCore+numberofEdge+numberofMidle;
							out1.write(" "+ value_final+" "+ value_bandwidth+" "+numberofCore/sum+" "+ numberofMidle/sum+" "+numberofEdge/sum);
							
						}
						else
						{
							out1.write(" 1MinMin ");
							
						}
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						alpha=0.5;
						beta=0.5;
						gama=alpha;
						theta=alpha;
						str = str.replace("1MinMin", "9MaxMin");
						if(heuristic(str))
						{
							sum= numberofCore+numberofEdge+numberofMidle;
							out1.write(" "+ value_final+" "+ value_bandwidth+" "+numberofCore/sum+" "+ numberofMidle/sum+" "+numberofEdge/sum);
							
						}
						else
						{
							out1.write("9MaxMin ");
							
						}
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						str = str.replace("9MaxMin", "9MinMin");
						if(heuristic_2(str))
						{
							sum= numberofCore+numberofEdge+numberofMidle;
							out1.write(" "+ value_final+" "+ value_bandwidth+" "+numberofCore/sum+" "+ numberofMidle/sum+" "+numberofEdge/sum);
							
						}
						else
						{
							out1.write(" 9MinMin ");
							
						}
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						str = str.replace("9MinMin", "random");
						if(Viterbi(str))
						{
							sum= numberofCore+numberofEdge+numberofMidle;
							//out1.write(" "+value_final+" "+ value_bandwidth);
							out1.write(" "+ value_final+" "+ value_bandwidth+" "+numberofCore/sum+" "+ numberofMidle/sum+" "+numberofEdge/sum);
							
						}
						else
						{
							out1.write("random");
							
						}
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						str = str.replace("random", "nonNFV");
						if(nonNFV(str))
						{
							out1.write(" "+value_bandwidth);
							out1.newLine();
						}
						else
						{
							out1.write("non-NFV ");
							out1.newLine();
						}
					}
					else
					{
						out1.write(" Khong du capacity ");
						out1.newLine();
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		} catch ( IOException e1 ) {
			e1.printStackTrace();
			} 
		try {
			out1.close();
		} catch (IOException e) {
			// TODO Auto-generated scatch block
			e.printStackTrace();
		}

	}
	
	public static void mainC(String[] args)
	{
		
		alpha=0.5;
		beta=0.5;
		gama=0.5;
		theta=0.5;
		currentTime=2;
		BufferedWriter out1 = null;
		double sum=0;
		//File dir = new File("data3TopoFat4");
		File dir = new File("data3TopoBCube4");
		String[] extensions = new String[] { "txt" };
		try {
			System.out.println("Getting all .txt in " + dir.getCanonicalPath()
					+ " including those in subdirectories");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		@SuppressWarnings("unchecked")
		List<File> files = (List<File>) FileUtils.listFiles(dir, extensions, true);

		File _f = new File("output\\output161201.txt");
		String str="";
		try {
			out1 = new BufferedWriter(new FileWriter(_f,true));
			for (File file : files) {
				try {
					System.out.println("file: " + file.getCanonicalPath());
					ReadInputFile(file.getPath());
					str=file.getName(); 
					//str="resultFat\\"+str;
					str="resultBCube\\"+str;
					str = str.replace("in", "out");
					out1.write(str);
					if (IsCapacity())
					{					
						_Dist();
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						str = str.replace("out", "out_MaxMin");
						if(heuristic(str))
						{
							out1.write(" "+m + " " +d +" "+n+" "+E +" "+ value_final+" "+ value_bandwidth+ " "+_duration);
							//out1.write(" "+m + " " +d +" "+n+" "+E +" "+ value_final+" "+ value_bandwidth);
							
						}
						else
						{
							out1.write(" "+m + " " +d +" "+n+" "+E +" MaxMin MaxMin MaxMin");
							
						}
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						str = str.replace("MaxMin", "MinMin");
						if(heuristic_2(str))
						{
							sum= numberofCore+numberofEdge+numberofMidle;
							out1.write(" "+ value_final+" "+ value_bandwidth+ " "+_duration);
							
						}
						else
						{
							out1.write(" MinMin MinMin MinMin");
							
						}
						
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						str = str.replace("MinMin", "random");
						if(Random(str))
						{
							sum= numberofCore+numberofEdge+numberofMidle;
							//out1.write(" "+value_final+" "+ value_bandwidth);
							out1.write(" "+ value_final+" "+ value_bandwidth+ " "+_duration);
							
						}
						else
						{
							out1.write(" random random random");
							
						}
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						str = str.replace("random", "nonNFV");
						if(nonNFV(str))
						{
							out1.write(" "+value_bandwidth+ " "+_duration);
						}
						else
						{
							out1.write(" non-NFV non-NFV");
							
						}
						_duration=0;
						value_final=0.0;
						ultilize_resource=0;
						value_bandwidth =0;
						str = str.replace("nonNFV", "Viterbi");
						if(Viterbi(str))
						{
							out1.write(" "+ value_final+ " "+ value_bandwidth+ " "+ _duration);
							
						}
						else
						{
							out1.write(" Viterbi Viterbi Viterbi");
							
						}
						out1.newLine();
					}
					else
					{
						out1.write(" Khong du capacity ");
						out1.newLine();
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		} catch ( IOException e1 ) {
			e1.printStackTrace();
			} 
		try {
			out1.close();
		} catch (IOException e) {
			// TODO Auto-generated scatch block
			e.printStackTrace();
		}

	}
	
public static void main(String[] args) {
	//UtilizeFunction.randomTopology("inputFile.txt");
	//UtilizeFunction.randomNewest("lib\\input170512.txt");
	//UtilizeFunction.randomNewest("lib\\input160521.txt");
	//UtilizeFunction.random1DemandNTopology("inputG2.txt");
	UtilizeFunction.random3Topology("lib\\inputTopology.txt");
}

}