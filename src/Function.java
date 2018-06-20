
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;


public class Function {
	private int id;
    private double bw;
    private Vector<Double> lamda= new Vector<>(3);
    //private myVector lamda= new myVector(3);
    
    // empty graph with V vertices
    public Function(int id) {//khoi tao random function
        if ( id <=0 ) throw new RuntimeException("Number of vertices must be nonnegative");
        this.id = id; 
        this.bw =  24 * Math.random()+1;//random requirement for function
//        for (int i=0;i<3;i++)
//        {
//        	double temp= UtilizeFunction.randDouble(50);
//        	lamda.addElement(temp);
//        } 
        
        switch (id) {
		case 1:
			Double[] req1 = {0.1,1.0,1.2};
			lamda = new Vector<>(Arrays.asList(req1));
			break;
		case 2:
			Double[] req2 = {0.1,2.0,5.0};
			lamda = new Vector<>(Arrays.asList(req2));
			break;
		case 3:
			Double[] req3 = {0.3,6.0,2.0};
			lamda = new Vector<>(Arrays.asList(req3));
			break;
		case 4:
			Double[] req4 = {0.2,5.0,1.2};
			lamda = new Vector<>(Arrays.asList(req4));
			break;
		case 5:
			Double[] req5 = {0.1,3.0,5.0};
			lamda = new Vector<>(Arrays.asList(req5));
			break;			
		default:
			break;
		}
        
    }
//    public Function(Vector<Integer> hso, int id) {//khoi tao random function
//        if ( id <=0 ) throw new RuntimeException("Number of vertices must be nonnegative");
//        this.id = id; 
//        for (int i=0;i<3;i++)
//        {
//        	double temp= UtilizeFunction.randDouble(hso.get(i));
//        	lamda.addElement(temp);
//        } 
//    }
    public Function(Vector<Double> hso, int id) {//khoi tao random function
        if ( id <=0 ) throw new RuntimeException("Number of vertices must be nonnegative");
        this.id = id; 
        for (int i=0;i<3;i++)
        {
        	//double temp= UtilizeFunction.randDouble(1,hso.get(i));
        	lamda.addElement(hso.get(i));
        } 
        
    }

    public Function(int id, double bw) {//gan id va bw cho 1 function
        this(id);
        this.bw = bw;        
    }
    public Function(int _id, Vector<Double> _lamda) {//gan id va bw cho 1 function
        id=_id;
        for (int i=0;i<3;i++)
        {
        	lamda.addElement(_lamda.get(i));
        }       
    }

    // number of vertices and edges
    public int id() { return id; }
    public double bw() {
    	return bw;}
    public Vector<Double> getLamda()
    {
    	return lamda;
    }



    // string representation of Graph - takes quadratic time
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(id + ": " + bw);
        return s.toString();
    }


    // test client
    public static void main(String[] args) {
        //int id = Integer.parseInt(args[0]);
    	int id = UtilizeFunction.randInt(1, 9);
        Function f = new Function(id);
        //Graph G = new Graph(V, E);
        System.out.print(f.toString());
        //StdOut.println(G);
    }

}