import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.jgrapht.VertexFactory;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.WeightedPseudograph;

import javax.swing.SwingUtilities;


//import org.jgraph.JGraph;
import org.jgrapht.DirectedGraph;
import org.jgrapht.GraphPath;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.VertexFactory;
import org.jgrapht.alg.*;
import org.jgrapht.graph.*;
//import org.jgrapht.Graph;
import org.jgrapht.generate.*;
import org.jgrapht.traverse.*;
import org.omg.DynamicAny._DynEnumStub;

public class MyGraph {
    private int V;
    private int E;
    double[][] link_bandwidth;//
    double[] K;
    double[] r;
    Vector<Vector<Double>> cap;
    Vector<Double> pricePernode;
    private double price_bandwidth;
    private boolean[] node;
    private boolean[][] link;
    
    //khoi tao random graph
    
    //chu y doc du lieu thi pai doc de tao mang cua K, r là V+1;
    
    public MyGraph(int NoVertex,double p, Vector<Vector<Double>> dataReal)
    {
//        DirectedGraph<Object, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
//        ScaleFreeGraphGenerator<Object, DefaultEdge> generator = new ScaleFreeGraphGenerator<>(NoVertex);
//        generator.generateGraph(graph, vertexFactory, null);
 

         
    	//UndirectedGraph<Object, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
    	//RandomGraphGenerator<Object, DefaultEdge> generator= new RandomGraphGenerator<>(NoVertex, NoEdge);
    	//generator.generateGraph(graph, vertexFactory, null);
       
    	final int seed = 33;
        final double edgeProbability = p;
        final int numberVertices = NoVertex;

        GraphGenerator<Integer, DefaultWeightedEdge, Integer> gg =
                new GnpRandomGraphGenerator<Integer, DefaultWeightedEdge>(
                    numberVertices, edgeProbability, seed, false);
//        GraphGenerator<Integer, DefaultWeightedEdge, Integer> gg =
//            new GnpRandomGraphGenerator<Integer, DefaultWeightedEdge>(
//                numberVertices, edgeProbability, seed, false);
//        
//        WeightedPseudograph<Integer, DefaultWeightedEdge> graph =
//                new WeightedPseudograph<>(DefaultWeightedEdge.class);
        
        DefaultDirectedGraph<Integer, DefaultWeightedEdge> graph =
                new DefaultDirectedGraph<>(DefaultWeightedEdge.class);
        VertexFactory<Integer> vertexFactoryInteger= new VertexFactory<Integer>() {
        	 private int i=0;

 	        @Override
 	        public Integer createVertex()
 	        {
 	            return ++i;
 	        }
		};
            gg.generateGraph(graph, vertexFactoryInteger, null);
    	
        //set cap and bandwidth cho do thi g
            
            this.V = NoVertex;
            this.E = 0;
            this.link_bandwidth = new double[NoVertex+1][NoVertex+1];
            this.K = new double[NoVertex+1];
            this.r = new double[NoVertex+1];
            this.node = new boolean[NoVertex+1];
            this.link = new boolean[NoVertex+1][NoVertex+1];
            this.cap = new Vector<Vector<Double>>(NoVertex+1);
            this.pricePernode = new Vector<Double>(NoVertex+1);
           for(int i=0;i<NoVertex+1;i++)
           	for (int j=0;j<NoVertex+1;j++)
           	{
           		link[i][j]=false;
           		link_bandwidth[i][j]=0.0;
           	} 
           int noE = 0;
           for (DefaultWeightedEdge edges : graph.edgeSet()) {
           	
           	int s = Integer.parseInt(graph.getEdgeSource(edges).toString());
           	int t = Integer.parseInt(graph.getEdgeTarget(edges).toString());
   			//System.out.println("Dinh: "+ s+ "..." + t+ "..."+w);
   			//double w= UtilizeFunction.randomDouble(new Integer[] {5000,6000,7000,8000,9000,10000});
   			if(s!=t)
   			{
   				double b= UtilizeFunction.randomDouble(new Integer[] {500,600,1200});
   				addEdge(s, t,b);
   				noE++;
   			}
   			else
   				System.out.println("Loop");
   		}
           
           this.price_bandwidth = 0.44;
           for (int i=0;i<NoVertex+1;i++)
           {
         	  int index = UtilizeFunction.randInt(0, dataReal.size()-1);
         	  Vector<Double> data = dataReal.get(index);
         	  Vector<Double> t= new Vector<>(3);
         	  for(int j=0;j<3;j++)
         		  t.add(data.get(j));
         	  cap.addElement(t);
         	  pricePernode.add(data.get(3));
         	  if(UtilizeFunction.isPositive(cap.get(i)))
         		  this.node[i]=true;
           }    
	   	E = noE;
        
        
    }
    public MyGraph(int V, int E, boolean h) {//old
   	 if (V < 0) throw new RuntimeException("Number of vertices must be nonnegative");
        this.V = V;
        this.E = 0;
        this.link_bandwidth = new double[V+1][V+1];
        this.K = new double[V+1];
        this.r = new double[V+1];
        this.node = new boolean[V+1];
        this.link = new boolean[V+1][V+1];
       if (E <= 0) throw new RuntimeException("Number of edges must be nonnegative");
       if (E > V*(V-1)/2) throw new RuntimeException("Too many edges");
       // can be inefficient
       for(int i=0;i<V;i++)
       	for (int j=0;j<V;j++)
       	{
       		link[i][j]=false;
       		link_bandwidth[i][j]=0.0;
       	}        
       while (this.E != E) {
       	int v = UtilizeFunction.randInt(1, V);
           int w = UtilizeFunction.randInt(1, V);            
           if (v!=w)
           {  	//random link_cost and link_bandwidth
       		double b= UtilizeFunction.randomDouble(new Integer[] {430, 510,600,730,800,860, 852,900,1000,1600,2000 });
           	addEdge(v, w,b);
           }
       }
       for (int i=0;i<V;i++)
       {
       	this.node[i+1] = true;
       }
       this.price_bandwidth = 25 * Math.random()+2;
       
   }
    public MyGraph (int type, int k,int n0,Vector<Vector<Double>> dataReal)//Create FatTree =0, VL2=1, Bcube=2, k is number of servers
    {
       
       	
    	if (type==0)//Tao FatTree 128 server => k = 8
    	{
    		//Link
    		//Double[] zero ={0.0,0.0,0.0};
			//cap.add(new Vector<Double>(Arrays.asList(zero)));
			//pricePerNode.add(0.0);new Vector<Double>(Arrays.asList(0.0,0.0,0.0))
    		
    		int V = (k*k*k+5*k*k)/4;
    		this.V=V;
    	        this.link_bandwidth = new double[V+1][V+1];
    	           this.K = new double[V+1];
    	           this.r = new double[V+1];
    	           this.node = new boolean[V+1];
    	           this.link = new boolean[V+1][V+1];
    	           this.cap = new Vector<Vector<Double>>(V+1);
    	           this.pricePernode = new Vector<Double>(V+1);
    	        //this.Nprice = new Vector<Vector<Double>>(V+1);

    	       // can be inefficient
    	       for(int i=0;i<V;i++)
    	       {
    	    	   for (int j=0;j<V;j++)
    	          	{
    	          		link[i][j]=false;
    	          		link_bandwidth[i][j]=0.0;
    	          	}  
    	       }
    		for (int i=0;i<V;i++)
            {
            	for (int j=0;j<V;j++)
            	{           
                    if (i!=j)//random link_cost and link_bandwidth
                    	addEdge(i+1, j+1,0.0);
            	}
            }
    		for (int h=0;h<k/2;h++)// core vs aggregation
    		{
    			for (int i=h*k/2;i<(h+1)*k/2;i++)
        		{
        			for (int j=0;j<k;j++)
        			{
        				int i_temp= k*k/4 +j*k/2+h*(k/2-1);
        				addEdge(i+1, i_temp+1, 1000.0);			
        			}
        		}
    		}
    		
    		for (int h=0;h<k;h++)// aggregation vs edge
    		{
    			for (int i=0;i<k/2;i++)
        		{
    				for (int j=0;j<k/2;j++)
    				{
    					int i1= h*k/2+ k*k/4+i*(k/2-1);
        				int i2= h*k/2 + 3*k*k/4 + j*(k/2-1);
        				addEdge(i1+1, i2+1, 500.0);		
    				}
    				
        		}
    		}
    		for (int h=0;h<k;h++)//  edge vs server
    		{
    			for (int j=0;j<k/2;j++)
    			{
    				for (int i=0;i<k/2;i++)
            		{
        				int i1= h*k/2+ 3*k*k/4+j*(k/2-1);
        				int i2= h*k + 5*k*k/4 + i*(k/2-1)+j*k/2;
        				addEdge(i1+1, i2+1, 500.0);		
            		}
    			}
    			
    		}
    		
    		//Node
    		this.price_bandwidth = 0.44;
            for (int i=0;i<V+1;i++)
            {
          	  int index = UtilizeFunction.randInt(0, dataReal.size()-1);
          	  Vector<Double> data = dataReal.get(index);
          	  Vector<Double> t= new Vector<>(3);
          	  for(int j=0;j<3;j++)
          		  t.add(data.get(j));
          	  this.cap.addElement(t);
          	  this.pricePernode.add(data.get(3));
          	  if(UtilizeFunction.isPositive(cap.get(i)))
            		  this.node[i] =true;
            	  else
            		  this.node[i] =false;
            }      
            
    	}
    	
    	if (type==1)//Tao VL2 128 server => k = 8
    	{
    		int V = (k*k*k+5*k*k)/4;
    		this.V=V;
    		 this.link_bandwidth = new double[V+1][V+1];
	           this.K = new double[V+1];
	           this.r = new double[V+1];
	           this.node = new boolean[V+1];
	           this.link = new boolean[V+1][V+1];
	           this.cap = new Vector<Vector<Double>>(V+1);
	           this.pricePernode = new Vector<Double>(V+1);
	        //this.Nprice = new Vector<Vector<Double>>(V+1);

	       // can be inefficient
	       for(int i=0;i<V;i++)
	       {
	    	   for (int j=0;j<V;j++)
	          	{
	          		link[i][j]=false;
	          		link_bandwidth[i][j]=0.0;
	          	}  
	       }
    		for (int i=0;i<V;i++)
            {
            	for (int j=0;j<V;j++)
            	{           
                    if (i!=j)//random link_cost and link_bandwidth
                    	addEdge(i+1, j+1,0.0);
            	}
            }
    		for (int h=0;h<k/2;h++)// core vs aggregation
    		{
    			for (int i=h*k/2;i<(h+1)*k/2;i++)
        		{
        			for (int j=0;j<k;j++)
        			{
        				int i_temp= k*k/4 +j*k/2+h*(k/2-1);
        				addEdge(i+1, i_temp+1, 50000.0);				
        			}
        		}
    		}
    		
    		for (int h=0;h<k;h++)// aggregation vs edge
    		{
    			for (int i=0;i<k/2;i++)
        		{
    				for (int j=0;j<k/2;j++)
    				{
    					int i1= h*k/2+ k*k/4+i*(k/2-1);
        				int i2= h*k/2 + 3*k*k/4 + j*(k/2-1);
        				addEdge(i1+1, i2+1, 50000.0);		
    				}	
        		}
    		}
    		for (int h=0;h<k;h++)//  edge vs server
    		{
    			for (int j=0;j<k/2;j++)
    			{
    				for (int i=0;i<k/2;i++)
            		{
        				int i1= h*k/2+ 3*k*k/4+j*(k/2-1);
        				int i2= h*k/2 + 5*k*k/4 + i*(k/2-1)+j*k/2;
        				addEdge(i1+1, i2+1, 5000.0);
            		}
    			}
    			
    		}
    		
    		//Node
    		this.price_bandwidth = 0.44;
            for (int i=0;i<V+1;i++)
            {
          	  int index = UtilizeFunction.randInt(0, dataReal.size()-1);
          	  Vector<Double> data = dataReal.get(index);
          	  Vector<Double> t= new Vector<>(3);
          	  for(int j=0;j<3;j++)
          		  t.add(data.get(j));
          	  this.cap.addElement(t);
          	  this.pricePernode.add(data.get(3));
          	if(UtilizeFunction.isPositive(cap.get(i)))
      		  this.node[i] =true;
      	  else
      		  this.node[i] =false;
            }      
    		
    	}
    	if (type==2)//Tao Bcube 128 server => k = 8
    	{
    		int V = (int)Math.pow(n0, k+1)+ (int)Math.pow(n0, k)*(k+1);
    		this.V=V;
    		 this.link_bandwidth = new double[V+1][V+1];
	           this.K = new double[V+1];
	           this.r = new double[V+1];
	           this.node = new boolean[V+1];
	           this.link = new boolean[V+1][V+1];
	           this.cap = new Vector<Vector<Double>>(V+1);
	           this.pricePernode = new Vector<Double>(V+1);
	        //this.Nprice = new Vector<Vector<Double>>(V+1);

	       // can be inefficient
	       for(int i=0;i<V;i++)
	       {
	    	   for (int j=0;j<V;j++)
	          	{
	          		link[i][j]=false;
	          		link_bandwidth[i][j]=0.0;
	          	}  
	       }
    		for (int i=0;i<V;i++)
            {
            	for (int j=0;j<V;j++)
            	{           
                    if (i!=j)//random link_cost and link_bandwidth
                    	addEdge(i+1, j+1,0.0);
            	}
            }
    		int i1=(int)Math.pow(n0, k+1);
    		for (int j=0;j<(int)Math.pow(n0, k);j++)// tach ra voi truong hop 1 cach nhau lien tiep n lan
    		{
    			for (int h=0;h<n0;h++)
    			{
    				int i2= j*n0+h;
    				addEdge(i1+1, i2+1, 5000.0);          	    	
    			}
    			i1++;
    			
    		}
    		int i2=0;
    		for (int i=1;i<k+1;i++)//cac truong hop con lai. xet voi so rong la h*n^i, va lien tiep nhau j, co phep nhay n0*(buoc nhay)
    		{
    			for (int j=0;j<(int)Math.pow(n0, k);j++)
        		{
    				
        			for (int h=0;h<n0;h++)
        			{
        				i2= h*(int)Math.pow(n0, i)+j+ n0*(j/(int)Math.pow(n0, i));
        				addEdge(i1+1, i2+1, 50000.0);           	    	
        			}
        			i1++;
        			
        		}
    		}    		
    		
    		//Node
    		this.price_bandwidth = 0.44;
            for (int i=0;i<V+1;i++)
            {
          	  int index = UtilizeFunction.randInt(0, dataReal.size()-1);
          	  Vector<Double> data = dataReal.get(index);
          	  Vector<Double> t= new Vector<>(3);
          	  for(int j=0;j<3;j++)
          		  t.add(data.get(j));
          	  this.cap.addElement(t);
          	  this.pricePernode.add(data.get(3));
          	if(UtilizeFunction.isPositive(cap.get(i)))
        		  this.node[i] =true;
        	  else
        		  this.node[i] =false;
            }      
    		
    	}
    	
    }
    public MyGraph(int V, int E, boolean h,Vector<Integer> hso_cap,Vector<Integer> hso_r) {//edit model
    	 if (V < 0) throw new RuntimeException("Number of vertices must be nonnegative");
         this.V = V;
         this.E = 0;
         this.link_bandwidth = new double[V+1][V+1];
         this.K = new double[V+1];
         this.r = new double[V+1];
         this.node = new boolean[V+1];
         this.link = new boolean[V+1][V+1];
         this.cap = new Vector<Vector<Double>>(V+1);
         //this.Nprice = new Vector<Vector<Double>>(V+1);
        if (E <= 0) throw new RuntimeException("Number of edges must be nonnegative");
        if (E > V*(V-1)/2) throw new RuntimeException("Too many edges");
        // can be inefficient
        for(int i=0;i<V;i++)
        	for (int j=0;j<V;j++)
        	{
        		link[i][j]=false;
        		link_bandwidth[i][j]=0.0;
        	}        
        while (this.E != E) {
        	int v = UtilizeFunction.randInt(1, V);
            int w = UtilizeFunction.randInt(1, V);            
            if (v!=w)
            {  	//random link_cost and link_bandwidth
        		double b= UtilizeFunction.randomDouble(new Integer[] {430, 510,600,730,800,860, 852,900,1000,1600,2000 });
            	addEdge(v, w,b);
            }
        }
        this.price_bandwidth = 25 * Math.random()+2;
        for (int i=0;i<V+1;i++)
        {
        	Vector<Double> t= new Vector<>(3);
	        for (int j=0;j<3;j++)
	        {
	        	double temp= UtilizeFunction.randDouble(hso_cap.get(j));
	        	t.addElement(temp);
	        } 
	        cap.addElement(t);
	        t= new Vector<>(3);
	        for (int j=0;j<3;j++)
	        {
	        	double temp= UtilizeFunction.randDouble(hso_r.get(j));
	        	t.addElement(temp);
	        } 
	        pricePernode.addElement(UtilizeFunction.randDouble(200));
	        //Nprice.addElement(t);
	        if(UtilizeFunction.isPositive(cap.get(i)))
      		  this.node[i]=true;
        }
        
    }
    public MyGraph(int V, int E, double r) {//old
   	 if (V < 0) throw new RuntimeException("Number of vertices must be nonnegative");
        this.V = V;
        this.E = 0;
        this.link_bandwidth = new double[V+1][V+1];
        this.K = new double[V+1];
        this.r = new double[V+1];
        this.node = new boolean[V+1];
        this.link = new boolean[V+1][V+1];
       if (E <= 0) throw new RuntimeException("Number of edges must be nonnegative");
       if (E > V*(V-1)/2) throw new RuntimeException("Too many edges");
       // can be inefficient
       for(int i=0;i<V;i++)
       	for (int j=0;j<V;j++)
       	{
       		link[i][j]=false;
       		link_bandwidth[i][j]=0.0;
       	}        
       while (this.E != E) {
       	int v = UtilizeFunction.randInt(1, V);
           int w = UtilizeFunction.randInt(1, V);            
           if (v!=w)
           {  	//random link_cost and link_bandwidth
       		double b= UtilizeFunction.randomDouble(new Integer[] {430, 510,600,730,800,860, 852,900,1000,1600,2000 });
           	addEdge(v, w,b);
           }
       }
       for (int i=0;i<V;i++)
       {
       	this.K[i+1]= r * Math.random()+2;
       	this.r[i+1]= 10 * Math.random()+3;
       	this.node[i+1] = true;
       }
       this.price_bandwidth = 25 * Math.random()+2;
       
   }
    
