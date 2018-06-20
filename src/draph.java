
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;

import ilog.cplex.*;
import ilog.concert.*;
import ilog.cplex.IloCplex.CplexStatus;


public class draph {
	static BufferedWriter out;
	static OutputStream out1;
	static BufferedReader in;
	static int c,n,m,d,z,E,_no;
	static MyGraph g;
	static Function[] functionArr;
	static Demand[] demandArr;
	private static String logFilename="cplex.log";
	private static FileOutputStream logFile;
	private static IloCplex cplex;
	static IloNumVar[][][][] x;// d=1->d, a=1->n; b=0->m; c=1->position (n*m)
	public static Function getFunction(int id)
	{
		if(id==0) return null;
		for(int i=0;i<m;i++)
			if (functionArr[i].id() ==id)
				return functionArr[i];
		return null;
	}
	
	public static void initValue(int nFunction,int nService,int nNode,int nEdge)
	{
		 int V = nNode;
	     int E = nEdge;
	     g = new MyGraph(V,E,true);
	     n=g.V();
	     m = nFunction;
	     functionArr = new Function[m];
	     for (int i=0;i< m;i++)
	        functionArr[i]= new Function(i+1);        
	     d = nService;
	     demandArr = new Demand[d];
	    for (int i=0;i<d;i++)
	    {
	    	demandArr[i]= new Demand(-1,-1,5,i+1,functionArr,-1,n);
	    } 
	    if (n*m <  m+4)
			_no=n*m;
		else
			_no = m+4;
	    x= new IloNumVar[d+1][n+1][m+1][n*m+1];
	}
	public static double getBwFunction(int id)
	{
		if(id==0) return 0;
		for(int i=0;i<m;i++)
			if (functionArr[i].id() ==id)
				return functionArr[i].bw();
		return -1;
	}
	
