package soinnm.test;

import static org.junit.Assert.*;

import org.junit.Test;

import soinnm.Cluster;
import soinnm.SOINNM;

public class SOINNMTest {

	@Test
	public void testInput() {

		// create empty topology
		SOINNM soinnm = new SOINNM(2);

		// input patterns
		double[] pattern1 = { 0.1, 0.1 };
		double[] pattern2 = { 0.2, 0.2 };
		double[] pattern3 = { 0.3, 0.3 };
		soinnm.input(pattern1);
		soinnm.input(pattern2);
		soinnm.input(pattern3);
		soinnm.input(pattern1);
		soinnm.input(pattern2);
		soinnm.input(pattern3);
		soinnm.input(pattern1);
		soinnm.input(pattern2);
		soinnm.input(pattern3);

		// input patterns
		double[] pattern7 = { 0.7, 0.7 };
		double[] pattern8 = { 0.8, 0.8 };
		double[] pattern9 = { 0.9, 0.9 };
		soinnm.input(pattern7);
		soinnm.input(pattern8);
		soinnm.input(pattern9);
		soinnm.input(pattern7);
		soinnm.input(pattern8);
		soinnm.input(pattern9);
		soinnm.input(pattern7);
		soinnm.input(pattern8);
		soinnm.input(pattern9);

		// check number of nodes, edges, clusters
		assertNotSame("number of nodes", 0, soinnm.getNodeSet().size());
		assertNotSame("number of edges", 0, soinnm.getEdgeSet().size());
		assertNotSame("number of clusters", 0, soinnm.getClusterSet().size());

	}

	@Test
	public void testInputLabelled() {

		// initialise
		int iterations = 10;

		// create empty topology
		SOINNM soinnm = new SOINNM(2);

		// set age dead much higher than iterations
		soinnm.setAgeDead(100);

		// set lambda much higher than iterations
		soinnm.setLambda(100);

		// input patterns
		String labelA = "A";
		double[] pattern1 = { 0.1, 0.1 };
		double[] pattern2 = { 0.2, 0.2 };
		double[] pattern3 = { 0.3, 0.3 };
		for (int i = 0; i < iterations; i++) {
			soinnm.input(pattern1, labelA);
			soinnm.input(pattern2, labelA);
			soinnm.input(pattern3, labelA);
		}

		// input patterns
		String labelB = "B";
		double[] pattern7 = { 0.7, 0.7 };
		double[] pattern8 = { 0.8, 0.8 };
		double[] pattern9 = { 0.9, 0.9 };
		for (int i = 0; i < iterations; i++) {
			soinnm.input(pattern7, labelB);
			soinnm.input(pattern8, labelB);
			soinnm.input(pattern9, labelB);
		}

		// count cluster occurrences
		int occurrencesA = 0;
		int occurrencesB = 0;
		for (Cluster cluster : soinnm.getClusterSet()) {
			if (cluster.getLabel().equals(labelA)) {
				occurrencesA++;
			}
			if (cluster.getLabel().equals(labelB)) {
				occurrencesB++;
			}
		}

		// check number of occurrences
		assertNotSame("occurrences of labelA", 0, occurrencesA);
		assertNotSame("occurrences of labelB", 0, occurrencesB);

	}

	@Test
	public void testClear() {

		// create empty topology
		SOINNM soinnm = new SOINNM(2);

		// input patterns
		double[] pattern1 = { 0.1, 0.1 };
		double[] pattern2 = { 0.2, 0.2 };
		double[] pattern3 = { 0.3, 0.3 };
		soinnm.input(pattern1);
		soinnm.input(pattern2);
		soinnm.input(pattern3);
		soinnm.input(pattern1);
		soinnm.input(pattern2);
		soinnm.input(pattern3);
		soinnm.input(pattern1);
		soinnm.input(pattern2);
		soinnm.input(pattern3);

		// input patterns
		double[] pattern7 = { 0.7, 0.7 };
		double[] pattern8 = { 0.8, 0.8 };
		double[] pattern9 = { 0.9, 0.9 };
		soinnm.input(pattern7);
		soinnm.input(pattern8);
		soinnm.input(pattern9);
		soinnm.input(pattern7);
		soinnm.input(pattern8);
		soinnm.input(pattern9);
		soinnm.input(pattern7);
		soinnm.input(pattern8);
		soinnm.input(pattern9);

		// check number of nodes, edges, clusters
		assertNotSame("number of nodes", 0, soinnm.getNodeSet().size());
		assertNotSame("number of edges", 0, soinnm.getEdgeSet().size());
		assertNotSame("number of clusters", 0, soinnm.getClusterSet().size());

		// clear topology
		soinnm.clear();

		// check number of nodes, edges, clusters
		assertEquals("number of nodes", 0, soinnm.getNodeSet().size());
		assertEquals("number of edges", 0, soinnm.getEdgeSet().size());
		assertEquals("number of clusters", 0, soinnm.getClusterSet().size());

	}

}
