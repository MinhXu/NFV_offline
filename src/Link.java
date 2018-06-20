


public class Link {
	private String name;
	private Node node1;
	private Node node2;
    private double capacity;
    private double cost;
    
    // empty graph with V vertices
    public Link(String _name,Node _node1, Node _node2, double  _capacity, double _cost) {//khoi tao random function
        name=_name;
    	node1= new Node(_node1.getName(),_node1.getX(),_node1.getY());
        node2= new Node(_node2.getName(),_node2.getX(),_node2.getY());
        capacity = _capacity;
        cost= _cost;
    }   

    // number of vertices and edges
    public String getName() { return name; }
    public Node getNode1() { return node1; }
    public Node getNode2() {
    	return node2;}
    public double getCapacity()
    {
    	return capacity;
    }
    public double getCost()
    {
    	return cost;
    }


    // test client
    public static void main(String[] args) {
    }

}