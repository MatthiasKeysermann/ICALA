package tdam;

/**
 * Interface for writing to and reading from an input buffer.
 * 
 * @author Matthias Keysermann
 *
 */
public interface InputHandler extends Runnable {

	public boolean isBufferEmpty();

	public Object pollBuffer();

	public void addToBuffer(Object object);

}
