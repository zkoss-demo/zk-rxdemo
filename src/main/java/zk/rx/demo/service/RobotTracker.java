package zk.rx.demo.service;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import zk.rx.demo.domain.Region;
import zk.rx.demo.domain.Robot;

public class RobotTracker {

	private static RobotBackend backend = new RobotBackend();

	static {
		backend.start();
	}

	public Observable<TrackEvent<Robot>> trackRobots(Predicate<Robot> filter) {


		Function<TrackEvent<Robot>, TrackingMatch> matcher = (TrackEvent<Robot> event) -> new TrackingMatch(event, filter);

		return backend.trackRobots()
				.map(matcher)
				.filter(TrackingMatch::anyMatch)
				.map(result -> {
					TrackEvent<Robot> event = result.getEvent();
					Robot current = event.getCurrent();
					Robot previous = event.getPrevious();

					if(result.allMatch()) {
						return event; //stays inside just ON_UPDATE
					} else {
						if(!result.isPreviousMatch()) {
							return new TrackEvent<>(TrackEvent.Name.ON_ENTER, current, previous);
						} else {
							return new TrackEvent<>(TrackEvent.Name.ON_LEAVE, current, previous);
						}
					}
				});
	}

	private static class TrackingMatch {
		TrackEvent<Robot> event;
		boolean currentMatch;
		boolean previousMatch;

		public TrackingMatch(TrackEvent<Robot> event, Predicate<Robot> filter) throws Exception {
			this.event = event;
			this.currentMatch = filter.test(event.getCurrent());
			this.previousMatch = filter.test(event.getPrevious());
		}

		public boolean isCurrentMatch() {
			return currentMatch;
		}

		public boolean isPreviousMatch() {
			return previousMatch;
		}

		public boolean anyMatch() {
			return isCurrentMatch() || isPreviousMatch();
		}

		public boolean allMatch() {
			return isCurrentMatch() && isPreviousMatch();
		}

		public TrackEvent<Robot> getEvent() {
			return event;
		}
	}

}