    public MyGraph(int V, int E,Vector<Vector<Double>> dataReal) {//edit model
    	// truyen vao so dinh, so canh, vector<vecto<Double>> the hien ((cpu,storage,ram, price))
    	//random ngau nhien mot so nguyen 1, vector.size -> chonj duoc node cho canh do
      	 if (V < 0) throw new RuntimeException("Number of vertices must be nonnegative");
           this.V = V;
           this.E = 0;
           this.link_bandwidth = new double[V+1][V+1];
           this.K = new double[V+1];
           this.r = new double[V+1];
           this.node = new boolean[V+1];
           this.link = new boolean[V+1][V+1];
           this.cap = new Vector<Vector<Double>>(V+1);
           this.pricePernode = new Vector<Double>(V+1);
          if (E <= 0) throw new RuntimeException("Number of edges must be nonnegative");
          if (E > V*(V-1)/2) throw new RuntimeException("Too many edges");
          // can be inefficient
          for(int i=0;i<V;i++)
          	for (int j=0;j<V;j++)
          	{
          		link[i][j]=false;
          		link_bandwidth[i][j]=0.0;
          	}        
          while (this.E != E) {
          	int v = UtilizeFunction.randInt(1, V);
              int w = UtilizeFunction.randInt(1, V);            
              if (v!=w)
              {  	//random link_cost and link_bandwidth
          		double b= UtilizeFunction.randomDouble(new Integer[] {1200,1600,2000,3000});
              	addEdge(v, w,b);
              }
          }
          this.price_bandwidth = 0.44;
          for (int i=0;i<V+1;i++)
          {
        	  int index = UtilizeFunction.randInt(0, dataReal.size()-1);
        	  Vector<Double> data = dataReal.get(index);
        	  Vector<Double> t= new Vector<>(3);
        	  for(int j=0;j<3;j++)
        		  t.add(data.get(j));
        	  cap.addElement(t);
        	  pricePernode.add(data.get(3));
        	  if(UtilizeFunction.isPositive(cap.get(i)))
        		  this.node[i]=true;
          }         
          
      }
    public MyGraph(int V,Vector<Vector<Double>> dataReal) {
      	 if (V < 0) throw new RuntimeException("Number of vertices must be nonnegative");
           this.V = V;
           this.link_bandwidth = new double[V+1][V+1];
           this.K = new double[V+1];
           this.r = new double[V+1];
           this.node = new boolean[V+1];
           this.link = new boolean[V+1][V+1];
           this.cap = new Vector<Vector<Double>>(V+1);
           this.pricePernode = new Vector<Double>(V+1);
          // can be inefficient
          for(int i=0;i<V;i++)
          	for (int j=0;j<V;j++)
          	{
          		link[i][j]=false;
          		link_bandwidth[i][j]=0.0;
          	}  
          for (int i=0;i<4;i++)
        	  for (int j=i+1;j<5;j++)
        	  {
        		  //core node
        		  double b= UtilizeFunction.randomDouble(new Integer[] {430, 510,600,730,800,860, 852,900,1000,1600,2000 });
                	addEdge(i+1, j+1,b);
        	  }
          for(int i=0;i<10;i++)
          {
        	  //aggregation node
        	  for (int j=0;j<5;j++)
        	  {
        		  double b= UtilizeFunction.randomDouble(new Integer[] {0,430, 510,600,730,800,860, 852,900,1000,1600,2000 });
              		addEdge(i+6, j+1,b);
              		
        	  }
//        	  for (int j=15;j<30;j++)
//        	  {
//        		  double b= UtilizeFunction.randomDouble(new Integer[] {0,430, 510,600,730,800,860, 852,900,1000,1600,2000 });
//              		addEdge(i+6, j+1,b);
//              		
//        	  }
          }
          for(int i=0;i<15;i++)
          {
        	  //aggregation node
        	  for (int j=0;j<10;j++)
        	  {
        		  double b= UtilizeFunction.randomDouble(new Integer[] {0,430, 510,600,730,800,860, 852,900,1000,1600,2000 });
              		addEdge(i+16, j+6,b);
              		
        	  }
          }
          this.price_bandwidth = 0.44;
          for (int i=0;i<V+1;i++)
          {
        	  int index = UtilizeFunction.randInt(0, dataReal.size()-1);
        	  Vector<Double> data = dataReal.get(index);
        	  Vector<Double> t= new Vector<>(3);
        	  for(int j=0;j<3;j++)
        		  t.add(data.get(j));
        	  cap.addElement(t);
        	  pricePernode.add(data.get(3));
        	  if(UtilizeFunction.isPositive(cap.get(i)))
        		  this.node[i]=true;
          }         
          
      }
    public MyGraph(double[] r, double[] K,double[][] w,double price_bw, boolean h) {//old
   	 if (r.length < 0) throw new RuntimeException("Number of vertices must be nonnegative");
        this.V = r.length-1;
        this.E = 0;
        this.link_bandwidth = new double[V+1][V+1];
        this.K = new double[V+1];
        this.r = new double[V+1];
        this.node = new boolean[V+1];
        this.link = new boolean[V+1][V+1];
       // can be inefficient
       
       for (int i=0;i<w.length-1;i++)
       	for (int j=i+1;j<w[0].length;j++)
       	{
       		addEdge(i,j,w[i][j]);
       	}
       for (int i=0;i<V+1;i++)
       {
       	this.K[i]= K[i];
       	this.r[i]= r[i];
       	if(K[i]>0)
       		this.node[i] = true;
       }
       this.price_bandwidth = price_bw;
       
   }
    
//    public Graph(Vector<Vector<Double>> K,Vector<Vector<Double>> r, double[][] w,double price_bw, boolean h) {//edit model
//      	 if (r.size() < 0) throw new RuntimeException("Number of vertices must be nonnegative");
//           this.V = r.size()-1;
//           this.E = 0;
//           this.link_bandwidth = new double[V+1][V+1];
//           this.K = new double[V+1];
//           this.r = new double[V+1];
//           this.cap = new Vector<Vector<Double>>(V+1);
//           this.Nprice = new Vector<Vector<Double>>(V+1);
//           this.node = new boolean[V+1];
//           this.link = new boolean[V+1][V+1];
//          // can be inefficient
//          
//          for (int i=0;i<w.length-1;i++)
//          	for (int j=i+1;j<w[0].length;j++)
//          	{
//          		addEdge(i,j,w[i][j]);
//          	}
//          for (int i=0;i<V+1;i++)
//          {
//        	  cap.addElement(K.get(i));
//        	  Nprice.addElement(r.get(i));
//        	  if(UtilizeFunction.isPositive(cap.get(i)))
//        		  this.node[i]=true;
//          }
//          this.price_bandwidth = price_bw;
//          
//      }
    public MyGraph(Vector<Vector<Double>> K,Vector<Double> r, double[][] w,double price_bw, boolean h) {//model newest
     	 if (r.size() < 0) throw new RuntimeException("Number of vertices must be nonnegative");
          this.V = r.size()-1;
          this.E = 0;
          this.link_bandwidth = new double[V+1][V+1];
          this.K = new double[V+1];
          this.r = new double[V+1];
          this.cap = new Vector<Vector<Double>>(V+1);
          this.pricePernode = new Vector<Double>(V+1);
          //this.Nprice = new Vector<Vector<Double>>(V+1);
          this.node = new boolean[V+1];
          this.link = new boolean[V+1][V+1];
         // can be inefficient
         
         for (int i=0;i<w.length-1;i++)
         	for (int j=i+1;j<w[0].length;j++)
         	{
         		addEdge(i,j,w[i][j]);
         	}
         for (int i=0;i<V+1;i++)
         {
       	  cap.addElement(K.get(i));
       	  pricePernode.add(r.get(i));
       	  if(UtilizeFunction.isPositive(cap.get(i)))
       		  this.node[i]=true;
         }
         this.price_bandwidth = price_bw;
         
     }
    public MyGraph(double[] r, double[] K,double[][] w,double price_bw) {//old
    	 if (r.length < 0) throw new RuntimeException("Number of vertices must be nonnegative");
         this.V = r.length;
         this.E = 0;
         this.link_bandwidth = new double[V+1][V+1];
         this.K = new double[V+1];
         this.r = new double[V+1];
         this.node = new boolean[V+1];
         this.link = new boolean[V+1][V+1];
        // can be inefficient
        
        for (int i=0;i<w.length;i++)
        	for (int j=0;j<w[0].length;j++)
        	{
        		link[i+1][j+1]=false;
        		addEdge(i+1,j+1,w[i][j]);
        	}
        for (int i=0;i<V;i++)
        {
        	this.K[i+1]= K[i];
        	this.r[i+1]= r[i];
        	this.node[i+1] = true;
        }
        this.price_bandwidth = price_bw;
        
    }
    
