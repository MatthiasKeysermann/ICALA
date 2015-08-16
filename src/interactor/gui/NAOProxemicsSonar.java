package interactor.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

import soinnm.Cluster;
import soinnm.Edge;
import soinnm.Node;
import soinnm.SOINNM;
import interactor.InteractorUDP;

import com.aldebaran.proxy.*;

public class NAOProxemicsSonar extends InteractorUDP {

	private static final String NAME = "NAOProxemicsSonar";

	private String host = "localhost";
	private int port = 9559;

	private ALTextToSpeechProxy tts;
	private boolean speakDistance = false;
	private boolean speakDesired = false;
	private boolean speakMovement = false;
	private ALSonarProxy sonar;
	//private static final double DISTANCE_MIN = 0.25; // required for display
	//private static final double DISTANCE_MAX = 2.55; // required for display	
	private static final double DISTANCE_MIN = 0.2; // required for display
	private static final double DISTANCE_MAX = 2.6; // required for display	
	private static final int NUM_GRID_LABELS = 24; // required for display
	private static final int sonarNumSamples = 5; // number of samples taken from sonar
	private static final long sonarInterval = 100; // sampling interval (ms)
	private ALMemoryProxy memory;
	private ALMotionProxy motion;
	private boolean legsPresent = false;
	private double ACTIVATION_OUTPUT_THRESHOLD = 0.001; // activation threshold for successful recall
	private long toleranceCm = 5; // movement tolerance in centimeters
	private float stepSize = 0.1f; // step size in meters
	private long SILENCING_TIME = 1000; // time to wait after movement (ms)

	private boolean learning = true;
	private boolean recalling = false;
	private static final int INPUT_DIM = 1;
	private double[] patternInput;
	private double[] patternOutput;
	private static final long CYCLE_TIME = 800;

	private JFrame frame;
	private JPanel panel;
	private JPanel pnControls;
	private JFileChooser fcSaveLoad;
	private JPanel pnTopology;

