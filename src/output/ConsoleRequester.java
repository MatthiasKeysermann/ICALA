package output;

import java.util.LinkedList;

/**
 * Class for regularly requesting data from a specific UDP port and printing the
 * received data to the standard output (console). The request consists of a
 * specified name. The received data is displayed line by line.
 * 
 * @see RequesterUDP
 * 
 * @author Matthias Keysermann
 *
 */
public class ConsoleRequester extends RequesterUDP {

	public ConsoleRequester() {
		super("ConsoleReader");
	}

	public void run() {

		while (true) {

			// request outputs
			LinkedList<String> outputs = request();
			for (String output : outputs) {
				System.out.println(output);
			}

			// wait
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	public static void main(String[] args) {
		ConsoleRequester consoleRequester = new ConsoleRequester();
		consoleRequester.run();
	}

}