    public MyGraph(Vector<Vector<Double>> K,Vector<Double> r, double[][] w,double price_bw) {//edit model su dung doc du lieu vao
    	
    	//r , K, w phai co kich thuoc (n+1) -> tinh ca id 0
   	 if (r.size() < 0) throw new RuntimeException("Number of vertices must be nonnegative");
        this.V = r.size()-1;
        this.link_bandwidth = new double[V+1][V+1];
        this.cap = new Vector<Vector<Double>>(V+1);
        this.pricePernode = new Vector<Double>(V+1);
        this.node = new boolean[V+1];
        this.link = new boolean[V+1][V+1];
       // can be inefficient
       
       for (int i=0;i<w.length;i++)
       	for (int j=0;j<w[0].length;j++)
       	{
       		link[i][j]=false;
       		addEdge(i,j,w[i][j]);
       	}
       for (int i=0;i<V+1;i++)
       {
     	  cap.addElement(K.get(i));
     	  pricePernode.add(r.get(i));
     	  //Nprice.addElement(r.get(i));
     	  if(UtilizeFunction.isPositive(cap.get(i)))
     		  this.node[i]=true;
       }
       this.price_bandwidth = price_bw;
       
   }
//public Graph(Vector<Vector<Double>> K,Vector<Vector<Double>> r, double[][] w,double price_bw) {//edit model su dung doc du lieu vao
//    	
//    	//r , K, w phai co kich thuoc (n+1) -> tinh ca id 0
//   	 if (r.size() < 0) throw new RuntimeException("Number of vertices must be nonnegative");
//        this.V = r.size()-1;
//        this.link_bandwidth = new double[V+1][V+1];
//        this.cap = new Vector<Vector<Double>>(V+1);
//        this.Nprice = new Vector<Vector<Double>>(V+1);
//        this.node = new boolean[V+1];
//        this.link = new boolean[V+1][V+1];
//       // can be inefficient
//       
//       for (int i=0;i<w.length;i++)
//       	for (int j=0;j<w[0].length;j++)
//       	{
//       		link[i][j]=false;
//       		addEdge(i,j,w[i][j]);
//       	}
//       for (int i=0;i<V+1;i++)
//       {
//     	  cap.addElement(K.get(i));
//     	  Nprice.addElement(r.get(i));
//     	  if(UtilizeFunction.isPositive(cap.get(i)))
//     		  this.node[i]=true;
//       }
//       this.price_bandwidth = price_bw;
//       
//   }


