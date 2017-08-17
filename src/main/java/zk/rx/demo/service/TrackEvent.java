package zk.rx.demo.service;

import zk.rx.demo.domain.Position;

public class TrackEvent<T> {
	public enum Name {ON_ENTER, ON_UPDATE, ON_LEAVE}

	private Name name;
	private T current;
	private T previous;

	public TrackEvent(Name name, T current, T previous) {
		this.name = name;
		this.current = current;
		this.previous = previous;
	}

	public Name getName() {
		return name;
	}

	public T getCurrent() {
		return current;
	}

	public T getPrevious() {
		return previous;
	}
}
