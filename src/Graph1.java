import java.util.Iterator;
import java.util.NoSuchElementException;



public class Graph1 {
    public int V;
    public int E;
    public double[][] link_bandwidth;//duoc tinh nho toa do cua x,y cua dinh v
    public double[] K;
    public double[] r;
    public double[] x;
    public double[] y;
    public double[][] cost;
    public double[][] cap;
    public boolean[] node;
    public boolean[][] link;
    
    //khoi tao random graph
    public Graph1(int V, int E, boolean h) {
    	 if (V < 0) throw new RuntimeException("Number of vertices must be nonnegative");
         this.V = V;
         this.E = 0;
         this.link_bandwidth = new double[V+1][V+1];
         this.K = new double[V+1];
         this.r = new double[V+1];
         this.x = new double[V+1];
         this.y = new double[V+1];
         this.node = new boolean[V+1];
         this.link = new boolean[V+1][V+1];
         this.cost = new double[V+1][V+1];
         this.cap = new double[V+1][V+1];
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
        	this.K[i+1]= 80 * Math.random()+2;
        	this.r[i+1]= 10 * Math.random()+3;
        	this.node[i+1] = true;
        }
        
    }
    public Graph1(double[] _r, double[] _K,double[] _x,double[] _y,double[][] _cap, double[][] _cost) {
   	 if (_r.length < 0) throw new RuntimeException("Number of vertices must be nonnegative");
        this.V = _r.length;
        this.E = 0;
        this.link_bandwidth = new double[V+1][V+1];
        this.K = new double[V+1];
        this.r = new double[V+1];
        this.x = new double[V+1];
        this.y = new double[V+1];
        this.cap = new double[V+1][V+1];
        this.cost = new double[V+1][V+1];
        this.node = new boolean[V+1];
        this.link = new boolean[V+1][V+1];
       // can be inefficient
       
       for (int i=0;i<_cap.length-1;i++)
       	for (int j=i+1;j<_cap[0].length;j++)
       	{
       		double w= (_x[j]-_x[i])*(_x[j]-_x[i]) + (_y[j]-_y[i])*(_y[j]-_y[i]);
       		addEdge(i+1,j+1,Math.sqrt(w));
       	}
       for (int i=0;i<V;i++)
       {
       	this.K[i+1]= _K[i];
       	this.r[i+1]= _r[i];
       	this.x[i+1]=_x[i];
       	this.y[i+1]=_y[i];
       	if(_K[i]>0)
       		this.node[i+1] = true;
       }
       for (int i=0;i<_cap.length;i++)
          	for (int j=0;j<_cap[0].length;j++)
          	{
          		this.cap[i+1][j+1]=_cap[i][j];
          		this.cost[i+1][j+1]=_cost[i][j];
          	}
   }
    
    public Graph1(double[] _r, double[] _K,double[] _x,double[] _y,double[][] _cap, double[][] _cost, double[][] _linkbandwidth) {
    	//truong hop r, k,... cung chieu voi graph
      	 if (_r.length < 0) throw new RuntimeException("Number of vertices must be nonnegative");
           this.V = _r.length;
           this.link_bandwidth = new double[V][V];
           this.K = new double[V];
           this.r = new double[V];
           this.x = new double[V];
           this.y = new double[V];
           this.cap = new double[V][V];
           this.cost = new double[V][V];
           this.node = new boolean[V];
           this.link = new boolean[V][V];
          // can be inefficient
          int dem=0;
          for (int i=0;i<_cap.length;i++)
          	for (int j=0;j<_cap[0].length;j++)
          	{
          		this.link_bandwidth[i][j]=_linkbandwidth[i][j];
          		if (_linkbandwidth[i][j]>0)
          		{
          			this.link[i][j]=true;
          			dem++;
          		}
          		else
          			this.link[i][j]=false;
          		this.cap[i][j]=_cap[i][j];
         		this.cost[i][j]=_cost[i][j];
          	}
          for (int i=0;i<V;i++)
          {
          	this.K[i]= _K[i];
          	this.r[i]= _r[i];
          	this.x[i]=_x[i];
          	this.y[i]=_y[i];
          	if(_K[i]>0)
          		this.node[i] = true;
          }
          this.E=dem;
      }
    

    // number of vertices and edges
    public int V() { return V; }
    public int E() { return E; }
    public double getPriceForUnitNode(int v)
    {
    	return r[v];
    }
    public double getCapacity(int v)
    {
    	return K[v];
    }
    public boolean setCapacity(int v, double c)
    {
    	K[v]=c;
    	return true;
    }
    public boolean setEdgeWeight(int v, int u,double c)
    {
    	link_bandwidth[u][v]=c;
    	link_bandwidth[u][v]=c;
    	link[v][u]=true;
    	link[u][v]=true;
    	return true;
    }
    public boolean setCap(int v,int u,double c)
    {
    	cap[u][v]=c;
    	cap[v][u]=c;
    	return true;
    }
    public double getEdgeWeight(int u, int v)
    {
    		return link_bandwidth[u][v];
    }
    public double getCap(int u, int v)
    {
    		return cap[u][v];
    }
    public double getCost(int u, int v)
    {
    		return cost[u][v];
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
        	link_bandwidth[w][v] = b;
        	link[v][w]=true;
        	link[w][v]=true;
        	}
        	
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
    	link[v][u]=false;
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