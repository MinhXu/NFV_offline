import java.util.ArrayList;

import gurobi.*;

public class Callback1 extends GRBCallback {
  private double     lastiter;
  private double     lastnode;
  private GRBVar[]   yvars;
  private GRBVar[] xvars;
  private double[] xvalues;
  private double[] yvalues;
  private int d;
  private int n;
  private int m;
  private double[][] w;
  private double[][] cap;
  private double[] b_d;
  private double[][] lambda;
  private int counterLimit;
  private int counter;
  //private FileWriter logfile;

  public Callback1(GRBVar[] _xvars,GRBVar[] _yvars,int _d,int _n,int _m,double[][] _w,double[] _b,double[][] _cap,double[][] _lambda,int _counterLimit) {
    lastiter = lastnode = -GRB.INFINITY;
    xvars= _xvars;
    yvars = _yvars;
    d=_d;
    n=_n;
    m=_m;
    w=_w;
    b_d = _b;
    cap=_cap;
    lambda=_lambda;
    counterLimit = _counterLimit;
    
    //logfile = xlogfile;
  }
  
   
	public void combinations(int noComb, ArrayList<Integer> arr, ArrayList<Integer> list,int startPoisition,ArrayList<ArrayList<Integer>> result) {
		
		
		if(list.size()<=0)
		{
			for (int i=0;i<noComb;i++)
				list.add(-1);
		}		
		if (noComb == 0){
			ArrayList<Integer> t= new ArrayList<>();
			for(int i=0;i<list.size();i++)
				t.add(list.get(i));
	          result.add(t);
	          list = new ArrayList<>();
	          return;
	      } 
		
	      for (int i = startPoisition; i <= arr.size()-noComb; i++){
	          list.set(list.size() - noComb,arr.get(i));
	          combinations(noComb-1,arr, list, i+1,result);
	      }
	    }
	
