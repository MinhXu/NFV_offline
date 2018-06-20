
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.io.BufferedWriter;

import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;


public class Demand {
	//source, destination,bandwidth,arrival time,set of function (number of function)
    private int idS;//id of demand
    private int noF;//number of Functions
    private double bwS;//bandwidth
    private int source;
    private int destination;
    private double arrivalTime;
    private double rate;//rate requirement for each demand
    private Function[] arrF;//set of functions
    static BufferedWriter out;
    public static Properties props;
    
    // empty graph with V vertices
    public Demand(int idS) {
        if (idS <= 0) throw new RuntimeException("Number of vertices must be nonnegative");
        this.idS = idS;
    }
    public Demand(int idS,double bwS) {
        if (idS <= 0) throw new RuntimeException("Number of vertices must be nonnegative");
        this.idS = idS;
        this.bwS = bwS;
    }
  
    
    public Demand(int Id, int noV, double currentTime, Function[] f,int MinServer,int MaxServer){
  	   this.idS= Id;
  	   this.source = UtilizeFunction.randInt(MinServer, MaxServer);
  	   while (true)
         {
         	int desTemp= UtilizeFunction.randInt(MinServer, MaxServer);
         	if(desTemp !=source)
         	{
         		this.destination = desTemp;
         		break;
         	}
         }
  	   Integer[] intArray = new Integer[] { 50,100,150,200 };
         this.bwS = UtilizeFunction.randomDouble(intArray);
  	   this.arrivalTime = currentTime;
  	   this.noF = UtilizeFunction.randInt(3, f.length);
  	   this.rate = UtilizeFunction.randDouble(0.0,5.0);
  	 this.arrF = new Function[this.noF];
 	int[] temp = new int[noF];
 	for (int i=0;i<this.noF;i++)
 	{
 		temp[i]=-1;
 	}
 	boolean flag=false;
 	for (int i=0;i<this.noF;i++)
 	{
 		flag=false;
 		while (!flag)
 		{
 			int idF= UtilizeFunction.randInt(0, f.length-1);
 			if(i>0)
 			{
 				for (int k=0;k<i;k++)
 					if(idF == temp[k])
 					{
 						flag =true;
 						break;
 					}
 			}
 			if(!flag)
 			temp[i]=idF;
 			flag = !flag;        		
 		}
 	
 	}
 	for (int i=0;i<this.noF;i++)
 	{
 		this.arrF[i]= f[temp[i]];// tạo một mảng các function cho từng service
 	
 	}
     }
    
    
public Demand(double bw_max,int idS, Function[] f,int noVirtualNode,double currentTime) {  //old  	
		
        this.idS=idS;
        this.source = UtilizeFunction.randInt(1, noVirtualNode);
        this.destination = UtilizeFunction.randInt(1, noVirtualNode);
        this.arrivalTime = currentTime;//gan thoi gian den cua demand
        this.noF = UtilizeFunction.randInt(1, f.length);
        
        	// random bandwidth for service
        	int mul= (int) (bw_max/8);
        	Integer[] intArray = new Integer[] { (int)(bw_max-7*mul), (int)(bw_max-6*mul),(int)(bw_max-5* mul),(int)(bw_max- 4*mul),(int)(bw_max- 3*mul) };
        	this.bwS = UtilizeFunction.randomDouble(intArray);
        	
        	this.arrF = new Function[this.noF];
        	int[] temp = new int[noF];
        	for (int i=0;i<this.noF;i++)
        	{
        		temp[i]=-1;
        	}
        	boolean flag=false;
        	for (int i=0;i<this.noF;i++)
        	{
        		flag=false;
        		while (!flag)
        		{
        			int idF= UtilizeFunction.randInt(0, f.length-1);
        			if(i>0)
        			{
        				for (int k=0;k<i;k++)
        					if(idF == temp[k])
        					{
        						flag =true;
        						break;
        					}
        			}
        			if(!flag)
        			temp[i]=idF;
        			flag = !flag;        		
        		}
        	
        	}
        	for (int i=0;i<this.noF;i++)
        	{
        		this.arrF[i]= f[temp[i]];// tạo một mảng các function cho từng service
        	
        	}
}
 
public Demand(double bw_max,int idS, Function[] f,int noVirtualNode,double currentTime,double _rate) {  //edit  	
	
    this.idS=idS;
    this.source = UtilizeFunction.randInt(1, noVirtualNode);
    this.destination = UtilizeFunction.randInt(1, noVirtualNode);
    this.arrivalTime = currentTime;//gan thoi gian den cua demand
    this.noF = UtilizeFunction.randInt(1, f.length);
    rate=_rate;
    	// random bandwidth for service
    	int mul= (int) (bw_max/8);
    	Integer[] intArray = new Integer[] { (int)(bw_max-7*mul), (int)(bw_max-6*mul),(int)(bw_max-5* mul),(int)(bw_max- 4*mul),(int)(bw_max- 3*mul) };
    	this.bwS = UtilizeFunction.randomDouble(intArray);
    	
    	this.arrF = new Function[this.noF];
    	int[] temp = new int[noF];
    	for (int i=0;i<this.noF;i++)
    	{
    		temp[i]=-1;
    	}
    	boolean flag=false;
    	for (int i=0;i<this.noF;i++)
    	{
    		flag=false;
    		while (!flag)
    		{
    			int idF= UtilizeFunction.randInt(0, f.length-1);
    			if(i>0)
    			{
    				for (int k=0;k<i;k++)
    					if(idF == temp[k])
    					{
    						flag =true;
    						break;
    					}
    			}
    			if(!flag)
    			temp[i]=idF;
    			flag = !flag;        		
    		}
    	
    	}
    	for (int i=0;i<this.noF;i++)
    	{
    		this.arrF[i]= f[temp[i]];// tạo một mảng các function cho từng service
    	
    	}
}

public Demand(int idS,int Source, int Destination, double ArrivalTime, Function[] f,double bwS,int noVirtualNode) {
	//old
	// random bandwidth for service
    this(idS);
    if(bwS < 0)
    {
    	this.noF = UtilizeFunction.randInt(1, f.length);
    	this.source = UtilizeFunction.randInt(1, noVirtualNode);
        this.destination = UtilizeFunction.randInt(1, noVirtualNode);
        this.arrivalTime = ArrivalTime;//gan thoi gian den cua demand
    	Integer[] intArray = new Integer[] { 50,100,150,200,400,500,600 };
    	this.bwS = UtilizeFunction.randomDouble(intArray);
    	
    	this.arrF = new Function[this.noF];
    	int[] temp = new int[noF];
    	for (int i=0;i<this.noF;i++)
    	{
    		temp[i]=-1;
    	}
    	boolean flag=false;
    	for (int i=0;i<this.noF;i++)
    	{
    		flag=false;
    		while (!flag)
    		{
    			int idF= UtilizeFunction.randInt(0, f.length-1);
    			if(i>0)
    			{
    				for (int k=0;k<i;k++)
    					if(idF == temp[k])
    					{
    						flag =true;
    						break;
    					}
    			}
    			if(!flag)
    			temp[i]=idF;
    			flag = !flag;        		
    		}
    	
    	}
    	for (int i=0;i<this.noF;i++)
    	{
    		this.arrF[i]= f[temp[i]];// tạo một mảng các function cho từng service
    	
    	}
    }
    else
    {
    	//truong hop khong random
    	this.noF = f.length;
    	this.source=Source;
    	this.destination = Destination;
    	this.arrivalTime = ArrivalTime;
    	this.bwS= bwS;
    	this.arrF = new Function[this.noF];
    	for (int i=0;i<this.noF;i++)
    	{
    		this.arrF[i]= f[i];// tạo một mảng các function cho từng service        	
    	}
    	
    }
    
}

public static boolean checkConnect (int src,int dest, MyGraph _g,double maxBw)
{
	SimpleDirectedWeightedGraph<String, DefaultWeightedEdge>  g_i = new SimpleDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class); 
    
