package zk.rx.demo.vm;

public class Robot {
	public enum Status {HAPPY, NEUTRAL, ANGRY}

	private long id;
	private double posX = 0;
	private double posY = 0;
	private Status status = Status.HAPPY;

	public Robot(long id) {
		this.id = id;
	}

	public Robot(long id, double posX, double posY, Status status) {
		this.id = id;
		this.posX = posX;
		this.posY = posY;
		this.status = status;
	}

	public long getId() {
		return id;
	}

	public double getPosX() {
		return posX;
	}

	public double getPosY() {
		return posY;
	}

	public Status getStatus() {
		return status;
	}

	public Robot update(double x, double y, Status status) {
		return new Robot(this.getId(), x, y, status);
	}
}