    // number of vertices and edges
    public int V() { return V; }
    public int E() { return E; }
    public double getPriceForUnitNode(int v)//old
    {
    	return r[v];
    }
//    public Vector<Double> getPriceNode(int v)//new Mode
//    {
//    	return Nprice.get(v);
//    }
    public double getPriceNode(int v)//new Mode
    {
    	return pricePernode.get(v);
    }
    public double getCapacity(int v)//old
    {
    	return K[v];
    }
    public Vector<Double> getCap(int v)//new model
    {
    	return cap.get(v);
    }
    public boolean setCap(int v, Vector<Double> c)//new model
    {
    	cap.set(v, c);
    	return true;
    }
    public boolean setCapacity(int v, double c)
    {
    	K[v]=c;
    	return true;
    }
    public boolean setEdgeWeight(int v, int u,double c)
    {
    	link_bandwidth[v][u]=c;
    	//link_bandwidth[u][v]=c;
    	//link[v][u]=true;
    	link[v][u]=true;
    	return true;
    }
    public double getEdgeWeight(int u, int v)
    {
    		return link_bandwidth[u][v];
    }
    public double getPriceBandwidth()
    {
    	return price_bandwidth;
    }
    public boolean getExistNode(int u)
    {
    	return node[u];
    } 
    public boolean getExistLink(int u, int v)
    {
    	return link[u][v];
    }
    