  public ArrayList<ArrayList<Integer>> FindCover(ArrayList<Integer> idSet, double[] b_d,double _w, int noCover)
  {
	  ArrayList<ArrayList<Integer>> temp = new ArrayList<>();
	  ArrayList<ArrayList<Integer>> comb = new ArrayList<>();
	  ArrayList<Integer> combLst = new ArrayList<>();
	  combinations(noCover, idSet, combLst,0,comb);
	  for(int i=0;i<comb.size();i++)
	  {
		  ArrayList<Integer> cover1= comb.get(i);
		  double sum=0;
		  for(int j=0;j<cover1.size();j++)
			  sum+= b_d[cover1.get(j)];
		  if(sum>_w)
			  temp.add(cover1);
	  }
	  return temp;
  }
  protected void callback()  {
	    try {
	    	if (where == GRB.CB_MIPNODE) {
	            // MIP node callback
	          
	            if (getIntInfo(GRB.CB_MIPNODE_STATUS) == GRB.OPTIMAL && counter<counterLimit ) {
	            	counter++;
	            	//System.out.println("aaa+ "+counter);
	        // MIP solution callback -> flow cover
	            	yvalues= getNodeRel(yvars);
	        double[][][] valY= new double[d][n][n];
	        
	        for (int i=0;i<yvalues.length;i++)
	        {
	        	int k= i%d;
        		int temp = i/d;
        		int j1= temp/n;
        		int j2= temp%n;
        		valY[k][j1][j2]=yvalues[i];
	        }
	        double[] ySub = new double[d];
	        for(int j1=0;j1<n;j1++)
	        	for(int j2=0;j2<n;j2++)
	        	{
	        		if(w[j1][j2]>0)
	        		{
	        			
	        			ArrayList<Double> ySubTemp = new ArrayList<>();
	        			ArrayList<Integer> idYsubTemp = new ArrayList<>();

		        		//double[] ySubTemp = new double[d];
		        		for(int k=0;k<d;k++)
		        		{
		        			ySub[k]= valY[k][j1][j2];
		        			//if(ySub[k]!=0)
		        			//{
		        				if(ySub[k]-0.5 <0)
		        					ySubTemp.add(-ySub[k]+0.5);
		        				else
		        					ySubTemp.add(ySub[k]-0.5);
		        				idYsubTemp.add(k);
		        			//}
		        		}
		        		
		        		int[] ySorted= sortVal(ySubTemp);
		        		//int[] ySorted= sortDecreasing(ySubTemp);
		        		if (ySorted==null)
		        			continue;
		        		double sum=0;
		        		int index=-1;
		        		for(int id=0;id<ySorted.length;id++)
		        		{
		        			int idY=idYsubTemp.get(ySorted[id]);
		        			sum+=b_d[idY];
		        			if(sum>w[j1][j2])
		        			{
		        				index=id+1;
		        				break;
		        			}
		        				
		        		}
		        		ArrayList<Integer> coverSetTemp = new ArrayList<>();
		        		ArrayList<ArrayList<Integer>> coverSet = new ArrayList<>();
		        		if(index==-1)
		        		{
		        			//System.out.println("khong lam gi");
		        			continue;
		        		}
		        		else
		        		{
		        			if (index + 2<ySub.length)
		        			{
		        				for (int i=0;i<index+2;i++)
		        					coverSetTemp.add(idYsubTemp.get(ySorted[i]));
		        				coverSet = FindCover(coverSetTemp, b_d, w[j1][j2], index);
		        			}
		        			else
		        			{	
		        				for (int i=0;i<index;i++)
		        					coverSetTemp.add(idYsubTemp.get(ySorted[i]));
		        				coverSet.add(coverSetTemp);
		        					
		        			}
		        		}
		        		if(coverSet.size()>0)
		        		{
		        			for (ArrayList<Integer> cover: coverSet)
		        			{
		        				sum =0.0;
		        				for(int i=0;i<cover.size();i++)
								{
									int idY=cover.get(i);
									sum+=b_d[idY];
								}
								double lambda = sum-w[j1][j2];
								sum =0.0;
								for(int i=0;i<cover.size();i++)
								{
									int idDemand = cover.get(i);
									double bwD = b_d[idDemand];
									
									int id = idDemand + j2*d+j1*n*d;
									sum += bwD * ySub[idDemand];					
									
									if(bwD>lambda)
									{
										sum += (bwD-lambda)*(1-ySub[idDemand]);
									}
								}
								if(sum <= w[j1][j2])
								{
									//System.out.println("No violation");
									continue;
								}
								else
									System.out.println("Violated Inequality");
				        		
				        		GRBLinExpr exp = new GRBLinExpr();	
								//String st = "cover["+(j1)+ "]["+(j2)+ "]";
								sum =0.0;
								for(int i=0;i<cover.size();i++)
								{
									int idY=cover.get(i);
									sum+=b_d[idY];
								}
								lambda = sum-w[j1][j2];
								for(int i=0;i<cover.size();i++)
								{
									int idDemand = cover.get(i);
									double bwD = b_d[idDemand];
									
									int id = idDemand + j2*d+j1*n*d;
									exp.addTerm(bwD, yvars[id]);					
									
									if(bwD>lambda)
									{
										exp.addConstant(bwD-lambda);
										exp.addTerm(lambda-bwD,yvars[id]);
									}
								}
								addCut(exp, GRB.LESS_EQUAL, w[j1][j2]);
								exp=null; 
		        			}
		        		}
						
	        		}      		
	        		
	        	}
//	        //Cover for xvalues
//	        
//	        xvalues= getNodeRel(xvars);
//	        double[][][] valX= new double[d][m][n];
//	        
//	        for (int i=0;i<xvalues.length;i++)
//	        {
//	        	int k= i%d;
//        		int temp = i/d;
//        		int j1= temp/n;
//        		int j2= temp%n;
//        		valX[k][j1][j2]=xvalues[i];
//	        }
//	        double[] xSub = new double[d*m];
//	        
//	        for(int compo=0;compo<3;compo++)
//			{
//				for(int j = 0; j < n; j++) //node
//		    	{
//					//tim cover
//					int dem =0;
//					//double[] xSubTemp = new double[d*m];
//					ArrayList<Double> xSubTemp = new ArrayList<>();
//					ArrayList<Integer> idxSubTemp = new ArrayList<>();
//					
//					for(int i=0;i<m;i++)
//					{
//						for(int k=0;k<d;k++)
//		        		{
//							
//							
//		        			xSub[dem]= valX[k][i][j];
//		        			//if(xSub[dem]!=0)
//		        			//{
//		        				if(xSub[dem]-0.5>0)
//		        					xSubTemp.add(xSub[dem]-0.5);
//		        				else
//		        					xSubTemp.add(-xSub[dem]+0.5);
//		        				idxSubTemp.add(dem);
//		        			//}
//		        			dem++;
//		        		}
//					}
//		        		
//	        		
//	        		int[] xSorted= sortVal(xSubTemp);
//	        		//int[] ySorted= sortDecreasing(ySubTemp);
//	        		if (xSorted==null)
//	        			continue;
//					
//					GRBLinExpr expr1= new GRBLinExpr();
//					double capVal = cap[j][compo];
//					double sum = 0.0;
//
//					int index =-1;
//					for(int id=0;id<xSorted.length;id++)
//	        		{
//						int fID = idxSubTemp.get(xSorted[id])/d;
//	        			sum+=lambda[fID][compo];
//	        			if(sum>capVal)
//	        			{
//	        				index=id+1;
//	        				break;
//	        			}
//	        				
//	        		}
//	        		if(index==-1)
//	        		{
//	        			//System.out.println("khong lam gi");
//	        			continue;
//	        		}
//	        		else
//	        		{
////	        			if(index<d-3)
////	        				index= index+3;
////	        			else
////	        				if(index<d-2)
////	        					index= index+2;
////	        				else
////	        					if(index<d*m-1)
////	        						index= index+1;
////	        					else
////	        						continue;
//	        		}
//					
//					
//					for(int i=0;i<index;i++)
//					{
//						int fID = idxSubTemp.get(xSorted[i])/d;
//						int dID = idxSubTemp.get(xSorted[i])%d;
//						int id = dID + j*d+fID*n*d;
//						expr1.addTerm(1, xvars[id]);							
//						
//					}
//					String st = "IC["+(j)+ "]["+compo+"]";
//					addCut(expr1, GRB.LESS_EQUAL, index-1);
////					
//					expr1 = null;
//					
//		    	}
//			}
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

	  int[] temp= new int[srcLst.length];
		int dem=0;
		double[] savelst = new double[srcLst.length];
		for(int i=0;i<srcLst.length;i++)
			savelst[i]=srcLst[i];
		
		while (dem<srcLst.length)
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
  protected int[] sortVal(ArrayList<Double> srcLst1)
  {
	  double[] srcLst = new double[srcLst1.size()];
	  for(int i=0;i<srcLst1.size();i++)
		  srcLst[i]= srcLst1.get(i);
	  int[] temp= new int[srcLst.length];
		int dem=0;
		
		while (dem<srcLst.length)
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
	  return yvalues;
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
        double[] x       = getSolution(yvars);
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
          yvalues= getNodeRel(yvars);
          double[][][] valY= new double[d][n][n];
	        
	        for (int i=0;i<yvalues.length;i++)
	        {
	        	int k= i%d;
    		int temp = i/d;
    		int j1= temp/n;
    		int j2= temp%n;
    		valY[k][j1][j2]=yvalues[i];
	        	if(yvalues[i]>0)
	        	{
	        		
	        		System.out.println("d["+i+1+"]="+yvalues[i] +":"+valY[k][j1][j2]);
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