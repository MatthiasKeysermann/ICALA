package input;

import java.net.*;
import java.util.LinkedList;

/**
 * Abstract class for reading input data and sending this data to a specific UDP
 * port. Data is received as a list and each list item is sent in a separate
 * packet. A specified name followed by a space is added to the beginning of
 * each list item.
 * 
 * @author Matthias Keysermann
 *
 */
public abstract class SenderUDP implements Runnable {

	private final static int UDP_PORT = 5000; // UDP port to send data to

	private String name;

	private DatagramSocket socket;

	public SenderUDP(String name) {

		// assign name
		this.name = name;

		// create socket
		try {
			socket = new DatagramSocket();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void run() {
		while (true) {
			LinkedList<byte[]> inputs = readInput();
			if (inputs != null) {
				for (byte[] input : inputs) {
					try {
						String strInput = new String(input);
						String strData = name + " " + strInput;
						byte[] data = strData.getBytes();
						InetAddress hostAddress = InetAddress.getByName("localhost");
						DatagramPacket packet = new DatagramPacket(data, data.length, hostAddress, UDP_PORT);
						socket.send(packet);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	protected abstract LinkedList<byte[]> readInput();

}
