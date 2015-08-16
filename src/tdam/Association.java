package tdam;

/**
 * Class for an association in the network.
 * <p>
 * An association stores an id to identify it, stores the source unit and the
 * destination unit (i.e. connected units), and has a strength.
 * 
 * @author Matthias Keysermann
 *
 */
public class Association {

	private long id;

	private Unit src;

	private Unit dst;

	private double strength; // V

	public Association(long id, Unit src, Unit dst) {
		this.id = id;
		this.src = src;
		this.dst = dst;
		strength = 0;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Unit getSrc() {
		return src;
	}

	public void setSrc(Unit src) {
		this.src = src;
	}

	public Unit getDst() {
		return dst;
	}

	public void setDst(Unit dst) {
		this.dst = dst;
	}

	public double getStrength() {
		return strength;
	}

	public void setStrength(double strength) {
		this.strength = strength;
	}

	public String toString() {
		String str = "Association";
		str += "  |  id=" + id;
		str += "  |  srcId=" + src.getId();
		str += "  |  dstId=" + dst.getId();
		str += "  |  strength=" + String.format("%.3f", strength);
		return str;
	}

	public void updateStrength() {
		strength += dst.getError() * src.getTrace();
	}

}
