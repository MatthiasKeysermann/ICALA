package features;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class ImageProcessorRGB {

	private int imageWidth;

	private int imageHeight;

	private IntensityDetector2D intensityDetector2D;

	public static final int NUM_FEATURE_TYPES = IntensityDetector2D.FEATURE_TYPES.length;

	public static int getFeatureVectorLength(int numLevels) {
		int windowCount = 1;
		int length = 0;
		for (int level = 1; level <= numLevels; level++) {
			length += windowCount * NUM_FEATURE_TYPES * 3;
			windowCount *= 4;
		}
		return length;
	}

	public ImageProcessorRGB(int imageWidth, int imageHeight, int numLevels) {
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		intensityDetector2D = new IntensityDetector2D(imageWidth, imageHeight, numLevels);
	}

	public double[] processImageRGB(BufferedImage image) {

		// create input for red channel
		double[][] inputRed = new double[imageWidth][imageHeight];
		for (int y = 0; y < imageHeight; y++) {
			for (int x = 0; x < imageWidth; x++) {
				inputRed[x][y] = (double) new Color(image.getRGB(x, y)).getRed();
			}
		}

		// normalise & process red channel
		double[][] inputRedNormalised = intensityDetector2D.normaliseInput(inputRed, 0, 255);
		double[] featureVectorRed = intensityDetector2D.process(inputRedNormalised);

		// create input for green channel
		double[][] inputGreen = new double[imageWidth][imageHeight];
		for (int y = 0; y < imageHeight; y++) {
			for (int x = 0; x < imageWidth; x++) {
				inputGreen[x][y] = (double) new Color(image.getRGB(x, y)).getGreen();
			}
		}

		// normalise & process green channel
		double[][] inputGreenNormalised = intensityDetector2D.normaliseInput(inputGreen, 0, 255);
		double[] featureVectorGreen = intensityDetector2D.process(inputGreenNormalised);

		// create input for blue channel
		double[][] inputBlue = new double[imageWidth][imageHeight];
		for (int y = 0; y < imageHeight; y++) {
			for (int x = 0; x < imageWidth; x++) {
				inputBlue[x][y] = (double) new Color(image.getRGB(x, y)).getBlue();
			}
		}

		// normalise & process blue channel		
		double[][] inputBlueNormalised = intensityDetector2D.normaliseInput(inputBlue, 0, 255);
		double[] featureVectorBlue = intensityDetector2D.process(inputBlueNormalised);

		// concatenate feature vectors
		double[] featureVectorRGB = new double[featureVectorRed.length + featureVectorGreen.length + featureVectorBlue.length];
		for (int i = 0; i < featureVectorRed.length; i++) {
			featureVectorRGB[i] = featureVectorRed[i];
		}
		for (int i = 0; i < featureVectorGreen.length; i++) {
			featureVectorRGB[featureVectorRed.length + i] = featureVectorGreen[i];
		}
		for (int i = 0; i < featureVectorBlue.length; i++) {
			featureVectorRGB[featureVectorRed.length + featureVectorGreen.length + i] = featureVectorBlue[i];
		}

		return featureVectorRGB;
	}

	public BufferedImage reconstructImageRGB(double[] featureVectorRGB) {

		// separate feature vector
		double[] featureVectorRed = new double[featureVectorRGB.length / 3];
		for (int i = 0; i < featureVectorRed.length; i++) {
			featureVectorRed[i] = featureVectorRGB[i];
		}
		double[] featureVectorGreen = new double[featureVectorRGB.length / 3];
		for (int i = 0; i < featureVectorGreen.length; i++) {
			featureVectorGreen[i] = featureVectorRGB[featureVectorRed.length + i];
		}
		double[] featureVectorBlue = new double[featureVectorRGB.length / 3];
		for (int i = 0; i < featureVectorBlue.length; i++) {
			featureVectorBlue[i] = featureVectorRGB[featureVectorRed.length + featureVectorGreen.length + i];
		}

		// reconstruct & denormalise red channel
		double[][] inputNormalisedRed = intensityDetector2D.reconstruct(featureVectorRed);
		double[][] inputRed = intensityDetector2D.denormaliseInput(inputNormalisedRed, 0, 255);

		// reconstruct & denormalise green channel
		double[][] inputNormalisedGreen = intensityDetector2D.reconstruct(featureVectorGreen);
		double[][] inputGreen = intensityDetector2D.denormaliseInput(inputNormalisedGreen, 0, 255);

		// reconstruct & denormalise blue channel
		double[][] inputNormalisedBlue = intensityDetector2D.reconstruct(featureVectorBlue);
		double[][] inputBlue = intensityDetector2D.denormaliseInput(inputNormalisedBlue, 0, 255);

		// create image
		BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < imageHeight; y++) {
			for (int x = 0; x < imageWidth; x++) {
				int red = (int) Math.round(inputRed[x][y]);
				int green = (int) Math.round(inputGreen[x][y]);
				int blue = (int) Math.round(inputBlue[x][y]);
				image.setRGB(x, y, new Color(red, green, blue, 255).getRGB());
			}
		}

		return image;
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
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();

		// create image processor
		int numLevels = 6;
		ImageProcessorRGB imageProcessorRGB = new ImageProcessorRGB(imageWidth, imageHeight, numLevels);

		// DEBUG
		long timeStart = System.currentTimeMillis();

		// process image
		double[] featureVectorRGB = imageProcessorRGB.processImageRGB(image);

		// DEBUG
		long timeEnd = System.currentTimeMillis();
		System.out.println("Processing took " + (timeEnd - timeStart) + " ms");

		// DEBUG
		System.out.println("Input has " + (imageWidth * imageHeight) + " dimensions");
		System.out.println("Vector has " + featureVectorRGB.length + " dimensions");

		// DEBUG
		timeStart = System.currentTimeMillis();

		// reconstruct image
		image = imageProcessorRGB.reconstructImageRGB(featureVectorRGB);

		// DEBUG
		timeEnd = System.currentTimeMillis();
		System.out.println("Reconstruction took " + (timeEnd - timeStart) + " ms");

		// write image
		try {
			ImageIO.write(image, "png", new File("res/Reconstruction.png"));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
