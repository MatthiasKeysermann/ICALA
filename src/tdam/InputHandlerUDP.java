package tdam;

import java.io.*;
import java.net.*;

/**
 * Class for writing to and reading from an input buffer using the UDP protocol.
 * <p>
 * Opens a UDP socket on a specific port, where data can be sent to. Any
 * received data pattern is appended to the input buffer.
 * 
 * @author Matthias Keysermann
 *
 */
public class InputHandlerUDP extends InputHandlerAbstract {

	private final static int UDP_PORT = 5000; // UDP port to listen for data

	private final static int PACKET_SIZE = 1024; // maximum size of data pattern

	private DatagramSocket socket;

	public InputHandlerUDP() {
		super();

		// create socket
		try {
			socket = new DatagramSocket(UDP_PORT);
		} catch (SocketException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void run() {

		// initialise
		String input = null;

		while (true) {

			// receive data
			try {
				byte[] data = new byte[PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(data, data.length);
				socket.receive(packet);
				input = new String(data).trim();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// add data to buffer
			addToBuffer(input);

		}

	}

}
