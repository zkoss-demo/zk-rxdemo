package zk.rx.demo.service;

import zk.rx.demo.domain.Position;

public class TrackEvent<T> {
	public enum Name {ON_ENTER, ON_UPDATE, ON_LEAVE}

	private Name name;
	private T target;
	private Position oldPosition;

	public TrackEvent(Name name, T target, Position oldPosition) {
		this.name = name;
		this.target = target;
		this.oldPosition = oldPosition;
	}

	public Name getName() {
		return name;
	}

	public T getTarget() {
		return target;
	}

	public Position getOldPosition() {
		return oldPosition;
	}
}
