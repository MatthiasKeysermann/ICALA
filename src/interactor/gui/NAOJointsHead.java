package interactor.gui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import soinnm.Cluster;
import soinnm.Edge;
import soinnm.Node;
import interactor.InteractorUDP;

import com.aldebaran.proxy.*;

public class NAOJointsHead extends InteractorUDP {

	private static final String NAME = "NAOJointsHead";

	private String host = "localhost";
	private int port = 9559;

	private ALMotionProxy motion;
	private String[] names = { "HeadYaw", "HeadPitch" };
	private float[] stiffnesses = { 0.1f, 0.4f };
	private float fractionMaxSpeed = 0.1f;
	//private static final float[] ANGLES_MIN = { -2.0857f, -0.6720f }; // required for display
	//private static final float[] ANGLES_MAX = { +2.0857f, +0.5149f }; // required for display
	private static final float[] ANGLES_MIN = { -2.1f, -0.7f }; // required for display
	private static final float[] ANGLES_MAX = { +2.1f, +0.6f }; // required for display
	private final static boolean INTERLEAVING = true;
	private float[] stiffnessesWeak = { 0.1f, 0.1f };
	private float[] stiffnessesStrong = { 0.1f, 0.4f };
	private float fractionMaxSpeedStrong = 0.1f;
	private long FORCE_TIME = 100; // milliseconds

	private static final int INPUT_DIM = 2;
	private double[] patternInput;
	private double[] patternOutput;
	private static final long CYCLE_TIME = 500;

	private JFrame frame;
	private JPanel panel;

