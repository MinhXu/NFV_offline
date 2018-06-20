import gurobi.*;

public class Callback extends GRBCallback {
  private double     lastiter;
  private double     lastnode;
  private GRBVar[]   vars;
  private double[] values;
  private int d;
  private int n;
  private double[][] w;
  private double[] b_d;
  //private FileWriter logfile;

  public Callback(GRBVar[] xvars,int _d,int _n,double[][] _w,double[] _b) {
    lastiter = lastnode = -GRB.INFINITY;
    vars = xvars;
    d=_d;
    n=_n;
    w=_w;
    b_d = _b;
    
    //logfile = xlogfile;
  }

  protected void callback1()
  {


	    try {
	      if (where == GRB.CB_MIPSOL) {
	        // MIP solution callback
	        int      nodecnt = (int) getDoubleInfo(GRB.CB_MIPSOL_NODCNT);
	        double   obj     = getDoubleInfo(GRB.CB_MIPSOL_OBJ);
	        int      solcnt  = getIntInfo(GRB.CB_MIPSOL_SOLCNT);
	        values       = getSolution(vars);
	        double[][][] valY= new double[d][n][n];
	        
	        for (int i=0;i<values.length;i++)
	        {
	        	int k= i%d;
      		int temp = i/d;
      		int j1= temp/n;
      		int j2= temp%n;
      		valY[k][j1][j2]=values[i];
	        	if(values[i]>0)
	        	{
	        		
	        		System.out.println("d["+i+1+"]="+values[i] +":"+valY[k][j1][j2]);
	        	}
	        }
	        double[] ySub = new double[d];
	        for(int j1=0;j1<n;j1++)
	        	for(int j2=0;j2<n;j2++)
	        	{
	        		if(w[j1][j2]>0)
	        		{

		        		
		        		
		        		int[] ySorted= sortVal(b_d);
		        		double sum=0;
		        		int index=-1;
		        		for(int id=0;id<ySorted.length;id++)
		        		{
		        			sum+=b_d[ySorted[id]];
		        			if(sum>=w[j1][j2])
		        			{
		        				index=id+1;
		        				break;
		        			}
		        				
		        		}
		        		if(index==-1)
		        			continue;
		        		else
		        		{
		        			if(index<d-3)
		        				index= index+3;
		        			else
		        				if(index<d-2)
		        					index= index+2;
		        				else
		        					if(index<d-1)
		        						index= index+1;
		        					else
		        						continue;
		        		}
		        		
		        		
		        		GRBLinExpr exp = new GRBLinExpr();	
						//String st = "cover["+(j1)+ "]["+(j2)+ "]";
						sum =0.0;
						for(int i=0;i<index;i++)
						{
							
							sum+=b_d[ySorted[i]];
						}
						double lambda = sum-w[j1][j2];
						for(int i=0;i<index;i++)
						{
							int idDemand = ySorted[i];
							double bwD = b_d[ySorted[i]];
							
							int id = idDemand + j2*d+j1*n*d;
							exp.addTerm(bwD, vars[id]);					
							
							if(bwD>lambda)
							{
								exp.addConstant(bwD-lambda);
								exp.addTerm(lambda-bwD,vars[id]);
							}
						}
						addCut(exp, GRB.LESS_EQUAL, w[j1][j2]);
						exp=null; 
	        		}      		
	        		
	        	}
	        
	        
	      }
	      
	    } catch (GRBException e) {
	      System.out.println("Error code: " + e.getErrorCode());
	      System.out.println(e.getMessage());
	      e.printStackTrace();
	    } catch (Exception e) {
	      System.out.println("Error during callback");
	      e.printStackTrace();
	    }
	  

  }
  