	for (int j=0;j<_g.V();j++)
    {
    	g_i.addVertex("node"+(j+1));
    }      
    for (int j=0;j<_g.V();j++)
    {	        	
    	for(int k=0;k<_g.V();k++)
    	{
    		if(j!=k&&_g.getEdgeWeight(j+1, k+1)>maxBw)
    		{
    			DefaultWeightedEdge e=g_i.addEdge("node"+(j+1),"node"+(k+1));	        			
        		g_i.setEdgeWeight(e, _g.getEdgeWeight((j+1), (k+1)));
    		}
    	}
    }       
    List<DefaultWeightedEdge> _p =   DijkstraShortestPath.findPathBetween(g_i, "node"+src, "node"+dest);
	if(_p!=null && _p.size()>0)
	{
		return true;
	}
	else
	{
		return false;
	}
	}

public Demand(int idS,int Source, int Destination,double bwS, double ArrivalTime,double _rate, Function[] f,int noVirtualNode, MyGraph _g) {
    //edit	
	// random bandwidth for service
        this(idS);
        if(bwS < 0)
        {
        	this.noF = UtilizeFunction.randInt(2, f.length);
        	//this.noF =5;
        	while (true)
        	{
        		this.source = UtilizeFunction.randInt(1, noVirtualNode);
            	this.destination = UtilizeFunction.randInt(1, noVirtualNode);
            	if ((this.source != this.destination) && checkConnect(this.source, this.destination, _g,0.000001))
            	{
            		break;
            	}
        	}
        	
        	
            this.rate = UtilizeFunction.randDouble(5);
            this.arrivalTime = ArrivalTime;//gan thoi gian den cua demand
        	Integer[] intArray = new Integer[] { 50,100 };
        	this.bwS = UtilizeFunction.randomDouble(intArray);
        	
        	this.arrF = new Function[this.noF];
        	int[] temp = new int[noF];
        	for (int i=0;i<this.noF;i++)
        	{
        		temp[i]=-1;
        	}
        	boolean flag=false;
        	for (int i=0;i<this.noF;i++)
        	{
        		flag=false;
        		while (!flag)
        		{
        			int idF= UtilizeFunction.randInt(0, f.length-1);
        			if(i>0)
        			{
        				for (int k=0;k<i;k++)
        					if(idF == temp[k])
        					{
        						flag =true;
        						break;
        					}
        			}
        			if(!flag)
        			temp[i]=idF;
        			flag = !flag;        		
        		}
        	
        	}
        	for (int i=0;i<this.noF;i++)
        	{
        		this.arrF[i]= f[temp[i]];// tạo một mảng các function cho từng service
        	
        	}
        }
        else
        {
        	//truong hop khong random
        	this.noF = f.length;
        	this.source=Source;
        	this.rate = _rate;
        	this.destination = Destination;
        	this.arrivalTime = ArrivalTime;
        	this.bwS= bwS;
        	this.arrF = new Function[this.noF];
        	for (int i=0;i<this.noF;i++)
        	{
        		this.arrF[i]= f[i];// tạo một mảng các function cho từng service        	
        	}
        	
        }
        
    }


	public void set_bwS(double _bwS)
	{
		this.bwS = _bwS;
	}
    // id of Service
	
    public int idS() { return idS; }
    // return Source
    public int sourceS() { return source; }
 // return Destination
    public int destinationS() { return destination; }
 // return arrival Time
    public double arrivalTimeS() { return arrivalTime; }
    // number of functions in service
    public int noF() { return noF; }
    // return bandwidth of service;
    public double bwS() { return this.bwS; }
    //return array of Function in service;
    public Function[] getFunctions() {return this.arrF;}
    public double getRate(){return rate;}
    /**If id cua function tu 1->m*/
    public int getOrderFunction(int id)    
    {
    	int temp =0;
    	if (id ==0)
    		return 0;
    	for (int x= 0; x<this.arrF.length; x++)
    	{
    		if (arrF[x].id()==id)
    		{
    			temp=x+1;
    			break;
    			
    		}
    	}
    	return temp;
    }

    // string representation of Graph - takes quadratic time
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(idS + ": "+source+": "+destination+": " + bwS +": "+arrivalTime+": "+noF+ " : ");
        for (int v = 0; v < noF; v++) {
            s.append(arrF[v].id() +"    ");
        }
        return s.toString();
    }
    // test client
    public static void main(String[] args) {
    	
    	
    	
    	int numberFunction = Integer.parseInt(args[0]);
        Function[] functions = new Function[numberFunction];
        for (int i=0;i< numberFunction;i++)
        	functions[i]= new Function(i+1);        
        int numberService = Integer.parseInt(args[1]);
        Demand[] services = new Demand[numberService];
    	for (int i=0;i<numberService;i++)
    	{
    		services[i]= new Demand(i+1,-1,-1,5,functions,-1,7);
    	}
    	
    	System.out.println("Function random:::" + numberFunction);
    	for (int i=0;i<numberFunction;i++)
    	{
    		
            System.out.println(functions[i].toString());
    	}
    	
    	
    	System.out.println("Service random:::" + numberService);
    	for (int i=0;i<numberService;i++)
    	{    		
            System.out.println(services[i].toString());
    	}
    }

}