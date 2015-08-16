package tdam;

import java.io.File;
import java.util.LinkedList;

/**
 * Class for a running TDAM simulation.
 * <p>
 * Runs in short repetitive cycles, each executing the required processing
 * steps. Provides UDP input and output handlers for receiving and sending data.
 * Handles external text commands sent as data. Allows to write to XML and read
 * from XML.
 * 
 * @author Matthias Keysermann
 *
 */
public class Simulation implements Runnable {

	private boolean running;

	private Network network;

	private long cycleTime; // milliseconds
	private long cycleDuration; // milliseconds
	private static final long CYCLE_TIME_DEFAULT = 100; // milliseconds

	private InputHandler inputHandler;
	private OutputHandler outputHandler;
	private static final double INPUT_DEFAULT = 1.0;

	protected File fileSave;
	protected File fileLoad;

	public Simulation() {
		init();
		resume();
	}

	public long getCycleTime() {
		return cycleTime;
	}

	public void setCycleTime(long cycleTime) {
		this.cycleTime = cycleTime;
	}

	public long getCycleDuration() {
		return cycleDuration;
	}

	public Network getNetwork() {
		return network;
	}

	public void pause() {
		running = false;
	}

	public void resume() {
		running = true;
	}

	public void clearNetwork() {
		network.clear();
	}

	private void init() {

		// initialise
		running = false;
		network = new Network();
		cycleTime = CYCLE_TIME_DEFAULT;

		// create input handler
		inputHandler = new InputHandlerUDP();
		Thread inputHandlerThread = new Thread(inputHandler);
		inputHandlerThread.start();

		// create output handler
		outputHandler = new OutputHandlerUDP(this);
		Thread outputHandlerThread = new Thread(outputHandler);
		outputHandlerThread.start();

		// unset saving and loading file
		fileSave = null;
		fileLoad = null;

	}

	private void step() {

		// read inputs
		while (!inputHandler.isBufferEmpty()) {

			// read data from buffer
			Object input = inputHandler.pollBuffer();

			// check for command
			String[] inputSplit = input.toString().split(" ");

			if (inputSplit.length > 1 && inputSplit[1].equals("COMMAND")) {

				// command handling
				handleCommand(input.toString());

			} else {

				// insert data into network
				Unit unit = network.insertData(input);

				// set input
				unit.setInput(INPUT_DEFAULT);

			}

		}

		// spread signal to all unit
		network.calculateSignalSums();

		// determine error in all unit
		network.calculateErrors();

		// update association strengths
		network.updateStrengths();

		// update traces
		network.updateTraces();

		// store signal sums
		network.storeSignalSums();

		// clear inputs
		network.resetInputs();

		// clean up network
		network.cleanUp();

	}

	@Override
	public void run() {

		// initialise
		long cycleStart = 0;
		long cycleEnd = 0;
		cycleDuration = 0;

		while (true) {

			if (running) {

				// measure time
				cycleStart = System.currentTimeMillis();

				// simulate step
				step();

				// measure time
				cycleEnd = System.currentTimeMillis();
				cycleDuration = cycleEnd - cycleStart;

				// wait until cycle is finished
				if (cycleDuration < cycleTime) {
					try {
						Thread.sleep(cycleTime - cycleDuration);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					System.err.println("Cycle time violation with duration " + cycleDuration + " ms");
				}

				// update user interface
				updateUI();

				// save network
				if (fileSave != null) {
					XMLWriter xmlWriter = new XMLWriter();
					xmlWriter.writeToFile(network, fileSave.getAbsolutePath());
					fileSave = null;
				}

				// load network
				if (fileLoad != null) {
					XMLReader xmlReader = new XMLReader();
					xmlReader.readFromFile(fileLoad.getAbsolutePath(), network);
					fileLoad = null;
				}

			}

			else {

				try {
					Thread.sleep(cycleTime);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		}

	}

	protected void handleCommand(String commandString) {

		// split command string
		String[] commandStringSplit = commandString.split(" ");

		// check token
		String token = commandStringSplit[1];
		if (!token.equals("COMMAND")) {
			System.err.println("Invalid command string");
			return;
		}

		// parse command
		String command = commandStringSplit[2];

		if (command.equals("PAUSE")) {

			pause();

		} else if (command.equals("RESUME")) {

			resume();

		} else if (command.equals("CLEARNETWORK")) {

			clearNetwork();

		} else if (command.equals("PRINT")) {

			// parse argument
			String argument = commandStringSplit[3];

			if (argument.equals("UNITS")) {

				LinkedList<Unit> units = network.getUnits();
				synchronized (units) {
					for (Unit unit : units) {
						System.out.println(unit);
					}
				}

			} else if (argument.equals("UNIT")) {

				String sender = commandStringSplit[0];
				Unit unit = network.getUnit(sender + " " + commandStringSplit[4]);
				System.out.println(unit);

			} else if (argument.equals("ASSOCIATIONS")) {

				LinkedList<Association> associations = network.getAssociations();
				synchronized (associations) {
					for (Association association : associations) {
						System.out.println(association);
					}
				}

			} else if (argument.equals("MESSAGE")) {

				int indexMessage = commandString.indexOf("PRINT MESSAGE") + 14;
				String message = commandString.substring(indexMessage);
				System.out.println(message);

			}

		} else if (command.equals("SPLITDATA")) {

			String sender = commandStringSplit[0];
			String dataOld = sender + " " + commandStringSplit[3];
			String dataNew = sender + " " + commandStringSplit[4];
			network.splitData(dataOld, dataNew);
			// DEBUG
			System.out.println("SPLITDATA:   OLD = " + dataOld + "   NEW = " + dataNew);

		} else if (command.equals("JOINDATA")) {

			String sender = commandStringSplit[0];
			String dataKeep = sender + " " + commandStringSplit[3];
			String dataDelete = sender + " " + commandStringSplit[4];
			network.joinData(dataKeep, dataDelete);
			// DEBUG
			System.out.println("JOINDATA:   KEEP = " + dataKeep + "   DELETE = " + dataDelete);

		} else if (command.equals("DELETEDATA")) {

			String sender = commandStringSplit[0];
			String data = sender + " " + commandStringSplit[3];
			network.deleteData(data);
			// DEBUG
			System.out.println("DELETEDATA:   " + data);

		}

	}

	protected void updateUI() {
		System.out.println("units: " + network.getUnits().size() + " | " + "associations: " + network.getAssociations().size() + " | " + "cycle duration: " + cycleDuration + " ms");
	}

	public static void main(String[] args) {
		Simulation simulation = new Simulation();
		simulation.run();
	}

}
