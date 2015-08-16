package output;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * A class for sending requests to a specific UDP port and receiving data over
 * this connection. The request consists of a specified name. The received data
 * is separated line by line and returned as a list.
 * 
 * @author Matthias Keysermann
 *
 */
public class RequesterUDP {

	private final static int UDP_PORT = 5001; // UDP port to send requests to

	private final static int PACKET_SIZE = 4096; // maximum size of received data

	private String name;

	private DatagramSocket socket;

	public RequesterUDP(String name) {

		// assign name
		this.name = name;

		// create socket
		try {
			socket = new DatagramSocket();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LinkedList<String> request() {
		LinkedList<String> outputs = new LinkedList<String>();

		// prepare request
		String request = name;

		try {

			// send request
			byte[] requestData = request.getBytes();
			InetAddress hostAddress = InetAddress.getByName("localhost");
			DatagramPacket sendPacket = new DatagramPacket(requestData, requestData.length, hostAddress, UDP_PORT);
			socket.send(sendPacket);

			// receive output
			byte[] outputData = new byte[PACKET_SIZE];
			DatagramPacket receivePacket = new DatagramPacket(outputData, outputData.length);
			socket.receive(receivePacket);
			String output = new String(outputData).trim();

			// prepare outputs
			StringTokenizer outputTokenizer = new StringTokenizer(output, "\n");
			while (outputTokenizer.hasMoreTokens()) {
				String line = outputTokenizer.nextToken();
				if (!line.equals("")) {
					outputs.add(line);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return outputs;
	}

}
