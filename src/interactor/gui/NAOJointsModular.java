package interactor.gui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
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

import com.aldebaran.proxy.ALMotionProxy;
import com.aldebaran.proxy.Variant;

import interactor.InteractorUDP;

public class NAOJointsModular {

	private class RShoulder extends Joint2D {

		private final static String NAME = "RShoulder";
		private final String[] names = { "RShoulderRoll", "RShoulderPitch" };
		private final float[] stiffnesses = { 0.3f, 0.5f };
		private final static float FRACTION_MAX_SPEED = 0.1f;
		private final float[] anglesMin = { -1.4f, -2.1f }; // required for display
		private final float[] anglesMax = { +0.4f, +2.1f }; // required for display
		private final static boolean INTERLEAVING = true;

		public RShoulder(JPanel panel) {
			super(NAME, panel);
			setParameters(names, stiffnesses, FRACTION_MAX_SPEED, anglesMin, anglesMax, INTERLEAVING);
		}

	}

	private class LShoulder extends Joint2D {

		private final static String NAME = "LShoulder";
		private final String[] names = { "LShoulderRoll", "LShoulderPitch" };
		private final float[] stiffnesses = { 0.3f, 0.5f };
		private final static float FRACTION_MAX_SPEED = 0.1f;
		private final float[] anglesMin = { -0.4f, -2.1f }; // required for display
		private final float[] anglesMax = { +1.4f, +2.1f }; // required for display
		private final static boolean INTERLEAVING = true;

		public LShoulder(JPanel panel) {
			super(NAME, panel);
			setParameters(names, stiffnesses, FRACTION_MAX_SPEED, anglesMin, anglesMax, INTERLEAVING);
		}

	}

	private class RElbow extends Joint2D {

		private final static String NAME = "RElbow";
		private final String[] names = { "RElbowRoll", "RElbowYaw" };
		private final float[] stiffnesses = { 0.2f, 0.1f };
		private final static float FRACTION_MAX_SPEED = 0.1f;
		private final float[] anglesMin = { +0.0f, -2.1f }; // required for display
		private final float[] anglesMax = { +1.6f, +2.1f }; // required for display
		private final static boolean INTERLEAVING = true;

		public RElbow(JPanel panel) {
			super(NAME, panel);
			setParameters(names, stiffnesses, FRACTION_MAX_SPEED, anglesMin, anglesMax, INTERLEAVING);
		}

	}

	private class LElbow extends Joint2D {

		private final static String NAME = "LElbow";
		private final String[] names = { "LElbowRoll", "LElbowYaw" };
		private final float[] stiffnesses = { 0.2f, 0.1f };
		private final static float FRACTION_MAX_SPEED = 0.1f;
		private final float[] anglesMin = { -1.6f, -2.1f }; // required for display
		private final float[] anglesMax = { -0.0f, +2.1f }; // required for display
		private final static boolean INTERLEAVING = true;

		public LElbow(JPanel panel) {
			super(NAME, panel);
			setParameters(names, stiffnesses, FRACTION_MAX_SPEED, anglesMin, anglesMax, INTERLEAVING);
		}

	}

	private class Joint2D extends InteractorUDP {

		private String host = "localhost";
		private int port = 9559;

		private ALMotionProxy motion;
		private String[] names;
		private float[] stiffnesses;
		private float fractionMaxSpeed;
		private float[] anglesMin;
		private float[] anglesMax;
		private boolean interleaving;

		private final float[] stiffnessesWeak = { 0.2f, 0.2f };
		private final float[] stiffnessesStrong = { 0.6f, 0.6f };
		private final float fractionMaxSpeedStrong = 0.1f;
		private final static long FORCE_TIME = 100; // milliseconds

		private final static int INPUT_DIM = 2;
		private double[] patternInput;
		private double[] patternOutput;
		private final static long CYCLE_TIME = 500; // milliseconds

		private JPanel panel;

		public Joint2D(String NAME, JPanel panel) {
			super(NAME, INPUT_DIM);
			this.panel = panel;

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
			// motionProxy.setSmartStiffnessEnabled(True);

			// initialise interactor
			setCycleTime(CYCLE_TIME);

		}

		public void setParameters(String[] names, float[] stiffnesses, float fractionMaxSpeed, float[] ANGLES_MIN, float[] ANGLES_MAX, boolean INTERLEAVING) {
			this.names = names;
			this.stiffnesses = stiffnesses;
			motion.setStiffnesses(new Variant(names), new Variant(stiffnesses));
			this.fractionMaxSpeed = fractionMaxSpeed;
			this.anglesMin = ANGLES_MIN;
			this.anglesMax = ANGLES_MAX;
			this.interleaving = INTERLEAVING;
		}

		@Override
		protected void shutdown() {

			// release stiffnesses
			stiffnesses = new float[stiffnesses.length];
			motion.setStiffnesses(new Variant(names), new Variant(stiffnesses));

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

				if (interleaving) {

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

				// clear pnLShoulder
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
				patternNorm[i] = (pattern[i] - anglesMin[i]) / (anglesMax[i] - anglesMin[i]);
			}
			return patternNorm;
		}

	}

	private Joint2D rShoulder;
	private Joint2D lShoulder;
	private Joint2D rElbow;
	private Joint2D lElbow;

	public NAOJointsModular() {

		// create frame
		JFrame frame = new JFrame("NAOJointsModular");
		frame.setSize(800, 800);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				shutdown();
			}
		});
		frame.setVisible(true);

		// set layout
		frame.setLayout(new GridLayout(2, 2));

		// create panel RShoulder
		JPanel pnRShoulder = new JPanel();
		pnRShoulder.setBorder(BorderFactory.createTitledBorder("RShoulder"));
		frame.add(pnRShoulder);

		// create panel LShoulder
		JPanel pnLShoulder = new JPanel();
		pnLShoulder.setBorder(BorderFactory.createTitledBorder("LShoulder"));
		frame.add(pnLShoulder);

		// create panel RElbow
		JPanel pnRElbow = new JPanel();
		pnRElbow.setBorder(BorderFactory.createTitledBorder("RElbow"));
		frame.add(pnRElbow);

		// create panel LElbow
		JPanel pnLElbow = new JPanel();
		pnLElbow.setBorder(BorderFactory.createTitledBorder("LElbow"));
		frame.add(pnLElbow);

		// create thread RShoulder
		rShoulder = new RShoulder(pnRShoulder);
		new Thread(rShoulder).start();

		// create thread LShoulder
		lShoulder = new LShoulder(pnLShoulder);
		new Thread(lShoulder).start();

		// create thread RElbow
		rElbow = new RElbow(pnRElbow);
		new Thread(rElbow).start();

		// create thread LElbow
		lElbow = new LElbow(pnLElbow);
		new Thread(lElbow).start();

	}

	public void shutdown() {

		// RShoulder
		System.out.print("Shutting down RShoulder...");
		rShoulder.shutdown();
		System.out.println("OK");

		// LShoulder
		System.out.print("Shutting down LShoulder...");
		lShoulder.shutdown();
		System.out.println("OK");

		// RElbow
		System.out.print("Shutting down RElbow...");
		rElbow.shutdown();
		System.out.println("OK");

		// LElbow
		System.out.print("Shutting down LElbow...");
		lElbow.shutdown();
		System.out.println("OK");

		System.exit(0);
	}

	public static void main(String[] args) {
		new NAOJointsModular();
	}

}
