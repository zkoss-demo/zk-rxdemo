package zk.rx.demo.domain;

public class Region {
	private double left;
	private double top;
	private double right;
	private double bottom;

	public Region(double left, double top, double right, double bottom) {
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
	}

	public boolean contains(Position pos) {
		return pos.getX() >= left &&
				pos.getX() <= right &&
				pos.getY() >= top &&
				pos.getY() <= bottom;
	}
}