	public NAOJointsHead() {
		super(NAME, INPUT_DIM);

		// initialise NAO
		try {
			BufferedReader bReader = new BufferedReader(new FileReader("NAOHostPort.txt"));
			host = bReader.readLine().trim();
			port = Integer.parseInt(bReader.readLine().trim());
			bReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		motion = new ALMotionProxy(host, port);
		motion.setStiffnesses(new Variant(names), new Variant(stiffnesses));

		// initialise interactor
		setCycleTime(CYCLE_TIME);

		// create frame
		frame = new JFrame(NAME);
		frame.setSize(400, 400);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				shutdown();
			}
		});
		frame.setVisible(true);

		// create panel
		panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Head"));
		frame.add(panel);

	}

	@Override
	protected void shutdown() {

		// release stiffnesses
		stiffnesses = new float[stiffnesses.length];
		motion.setStiffnesses(new Variant(names), new Variant(stiffnesses));

		System.exit(0);
	}

	@Override
	protected double[] readInput() {

		// read angles
		float[] angles = motion.getAngles(new Variant(names), true);
		double[] pattern = { angles[0], angles[1] };

		// set input pattern
		patternInput = pattern;

		return pattern;
	}

	@Override
	protected void writeOutput(double[] pattern) {
		if (pattern != null) {

			if (INTERLEAVING) {

				// set stiffnesses to strong
				motion.setStiffnesses(new Variant(names), new Variant(stiffnessesStrong));

				// set angles
				float[] angles = { (float) pattern[0], (float) pattern[1] };
				motion.setAngles(new Variant(names), new Variant(angles), fractionMaxSpeedStrong);

				// wait
				try {
					Thread.sleep(FORCE_TIME);
				} catch (Exception e) {
					e.printStackTrace();
				}

				// set stiffnesses to weak
				motion.setStiffnesses(new Variant(names), new Variant(stiffnessesWeak));

			} else {

				// set angles
				float[] angles = { (float) pattern[0], (float) pattern[1] };
				motion.setAngles(new Variant(names), new Variant(angles), fractionMaxSpeed);
			}
			// set output pattern
			patternOutput = pattern;

		}
	}

	@Override
	protected void updateUI() {
		synchronized (panel) {

			// initialise
			Insets insets = panel.getBorder().getBorderInsets(panel);
			int width = panel.getWidth() - insets.left - insets.right;
			int height = panel.getHeight() - insets.top - insets.bottom;
			Graphics g = panel.getGraphics().create(insets.left, insets.top, width, height);
			int xPos, yPos;
			double[] pattern;
			double[] patternNorm;

			// clear panel
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, width, height);

			// draw grid
			g.setColor(Color.DARK_GRAY);
			g.drawLine(width / 2, 0, width / 2, height - 1);
			g.drawLine(0, height / 2, width - 1, height / 2);

			// draw nodes
			g.setColor(Color.RED);
			int diameterNode = 8;
			for (Node node : getTopology().getNodeSet()) {
				pattern = node.getPattern();
				patternNorm = normalisePattern(pattern);
				xPos = (int) Math.round(patternNorm[0] * width);
				yPos = (int) Math.round(patternNorm[1] * height);
				g.fillOval(xPos, yPos, diameterNode, diameterNode);
			}

			// draw edges
			g.setColor(Color.BLUE);
			int xPosA, yPosA, xPosB, yPosB;
			for (Edge edge : getTopology().getEdgeSet()) {
				pattern = edge.getNodeA().getPattern();
				patternNorm = normalisePattern(pattern);
				xPosA = (int) Math.round(patternNorm[0] * width);
				yPosA = (int) Math.round(patternNorm[1] * height);
				pattern = edge.getNodeB().getPattern();
				patternNorm = normalisePattern(pattern);
				xPosB = (int) Math.round(patternNorm[0] * width);
				yPosB = (int) Math.round(patternNorm[1] * height);
				g.drawLine(xPosA, yPosA, xPosB, yPosB);
			}

			// draw clusters
			g.setColor(Color.GREEN);
			int diameterCluster = 22;
			FontMetrics metrics = g.getFontMetrics();
			int textHeight = metrics.getHeight();
			int textWidth;
			for (Cluster cluster : getTopology().getClusterSet()) {
				// only clusters with more than one node
				if (cluster.getNodes().size() >= getActivateClusterThreshold()) {
					// highlight activated cluster
					if (cluster == getTopology().getActivatedCluster()) {
						diameterCluster = 36;
						g.setColor(Color.WHITE);
					} else {
						diameterCluster = 22;
						g.setColor(Color.GRAY);
					}
					pattern = cluster.getMean();
					patternNorm = normalisePattern(pattern);
					xPos = (int) Math.round(patternNorm[0] * width);
					yPos = (int) Math.round(patternNorm[1] * height);
					g.drawOval(xPos - diameterCluster / 2, yPos - diameterCluster / 2, diameterCluster, diameterCluster);
					String strId = String.valueOf(cluster.getId());
					textWidth = metrics.stringWidth(strId);
					g.drawString(strId, xPos - textWidth / 2, yPos + textHeight / 2);
				}
			}

			// draw input
			if (patternInput != null) {
				patternNorm = normalisePattern(patternInput);
				xPos = (int) Math.round(patternNorm[0] * width);
				yPos = (int) Math.round(patternNorm[1] * height);
				g.setColor(Color.WHITE);
				g.fillOval(xPos, yPos, diameterNode, diameterNode);
			}

			// draw output
			if (patternOutput != null) {
				patternNorm = normalisePattern(patternOutput);
				xPos = (int) Math.round(patternNorm[0] * width);
				yPos = (int) Math.round(patternNorm[1] * height);
				g.setColor(Color.GREEN);
				g.fillRect(xPos, yPos, diameterNode, diameterNode);
			}

		}
	}

	private double[] normalisePattern(double[] pattern) {
		double[] patternNorm = new double[pattern.length];
		for (int i = 0; i < patternNorm.length; i++) {
			patternNorm[i] = (pattern[i] - ANGLES_MIN[i]) / (ANGLES_MAX[i] - ANGLES_MIN[i]);
		}
		return patternNorm;
	}

	public static void main(String[] args) {
		NAOJointsHead interactor = new NAOJointsHead();
		interactor.run();
	}

}
