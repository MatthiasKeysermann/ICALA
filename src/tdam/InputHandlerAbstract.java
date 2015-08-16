package tdam;

import java.util.LinkedList;

/**
 * Abstract class for writing to and reading from an input buffer. The size of
 * the buffer is not bounded.
 * 
 * @author Matthias Keysermann
 *
 */
public abstract class InputHandlerAbstract implements InputHandler {

	protected LinkedList<Object> buffer;

	public InputHandlerAbstract() {
		buffer = new LinkedList<Object>();
	}

	@Override
	public boolean isBufferEmpty() {
		synchronized (buffer) {
			return buffer.isEmpty();
		}
	}

	@Override
	public Object pollBuffer() {
		synchronized (buffer) {
			return buffer.poll();
		}
	}

	@Override
	public void addToBuffer(Object object) {
		synchronized (buffer) {
			buffer.add(object);
		}
	}

}
