package evaluation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import com.jhlabs.image.RotateFilter;

import util.ImagePanel;

public class DisplayerFacesATT implements Runnable {

	private final static String NAME = "DisplayerFacesATT";

	private boolean running;

	private final static String FACES_PATH = "res/ATTFaces/";
	private final static int NUM_PERSONS_MAX = 40; // 40
	private final static int NUM_FACES_MAX = 10; // 10
	BufferedImage[][] images;

	private int personIndex;
	private int faceIndex;
	private boolean updatePersonIndex;
	private boolean updateFaceIndex;

	public static enum Order {
		RANDOM, SEQUENTIAL//, PERMUTATED, PROBABILISTIC
	};

	private int numPersons;
	private int numFaces;
	private Order personOrder;
	private Order faceOrder;
	private long timePerPerson;
	private long timePerFace;
	private long timeLastUpdatePerson;
	private long timeLastUpdateFace;

	private Random random;

	private String labelText;
	public final static String PREFIX_PERSON = "Person";

	private final static int IMAGE_TYPE = BufferedImage.TYPE_BYTE_GRAY;
	//private final static int IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;
	private final static int SIZE_FACTOR = 6;
	private final static int IMAGE_WIDTH = 92 * SIZE_FACTOR;
	private final static int IMAGE_HEIGHT = 112 * SIZE_FACTOR;
	private final static BufferedImage IMAGE_BLANK = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_TYPE);

	private boolean transformImage = true;
	private float scaleFactorMax = 0.01f; // 0.01f
	private int rotateAngleMax = 1; // 1
	private int translateMax = 10; // 10

	private JFrame frame;
	private ImagePanel imagePanel;

	public DisplayerFacesATT() {

		// initialise
		running = true;
		random = new Random();
		numPersons = 40;
		numFaces = 10;
		personOrder = Order.SEQUENTIAL;
		faceOrder = Order.SEQUENTIAL;
		timePerPerson = 10000; // ms
		timePerFace = 1000; // ms
		timeLastUpdatePerson = 0;
		timeLastUpdateFace = 0;
		personIndex = -1;
		faceIndex = -1;
		updatePersonIndex = true;
		updateFaceIndex = true;

		// load images
		images = new BufferedImage[NUM_PERSONS_MAX][NUM_FACES_MAX];
		for (int p = 0; p < NUM_PERSONS_MAX; p++) {
			for (int f = 0; f < NUM_FACES_MAX; f++) {
				String path = FACES_PATH + "s" + (p + 1) + "/" + (f + 1) + ".png";
				System.out.println("Reading image " + path);
				try {
					// read image
					BufferedImage imageOriginal = ImageIO.read(new File(path));
					// convert to image type					
					BufferedImage image = new BufferedImage(imageOriginal.getWidth(), imageOriginal.getHeight(), IMAGE_TYPE);
					image.createGraphics().drawImage(imageOriginal, 0, 0, null);
					// store image
					images[p][f] = image;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		// create frame
		frame = new JFrame(NAME);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(600, 600);
		frame.setLocation(0, 0);
		frame.setLayout(new GridBagLayout());
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.getContentPane().setBackground(Color.BLACK);
		frame.setVisible(true);

		// create image panel
		imagePanel = new ImagePanel(IMAGE_BLANK);
		frame.add(imagePanel);

	}

	public void pause() {
		running = false;
	}

	public void resume() {
		running = true;
	}

	@Override
	public void run() {

		// initialise
		int personIndexOld = -1;
		int faceIndexOld = -1;

		while (true) {

			if (running) {

				// update indeces
				long time = System.currentTimeMillis();

				// update person index
				if (updatePersonIndex) {
					if (time - timeLastUpdatePerson >= timePerPerson) {
						timeLastUpdatePerson = time;
						switch (personOrder) {
						case RANDOM:
							personIndex = random.nextInt(numPersons);
							break;
						case SEQUENTIAL:
							personIndex = (personIndex + 1) % numPersons;
							break;
						}
					}
				}

				// update face index
				if (updateFaceIndex) {
					if (time - timeLastUpdateFace >= timePerFace) {
						timeLastUpdateFace = time;
						switch (faceOrder) {
						case RANDOM:
							faceIndex = random.nextInt(numFaces);
							break;
						case SEQUENTIAL:
							faceIndex = (faceIndex + 1) % numFaces;
							break;
						}
					}
				}

				// update image and label text
				if (personIndex != personIndexOld || faceIndex != faceIndexOld) {

					// update old indices
					personIndexOld = personIndex;
					faceIndexOld = faceIndex;

					// fetch image
					BufferedImage image = IMAGE_BLANK;
					if (personIndex > -1 && faceIndex > -1) {
						image = images[personIndex][faceIndex];
					}

					// resize image
					BufferedImage imageResized = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, image.getType());
					Graphics g = imageResized.getGraphics();
					((Graphics2D) g).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					g.drawImage(image, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, null);
					image = imageResized;

					// transform image
					if (transformImage) {

						// scale image
						float scaleFactor = random.nextFloat() * scaleFactorMax * 2 - scaleFactorMax;
						int scaledWidth = image.getWidth() + (int) ((float) image.getWidth() * scaleFactor);
						int scaledHeight = image.getHeight() + (int) ((float) image.getHeight() * scaleFactor);
						BufferedImage imageScaled = new BufferedImage(scaledWidth, scaledHeight, image.getType());
						Graphics gScaled = imageScaled.getGraphics();
						gScaled.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
						image = imageScaled;

						// rotate image
						float angleDegrees = (float) (random.nextInt(rotateAngleMax * 2) - rotateAngleMax);
						float angleRadians = angleDegrees * (float) Math.PI / 180.0f;
						float angleCos = (float) Math.cos(Math.abs(angleRadians));
						float angleSin = (float) Math.sin(Math.abs(angleRadians));
						int imageWidth = image.getWidth();
						int imageHeight = image.getHeight();
						int rotatedWidth = (int) Math.ceil(angleCos * imageWidth + angleSin * imageHeight);
						int rotatedHeight = (int) Math.ceil(angleSin * imageWidth + angleCos * imageHeight);
						BufferedImage imageRotated = new BufferedImage(rotatedWidth, rotatedHeight, image.getType());
						RotateFilter rotateFilter = new RotateFilter(angleRadians);
						rotateFilter.filter(image, imageRotated);
						image = imageRotated;

						// translate image
						int translateX = 0;
						int translateY = 0;
						if (translateMax > 0) {
							translateX = random.nextInt(translateMax * 2) - translateMax;
							translateY = random.nextInt(translateMax * 2) - translateMax;
						}
						BufferedImage imageTranslated = new BufferedImage(image.getWidth() + translateMax, image.getHeight() + translateMax, image.getType());
						Graphics gTranslate = imageTranslated.getGraphics();
						gTranslate.drawImage(image, translateMax / 2 + translateX, translateMax / 2 + translateY, image.getWidth(), image.getHeight(), null);
						image = imageTranslated;

					}

					// update image
					imagePanel.setImage(image);
					imagePanel.updateUI();

					// update label text
					labelText = "";
					if (personIndex > -1 && faceIndex > -1) {
						labelText = PREFIX_PERSON + (personIndex + 1);
					}

				}

			}

		}

	}

	public String getLabelText() {
		return labelText;
	}

	public int getNumPersons() {
		return numPersons;
	}

	public void setNumPersons(int numPersons) {
		this.numPersons = numPersons;
	}

	public int getNumFaces() {
		return numFaces;
	}

	public void setNumFaces(int numFaces) {
		this.numFaces = numFaces;
	}

	public Order getPersonOrder() {
		return personOrder;
	}

	public void setPersonOrder(Order personOrder) {
		this.personOrder = personOrder;
	}

	public Order getFaceOrder() {
		return faceOrder;
	}

	public void setFaceOrder(Order faceOrder) {
		this.faceOrder = faceOrder;
	}

	public long getTimePerPerson() {
		return timePerPerson;
	}

	public void setTimePerPerson(long timePerPerson) {
		this.timePerPerson = timePerPerson;
	}

	public long getTimePerFace() {
		return timePerFace;
	}

	public void setTimePerFace(long timePerFace) {
		this.timePerFace = timePerFace;
	}

	public int getPersonIndex() {
		return personIndex;
	}

	public void setPersonIndex(int personIndex) {
		this.personIndex = personIndex;
	}

	public int getFaceIndex() {
		return faceIndex;
	}

	public void setFaceIndex(int faceIndex) {
		this.faceIndex = faceIndex;
	}

	public boolean isUpdatePersonIndex() {
		return updatePersonIndex;
	}

	public void setUpdatePersonIndex(boolean updatePersonIndex) {
		this.updatePersonIndex = updatePersonIndex;
	}

	public boolean isUpdateFaceIndex() {
		return updateFaceIndex;
	}

	public void setUpdateFaceIndex(boolean updateFaceIndex) {
		this.updateFaceIndex = updateFaceIndex;
	}

	public boolean isTransformImage() {
		return transformImage;
	}

	public void setTransformImage(boolean transformImage) {
		this.transformImage = transformImage;
	}

	public float getScaleFactorMax() {
		return scaleFactorMax;
	}

	public void setScaleFactorMax(float scaleFactorMax) {
		this.scaleFactorMax = scaleFactorMax;
	}

	public int getRotateAngleMax() {
		return rotateAngleMax;
	}

	public void setRotateAngleMax(int rotateAngleMax) {
		this.rotateAngleMax = rotateAngleMax;
	}

	public int getTranslateMax() {
		return translateMax;
	}

	public void setTranslateMax(int translateMax) {
		this.translateMax = translateMax;
	}

	public static void main(String[] args) {
		DisplayerFacesATT displayerFacesATT = new DisplayerFacesATT();
		displayerFacesATT.run();
	}

}
