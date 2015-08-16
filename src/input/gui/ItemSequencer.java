package input.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import input.SenderUDP;

public class ItemSequencer extends SenderUDP {

	private final static String TITLE = "Item Sequencer";

	private JFrame frame;

	private JTextField tfSequence;

	private JTextField tfItemLength;

	private JTextField tfGap;

	private JTextField tfRepetitions;

	private JLabel lbRepetition;

	private JButton btPlayStop;

	private boolean playing;

	private int repetitions = 10;

	private int repetitionCount = 0;

	private int index;

	private long indexMillis;

	public ItemSequencer() {
		super("ItemSequencer");

		// frame

		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle(TITLE);
		frame.setSize(250, 150);
		frame.setLocation(50, 150);

		JPanel pnMain = new JPanel();
		pnMain.setLayout(new BoxLayout(pnMain, BoxLayout.Y_AXIS));
		frame.getContentPane().add(pnMain);

		// sequence

		JPanel pnSequence = new JPanel();
		pnSequence.setLayout(new BoxLayout(pnSequence, BoxLayout.X_AXIS));
		pnMain.add(pnSequence);

		JLabel lbSequence = new JLabel("Sequence:");
		lbSequence.setMinimumSize(new Dimension(125, 25));
		lbSequence.setMaximumSize(new Dimension(125, 25));
		pnSequence.add(lbSequence);

		tfSequence = new JTextField("abcde");
		tfSequence.setMinimumSize(new Dimension(75, 25));
		tfSequence.setMaximumSize(new Dimension(75, 25));
		pnSequence.add(tfSequence);

		// item length

		JPanel pnItemLength = new JPanel();
		pnItemLength.setLayout(new BoxLayout(pnItemLength, BoxLayout.X_AXIS));
		pnMain.add(pnItemLength);

		JLabel lbItemLength = new JLabel("Item Length (ms):");
		lbItemLength.setMinimumSize(new Dimension(125, 25));
		lbItemLength.setMaximumSize(new Dimension(125, 25));
		pnItemLength.add(lbItemLength);

		tfItemLength = new JTextField("800");
		tfItemLength.setMinimumSize(new Dimension(75, 25));
		tfItemLength.setMaximumSize(new Dimension(75, 25));
		pnItemLength.add(tfItemLength);

		// gap

		JPanel pnGap = new JPanel();
		pnGap.setLayout(new BoxLayout(pnGap, BoxLayout.X_AXIS));
		pnMain.add(pnGap);

		JLabel lbGap = new JLabel("Gap (ms):");
		lbGap.setMinimumSize(new Dimension(125, 25));
		lbGap.setMaximumSize(new Dimension(125, 25));
		pnGap.add(lbGap);

		tfGap = new JTextField("200");
		tfGap.setMinimumSize(new Dimension(75, 25));
		tfGap.setMaximumSize(new Dimension(75, 25));
		pnGap.add(tfGap);

		// repetitions

		JPanel pnRepetitions = new JPanel();
		pnRepetitions.setLayout(new BoxLayout(pnRepetitions, BoxLayout.X_AXIS));
		pnMain.add(pnRepetitions);

		JLabel lbRepetitions = new JLabel("Repetitions:");
		lbRepetitions.setMinimumSize(new Dimension(125, 25));
		lbRepetitions.setMaximumSize(new Dimension(125, 25));
		pnRepetitions.add(lbRepetitions);

		tfRepetitions = new JTextField(String.valueOf(repetitions));
		tfRepetitions.setMinimumSize(new Dimension(75, 25));
		tfRepetitions.setMaximumSize(new Dimension(75, 25));
		pnRepetitions.add(tfRepetitions);

		// repetition & play/stop

		JPanel pnPlayStop = new JPanel();
		pnPlayStop.setLayout(new BoxLayout(pnPlayStop, BoxLayout.X_AXIS));
		pnMain.add(pnPlayStop);

		lbRepetition = new JLabel("Count: " + repetitionCount + "/" + repetitions);
		lbRepetition.setMinimumSize(new Dimension(125, 25));
		lbRepetition.setMaximumSize(new Dimension(125, 25));
		pnPlayStop.add(lbRepetition);

		// play/stop button
		btPlayStop = new JButton("Play");
		btPlayStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				playing = !playing;
				if (playing) {
					index = 0;
					indexMillis = System.currentTimeMillis();
					repetitions = Integer.parseInt(tfRepetitions.getText());
					repetitionCount = 0;
					tfSequence.setEditable(false);
					tfItemLength.setEditable(false);
					tfGap.setEditable(false);
					tfRepetitions.setEditable(false);
					lbRepetition.setText("Count: " + repetitionCount + "/" + repetitions);
					btPlayStop.setText("Stop");
				} else {
					tfSequence.setEditable(true);
					tfItemLength.setEditable(true);
					tfGap.setEditable(true);
					tfRepetitions.setEditable(true);
					btPlayStop.setText("Play");
				}
			}
		});
		pnPlayStop.add(btPlayStop);

		// finalise
		frame.setVisible(true);

	}

	@Override
	protected LinkedList<byte[]> readInput() {
		LinkedList<byte[]> inputs = new LinkedList<byte[]>();

		if (playing) {

			// calculate current index
			String sequence = tfSequence.getText();

			// prepare input
			String input = sequence.substring(index, index + 1);

			// prepare data		
			for (int i = 0; i < input.length(); i++) {
				String inputChar = input.substring(i, i + 1) + "\n";
				inputs.add(inputChar.getBytes());
			}

			// increase index
			int itemLength = Integer.parseInt(tfItemLength.getText());
			if (System.currentTimeMillis() > indexMillis + itemLength) {
				index++;
				indexMillis = System.currentTimeMillis();
				if (index >= sequence.length()) {
					index = 0;

					// increase repetition count
					repetitionCount++;
					lbRepetition.setText("Count: " + repetitionCount + "/" + repetitions);

					// check for maximum number of repetitions
					if (repetitionCount == repetitions) {
						btPlayStop.doClick();
					}

				}
			}

			// sleep			
			try {
				Thread.sleep(Integer.parseInt(tfGap.getText()));
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return inputs;
	}

	public static void main(String[] args) {
		ItemSequencer itemSequencer = new ItemSequencer();
		itemSequencer.run();
	}
}
