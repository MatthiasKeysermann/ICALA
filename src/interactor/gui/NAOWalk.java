package interactor.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

import soinnm.Cluster;
import soinnm.Edge;
import soinnm.Node;
import soinnm.SOINNM;
import interactor.InteractorUDP;

import com.aldebaran.proxy.*;

public class NAOWalk extends InteractorUDP implements KeyListener {

	private static final String NAME = "NAOWalk";

	private String host = "localhost";
	private int port = 9559;

	private ALTextToSpeechProxy tts;
	private ALMotionProxy motion;
	private static final double VALUE_MIN = -1.2; // required for display
	private static final double VALUE_MAX = +1.2; // required for display		
	private boolean legsPresent = false;
	private double moveForward;
	private double moveBackward;
	private double moveLeft;
	private double moveRight;
	private boolean turning = true;
	private float moveThreshold = 0.2f;
	private double activationOutputThreshold = 0.01; // activation threshold for successful recall

	private static final int INPUT_DIM = 2;
	private double[] patternInput;
	private double[] patternOutput;
	private static final long CYCLE_TIME = 800;

	private JFrame frame;
	private JPanel panel;
	private JPanel pnControls;
	private JCheckBox cbLearn;
	private JCheckBox cbRecall;
	private JFileChooser fcSaveLoad;
	private JPanel pnTopology;
	private JPanel pnMovement;

