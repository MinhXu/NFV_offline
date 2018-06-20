


public class Node {
	private String name;
	private double x;
    private double y;
    
    // empty graph with V vertices
    public Node(String _name,double _x, double _y) {//khoi tao random function
        name=_name;
    	x=_x;
        y=_y;
    }
    public Node(){
    	name="";
    	x=0;
    	y=0;
    }

    

    // number of vertices and edges
    public double getX() { return x; }
    public double getY() {
    	return y;}
    public String getName(){
    	return name;
    }



    // string representation of Graph - takes quadratic time
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(x + ": " + y);
        return s.toString();
    }


    // test client
    public static void main(String[] args) {
    }

}