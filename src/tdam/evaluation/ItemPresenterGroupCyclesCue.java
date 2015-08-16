package tdam.evaluation;

import input.SenderUDP;

import java.util.ArrayList;
import java.util.LinkedList;

public class ItemPresenterGroupCyclesCue extends SenderUDP {

	private LinkedList<String> itemsList;

	private LinkedList<Long> timesList;

	private int index;

	private long timeFinish;

	private long timeAfterFinish;

	public final static int ASSUMED_CYCLE_TIME = 100;

	public ItemPresenterGroupCyclesCue() {
		super("ItemPresenterGroupCyclesCue");
		itemsList = new LinkedList<String>();
		timesList = new LinkedList<Long>();
		index = 0;
		timeFinish = 0;
		timeAfterFinish = 0;
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
			//System.out.println("Finished");
			//System.exit(0);
			System.out.println("Time after Finish: " + timeAfterFinish + " ms");
			itemsList.add("");
			timesList.add(new Long(ASSUMED_CYCLE_TIME));
			timeAfterFinish += ASSUMED_CYCLE_TIME;
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
		ItemPresenterGroupCyclesCue itemPresenter = new ItemPresenterGroupCyclesCue();

		// prepare items
		ArrayList<Integer> cyclesRecallList = new ArrayList<Integer>();
		cyclesRecallList.add(10);
		cyclesRecallList.add(20);
		cyclesRecallList.add(30);
		cyclesRecallList.add(40);
		cyclesRecallList.add(50);
		cyclesRecallList.add(60);
		cyclesRecallList.add(70);
		cyclesRecallList.add(80);
		cyclesRecallList.add(90);
		cyclesRecallList.add(100);

		for (Integer cyclesRecall : cyclesRecallList) {

			// initialise
			itemPresenter.addBreak(100 * ASSUMED_CYCLE_TIME);

			// presentation
			itemPresenter.addCommand("PRINT MESSAGE Learn Phase: Presenting ABCDEFGHIJ (10 items) for 100 cycles");
			itemPresenter.addItem("ABCDEFGHIJ", 100 * ASSUMED_CYCLE_TIME);

			// gap
			itemPresenter.addBreak(100 * ASSUMED_CYCLE_TIME);
			itemPresenter.addCommand("PRINT MESSAGE Decayed activation after 100 cycles:");
			itemPresenter.addCommand("PRINT UNITS");

			// recall
			itemPresenter.addCommand("PRINT MESSAGE Recall Phase: Presenting A (1 item) for " + cyclesRecall + " cycles");
			itemPresenter.addItem("A", cyclesRecall * ASSUMED_CYCLE_TIME);
			itemPresenter.addCommand("PRINT MESSAGE Recalled activation after 0 cycles:");
			itemPresenter.addCommand("PRINT UNITS");
			for (int cyclesAfter = 1; cyclesAfter <= 30; cyclesAfter++) {
				itemPresenter.addBreak(1 * ASSUMED_CYCLE_TIME);
				itemPresenter.addCommand("PRINT MESSAGE Recalled activation after " + cyclesAfter + " cycles:");
				itemPresenter.addCommand("PRINT UNITS");
			}

			// clear network			
			itemPresenter.addCommand("PRINT MESSAGE Clearing network");
			itemPresenter.addCommand("CLEARNETWORK");

		}

		itemPresenter.run();
	}

}
