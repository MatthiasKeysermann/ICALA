package features;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import features.Feature2D.FEATURE2D;

public class EdgeDetector2D extends Processor2D {

	public final static FEATURE2D[] FEATURE_TYPES = { FEATURE2D.EDGE_VERTICAL, FEATURE2D.EDGE_HORIZONTAL, FEATURE2D.EDGE_DIAGONAL_A, FEATURE2D.EDGE_DIAGONAL_B };

	public EdgeDetector2D(int inputWidth, int inputHeight, int numLevels) {
		super(inputWidth, inputHeight, FEATURE_TYPES, numLevels);
	}

	// normalise input into [-1,1]
	@Override
	public double[][] normaliseInput(double[][] input, double min, double max) {
		double[][] inputNormalised = new double[inputWidth][inputHeight];
		for (int y = 0; y < inputHeight; y++) {
			for (int x = 0; x < inputWidth; x++) {
				inputNormalised[x][y] = (input[x][y] - min) / ((max - min) / 2.0) - 1.0;
			}
		}
		return inputNormalised;
	}

	// denormalise input from [-1,1]
	@Override
	public double[][] denormaliseInput(double[][] inputNormalised, double min, double max) {
		double[][] input = new double[inputWidth][inputHeight];
		for (int y = 0; y < inputHeight; y++) {
			for (int x = 0; x < inputWidth; x++) {
				input[x][y] = (inputNormalised[x][y] + 1.0) * ((max - min) / 2.0) + min;
			}
		}
		return input;
	}

	public static void main(String[] args) {

		// read image
		BufferedImage image = null;
		try {
			image = ImageIO.read(new File("res/Signs/Sign1.png"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		// create input
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		double[][] input = new double[imageWidth][imageHeight];
		for (int y = 0; y < imageHeight; y++) {
			for (int x = 0; x < imageWidth; x++) {
				input[x][y] = (double) new Color(image.getRGB(x, y)).getGreen();
			}
		}

		// create processor
		int numLevels = 6;
		EdgeDetector2D edgeDetector2D = new EdgeDetector2D(imageWidth, imageHeight, numLevels);

		// DEBUG
		long timeStart = System.currentTimeMillis();

		// normalise & process input
		double[][] inputNormalised = edgeDetector2D.normaliseInput(input, 0, 255);
		double[] featureVector = edgeDetector2D.process(inputNormalised);

		// DEBUG
		long timeEnd = System.currentTimeMillis();
		System.out.println("Processing took " + (timeEnd - timeStart) + " ms");

		// DEBUG
		System.out.println("Input has " + (imageWidth * imageHeight) + " dimensions");
		System.out.println("Vector has " + featureVector.length + " dimensions");

		// DEBUG
		/*
		for (int i = 0; i < featureVector.length; i++) {
			System.out.println(featureVector[i] + " ");
		}
		*/

		// DEBUG
		timeStart = System.currentTimeMillis();

		// reconstruct & denormalise input
		inputNormalised = edgeDetector2D.reconstruct(featureVector);
		input = edgeDetector2D.denormaliseInput(inputNormalised, 0, 255);

		// DEBUG
		timeEnd = System.currentTimeMillis();
		System.out.println("Reconstruction took " + (timeEnd - timeStart) + " ms");

		// create image
		image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < imageHeight; y++) {
			for (int x = 0; x < imageWidth; x++) {
				int green = (int) Math.round(input[x][y]);
				Color color = new Color(green, green, green, 255);
				int rgb = color.getRGB();
				image.setRGB(x, y, rgb);
			}
		}

		// write to file
		try {
			ImageIO.write(image, "png", new File("res/Reconstruction.png"));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
