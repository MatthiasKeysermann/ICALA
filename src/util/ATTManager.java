package util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class ATTManager {

	private String path;
	private int numPersons;
	private int numFaces;
	private int currentImage; // starting at 1
	private int currentLabel; // starting at 1

	private final static boolean PRELOAD_IMAGES = true;
	private ArrayList<int[][]> images;

	public ATTManager(String path, int numPersons, int numFaces) {
		this.path = path;
		this.numPersons = numPersons;
		this.numFaces = numFaces;
		this.currentImage = 1;
		this.currentLabel = 1;

		// preload images
		if (PRELOAD_IMAGES) {

			// DEBUG
			System.out.println("Preloading images...");

			// initialise
			images = new ArrayList<int[][]>();

			// loop over persons
			for (int p = 1; p <= numPersons; p++) {

				// DEBUG
				System.out.println("Person " + p + " of " + numPersons + "...");

				// loop over faces
				for (int f = 1; f <= numFaces; f++) {

					// determine path
					String pathFull = path + "s" + p + "/" + f + ".png";

					// read image
					int image[][] = new int[112][92];
					try {
						BufferedImage imageGrey = ImageIO.read(new File(pathFull));
						BufferedImage imageRGB = new BufferedImage(imageGrey.getWidth(), imageGrey.getHeight(), BufferedImage.TYPE_INT_ARGB);
						imageRGB.createGraphics().drawImage(imageGrey, 0, 0, null);
						for (int y = 0; y < imageRGB.getHeight(); y++) {
							for (int x = 0; x < imageRGB.getWidth(); x++) {
								int rgb = imageRGB.getRGB(x, y);
								int red = imageRGB.getColorModel().getRed(rgb);
								image[y][x] = red;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					// add image
					images.add(image);

				}

			}

		}

	}

	public int getImageCount() {
		return numPersons * numFaces;
	}

	public int getLabelCount() {
		return numPersons * numFaces;
	}

	public void setCurrent(int current) {
		//this.currentImage = current;
		//this.currentLabel = current;
		// wrap around
		this.currentImage = (current - 1) % getImageCount() + 1;
		this.currentLabel = (current - 1) % getLabelCount() + 1;
	}

	public int[][] readImage() {
		int[][] image = null;

		if (PRELOAD_IMAGES) {

			// read image
			image = images.get(currentImage - 1);

		} else {

			// determine path
			int p = (currentImage - 1) / numFaces + 1;
			int f = (currentImage - 1) % numFaces + 1;
			String pathFull = path + "s" + p + "/" + f + ".png";

			// read image
			image = new int[112][92];
			try {
				BufferedImage bufferedImage = ImageIO.read(new File(pathFull));
				for (int y = 0; y < bufferedImage.getHeight(); y++) {
					for (int x = 0; x < bufferedImage.getWidth(); x++) {
						int rgb = bufferedImage.getRGB(x, y);
						int red = bufferedImage.getColorModel().getRed(rgb);
						image[y][x] = red;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		// increase current index
		currentImage++;

		return image;
	}

	public int readLabel() {
		int label = 0;

		// determine label
		label = (currentLabel - 1) / numFaces + 1;

		// increase current index
		currentLabel++;

		return label;
	}

}
