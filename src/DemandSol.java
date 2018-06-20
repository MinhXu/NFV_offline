
import java.util.Properties;
import java.io.BufferedWriter;


public class DemandSol {
	//source, destination,bandwidth,arrival time,set of function (number of function)
    private int id;//id of demand
    private double cost;
    private double bw;//bandwidth
    private int source;
    private int destination;
    private Function[] setFunction;//function for demand
    private int[] path;//trafic path for demand
    private int[][] loc;// function put on virtual: loc(f,v)=1 when f is put on v
    private double arrivalTime;
    static BufferedWriter out;
    public static Properties props;
    
    // empty graph with V vertices
    public DemandSol(int Id) {
        if (Id <= 0) throw new RuntimeException("Number of vertices must be nonnegative");
        this.id = Id;
    }
    
public DemandSol(double bw_max,int idS, int noF,int noVirtualNode,double currentTime,double Cost,int Source, int Destination) {	
	//check available resource for each node
        this(idS);
        this.source=Source;
        this.destination = Destination;
        this.arrivalTime = currentTime;//gan thoi gian den cua demand        
        	// random bandwidth for service
        	int mul= (int) (bw_max/8);
        	Integer[] intArray = new Integer[] { (int)(bw_max-7*mul), (int)(bw_max-6*mul),(int)(bw_max-5* mul),(int)(bw_max- 4*mul),(int)(bw_max- 3*mul) };
        	this.bw = UtilizeFunction.randomDouble(intArray);
        	int lengPath= UtilizeFunction.randInt(1, noVirtualNode);
        	
        	this.path = new int[lengPath];
        	this.loc= new int[noF+1][noVirtualNode+1];
        	//create array for path of demand
        	for(int i=0;i<lengPath;i++)
        	{
        		boolean flag=false;
        		while (!flag)
        		{
        		int temp=UtilizeFunction.randInt(1, noVirtualNode);
        		if(i>1 && path[i-1]!=temp)
        			if((i>2)&& path[i-2]!=temp)
        			{
        				path[i]=temp;
        				flag=true;
        			}
        		}
        	}
        	for (int i=0;i<noF+1;i++)
        		for (int j=0;j<noVirtualNode+1;j++)
        			loc[i][j]=0;
        	//create array for location all functions
        	for (int i=0;i<lengPath;i++)
        	{
        		int interNode=UtilizeFunction.randInt(0,1);
        		if (interNode==0)
        		{
        			int _f=UtilizeFunction.randInt(1, noF);
        			loc[_f][path[i]]=1;
        			
        		}
        	}
        	this.cost=Cost;
}
   
    public DemandSol(double BwS,int IdS,int Source,int Destination,Function[] SetFunction,int[] Path, int[][] Loc, double ArrivalTime, int noF,int noVirtualNode,double Cost) {
    	//Path: 0->path.length
    	//loc: (f:0->noF +1;;; v:0->noVirtualNode+1)
    	
    	// random bandwidth for service
        this(IdS);
        int bw_max=2000;
        if(BwS < 0)
        {
            this.arrivalTime = ArrivalTime;//gan thoi gian den cua demand        
            	// random bandwidth for service
            	int mul= (int) (bw_max/8);
            	Integer[] intArray = new Integer[] { (int)(bw_max-7*mul), (int)(bw_max-6*mul),(int)(bw_max-5* mul),(int)(bw_max- 4*mul),(int)(bw_max- 3*mul) };
            	this.bw = UtilizeFunction.randomDouble(intArray);
            	int lengPath= UtilizeFunction.randInt(1, noVirtualNode);
            	this.source=Source;
            	this.destination = Destination;
            	this.path = new int[lengPath];
            	this.loc= new int[noF+1][noVirtualNode+1];
            	//create array for path of demand
            	for(int i=0;i<lengPath;i++)
            	{
            		boolean flag=false;
            		while (!flag)
            		{
            		int temp=UtilizeFunction.randInt(1, noVirtualNode);
            		if(i>1 && path[i-1]!=temp)
            			if((i>2)&& path[i-2]!=temp)
            			{
            				path[i]=temp;
            				flag=true;
            			}
            		}
            	}
            	for (int i=0;i<noF+1;i++)
            		for (int j=0;j<noVirtualNode+1;j++)
            			loc[i][j]=0;
            	//create array for location all functions
            	int dem=0;
            	for (int i=0;i<lengPath;i++)
            	{
            		int interNode=UtilizeFunction.randInt(0,1);
            		if (interNode==0)
            		{
            			int _f=UtilizeFunction.randInt(1, noF);
            			//setFunction[dem++]=ExtandingModel.getFunction(_f);
            			loc[_f][path[i]]=1;
            			
            		}
            	}
            	
        }
        else
        {
        	//truong hop khong random
        	this.arrivalTime= ArrivalTime;
        	this.bw=BwS;
        	this.source=Source;
        	this.destination = Destination;
        	this.path=new int[Path.length];
        	for (int i=0;i<Path.length;i++)
        		this.path[i]=Path[i];
        	this.loc=new int[noF+1][noVirtualNode];
        	for (int i=0;i<noF+1;i++)
        		for (int j=0;j<noVirtualNode+1;j++)
        			this.loc[i][j]=Loc[i][j]; 
        	for (int i=0;i<SetFunction.length;i++)
        		this.setFunction[i]=SetFunction[i];
        }
        this.cost=Cost;
        
    }

    // id of Service
    public int idDemandSol() { return id; }
    public int SrcDemandSol() { return source; }
    public int DesDemandSol() { return destination; }
    public double arrivalTimeD() { return arrivalTime; }
    // return bandwidth of service;
    public double bwDemandSol() { return this.bw; }
    //return array of Function in service;
    public int[] getPath() {return path;}
    public Function[] getSetFunctionID(){return setFunction;    }
    public double getCost(){return cost;}
    public int[] getLocNode(int idF)
    {
    	//tra ve gia tri cua virtual node doi voi function idF
    	int temp[]=new int[loc[0].length];
    	int dem=0;
    	for (int i=1;i<loc[0].length;i++)
    	{
    		if(loc[idF][i]==1)
    		{
    			temp[dem++]=i;
    		}
    			
    	}
    	return temp;
    }
    public int[] getLocFunction(int idN)
    {
    	//tra ve gia tri cua virtual node doi voi function idF
    	int temp[]=new int[loc.length];
    	int dem=0;
    	for (int i=1;i<loc.length;i++)
    	{
    		if(loc[i][idN]==1)
    		{
    			temp[dem++]=i;
    		}
    			
    	}
    	return temp;
    }

    // string representation of Graph - takes quadratic time
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(id + ": "+ bw +": "+arrivalTime+"\n");
        for (int v = 0; v < path.length; v++) {
            s.append(path[v] +"    ");
        }
        s.append("\n");
        for (int f=1;f<loc.length;f++)
        {
        	for (int v = 1; v < loc[0].length; v++) {
        		if(loc[f][v]==1)
        			s.append("("+f+","+v+")"+"    ");
        	}
        }
        return s.toString();
    }
    // test client
    public static void main(String[] args) {
    }

}