package tdam;

import java.util.LinkedList;

/**
 * Class for a unit in the network.
 * <p>
 * A unit stores an id to identify it, can hold any kind of data pattern, has an
 * input value, a trace value, a signal sum, an old (previous) signal sum and a
 * prediction error. Also stores all incoming and all outgoing associations for
 * faster access.
 * 
 * @author Matthias Keysermann
 *
 */
public class Unit {

	private long id;

	private Object data;

	private double input; // X (if source/CS) or lambda (if destination/US)

	private double trace; // trace

	private static final double DELTA = 0.2;

	private double signalSum; // Vbar

	private double signalSumOld; // old Vbar

	private double error; // alpha beta error

	private static final double ALPHA = 0.1; //0.1;

	private static final double BETA = 1.0;

	private static final double GAMMA = 0.95;

	private LinkedList<Association> associationsIn;

	private LinkedList<Association> associationsOut;

	public Unit(long id, Object data) {
		this.id = id;
		this.data = data;
		input = 0;
		trace = 0;
		signalSum = 0;
		signalSumOld = 0;
		error = 0;
		associationsIn = new LinkedList<Association>();
		associationsOut = new LinkedList<Association>();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public double getInput() {
		return input;
	}

	public void setInput(double input) {
		this.input = input;
	}

	public double getTrace() {
		return trace;
	}

	public void setTrace(double trace) {
		this.trace = trace;
	}

	public double getSignalSum() {
		return signalSum;
	}

	public void setSignalSum(double signalSum) {
		if (signalSum < 0) {
			signalSum = 0;
		}
		this.signalSum = signalSum;
	}

	public double getSignalSumOld() {
		return signalSumOld;
	}

	public void setSignalSumOld(double signalSumOld) {
		if (signalSumOld < 0) {
			signalSumOld = 0;
		}
		this.signalSumOld = signalSumOld;
	}

	public double getError() {
		return error;
	}

	public void setError(double error) {
		this.error = error;
	}

	public LinkedList<Association> getAssociationsIn() {
		return associationsIn;
	}

	public void setAssociationsIn(LinkedList<Association> associationsIn) {
		this.associationsIn = associationsIn;
	}

	public LinkedList<Association> getAssociationsOut() {
		return associationsOut;
	}

	public void setAssociationsOut(LinkedList<Association> associationsOut) {
		this.associationsOut = associationsOut;
	}

	public String toString() {
		String str = "Unit";
		str += "  |  id=" + id;
		str += "  |  data=" + data;
		str += "  |  input=" + String.format("%.3f", input);
		str += "  |  trace=" + String.format("%.3f", trace);
		str += "  |  signalSum=" + String.format("%.3f", signalSum);
		str += "  |  error=" + String.format("%.3f", error);
		return str;
	}

	public void calculateError() {
		error = ALPHA * BETA * (input + GAMMA * signalSum - signalSumOld);
	}

	public void updateTrace() {
		trace += DELTA * (input - trace);
	}

	public void storeSignalSum() {
		signalSumOld = signalSum;
	}

	public void resetInput() {
		input = 0;
	}

}
