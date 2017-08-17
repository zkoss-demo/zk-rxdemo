package zk.rx.demo.domain;

public class Robot {
	public enum Status {HAPPY, NEUTRAL, ANGRY}

	private long id;
	private Position position;
	private Status status = Status.HAPPY;

	public Robot(long id) {
		this.id = id;
	}

	public Robot(long id, Position position, Status status) {
		this.id = id;
		this.position = position;
		this.status = status;
	}

	public long getId() {
		return id;
	}

	public Position getPosition() {
		return position;
	}

	public Status getStatus() {
		return status;
	}

	public Robot update(Position position, Status status) {
		return new Robot(this.getId(), position, status);
	}
}
