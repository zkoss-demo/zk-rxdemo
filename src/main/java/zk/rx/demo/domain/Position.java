package zk.rx.demo.domain;

public class Position {

	private double x = 0;
	private double y = 0;

	public Position(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}
}
