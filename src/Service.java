
import java.util.Properties;
import java.io.BufferedWriter;


public class Service {
	//gồm có số lượng function, mảng các function, băng thông
    private int idS;
    private int noF;
    private double bwS;
    private Function[] arrF;
    static BufferedWriter out;
    public static Properties props;
    
    // empty graph with V vertices
    public Service(int idS) {
        if (idS <= 0) throw new RuntimeException("Number of vertices must be nonnegative");
        this.idS = idS;
    }
    
public Service(double bw_max,int idS, Function[] f) {
    	
        this(idS);this.noF = UtilizeFunction.randInt(1, f.length);
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
   
    public Service(int idS, Function[] f,double bwS) {
    	
        this(idS);
        if(bwS < 0)
        {
        	this.noF = UtilizeFunction.randInt(1, f.length);
        	// random bandwidth for service
        	
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
        	this.bwS= bwS;
        	this.arrF = new Function[this.noF];
        	for (int i=0;i<this.noF;i++)
        	{
        		this.arrF[i]= f[i];// tạo một mảng các function cho từng service        	
        	}
        	
        }
        
    }
    public Service(int idS, int h) {
        this(idS);
        this.noF = UtilizeFunction.randInt(1, 9);
        this.bwS= 90 * Math.random()+1;
        this.arrF = new Function[this.noF];
        // can be inefficient
        for (int i=0;i<this.noF;i++)
        {
        	int idF= UtilizeFunction.randInt(1, 20);
        	this.arrF[i]= new Function(idF);// tạo một mảng các function cho từng service
        }
        
    }

    // id of Service
    public int idS() { return idS; }
    // number of functions in service
    public int noF() { return noF; }
    // return bandwidth of service;
    public double bwS() { return this.bwS; }
    //return array of Function in service;
    public Function[] getFunctions() {return this.arrF;}
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
        s.append(idS + ": " + bwS +": "+noF+ " : ");
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
        Service[] services = new Service[numberService];
    	for (int i=0;i<numberService;i++)
    	{
    		services[i]= new Service(i+1,functions,-1);
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