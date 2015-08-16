package output.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import output.RequesterUDP;

public class GUIRequester extends RequesterUDP {

	private JFrame frame;
	private JTextField tfName;
	private JTextArea taOutputs;
	private JLabel lbStatus;

	public GUIRequester() {
		super("KeyReader");

		// frame
		frame = new JFrame("GUIRequester");
		frame.setSize(400, 400);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());

		// name
		tfName = new JTextField("KeyReader");
		tfName.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setName(tfName.getText());
			}
		});
		frame.add(tfName, BorderLayout.PAGE_START);

		// outputs
		taOutputs = new JTextArea();
		taOutputs.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(taOutputs);
		frame.add(scrollPane, BorderLayout.CENTER);

		// status
		lbStatus = new JLabel("Requesting...");
		frame.add(lbStatus, BorderLayout.PAGE_END);

		// show frame
		frame.setVisible(true);

	}

	public void run() {

		while (true) {

			// request outputs
			LinkedList<String> outputs = request();

			// update outputs
			taOutputs.setText("");
			String strOutputs = "";
			for (String output : outputs) {
				strOutputs += output + "\n";
			}
			taOutputs.setText(strOutputs);

			// update status
			lbStatus.setText("Received " + outputs.size() + " outputs");

			// wait
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	public static void main(String[] args) {
		GUIRequester guiRequester = new GUIRequester();
		guiRequester.run();
	}

}
