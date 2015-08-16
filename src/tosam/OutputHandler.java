package tosam;

import java.util.LinkedList;

/**
 * Interface for providing a list of units matching a given name.
 * 
 * @author Matthias Keysermann
 *
 */
public interface OutputHandler extends Runnable {

	public LinkedList<Unit> fetchUnits(String name);

}