	public NAOProxemicsSonar() {
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
		sonar = new ALSonarProxy(host, port);
		sonar.subscribe(NAME);
		memory = new ALMemoryProxy(host, port);
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

		// initialise joints
		if (legsPresent) {

			// DEBUG
			System.out.println("Here is the problem with the sonar:");
			System.out.println("When setting the stiffnesses greater than 0, the motors");
			System.out.println("create a noise which in turn distorts the sonar reading.");

			// set stiffnesses
			motion.setStiffnesses(new Variant("Body"), new Variant(1.0f));

			// crouch sensing
			tts.say("Going into sensing position.");
			crouchSensing();

			// release stiffnesses
			motion.setStiffnesses(new Variant("Body"), new Variant(0.0f));

		}

		// set interactor parameters
		setCycleTime(CYCLE_TIME);
		setInnerFeedback(false);
		setActivateClusterThreshold(3);
		setIterationCountStop(0);

		// set topology parameters
		SOINNM topology = getTopology();
		topology.setNoiseLevel(0.02);
		topology.setUseFixedThreshold(true);
		topology.setFixedThreshold(0.05);
		topology.setAgeDead(1000);
		topology.setConnectNewNodes(false);
		topology.setLambda(50);
		topology.setEdgeMaxRemoval(false);
		topology.setNodeNumSignalsMinRemoval(false);
		topology.setReduceErrorInsertion(false);
		topology.setSmallClusterRemoval(true);
		topology.setC2Param(0.001);
		topology.setC1Param(0.1);
		topology.setClusterJoining(true);
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

		// create panel
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		frame.add(panel);

		// create panel for controls
		pnControls = new JPanel();
		pnControls.setBorder(BorderFactory.createTitledBorder("Controls"));

		JButton btMode = new JButton("Switch Mode");
		btMode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				switchMode();
			}
		});
		pnControls.add(btMode);

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

		// update panel		
		panel.updateUI();
	}

	@Override
	protected void shutdown() {

		// pause interactor
		pause();

		// unsubscribe
		sonar.unsubscribe(NAME);

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

		// check for shutdown		
		if (memory.getData("RearTactilTouched").toFloat() == 1.0f) {
			tts.say("I am stopping now.");
			shutdown();
		}

		// check for mode
		if (memory.getData("FrontTactilTouched").toFloat() == 1.0f) {
			switchMode();
		}

		// supersampling
		double distanceLeft = 0;
		double distanceRight = 0;
		for (int i = 0; i < sonarNumSamples; i++) {

			// read distances
			Variant varDistanceLeft = memory.getData("Device/SubDeviceList/US/Left/Sensor/Value");
			Variant varDistanceRight = memory.getData("Device/SubDeviceList/US/Right/Sensor/Value");
			distanceLeft += (double) varDistanceLeft.toFloat();
			distanceRight += (double) varDistanceRight.toFloat();

			try {
				Thread.sleep(sonarInterval);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		distanceLeft /= sonarNumSamples;
		distanceRight /= sonarNumSamples;

		// take minimum from left and right
		double distance = Math.min(distanceLeft, distanceRight);

		// create pattern
		double[] pattern = new double[INPUT_DIM];
		pattern[0] = distance;

		// set input pattern
		patternInput = pattern;

		// current distance
		if (patternInput != null) {
			long distanceCm = Math.round(patternInput[0] * 100);
			System.out.println("distance is " + String.format("%3dcm", distanceCm));

			// speak
			if (speakDistance) {
				String ttsDistance = "Distance is " + distanceCm + " centimeters.";
				tts.say(ttsDistance);
			}
		}

		if (!learning) {
			return null;
		}
		return pattern;
	}

	@Override
	protected void writeOutput(double[] pattern) {

		// set output pattern
		patternOutput = pattern;

		if (recalling) {

			if (patternOutput != null) {

				// check activation of output
				if (getActivationOutput() < ACTIVATION_OUTPUT_THRESHOLD) {

					System.out.println("activation of output is too low " + String.format("(%.3f < %.3f)", getActivationOutput(), ACTIVATION_OUTPUT_THRESHOLD));

				} else {

					// desired distance
					long desiredCm = Math.round(patternOutput[0] * 100);
					System.out.println(" desired is " + String.format("%3dcm", desiredCm));

					// speak
					if (speakDesired) {
						String ttsDesired = "Desired is " + desiredCm + " centimeters.";
						tts.say(ttsDesired);
					}

					// adjust distance
					if (legsPresent) {

						long distanceCm = Math.round(patternInput[0] * 100);
						if (desiredCm < distanceCm - toleranceCm) {
							System.out.println("moving forward");

							// speak
							if (speakMovement) {
								tts.say("Moving forward.");
							}

							// set stiffnesses
							motion.setStiffnesses(new Variant("Body"), new Variant(1.0f));

							// stand up
							motion.walkInit();

							// move forward
							motion.walkTo(+stepSize, 0, 0);

							// crouch sensing
							crouchSensing();

							// release stiffnesses						
							motion.setStiffnesses(new Variant("Body"), new Variant(0.0f));

							// wait for silence
							try {
								Thread.sleep(SILENCING_TIME);
							} catch (Exception e) {
								e.printStackTrace();
							}

						} else if (desiredCm > distanceCm + toleranceCm) {
							System.out.println("moving backward");

							// speak
							if (speakMovement) {
								tts.say("Moving backward.");
							}

							// set stiffnesses
							motion.setStiffnesses(new Variant("Body"), new Variant(1.0f));

							// stand up
							motion.walkInit();

							// move backward
							motion.walkTo(-stepSize, 0, 0);

							// crouch sensing
							crouchSensing();

							// release stiffnesses
							motion.setStiffnesses(new Variant("Body"), new Variant(0.0f));

							// wait for silence
							try {
								Thread.sleep(SILENCING_TIME);
							} catch (Exception e) {
								e.printStackTrace();
							}

						}

						else {
							System.out.println("keeping position");

							// speak
							if (speakMovement) {
								tts.say("Keeping position.");
							}

							// keep position

						}

					}

				}

			}

		}

	}

	@Override
	protected void updateUI() {

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
			g.drawLine(width / 2, 0, width / 2, height - 1);
			for (int i = 0; i <= NUM_GRID_LABELS; i++) {
				long labelDistance = Math.round(100 * (DISTANCE_MIN + i * (DISTANCE_MAX - DISTANCE_MIN) / NUM_GRID_LABELS));
				String strDistance = String.valueOf(labelDistance);
				textWidth = metrics.stringWidth(strDistance);
				yPos = (int) Math.round((double) i / NUM_GRID_LABELS * height);
				int xPos1 = (int) Math.round(0.4 * width);
				int xPos2 = (int) Math.round(0.6 * width);
				g.drawLine(xPos1, yPos, xPos2, yPos);
				xPos = (int) Math.round(0.7 * width);
				g.drawString(strDistance, xPos - textWidth / 2, yPos + textHeight / 2);
			}

			// draw nodes
			g.setColor(Color.RED);
			int diameterNode = 8;
			for (Node node : getTopology().getNodeSet()) {
				pattern = node.getPattern();
				patternNorm = normalisePattern(pattern);
				xPos = (int) Math.round(0.5 * width);
				yPos = (int) Math.round(patternNorm[0] * height);
				g.fillOval(xPos, yPos, diameterNode, diameterNode);
			}

			// draw edges
			g.setColor(Color.BLUE);
			int xPosA, yPosA, xPosB, yPosB;
			for (Edge edge : getTopology().getEdgeSet()) {
				pattern = edge.getNodeA().getPattern();
				patternNorm = normalisePattern(pattern);
				xPosA = (int) Math.round(0.5 * width);
				yPosA = (int) Math.round(patternNorm[0] * height);
				pattern = edge.getNodeB().getPattern();
				patternNorm = normalisePattern(pattern);
				xPosB = (int) Math.round(0.5 * width);
				yPosB = (int) Math.round(patternNorm[0] * height);
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
					xPos = (int) Math.round(0.5 * width);
					yPos = (int) Math.round(patternNorm[0] * height);
					g.drawOval(xPos - diameterCluster / 2, yPos - diameterCluster / 2, diameterCluster, diameterCluster);
					String strId = String.valueOf(cluster.getId());
					textWidth = metrics.stringWidth(strId);
					g.drawString(strId, xPos - textWidth / 2, yPos + textHeight / 2);
				}
			}

			// draw input
			if (patternInput != null) {
				patternNorm = normalisePattern(patternInput);
				xPos = (int) Math.round(0.5 * width);
				yPos = (int) Math.round(patternNorm[0] * height);
				g.setColor(Color.WHITE);
				g.fillOval(xPos, yPos, diameterNode, diameterNode);
			}

			// draw output
			if (patternOutput != null) {
				patternNorm = normalisePattern(patternOutput);
				xPos = (int) Math.round(0.5 * width);
				yPos = (int) Math.round(patternNorm[0] * height);
				g.setColor(Color.GREEN);
				g.fillRect(xPos, yPos, diameterNode, diameterNode);
			}

		}
	}

	private double[] normalisePattern(double[] pattern) {
		double[] patternNorm = new double[pattern.length];
		for (int i = 0; i < patternNorm.length; i++) {
			patternNorm[i] = (pattern[i] - DISTANCE_MIN) / (DISTANCE_MAX - DISTANCE_MIN);
		}
		return patternNorm;
	}

	private void switchMode() {
		learning = !learning;
		recalling = !recalling;
		System.out.println("learning = " + learning);
		System.out.println("recalling = " + recalling);
		if (learning) {
			tts.say("Now I am learning.");
		}
		if (recalling) {
			tts.say("Now I am recalling.");
		}
	}

	private void crouchSensing() {

		float time = 1.0f;

		String[] namesArray = { "HeadYaw", "HeadPitch", "LShoulderPitch", "LShoulderRoll", "LElbowYaw", "LElbowRoll", "RShoulderPitch", "RShoulderRoll", "RElbowYaw", "RElbowRoll", "LHipYawPitch",
				"LHipRoll", "LHipPitch", "LKneePitch", "LAnklePitch", "LAnkleRoll", "RHipRoll", "RHipPitch", "RKneePitch", "RAnklePitch", "RAnkleRoll" };
		float[] keysArray = { 0.0f, 0.0f, 1.3f, 0.7f, -0.3f, -0.9f, 1.3f, -0.7f, 0.3f, 0.9f, 0.01538f, -0.00456f, -0.90962f, 2.11255f, -1.18889f, 0.00464f, 0.10589f, -0.90203f, 2.11255f, -1.18630f,
				-0.10427f };
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
		NAOProxemicsSonar interactor = new NAOProxemicsSonar();
		interactor.run();
	}

}