	public static double getBwService(int id)
	{
		if(id==0) return 0;
		for(int i=0;i<m;i++)
			if (demandArr[i].idS() ==id)
				return demandArr[i].bwS();
		return -1;
	}
	public static Demand getDemand(int id)
	{
		for (int i=0;i<d;i++)
			if(demandArr[i].idS()==id)
				return demandArr[i];
		return null;
	}
	public static void ReadInputFile(String fileName)
	{
		File file = new File(fileName);
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
			demandArr = new Demand[d];
			//m function
			for(int i = 0;i<m;i++)
				functionArr[i]= new Function(i+1,Double.parseDouble(line[0].split(" ")[i]));
			String[] tempLine;
			//d demand
			for (int i=0;i<d;i++)
			{
				tempLine = line[i+1].split(" ");
				Function[] f = new Function[tempLine.length-1];
				for (int j=0;j<f.length;j++)
					f[j]= getFunction(Integer.parseInt(tempLine[j+1]));
				demandArr[i] = new Demand(i+1,Integer.parseInt(tempLine[0]),Integer.parseInt(tempLine[1]),Double.parseDouble(tempLine[3]),f,Double.parseDouble(tempLine[2]),n);
			}
			double[] r = new double[n];
			double[] K = new double[n];
			double[][] w = new double[n][n];
			double price_bandwidth = Double.parseDouble(line[d+1]);
			
			// virtual network
			for (int i=0;i <n;i++)
			{
				tempLine =line[i+d+2].split(" ");
				r[i]= Double.parseDouble(tempLine[0]);
				K[i] = Double.parseDouble(tempLine[1]);
				tempLine = line[i+d+n+2].split(" ");
				for (int j=0;j<n;j++)
					w[i][j] = Double.parseDouble(tempLine[j]);
			}
			g= new MyGraph(r,K,w,price_bandwidth);
			if (n*m <  m+4)
				_no=n*m;
			else
				_no = m+6;
			x= new IloNumVar[d+1][n+1][m+1][_no+1];   
			
            // Always close files.
            in.close();  
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		}
	
	
	//TODO create model for cplex
	public static void model2(String outputFile)
	{
		try {
			File file = new File(outputFile);
	        out = new BufferedWriter(new FileWriter(file));
	        out.write("Function random:::" + m);
	        out.newLine();
	        try{
	        	if (cplex != null)
	        		cplex.end();
	        	if (logFile == null)
				try {
					logFile = new FileOutputStream(logFilename);
				} catch (FileNotFoundException e3) {
					// TODO Auto-generated catch block
					e3.printStackTrace();
				}
	        	cplex = new IloCplex();
	        	cplex.setOut(logFile);
	        	cplex.setWarning(logFile);
	        	IloLQNumExpr obj = cplex.lqNumExpr();
	        	IloQuadNumExpr[][] expr2 = new IloQuadNumExpr[n][n];
	        	
	        	for (int i=0;i<n;i++)
	        		for (int j=0;j<n;j++)
	        			expr2[i][j]= cplex.quadNumExpr();
	        	
	        	//variable declaire
				for(int i = 0; i < d; i++) 
				    for(int j = 0; j < n; j++)
				    	for(int k = 0; k < m+1; k++)
				    		for (int l=0;l<_no;l++)
				    			x[i+1][j+1][k][l+1] = cplex.numVar(0, 1);
			
				out.write("Function: " + m);
		   		out.newLine();
				for (int i=0;i<m;i++)
		       	{
	               out.write(functionArr[i].toString());
	               out.newLine();
		       	}
				
		   		out.write("Service: " + d);
		   		out.newLine();
		       	for (int i=0;i<d;i++)
		       	{    		
		       		out.write(demandArr[i].toString());
		       		out.newLine();
		       	}
		       	
				System.out.println(" ham muc tieu");
				for(int i = 0; i < d; i++) //for each demand
				{
					for(int j = 0; j < n; j++)
				    	for(int k = 0; k <= m; k++) 
				    		for(int l=0;l<_no;l++)
				    			if( getDemand(i+1).getOrderFunction(k)!=0)
				    				obj.addTerm(x[i+1][j+1][k][l+1],g.getPriceForUnitNode(j+1));
					
					System.out.println(" so hang 2 cua ham muc tieu");
					for(int j = 0; j < n; j++)
						for(int k = 0; k <= m; k++)
							for(int l=0;l<_no-1;l++)
								for(int a=0;a<n;a++)
									for(int b=0;b<=m;b++)
										obj.addTerm(g.getEdgeWeight(j+1, a+1)*g.getPriceBandwidth()*0.001, 
														x[i+1][j+1][k][l+1], 
														x[i+1][a+1][b][l+2]);	
				}
				cplex.addMinimize(obj);
				
				//add constraints
				System.out.println(" rang buoc 1");//khong co truong hop b==0
				for(int j = 0; j < n; j++) //node
			    {
					IloLQNumExpr expr1= cplex.lqNumExpr();
					for(int i = 0; i < d; i++) //demand
						for(int k = 0; k <= m; k++) //function
							for(int l=0;l<_no;l++) //possition
								expr1.addTerm(x[i+1][j+1][k][l+1],getBwFunction(k));
					String st = "c["+(j+1)+ "]";
					cplex.addLe(expr1, g.getCapacity(j+1),st);
					expr1 = null;
			    	}
				System.gc();
				
				System.out.println(" rang buoc 2");
				for(int j = 0; j < n; j++) //node a1
					for(int a=0;a<n;a++) //node a2
						for (int i =0;i<d;i++) //demand
							for(int k = 0; k <= m; k++) //function b1
								for(int b=0;b<=m;b++) //function b2
									for(int l=0;l<_no-1;l++)
										if(j!=a)
											expr2[j][a].addTerm(getBwService(i+1),
															x[i+1][j+1][k][l+1],
															x[i+1][a+1][b][l+2]);
				
				for(int j = 0; j < n; j++) //node a1
					for(int a=j;a<n;a++) //node a2
						{
							if(j!=a)
							{
								IloQuadNumExpr expr4 = cplex.quadNumExpr();
								expr4.add(expr2[j][a]);
								expr4.add(expr2[a][j]);
								String st = "h["+(j+1)+ "]";
								cplex.addLe(expr4,g.getEdgeWeight(j+1, a+1), st);
								expr4= null;
							}
						}
				for(int j = 0; j < n; j++) //node a1
					for(int a=j;a<n;a++) //node a2
						expr2[j][a]=null;
				
				System.gc();
				
				System.out.println(" rang buoc 3");//khong co truong hop b==0, khong xet cac nut khong gian
				for (int i =0;i<d;i++) //demand
					for (int k = 0;k<=m-1;k++)
						for (int b=k+1;b<=m;b++)
						{
							int id1 = getDemand(i+1).getOrderFunction(k);
							int id2 = getDemand(i+1).getOrderFunction(b);
							if (id1!=0 && id2!=0 && id1 < id2)//truong hop b1 < b2
							{
							for (int j=0;j<n;j++)
								for(int l=0;l<_no;l++)
									for (int a=0;a<n;a++)
										for(int c=0;c<_no;c++)
											{
												IloQuadNumExpr expr3= cplex.quadNumExpr();	
												expr3.addTerm(c-l, x[i+1][j+1][k][l+1], x[i+1][a+1][b][c+1]);
												cplex.addGe(expr3,0);
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
												IloQuadNumExpr expr3= cplex.quadNumExpr();	
												expr3.addTerm(c-l, x[i+1][j+1][k][l+1], x[i+1][a+1][b][c+1]);
												cplex.addLe(expr3,0);
												expr3 = null;
											}
							}
						}
				
				System.out.println(" rang buoc 4");
				for (int i =0;i<d;i++) //demand
					for (int k = 0;k<=m;k++)
						{	
							int id = getDemand(i+1).getOrderFunction(k);
							if (id!=0)//truong hop function in demand
							{
								IloLinearNumExpr expr5= cplex.linearNumExpr();
								for (int j=0;j<n;j++)
									for (int l =0;l<_no;l++)								
										expr5.addTerm(1, x[i+1][j+1][k][l+1]);	
								String st = "f["+(i+1)+ "]["+(k)+ "]";
								cplex.addEq(expr5,1,st);
								expr5 = null;
							}
							else
							{
								if (k!=0)
								{
									
									for (int j=0;j<n;j++)
										for (int l =0;l<_no;l++)
										{
											IloLinearNumExpr expr5= cplex.linearNumExpr();	
											expr5.addTerm(1, x[i+1][j+1][k][l+1]);
											String st = "f["+(i+1)+ "]["+(k)+ "]";
											cplex.eq(expr5, 0, st);
											expr5 = null;
										}
									
								}
								
							}
							
						}
				System.gc();
				
				System.out.println(" rang buoc 5");
				for (int i=0;i<d;i++)
					for (int l=_no-1;l>0;l--)
						for (int j=0;j<n;j++)
							for (int k=0;k<=m;k++)
							{
								IloLQNumExpr expr6= cplex.lqNumExpr();
								expr6.addTerm(-1, x[i+1][j+1][k][l+1]);
								for (int a=0;a<n;a++)
									for (int b=0;b <=m;b++)
										expr6.addTerm(1, x[i+1][j+1][k][l+1], x[i+1][a+1][b][l]);
								String st = "g["+(i+1)+ "]["+(l+1)+ "]["+(c+1)+ "]";
								cplex.addGe(expr6, 0,st);
								expr6 = null;
							}
					
				
				System.out.println(" rang buoc 6");
				for (int i=0;i<d;i++)
					for (int l=0;l<_no;l++)
					{
						IloLinearNumExpr expr7= cplex.linearNumExpr();
						for (int j=0;j<n;j++)
							for (int k=0;k<=m;k++)
								expr7.addTerm(1, x[i+1][j+1][k][l+1]);
						String st = "h["+(i+1)+ "]["+(l+1)+ "]";
						cplex.le(expr7, 1, st);
						expr7 = null;
					}
				System.gc();
				//rang buoc 7 :: function k truoc function 0 tren cung 1 node
				for (int i=0;i<d;i++)
					for (int j=0;j<n;j++)
						for (int k=0;k<=m;k++)
							for (int l=0;l<_no-1;l++)
							{
								IloQuadNumExpr expr8= cplex.quadNumExpr();
								expr8.addTerm(1, x[i+1][j+1][k][l+1],x[i+1][j+1][0][l+2]);
								String st = "k["+(i+1)+ "]["+(l+1)+ "]";
								cplex.eq(expr8,  0, st);
								expr8 = null;
							}
				System.gc();
				//rang buoc 8 :: function 0 truoc function k tren cung 1 node
				for (int i=0;i<d;i++)
					for (int j=0;j<n;j++)
						for (int k=0;k<=m;k++)
							for (int l=1;l<_no;l++)
							{
								IloQuadNumExpr expr9= cplex.quadNumExpr();
								expr9.addTerm(1, x[i+1][j+1][k][l+1],x[i+1][j+1][0][l]);
								String st = "k["+(i+1)+ "]["+(l+1)+ "]";
								cplex.eq(expr9,  0, st);
								expr9 = null;
							}
				System.gc();
				
//rang buoc 9; tranh truong hop x(d,v,0,k)=1 va tat ca x sau k deu bang 0 
				
				for (int i=0;i<d;i++)
					for (int l=_no-1;l>0;l--)
					{
						IloLinearNumExpr expr10= cplex.linearNumExpr();
						for (int j=0;j<n;j++)
							expr10.addTerm(1, x[i+1][j+1][0][l]);
						for (int a=0;a<n;a++)
							for (int b=0;b <=m;b++)
								expr10.addTerm(-1, x[i+1][a+1][b][l+1]);
						String st = "h["+(i+1)+ "]["+(l+1)+ "]";
						cplex.le(expr10, 0, st);
						expr10 = null;
					}
				System.gc();
				
				out.write("Solution for the problem:");
				out.newLine();		
				
				
				if(cplex.solve())
				{
					CplexStatus optimstatus = cplex.getCplexStatus(); 
					if (optimstatus == CplexStatus.Optimal) 
					{ 
						out.write("Objective optimal Value: "+cplex.getObjValue());
						out.newLine();
						for(int i = 0; i < d; i++) 
						    for(int j = 0; j < n; j++)
						    	for(int k = 0; k < m+1; k++)
						    		for (int l=0;l<_no;l++)
						    		{
						    			if(cplex.getValue(x[i+1][j+1][k][l+1])>0)
						    			{
						    			System.out.println(x[i+1][j+1][k][l+1].getName()
						    					+ " " +cplex.getValue(x[i+1][j+1][k][l+1]));
										 out.write(x[i+1][j+1][k][l+1].getName()
							    					+ ":" +cplex.getValue(x[i+1][j+1][k][l+1]));
										 out.newLine();
						    			}
						    		}	
					 } else if (optimstatus == CplexStatus.InfOrUnbd) 
					 	{ 
					        System.out.println("Model is infeasible or unbounded"); 
					        return;
					 	} else if (optimstatus == CplexStatus.Infeasible) 
					        	{ 
							        System.out.println("Model is infeasible"); 
							        return; 
					        	} 
					 else
					 {
						 out.write("Objective feasible Value: "+cplex.getObjValue());
						 out.newLine();
						 for(int i = 0; i < d; i++) 
							 for(int j = 0; j < n; j++)
								 for(int k = 0; k < m+1; k++)
									 for (int l=0;l<_no;l++)
									 {
										 if(cplex.getValue(x[i+1][j+1][k][l+1])>0)
							    			{
										 System.out.println(x[i+1][j+1][k][l+1].getName()
						    					+ " " +cplex.getValue(x[i+1][j+1][k][l+1]));
										 out.write(x[i+1][j+1][k][l+1].getName()
							    					+ ":" +cplex.getValue(x[i+1][j+1][k][l+1]));
										 out.newLine();
							    			}
						    		}
					  }
					
				}
				else
					System.out.println ("the problem is not solved");
				
			} catch(IloException exept){
			 	exept.printStackTrace();}
	        
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
	  				e2.printStackTrace();}
		
	}
	