	public NAOWalk() {
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
		tts = new ALTextToSpeechProxy(host, port);
		motion = new ALMotionProxy(host, port);

		// check for legs
		Variant varRobotConfig = motion.getRobotConfig();
		Variant varKeys = varRobotConfig.getElement(0);
		Variant varValues = varRobotConfig.getElement(1);
		for (int i = 0; i < varKeys.getSize(); i++) {
			String key = varKeys.getElement(i).toString();
			if (key.equals("Legs")) {
				boolean value = varValues.getElement(i).toBoolean();
				System.out.println("legs = " + value);
				legsPresent = value;
				break;
			}
		}

		// DEBUG
		//legsPresent = false;

		// initialise joints
		if (legsPresent) {

			// set stiffnesses
			motion.setStiffnesses(new Variant("Body"), new Variant(1.0f));

			// init head
			String[] names = { "HeadYaw", "HeadPitch" };
			float[] angles = { 0.0f, 0.0f };
			float[] times = { 1.0f, 1.0f };
			motion.angleInterpolation(new Variant(names), new Variant(angles), new Variant(times), true);

			// go into initial walk position
			tts.say("Going into walk position.");
			motion.walkInit();

		}

		// set interactor parameters
		setCycleTime(CYCLE_TIME);
		setInnerFeedback(false);
		setActivateClusterThreshold(2);
		setIterationCountStop(0);

		// set topology parameters
		SOINNM topology = getTopology();
		topology.setNoiseLevel(0.0);
		topology.setUseFixedThreshold(false);
		topology.setFixedThreshold(0.1);
		topology.setAgeDead(1000);
		topology.setConnectNewNodes(false);
		topology.setLambda(50);
		topology.setEdgeMaxRemoval(false);
		topology.setNodeNumSignalsMinRemoval(false);
		topology.setReduceErrorInsertion(false);
		topology.setSmallClusterRemoval(false);
		topology.setC2Param(0.001);
		topology.setC1Param(0.1);
		topology.setClusterJoining(false);
		topology.setJoinTolerance(1.0);
		topology.setUseAbsoluteJoinTolerance(false);
		topology.setJoinToleranceAbsolute(0.1);
		topology.setJoiningIterationsMax(5);

		// create frame
		frame = new JFrame(NAME);
		frame.setSize(600, 600);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				shutdown();
			}
		});
		frame.setLocation(600, 0);
		frame.setVisible(true);
		frame.addKeyListener(this);
		frame.setFocusable(true);

		// create panel
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		frame.add(panel);

		// create panel for controls
		pnControls = new JPanel();
		pnControls.setBorder(BorderFactory.createTitledBorder("Controls"));

		cbLearn = new JCheckBox("Learn");
		cbLearn.setSelected(true);
		pnControls.add(cbLearn);

		cbRecall = new JCheckBox("Recall");
		cbRecall.setSelected(true);
		pnControls.add(cbRecall);

		fcSaveLoad = new JFileChooser();
		fcSaveLoad.setSelectedFile(new File(System.getProperty("user.dir") + "/" + NAME + ".xml"));

		JButton btSave = new JButton("Save topology");
		btSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fcSaveLoad.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
					fileSave = fcSaveLoad.getSelectedFile();
				}
			}
		});
		pnControls.add(btSave);

		JButton btLoad = new JButton("Load topology");
		btLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fcSaveLoad.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					fileLoad = fcSaveLoad.getSelectedFile();
				}
			}
		});
		pnControls.add(btLoad);

		JButton btInsert = new JButton("Insert topology");
		btInsert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (fcSaveLoad.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					fileInsert = fcSaveLoad.getSelectedFile();
				}
			}
		});
		pnControls.add(btInsert);

		panel.add(pnControls, BorderLayout.PAGE_START);

		// create panel for topology
		pnTopology = new JPanel();
		pnTopology.setBorder(BorderFactory.createTitledBorder("Topology"));
		panel.add(pnTopology, BorderLayout.CENTER);

		// create panel for movement
		pnMovement = new JPanel();
		pnMovement.setBorder(BorderFactory.createTitledBorder("Movement"));
		pnMovement.setLayout(new GridLayout(3, 3));

		// create buttons

		JButton btForwardLeft = new JButton("Forward Left");
		btForwardLeft.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveLeft = 1;
				moveRight = 0;
				moveForward = 1;
				moveBackward = 0;
			}
		});
		pnMovement.add(btForwardLeft);

		JButton btForward = new JButton("Forward");
		btForward.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveLeft = 0;
				moveRight = 0;
				moveForward = 1;
				moveBackward = 0;
			}
		});
		pnMovement.add(btForward);

		JButton btForwardRight = new JButton("Forward Right");
		btForwardRight.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveLeft = 0;
				moveRight = 1;
				moveForward = 1;
				moveBackward = 0;
			}
		});
		pnMovement.add(btForwardRight);

		JButton btLeft = new JButton("Left");
		btLeft.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveLeft = 1;
				moveRight = 0;
				moveForward = 0;
				moveBackward = 0;
			}
		});
		pnMovement.add(btLeft);

		JButton btStop = new JButton("Stop");
		btStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveLeft = 0;
				moveRight = 0;
				moveForward = 0;
				moveBackward = 0;
			}
		});
		pnMovement.add(btStop);

		JButton btRight = new JButton("Right");
		btRight.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveLeft = 0;
				moveRight = 1;
				moveForward = 0;
				moveBackward = 0;
			}
		});
		pnMovement.add(btRight);

		JButton btBackwardLeft = new JButton("Backward Left");
		btBackwardLeft.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveLeft = 1;
				moveRight = 0;
				moveForward = 0;
				moveBackward = 1;
			}
		});
		pnMovement.add(btBackwardLeft);

		JButton btBackward = new JButton("Backward");
		btBackward.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveLeft = 0;
				moveRight = 0;
				moveForward = 0;
				moveBackward = 1;
			}
		});
		pnMovement.add(btBackward);

		JButton btBackwardRight = new JButton("Backward Right");
		btBackwardRight.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveLeft = 0;
				moveRight = 1;
				moveForward = 0;
				moveBackward = 1;
			}
		});
		pnMovement.add(btBackwardRight);

		panel.add(pnMovement, BorderLayout.PAGE_END);

		// update panel		
		panel.updateUI();
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case 37: // left
		case 65: // a
			moveLeft = 1;
			break;
		case 38: // up
		case 87: // w
			moveForward = 1;
			break;
		case 39: // right
		case 68: // d
			moveRight = 1;
			break;
		case 40: // down
		case 83: // s
			moveBackward = 1;
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		case 37: // left
		case 65: // a
			moveLeft = 0;
			break;
		case 38: // up
		case 87: // w
			moveForward = 0;
			break;
		case 39: // right
		case 68: // d
			moveRight = 0;
			break;
		case 40: // down
		case 83: // s
			moveBackward = 0;
			break;
		}
	}

	@Override
	protected void shutdown() {

		// pause interactor
		pause();

		// reset joints
		if (legsPresent) {

			// stop walk			
			motion.stopWalk();
			motion.waitUntilWalkIsFinished();

			// go into crouch position
			tts.say("Going into crouch position.");
			motion.setStiffnesses(new Variant("Body"), new Variant(1.0f));
			crouch();

			// release stiffnesses			
			motion.setStiffnesses(new Variant("Body"), new Variant(0.0f));

		}

		System.exit(0);
	}

	@Override
	protected double[] readInput() {

		// create pattern
		double[] pattern = new double[INPUT_DIM];
		pattern[0] = moveRight - moveLeft;
		pattern[1] = moveBackward - moveForward;

		// set input pattern
		patternInput = pattern;

		if (!cbLearn.isSelected()) {
			return null;
		}
		return pattern;
	}

	@Override
	protected void writeOutput(double[] pattern) {

		// set output pattern
		patternOutput = pattern;

		if (!cbRecall.isSelected()) {

			if (legsPresent) {
				motion.stopWalk();
			}

		} else {

			if (patternOutput != null) {

				if (getActivationOutput() < activationOutputThreshold) {

					System.out.println("activation of output is too low " + String.format("(%.3f < %.3f)", getActivationOutput(), activationOutputThreshold));
					if (legsPresent) {
						motion.stopWalk();
					}
				} else {

					if (legsPresent) {

						// read pattern
						float x = (float) -pattern[1];
						float y = (float) -pattern[0];
						float theta = 0.0f;

						// turn
						if (turning) {
							/*
							theta = x * y;
							y = 0;
							*/
							//
							theta = y * 0.4f;
							y = 0;
						}

						// move
						if (Math.abs(x) > moveThreshold || Math.abs(y) > moveThreshold || Math.abs(theta) > moveThreshold) {
							//motion.walkTo(x / 10, y / 10, theta);
							motion.setWalkTargetVelocity(x, y, theta, 0.5f);
							System.out.println("Moving to " + String.format("x=%.1f y=%.1f theta=%.1f", x, y, theta));
						} else {
							motion.stopWalk();
							System.out.println("below move threshold of " + String.format("%.1f", moveThreshold));
						}

					}

				}

			}

		}

	}

	@Override
	protected void updateUI() {

		// focus on frame for handling key events
		frame.requestFocusInWindow();

		/*
		// topology status
		System.out.println("Topology has");
		System.out.println("  " + getTopology().getNodeSet().size() + " nodes");
		System.out.println("  " + getTopology().getEdgeSet().size() + " edges");
		System.out.println("  " + getTopology().getClusterSet().size() + " clusters");
		System.out.println();
		*/

		synchronized (pnTopology) {

			// initialise
			Insets insets = pnTopology.getBorder().getBorderInsets(pnTopology);
			int width = pnTopology.getWidth() - insets.left - insets.right;
			int height = pnTopology.getHeight() - insets.top - insets.bottom;
			Graphics g = pnTopology.getGraphics().create(insets.left, insets.top, width, height);
			FontMetrics metrics = g.getFontMetrics();
			int textHeight = metrics.getHeight();
			int textWidth;
			int xPos, yPos;
			double[] pattern;
			double[] patternNorm;

			// clear panel
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, width, height);

			// draw grid and labels
			g.setColor(Color.DARK_GRAY);
			g.drawLine(0, height / 2, width - 1, height / 2);
			g.drawLine(width / 2, 0, width / 2, height - 1);
			String strLeft = "Left";
			if (turning) {
				strLeft = "Turn Left";
			}
			textWidth = metrics.stringWidth(strLeft);
			g.drawString(strLeft, (int) Math.round(width * 0.1), (int) Math.round(height * 0.5));
			String strRight = "Right";
			if (turning) {
				strRight = "Turn Right";
			}
			textWidth = metrics.stringWidth(strRight);
			g.drawString(strRight, (int) Math.round(width * 0.9) - textWidth, (int) Math.round(height * 0.5));
			String strForward = "Forward";
			textWidth = metrics.stringWidth(strForward);
			g.drawString(strForward, (int) Math.round(width * 0.5) - textWidth / 2, (int) Math.round(height * 0.1));
			String strBackward = "Backward";
			textWidth = metrics.stringWidth(strBackward);
			g.drawString(strBackward, (int) Math.round(width * 0.5) - textWidth / 2, (int) Math.round(height * 0.9));

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
			patternNorm[i] = (pattern[i] - VALUE_MIN) / (VALUE_MAX - VALUE_MIN);
		}
		return patternNorm;
	}

	private void crouch() {

		float time = 1.0f;

		String[] namesArray = { "HeadYaw", "HeadPitch", "LShoulderPitch", "LShoulderRoll", "LElbowYaw", "LElbowRoll", "RShoulderPitch", "RShoulderRoll", "RElbowYaw", "RElbowRoll", "LHipYawPitch",
				"LHipRoll", "LHipPitch", "LKneePitch", "LAnklePitch", "LAnkleRoll", "RHipRoll", "RHipPitch", "RKneePitch", "RAnklePitch", "RAnkleRoll" };
		float[] keysArray = { -0.03379f, 0.32823f, 1.62600f, -0.01998f, -0.98640f, -1.24250f, 1.51870f, -0.05680f, 0.72554f, 1.26099f, 0.01538f, -0.00456f, -0.90962f, 2.11255f, -1.18889f, 0.00464f,
				0.10589f, -0.90203f, 2.11255f, -1.18630f, -0.10427f };
		float[] timesArray = { time, time, time, time, time, time, time, time, time, time, time, time, time, time, time, time, time, time, time, time, time };

		Variant namesVar = new Variant(namesArray);
		Variant keysVar = new Variant(keysArray);
		Variant timesVar = new Variant(timesArray);

		motion.angleInterpolation(namesVar, keysVar, timesVar, true);

		try {
			Thread.sleep(Math.round(time * 1000));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		NAOWalk interactor = new NAOWalk();
		interactor.run();
	}

}
