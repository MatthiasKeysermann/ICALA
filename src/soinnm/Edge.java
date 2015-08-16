package soinnm;

import soinnm.Node;

/**
 * Class for an edge in the topology.
 * <p>
 * An edge stores an id to identify it, has an age, and stores the connected
 * nodes.
 * 
 * @author Matthias Keysermann
 *
 */
public class Edge {

	private long id;
	private long age;
	private Node nodeA;
	private Node nodeB;

	public Edge(long id, Node nodeA, Node nodeB) {

		// set id
		this.id = id;

		// set connected nodes
		this.nodeA = nodeA;
		this.nodeB = nodeB;

		// initialise age
		age = 0;

	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getAge() {
		return age;
	}

	public void setAge(long age) {
		this.age = age;
	}

	public Node getNodeA() {
		return nodeA;
	}

	public void setNodeA(Node nodeA) {
		this.nodeA = nodeA;
	}

	public Node getNodeB() {
		return nodeB;
	}

	public void setNodeB(Node nodeB) {
		this.nodeB = nodeB;
	}

}