	public static void model1()
	{
		try{
			IloCplex cplex = new IloCplex();
			//variable declaire
			IloNumVar x = cplex.numVar(0, Double.MAX_VALUE, "x");
			IloNumVar y = cplex.numVar(0, Double.MAX_VALUE, "y");
			
			//expresion
			IloLinearNumExpr objective= cplex.linearNumExpr();
			objective.addTerm(0.12, x);
			objective.addTerm(0.15, y);
			//objective min
			cplex.addMinimize(objective);
			//add constraints
			cplex.addGe(cplex.sum(cplex.prod(60,x),cplex.prod(60,y)), 300);
			cplex.addGe(cplex.sum(cplex.prod(12,x),cplex.prod(6,y)), 36);
			cplex.addGe(cplex.sum(cplex.prod(10,x),cplex.prod(30,y)), 90);
			
			if(cplex.solve())
			{
				System.out.println("obj = "+ cplex.getObjValue());
				System.out.println("x = " + cplex.getValue(x));
				System.out.println("y = " + cplex.getValue(y));
				
			}
			else
				System.out.println ("the problem is not solved");
			
			
			
		} catch(IloException exept){
			exept.printStackTrace();
			
		}
	}
	
	
	public static void main(String[] args) {
    	ReadInputFile(args[0]);
    	String str = args[0].replace("in", "Cplex_out");
    	//initValue(Integer.parseInt(args[0]),Integer.parseInt(args[1]),Integer.parseInt(args[2]),Integer.parseInt(args[3]));
    	model2(str);
		
    }	

}