    public void addEdge(int v, int w,double b) {
        if (link_bandwidth[v][w]==0)
        	{
        	E=E+1;        	
        	link_bandwidth[v][w] = b;
        	//link_bandwidth[w][v] = b;
        	link[v][w]=true;
        	//link[w][v]=true;
        	}
        	
    }
    public boolean addEdge1(int v, int w,double b) {
        if (link_bandwidth[v][w]==0)
        	{
        	E=E+1;        	
        	link_bandwidth[v][w] = b;
        	//link_bandwidth[w][v] = b;
        	link[v][w]=true;
        	//link[w][v]=true;
        	return true;
        	}
        return false;
        	
    }
    public void removeNode(int u)
    {
    	//danh dau dinh khong duoc xet
    	
    	node[u]=false;
    }
    public void removeLink(int u,int v)
    {
    	//danh dau dinh khong duoc xet
    	
    	link[u][v]=false;
    	//link[v][u]=false;
    }

    // does the graph contain the edge v-w?
    public boolean contains(int v, int w) {
        return (link_bandwidth[v][w]>0);
    }

    // return list of neighbors of v
    public Iterable<Integer> link_bandwidth(int v) {
        return new AdjIterator(v);
    }

    // support iteration over graph vertices
    private class AdjIterator implements Iterator<Integer>, Iterable<Integer> {
        int v, w = 0;
        AdjIterator(int v) { this.v = v; }

