package soinnm.gui;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import util.ImagePanel;
import util.WrapLayout;

import mnist.tools.MnistManager;

public class DisplayDigits {

	private final static String MNIST_PATH = "res/MNISTDigits/";

	private MnistManager mnist;

	private JFrame frame;

	private JPanel panel;

	public DisplayDigits() {

		try {
			mnist = new MnistManager(MNIST_PATH + "t10k-images-idx3-ubyte", MNIST_PATH + "t10k-labels-idx1-ubyte");
		} catch (Exception e) {
			e.printStackTrace();
		}

		frame = new JFrame("MNIST");
		frame.setSize(600, 400);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		panel = new JPanel();
		panel.setLayout(new WrapLayout());
		frame.add(panel);

		JScrollPane spImages = new JScrollPane(panel);
		spImages.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		spImages.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		frame.add(spImages);

		int mnistCount = mnist.getImages().getCount();
		try {
			for (int i = 0; i < mnistCount; i++) {
				System.out.println("Reading image " + (i + 1) + " of " + mnistCount);
				int[][] imageData = mnist.readImage();
				BufferedImage image = getBufferedImage(imageData);
				panel.add(new ImagePanel(image));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		spImages.repaint();
	}

	private BufferedImage getBufferedImage(int[][] imageData) {
		int height = imageData.length;
		int width = imageData[0].length;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int value = imageData[y][x];
				int rgba = new Color(value, value, value, 255).getRGB();
				image.setRGB(x, y, rgba);
			}
		}
		return image;
	}

	public static void main(String[] args) {
		new DisplayDigits();
	}

}
