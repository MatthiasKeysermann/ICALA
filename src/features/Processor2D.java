package features;

import features.Feature2D.FEATURE2D;

import java.util.LinkedList;

public abstract class Processor2D {

	protected int inputWidth;

	protected int inputHeight;

	protected FEATURE2D[] featureTypes;

	protected int numLevels;

	protected Feature2D[] features; // entire feature list for all levels and windows

	public Processor2D(int inputWidth, int inputHeight, FEATURE2D[] featureTypes, int numLevels) {

		// initialise
		this.inputWidth = inputWidth;
		this.inputHeight = inputHeight;
		this.featureTypes = featureTypes;
		this.numLevels = numLevels;
		LinkedList<Feature2D> featureList = new LinkedList<Feature2D>();

		// loop over levels
		for (int level = 1; level <= numLevels; level++) {

			// compute parameters
			int windowsPerDim = (int) Math.pow(2, level - 1);
			double windowStepX = Math.pow(0.5, level - 1) * inputWidth;
			double windowStepY = Math.pow(0.5, level - 1) * inputHeight;
			int windowSizeX = (int) Math.round(windowStepX);
			int windowSizeY = (int) Math.round(windowStepY);

			// create features for this level
			Feature2D[] featuresForLevel = new Feature2D[featureTypes.length];
			for (int i = 0; i < featureTypes.length; i++) {
				featuresForLevel[i] = new Feature2D(featureTypes[i], windowSizeX, windowSizeY);
			}

			// move over input
			for (int windowNumY = 0; windowNumY < windowsPerDim; windowNumY++) {
				for (int windowNumX = 0; windowNumX < windowsPerDim; windowNumX++) {

					// loop over features
					for (int i = 0; i < featuresForLevel.length; i++) {

						// append feature to list
						featureList.add(featuresForLevel[i]);

					}

				}
			}

		}

		// convert feature list to array
		features = new Feature2D[featureList.size()];
		for (int i = 0; i < featureList.size(); i++) {
			features[i] = featureList.get(i);
		}

	}

	public abstract double[][] normaliseInput(double[][] input, double min, double max);

	/*
	 * Processes the given (normalised) input and computes a feature vector
	 */
	public double[] process(double[][] inputNormalised) {

		// initialise
		double[] featureVector = new double[features.length];
		int featureVectorIndex = 0;

		// loop over levels
		for (int level = 1; level <= numLevels; level++) {

			// compute parameters
			int windowsPerDim = (int) Math.pow(2, level - 1);
			double windowStepX = Math.pow(0.5, level - 1) * inputWidth;
			double windowStepY = Math.pow(0.5, level - 1) * inputHeight;
			int windowSizeX = (int) Math.round(windowStepX);
			int windowSizeY = (int) Math.round(windowStepY);

			// move over input
			for (int windowNumY = 0; windowNumY < windowsPerDim; windowNumY++) {
				int windowOffsetY = (int) Math.floor(windowNumY * windowStepY);
				for (int windowNumX = 0; windowNumX < windowsPerDim; windowNumX++) {
					int windowOffsetX = (int) Math.floor(windowNumX * windowStepX);

					// loop over features
					for (int i = 0; i < featureTypes.length; i++) {

						// retrieve matrix
						double[][] matrix = features[featureVectorIndex].getMatrix();

						// compute overlap
						double sum = 0.0;
						double count = 0.0;
						for (int y = 0; y < windowSizeY; y++) {
							for (int x = 0; x < windowSizeX; x++) {
								sum += inputNormalised[windowOffsetX + x][windowOffsetY + y] * matrix[x][y];
								count += Math.abs(matrix[x][y]);
							}
						}
						double overlap = 0;
						if (count > 0) {
							overlap = sum / count;
						}

						// set output
						featureVector[featureVectorIndex] = overlap;

						// increment index
						featureVectorIndex++;

					}

				}
			}

		}

		return featureVector;
	}

	/*
	 * Overlays all features according to the given feature vector and reconstructs a (normalised) input 
	 */
	public double[][] reconstruct(double[] featureVector) {

		// initialise
		double[][] inputSum = new double[inputWidth][inputHeight];
		double[][] inputCount = new double[inputWidth][inputHeight];
		int featureVectorIndex = 0;

		// loop over levels
		for (int level = 1; level <= numLevels; level++) {

			// compute parameters
			int windowsPerDim = (int) Math.pow(2, level - 1);
			double windowStepX = Math.pow(0.5, level - 1) * inputWidth;
			double windowStepY = Math.pow(0.5, level - 1) * inputHeight;
			int windowSizeX = (int) Math.round(windowStepX);
			int windowSizeY = (int) Math.round(windowStepY);

			// move over input
			for (int windowNumY = 0; windowNumY < windowsPerDim; windowNumY++) {
				int windowOffsetY = (int) Math.floor(windowNumY * windowStepY);
				for (int windowNumX = 0; windowNumX < windowsPerDim; windowNumX++) {
					int windowOffsetX = (int) Math.floor(windowNumX * windowStepX);

					// loop over features
					for (int i = 0; i < featureTypes.length; i++) {

						// retrieve matrix
						double[][] matrix = features[featureVectorIndex].getMatrix();

						// compose input
						for (int y = 0; y < windowSizeY; y++) {
							for (int x = 0; x < windowSizeX; x++) {
								inputSum[windowOffsetX + x][windowOffsetY + y] += featureVector[featureVectorIndex] * matrix[x][y];
								inputCount[windowOffsetX + x][windowOffsetY + y] += Math.abs(matrix[x][y]);
							}
						}

						// increment index
						featureVectorIndex++;

					}

				}
			}

		}

		// normalise input
		double[][] inputNormalised = new double[inputWidth][inputHeight];
		for (int y = 0; y < inputHeight; y++) {
			for (int x = 0; x < inputWidth; x++) {
				inputNormalised[x][y] = 0;
				if (inputCount[x][y] > 0) {
					inputNormalised[x][y] = inputSum[x][y] / inputCount[x][y];
				}
			}
		}

		return inputNormalised;
	}

	public abstract double[][] denormaliseInput(double[][] inputNormalised, double min, double max);

}