        public Iterator<Integer> iterator() { return this; }

        public boolean hasNext() {
            while (w < V) {
                if (link_bandwidth[v][w]>0) return true;
                w++;
            }
            return false;
        }

        public Integer next() {
            if (hasNext()) { return w++;                         }
            else           { throw new NoSuchElementException(); }
        }

        public void remove()  { throw new UnsupportedOperationException();  }
    }


    // string representation of Graph - takes quadratic time
    public String toString() {
        String NEWLINE = System.getProperty("line.separator");
        StringBuilder s = new StringBuilder();
        s.append(V + " " + E + NEWLINE);
        for (int v = 1; v <= V; v++) {
            s.append(v + ": " + K[v] + "||" + r[v]+"||");
            for (int w : link_bandwidth(v)) {
                s.append(w + ";"+ link_bandwidth[v][w]+" ");
            }
            s.append(NEWLINE);
        }
        return s.toString();
    }


    // test client
    public static void main(String[] args) {
        int V = Integer.parseInt(args[0]);
        int E = Integer.parseInt(args[1]);
        MyGraph G = new MyGraph(V,E,true);
        //Graph G = new Graph(V, E);
        System.out.print(G.toString());
        System.out.println ("\n"+ "Minh Ham Minh Ham "+G.link_bandwidth[1][1]);
        System.out.println (G.K[1] + ":::"+G.r[1]);
        //StdOut.println(G);
    }

}