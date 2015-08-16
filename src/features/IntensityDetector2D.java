package features;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import features.Feature2D.FEATURE2D;

public class IntensityDetector2D extends Processor2D {

	public final static FEATURE2D[] FEATURE_TYPES = { FEATURE2D.INTENSITY_VERTICAL, FEATURE2D.INTENSITY_VERTICAL_INVERSE, FEATURE2D.INTENSITY_HORIZONTAL, FEATURE2D.INTENSITY_HORIZONTAL_INVERSE,
	//FEATURE2D.INTENSITY_DIAGONAL_A, FEATURE2D.INTENSITY_DIAGONAL_A_INVERSE, FEATURE2D.INTENSITY_DIAGONAL_B, FEATURE2D.INTENSITY_DIAGONAL_B_INVERSE
	};

	public IntensityDetector2D(int inputWidth, int inputHeight, int numLevels) {
		super(inputWidth, inputHeight, FEATURE_TYPES, numLevels);
	}

	// normalise input into [0,1]
	@Override
	public double[][] normaliseInput(double[][] input, double min, double max) {
		double[][] inputNormalised = new double[inputWidth][inputHeight];
		for (int y = 0; y < inputHeight; y++) {
			for (int x = 0; x < inputWidth; x++) {
				inputNormalised[x][y] = (input[x][y] - min) / (max - min);
			}
		}
		return inputNormalised;
	}

	// denormalise input from [0,1]
	@Override
	public double[][] denormaliseInput(double[][] inputNormalised, double min, double max) {
		double[][] input = new double[inputWidth][inputHeight];
		for (int y = 0; y < inputHeight; y++) {
			for (int x = 0; x < inputWidth; x++) {
				input[x][y] = inputNormalised[x][y] * (max - min) + min;
			}
		}
		return input;
	}

	public static void main(String[] args) {

		// read image
		BufferedImage image = null;
		try {
			image = ImageIO.read(new File("res/Sign02.png"));
			//image = ImageIO.read(new File("res/FollowTheNAO1.jpg"));
			//image = ImageIO.read(new File("res/Nature.png"));
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
		IntensityDetector2D intensityDetector2D = new IntensityDetector2D(imageWidth, imageHeight, numLevels);

		// DEBUG
		long timeStart = System.currentTimeMillis();

		// normalise input
		double[][] inputNormalised = intensityDetector2D.normaliseInput(input, 0, 255);

		// process input
		double[] featureVector = intensityDetector2D.process(inputNormalised);

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

		// reconstruct input
		inputNormalised = intensityDetector2D.reconstruct(featureVector);

		// denormalise input
		input = intensityDetector2D.denormaliseInput(inputNormalised, 0, 255);

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
