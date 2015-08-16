package util;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PatternPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private ImagePanel imagePanel;
	private JLabel lbTop;
	private JLabel lbBottom;

	public PatternPanel(ImagePanel imagePanel, String strTop, String strBottom) {
		super();

		this.imagePanel = imagePanel;
		this.lbTop = new JLabel(strTop);
		this.lbBottom = new JLabel(strBottom);

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEtchedBorder());
		add(lbTop, BorderLayout.PAGE_START);
		add(imagePanel);
		add(lbBottom, BorderLayout.PAGE_END);
		Dimension dim = new Dimension(imagePanel.getWidth() + 4, imagePanel.getHeight() + 40);
		setMinimumSize(dim);
		setMaximumSize(dim);
		setPreferredSize(dim);
		setSize(dim);
	}

	public ImagePanel getImagePanel() {
		return imagePanel;
	}

	public JLabel getLbTop() {
		return lbTop;
	}

	public JLabel getLbBottom() {
		return lbBottom;
	}

}
