package zk.rx.demo.domain;

public class Robot {
	public enum Mood {HAPPY, NEUTRAL, ANGRY}

	private long id;
	private Position position;
	private Mood mood = Mood.HAPPY;

	public Robot(long id, Position position, Mood status) {
		this.id = id;
		this.position = position;
		this.mood = status;
	}

	public long getId() {
		return id;
	}

	public Position getPosition() {
		return position;
	}

	public Mood getMood() {
		return mood;
	}

	public Robot update(Position position, Mood mood) {
		return new Robot(this.getId(), position, mood);
	}
}
