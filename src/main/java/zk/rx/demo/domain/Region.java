package zk.rx.demo.domain;

public class Region {
	private double left;
	private double top;
	private double right;
	private double bottom;
	private boolean inverted;

	public Region(double left, double top, double right, double bottom, boolean inverted) {
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
		this.inverted = inverted;
	}

	public boolean contains(Position pos) {
		return (pos.getX() >= left &&
				pos.getX() <= right &&
				pos.getY() >= top &&
				pos.getY() <= bottom) == !inverted;
	}
}