  protected void callback()  {

	    try {
	    	if (where == GRB.CB_MIPNODE) {
	            // MIP node callback
	          
	            if (getIntInfo(GRB.CB_MIPNODE_STATUS) == GRB.OPTIMAL) {
	        // MIP solution callback
	            	//System.out.println("**** New node ****");
	            	values= getNodeRel(vars);
	       // values       = getSolution(vars);
	        double[][][] valY= new double[d][n][n];
	        
	        for (int i=0;i<values.length;i++)
	        {
	        	int k= i%d;
        		int temp = i/d;
        		int j1= temp/n;
        		int j2= temp%n;
        		valY[k][j1][j2]=values[i];
//	        	if(values[i]>0)
//	        	{
//	        		
//	        		System.out.println("d["+i+1+"]="+values[i] +":"+valY[k][j1][j2]);
//	        	}
	        }
	        double[] ySub = new double[d];
	        for(int j1=0;j1<n;j1++)
	        	for(int j2=0;j2<n;j2++)
	        	{
	        		if(w[j1][j2]>0)
	        		{

		        		double[] ySubTemp = new double[d];
		        		for(int k=0;k<d;k++)
		        		{
		        			ySub[k]= valY[k][j1][j2];
		        			ySubTemp[k]=ySub[k]-0.5;
		        			if(ySubTemp[k]<0)
		        				ySubTemp[k]=-ySubTemp[k];
		        		}
		        		
		        		int[] ySorted= sortVal(ySubTemp);
		        		//int[] ySorted= sortDecreasing(ySubTemp);
		        		if (ySorted==null)
		        			continue;
		        		double sum=0;
		        		int index=-1;
		        		for(int id=0;id<ySorted.length;id++)
		        		{
		        			sum+=b_d[ySorted[id]];
		        			if(sum>=w[j1][j2])
		        			{
		        				index=id+1;
		        				break;
		        			}
		        				
		        		}
		        		if(index==-1)
		        			continue;
		        		else
		        		{
//		        			if(index<d-3)
//		        				index= index+3;
//		        			else
//		        				if(index<d-2)
//		        					index= index+2;
//		        				else
		        					if(index<d-1)
		        						index= index+1;
		        					else
		        						continue;
		        		}
		        		
		        		
		        		GRBLinExpr exp = new GRBLinExpr();	
						//String st = "cover["+(j1)+ "]["+(j2)+ "]";
						sum =0.0;
						for(int i=0;i<index;i++)
						{
							
							sum+=b_d[ySorted[i]];
						}
						double lambda = sum-w[j1][j2];
						for(int i=0;i<index;i++)
						{
							int idDemand = ySorted[i];
							double bwD = b_d[ySorted[i]];
							
							int id = idDemand + j2*d+j1*n*d;
							exp.addTerm(bwD, vars[id]);					
							
							if(bwD>lambda)
							{
								exp.addConstant(bwD-lambda);
								exp.addTerm(lambda-bwD,vars[id]);
							}
						}
						addCut(exp, GRB.LESS_EQUAL, w[j1][j2]);
						exp=null; 
	        		}      		
	        		
	        	}
	        
	      }
	      }
	    } catch (GRBException e) {
	      System.out.println("Error code: " + e.getErrorCode());
	      System.out.println(e.getMessage());
	      e.printStackTrace();
	    } catch (Exception e) {
	      System.out.println("Error during callback");
	      e.printStackTrace();
	    }
	  
  }
  protected int[] sortDecreasing(double[] srcLst)
  {

	  int[] temp= new int[d];
		int dem=0;
		double[] savelst = new double[d];
		for(int i=0;i<srcLst.length;i++)
			savelst[i]=srcLst[i];
		
		while (dem<d)
		{
			double max=-1.0;
			int id=-1;
			for (int i=0;i< srcLst.length;i++)
			{
				double dtemp= srcLst[i];
				if(dtemp>max && dtemp!=-1)
				{
					max = dtemp;
					id=i;
				}
			
			}			
			if(id==-1)
			{
				System.out.println("Het chua 1 "+ dem);
				return null;
			}
			srcLst[id] = -1.0;
			temp[dem]=id;
			dem++;
		}
		return temp;
	
	  
  
  }
  protected int[] sortVal(double[] srcLst)
  {
	  int[] temp= new int[d];
		int dem=0;
		
		while (dem<d)
		{
			double min=10000.0;
			int id=-1;
			for (int i=0;i< srcLst.length;i++)
			{
				double dtemp= srcLst[i];
				if(dtemp<min)
				{
					min = dtemp;
					id=i;
				}
			
			}			
			if(id==-1)
			{
				System.out.println("Het chua "+ dem);
				continue;
			}
			srcLst[id] =100000.0;
			temp[dem]=id;
			dem++;
		}
		return temp;
	
	  
  }
  protected double[] getVar()
  {
	  return values;
  }
  protected void callbackChuan() {
    try {
      if (where == GRB.CB_POLLING) {
        // Ignore polling callback
      } else if (where == GRB.CB_PRESOLVE) {
        // Presolve callback
        int cdels = getIntInfo(GRB.CB_PRE_COLDEL);
        int rdels = getIntInfo(GRB.CB_PRE_ROWDEL);
        
//        values       = getSolution(vars);
//        double[][][] valY= new double[d][n][n];
//
//        for (int i=0;i<values.length;i++)
//        {
//        	int k= i%d;
//    		int temp = i/d;
//    		int j1= temp/n;
//    		int j2= temp%n;
//    		valY[k][j1][j2]=values[i];
//        	if(values[i]>0)
//        	{
//        		
//        		System.out.println("d["+i+1+"]="+values[i] +":"+valY[k][j1][j2]);
//        	}
//        }
        if (cdels != 0 || rdels != 0) {
          System.out.println(cdels + " columns and " + rdels
              + " rows are removed");
        }
      } else if (where == GRB.CB_SIMPLEX) {
        // Simplex callback
        double itcnt = getDoubleInfo(GRB.CB_SPX_ITRCNT);
        if (itcnt - lastiter >= 100) {
          lastiter = itcnt;
          double obj    = getDoubleInfo(GRB.CB_SPX_OBJVAL);
          int    ispert = getIntInfo(GRB.CB_SPX_ISPERT);
          double pinf   = getDoubleInfo(GRB.CB_SPX_PRIMINF);
          double dinf   = getDoubleInfo(GRB.CB_SPX_DUALINF);
          char ch;
          if (ispert == 0)      ch = ' ';
          else if (ispert == 1) ch = 'S';
          else                  ch = 'P';
          System.out.println(itcnt + " " + obj + ch + " "
              + pinf + " " + dinf);
        }
      } else if (where == GRB.CB_MIP) {
        // General MIP callback
        double nodecnt = getDoubleInfo(GRB.CB_MIP_NODCNT);
        double objbst  = getDoubleInfo(GRB.CB_MIP_OBJBST);
        double objbnd  = getDoubleInfo(GRB.CB_MIP_OBJBND);
        int    solcnt  = getIntInfo(GRB.CB_MIP_SOLCNT);
        if (nodecnt - lastnode >= 100) {
          lastnode = nodecnt;
          int actnodes = (int) getDoubleInfo(GRB.CB_MIP_NODLFT);
          int itcnt    = (int) getDoubleInfo(GRB.CB_MIP_ITRCNT);
          int cutcnt   = getIntInfo(GRB.CB_MIP_CUTCNT);
          System.out.println(nodecnt + " " + actnodes + " "
              + itcnt + " " + objbst + " " + objbnd + " "
              + solcnt + " " + cutcnt);
        }
        if (Math.abs(objbst - objbnd) < 0.1 * (1.0 + Math.abs(objbst))) {
          System.out.println("Stop early - 10% gap achieved");
          abort();
        }
        if (nodecnt >= 10000 && solcnt > 0) {
          System.out.println("Stop early - 10000 nodes explored");
          abort();
        }
      } else if (where == GRB.CB_MIPSOL) {
        // MIP solution callback
        int      nodecnt = (int) getDoubleInfo(GRB.CB_MIPSOL_NODCNT);
        double   obj     = getDoubleInfo(GRB.CB_MIPSOL_OBJ);
        int      solcnt  = getIntInfo(GRB.CB_MIPSOL_SOLCNT);
        double[] x       = getSolution(vars);
        System.out.println("**** New solution at node " + nodecnt
            + ", obj " + obj + ", sol " + solcnt
            + ", x[0] = " + x[0] + " ****");
      } else if (where == GRB.CB_MIPNODE) {
        // MIP node callback
        System.out.println("**** New node ****");
        if (getIntInfo(GRB.CB_MIPNODE_STATUS) == GRB.OPTIMAL) {
          //double[] x = getNodeRel(vars);
          //setSolution(vars, x);
        	
        	System.out.println("Co len co len");
          values= getNodeRel(vars);
          double[][][] valY= new double[d][n][n];
	        
	        for (int i=0;i<values.length;i++)
	        {
	        	int k= i%d;
    		int temp = i/d;
    		int j1= temp/n;
    		int j2= temp%n;
    		valY[k][j1][j2]=values[i];
	        	if(values[i]>0)
	        	{
	        		
	        		System.out.println("d["+i+1+"]="+values[i] +":"+valY[k][j1][j2]);
	        	}
	        }
          
        }
      } else if (where == GRB.CB_BARRIER) {
        // Barrier callback
        int    itcnt   = getIntInfo(GRB.CB_BARRIER_ITRCNT);
        double primobj = getDoubleInfo(GRB.CB_BARRIER_PRIMOBJ);
        double dualobj = getDoubleInfo(GRB.CB_BARRIER_DUALOBJ);
        double priminf = getDoubleInfo(GRB.CB_BARRIER_PRIMINF);
        double dualinf = getDoubleInfo(GRB.CB_BARRIER_DUALINF);
        double cmpl    = getDoubleInfo(GRB.CB_BARRIER_COMPL);
        System.out.println(itcnt + " " + primobj + " " + dualobj + " "
            + priminf + " " + dualinf + " " + cmpl);
      } else if (where == GRB.CB_MESSAGE) {
        // Message callback
        //String msg = getStringInfo(GRB.CB_MSG_STRING);
        //if (msg != null) logfile.write(msg);
      }
    } catch (GRBException e) {
      System.out.println("Error code: " + e.getErrorCode());
      System.out.println(e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      System.out.println("Error during callback");
      e.printStackTrace();
    }
  }
}