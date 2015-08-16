package tdam;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.LinkedList;

/**
 * Class for providing a list of units containing the given name as a first word
 * in their data pattern and sending this list on request over a UDP connection.
 * <p>
 * Opens a UDP socket on a specific port, where requests can be sent to. A
 * request contains only the name that units should be matched against. Sends
 * back a message with all matching units in separate lines. Each line contains
 * the unit's data pattern and the unit's signal sum, separated by a space.
 * 
 * @author Matthias Keysermann
 *
 */
public class OutputHandlerUDP extends OutputHandlerAbstract {

	private final static int UDP_PORT = 5001; // UDP port to listen for requests

	private final static int PACKET_SIZE = 1024; // maximum size of request message

	private DatagramSocket socket;

	public OutputHandlerUDP(Simulation simulation) {
		super(simulation);

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
		InetAddress clientAddress = null;
		int clientPort = 0;
		String request = null;
		String output = null;

		while (true) {

			// receive request
			try {
				byte[] requestData = new byte[PACKET_SIZE];
				DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length);
				socket.receive(requestPacket);
				clientAddress = requestPacket.getAddress();
				clientPort = requestPacket.getPort();
				request = new String(requestData).trim();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// fetch units
			LinkedList<Unit> fetchedUnits = fetchUnits(request);

			// prepare output
			output = "";
			for (Unit unit : fetchedUnits) {
				output += unit.getData() + " " + unit.getSignalSum() + "\n";
			}

			// send output
			try {
				byte[] outputData = output.getBytes();
				DatagramPacket outputPacket = new DatagramPacket(outputData, outputData.length, clientAddress, clientPort);
				socket.send(outputPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

}
