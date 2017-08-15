package zk.rx.demo.service;

public class TrackEvent<T> {
	public enum Name {ON_ENTER, ON_UPDATE, ON_LEAVE}

	private Name name;
	private T target;

	public TrackEvent(Name name, T target) {
		this.target = target;
	}

	public Name getName() {
		return name;
	}

	public T getTarget() {
		return target;
	}
}
