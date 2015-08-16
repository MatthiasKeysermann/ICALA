package input.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import input.SenderUDP;

public class ColourSelector extends SenderUDP {

	private final static String TITLE = "Colour Selector";
	private JFrame frame;

	private ColourWheel colourWheel;
	private BrightnessBar brightnessBar;

	private JTextField tfValue;
	private JCheckBox cbSend;

	public ColourSelector() {
		super("ColourSelector");

		// frame

		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle(TITLE);
		frame.setSize(220, 160);
		frame.setLocation(50, 150);

		JPanel pnMain = new JPanel();
		pnMain.setLayout(new BoxLayout(pnMain, BoxLayout.Y_AXIS));
		frame.getContentPane().add(pnMain);

		// selector

		JPanel pnSelector = new JPanel();
		pnSelector.setLayout(new BoxLayout(pnSelector, BoxLayout.X_AXIS));
		pnMain.add(pnSelector);

		colourWheel = new ColourWheel();
		pnSelector.add(colourWheel);

		brightnessBar = new BrightnessBar();
		pnSelector.add(brightnessBar);

		// value

		JPanel pnValue = new JPanel();
		pnValue.setLayout(new BoxLayout(pnValue, BoxLayout.X_AXIS));
		pnMain.add(pnValue);

		JLabel lbValue = new JLabel("Value: ");
		pnValue.add(lbValue);

		tfValue = new JTextField("#000000");
		tfValue.setMinimumSize(new Dimension(75, 25));
		tfValue.setMaximumSize(new Dimension(75, 25));
		tfValue.setEditable(false);
		pnValue.add(tfValue);

		cbSend = new JCheckBox("Send");
		pnValue.add(cbSend);

		// finalise

		for (Component component : pnMain.getComponents()) {
			((JComponent) component).setAlignmentX(Component.LEFT_ALIGNMENT);
		}

		frame.setVisible(true);

	}

	public void updateValue() {

		float hue = colourWheel.selectedHue;
		float saturation = colourWheel.selectedSaturation;
		float brightness = brightnessBar.selectedBrightness;

		//int rgb = Color.HSBtoRGB(hue, saturation, brightness);
		Color colour = Color.getHSBColor(hue, saturation, brightness);
		int rgb = colour.getRed() * 65536 + colour.getGreen() * 256 + colour.getBlue();
		String str = "000000" + Integer.toString(rgb, 16).toUpperCase();
		String value = "#" + str.substring(str.length() - 6);

		tfValue.setText(value);
	}

	@Override
	protected LinkedList<byte[]> readInput() {
		LinkedList<byte[]> inputs = new LinkedList<byte[]>();
		if (cbSend.isSelected()) {

			// prepare input
			String input = tfValue.getText();

			// prepare data		
			String inputChar = input + "\n";
			inputs.add(inputChar.getBytes());

			// sleep
			try {
				Thread.sleep(10);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return inputs;
	}

	private class ColourWheel extends JPanel implements MouseListener {

		private static final long serialVersionUID = 1L;

		private int xSize;
		private int ySize;

		protected float selectedHue;
		protected float selectedSaturation;

		public ColourWheel() {
			super();
			xSize = 100;
			ySize = 100;
			addMouseListener(this);
		}

		@Override
		protected void paintComponent(Graphics g) {

			g.setColor(Color.black);
			g.fillRect(0, 0, xSize, ySize);

			// initialise
			int xCenter = xSize / 2;
			int yCenter = ySize / 2;
			int radiusMax = Math.min(xCenter, yCenter);

			for (int y = 0; y < ySize; y++) {
				for (int x = 0; x < xSize; x++) {

					// determine radius
					int xDiff = x - xCenter;
					int yDiff = y - yCenter;
					double radius = Math.sqrt(xDiff * xDiff + yDiff * yDiff);

					if (radius < radiusMax) {

						// determine angle
						double angle = Math.atan2(yDiff, xDiff);

						// calculate colour
						float hue = (float) (angle / (2 * Math.PI));
						float saturation = (float) radius / radiusMax;
						float brightness = (float) 1.0;

						// draw colour
						g.setColor(Color.getHSBColor(hue, saturation, brightness));
						g.drawRect(x, y, 1, 1);

					}

				}
			}

		}

		@Override
		public void mouseClicked(MouseEvent e) {

			// read coordinates
			int x = e.getPoint().x;
			int y = e.getPoint().y;

			// initialise
			int xCenter = xSize / 2;
			int yCenter = ySize / 2;
			int radiusMax = Math.min(xCenter, yCenter);

			// determine radius
			int xDiff = x - xCenter;
			int yDiff = y - yCenter;
			double radius = Math.sqrt(xDiff * xDiff + yDiff * yDiff);

			if (radius < radiusMax) {

				// determine angle
				double angle = Math.atan2(yDiff, xDiff);

				// calculate hue and saturation
				float hue = (float) (angle / (2 * Math.PI));
				float saturation = (float) radius / radiusMax;

				// set hue and saturation
				selectedHue = hue;
				selectedSaturation = saturation;
				updateValue();

			}

		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

	}

	private class BrightnessBar extends JPanel implements MouseListener {

		private static final long serialVersionUID = 1L;

		private int xSize;
		private int ySize;

		protected float selectedBrightness;

		public BrightnessBar() {
			super();
			xSize = 10;
			ySize = 100;
			addMouseListener(this);
		}

		@Override
		protected void paintComponent(Graphics g) {

			for (int y = 0; y < ySize; y++) {
				float brightness = (float) y / ySize;
				g.setColor(Color.getHSBColor(0, 0, brightness));
				g.drawRect(0, y, xSize, 1);
			}

		}

		@Override
		public void mouseClicked(MouseEvent e) {

			// read coordinate
			int y = e.getPoint().y;

			// calculate brightness
			float brightness = (float) y / ySize;

			// set brightness
			selectedBrightness = brightness;
			updateValue();

		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

	}

	public static void main(String[] args) {
		ColourSelector colourSelector = new ColourSelector();
		colourSelector.run();
	}

}
