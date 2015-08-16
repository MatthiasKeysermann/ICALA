package input;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;

/**
 * Class for repetitively reading a line from the standard input (console) and
 * sending it to a specific UDP port.
 * 
 * @see SenderUDP
 * 
 * @author Matthias Keysermann
 *
 */
public class ConsoleReader extends SenderUDP {

	protected BufferedReader bufferedReader;

	public ConsoleReader() {
		super("ConsoleReader");
		bufferedReader = new BufferedReader(new InputStreamReader(System.in));
	}

	@Override
	protected LinkedList<byte[]> readInput() {
		LinkedList<byte[]> inputs = new LinkedList<byte[]>();

		try {
			String input = bufferedReader.readLine() + "\n";
			inputs.add(input.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return inputs;
	}

	public static void main(String[] args) {
		ConsoleReader consoleReader = new ConsoleReader();
		consoleReader.run();
	}

}
