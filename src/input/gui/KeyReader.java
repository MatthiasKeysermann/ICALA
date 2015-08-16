package input.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import input.SenderUDP;

public class KeyReader extends SenderUDP implements KeyListener {

	private final static String TITLE = "Key Reader";
	private JFrame frame;

	private JLabel lbInput;

	private boolean[] keys;

	public KeyReader() {
		super("KeyReader");

		// intialise
		keys = new boolean[256];

		// frame

		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle(TITLE);
		frame.setSize(200, 150);
		frame.setLocation(50, 150);

		JPanel pnMain = new JPanel();
		pnMain.setLayout(new BorderLayout());
		frame.getContentPane().add(pnMain);

		// input

		lbInput = new JLabel();
		Font font = new Font("Courier", Font.BOLD, 48);
		lbInput.setFont(font);
		pnMain.add(lbInput);

		frame.addKeyListener(this);

		// finalise
		frame.setVisible(true);

	}

	@Override
	protected LinkedList<byte[]> readInput() {
		LinkedList<byte[]> inputs = new LinkedList<byte[]>();

		// prepare input
		String input = "";
		for (int i = 0; i < 256; i++) {
			if (keys[i]) {
				input += (char) i;
			}
		}
		lbInput.setText(input);

		// prepare data		
		for (int i = 0; i < input.length(); i++) {
			String inputChar = input.substring(i, i + 1) + "\n";
			inputs.add(inputChar.getBytes());
		}

		// sleep
		try {
			Thread.sleep(10);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return inputs;
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		byte index = (byte) e.getKeyChar();
		if (index >= 0 && index < 256) {
			keys[index] = true;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		byte index = (byte) e.getKeyChar();
		if (index >= 0 && index < 256) {
			keys[index] = false;
		}
	}

	public static void main(String[] args) {
		KeyReader keyReader = new KeyReader();
		keyReader.run();
	}

}
