package features;

public class Feature2D {

	public static enum FEATURE2D {
		EDGE_VERTICAL, EDGE_HORIZONTAL, EDGE_DIAGONAL_A, EDGE_DIAGONAL_B, INTENSITY_VERTICAL, INTENSITY_VERTICAL_INVERSE, INTENSITY_HORIZONTAL, INTENSITY_HORIZONTAL_INVERSE, INTENSITY_DIAGONAL_A, INTENSITY_DIAGONAL_A_INVERSE, INTENSITY_DIAGONAL_B, INTENSITY_DIAGONAL_B_INVERSE
	}

	private double[][] matrix;

	public Feature2D(FEATURE2D type, int width, int height) {
		matrix = new double[width][height];

		switch (type) {

		case EDGE_VERTICAL:
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (x < width / 2) {
						matrix[x][y] = +1.0;
					} else {
						matrix[x][y] = -1.0;
					}
				}
			}
			break;

		case EDGE_HORIZONTAL:
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (y < height / 2) {
						matrix[x][y] = +1.0;
					} else {
						matrix[x][y] = -1.0;
					}
				}
			}
			break;

		case EDGE_DIAGONAL_A:
			for (int y = 0; y < height; y++) {
				int maxX = Math.round((float) y / height * width);
				for (int x = 0; x < width; x++) {
					if (x < maxX) {
						matrix[x][y] = +1.0;
					} else {
						matrix[x][y] = -1.0;
					}
				}
			}
			break;

		case EDGE_DIAGONAL_B:
			for (int y = 0; y < height; y++) {
				int maxX = Math.round((float) y / height * width);
				for (int x = 0; x < width; x++) {
					if (x < width - maxX) {
						matrix[x][y] = -1.0;
					} else {
						matrix[x][y] = +1.0;
					}
				}
			}
			break;

		case INTENSITY_VERTICAL:
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (x < width / 2) {
						matrix[x][y] = 1.0;
					} else {
						matrix[x][y] = 0.0;
					}
				}
			}
			break;

		case INTENSITY_VERTICAL_INVERSE:
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (x < width / 2) {
						matrix[x][y] = 0.0;
					} else {
						matrix[x][y] = 1.0;
					}
				}
			}
			break;

		case INTENSITY_HORIZONTAL:
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (y < height / 2) {
						matrix[x][y] = 1.0;
					} else {
						matrix[x][y] = 0.0;
					}
				}
			}
			break;

		case INTENSITY_HORIZONTAL_INVERSE:
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (y < height / 2) {
						matrix[x][y] = 0.0;
					} else {
						matrix[x][y] = 1.0;
					}
				}
			}
			break;

		case INTENSITY_DIAGONAL_A:
			for (int y = 0; y < height; y++) {
				int maxX = Math.round((float) y / height * width);
				for (int x = 0; x < width; x++) {
					if (x < maxX) {
						matrix[x][y] = 1.0;
					} else {
						matrix[x][y] = 0.0;
					}
				}
			}
			break;

		case INTENSITY_DIAGONAL_A_INVERSE:
			for (int y = 0; y < height; y++) {
				int maxX = Math.round((float) y / height * width);
				for (int x = 0; x < width; x++) {
					if (x < maxX) {
						matrix[x][y] = 0.0;
					} else {
						matrix[x][y] = 1.0;
					}
				}
			}
			break;

		case INTENSITY_DIAGONAL_B:
			for (int y = 0; y < height; y++) {
				int maxX = Math.round((float) y / height * width);
				for (int x = 0; x < width; x++) {
					if (x < width - maxX) {
						matrix[x][y] = 1.0;
					} else {
						matrix[x][y] = 0.0;
					}
				}
			}
			break;

		case INTENSITY_DIAGONAL_B_INVERSE:
			for (int y = 0; y < height; y++) {
				int maxX = Math.round((float) y / height * width);
				for (int x = 0; x < width; x++) {
					if (x < width - maxX) {
						matrix[x][y] = 0.0;
					} else {
						matrix[x][y] = 1.0;
					}
				}
			}
			break;

		default:
			break;

		}
	}

	public double[][] getMatrix() {
		return matrix;
	}

}
