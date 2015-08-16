package tosam.evaluation;

import java.util.LinkedList;

import input.SenderUDP;

public class STDPGraph extends SenderUDP {

	private LinkedList<String> itemsList;

	private LinkedList<Long> timesList;

	private int index;

	private long timeFinish;

	public final static int ASSUMED_CYCLE_TIME = 100;

	public STDPGraph() {
		super("STDPGraph");
		itemsList = new LinkedList<String>();
		timesList = new LinkedList<Long>();
		index = 0;
		timeFinish = 0;
		System.out.println("Assuming a cycle time of " + ASSUMED_CYCLE_TIME + " ms");
	}

	public void addItem(String item, long time) {
		itemsList.add(item);
		timesList.add(time);
	}

	public void addBreak(long time) {
		addItem("", time);
	}

	public void addCommand(String command) {
		addItem("COMMAND " + command, 0);
	}

	@Override
	protected LinkedList<byte[]> readInput() {

		// check for end of list
		if (index == itemsList.size()) {
			System.out.println("Finished");
			System.exit(0);
		}

		// fetch item
		String items = itemsList.get(index);
		Long time = timesList.get(index);

		// set time
		if (timeFinish == 0) {
			timeFinish = System.currentTimeMillis() + time.longValue();
			if (items.equals(""))
				System.out.println("No input for " + time + " ms (" + (time / ASSUMED_CYCLE_TIME) + " cycles)...");
			else if (items.startsWith("COMMAND"))
				System.out.println("Sending command: " + items);
			else
				System.out.println("Presenting items " + items + " for " + time + " ms (" + (time / ASSUMED_CYCLE_TIME) + " cycles)...");
		}

		// prepare inputs
		LinkedList<byte[]> inputs = new LinkedList<byte[]>();
		if (items.startsWith("COMMAND")) {
			inputs.add(items.getBytes());
		} else {
			for (int i = 0; i < items.length(); i++) {
				String inputChar = items.substring(i, i + 1) + "\n";
				inputs.add(inputChar.getBytes());
			}
		}

		// check for finish time
		long timeCurrent = System.currentTimeMillis();
		if (timeCurrent >= timeFinish) {
			index += 1;
			timeFinish = 0;
		}

		return inputs;
	}

	public static void main(String[] args) {
		STDPGraph stdpGraph = new STDPGraph();

		int cyclesIntervalMax = 20;

		// loop over interval
		for (int cyclesInterval = 0; cyclesInterval < cyclesIntervalMax; cyclesInterval++) {

			// initialisation
			stdpGraph.addCommand("PRINT MESSAGE " + cyclesInterval + " cycles");
			stdpGraph.addItem("A", 0);
			stdpGraph.addItem("B", 0);
			stdpGraph.addBreak(50 * ASSUMED_CYCLE_TIME);

			// interval
			stdpGraph.addItem("A", 0);
			stdpGraph.addBreak(cyclesInterval * ASSUMED_CYCLE_TIME);
			stdpGraph.addItem("B", 0);
			stdpGraph.addBreak(50 * ASSUMED_CYCLE_TIME);
			stdpGraph.addCommand("PRINT ASSOCIATIONS");

			// clear network
			stdpGraph.addCommand("CLEARNETWORK");

		}

		stdpGraph.run();
	}

}
