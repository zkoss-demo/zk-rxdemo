package zk.rx.demo.service;

import io.reactivex.Observable;
import io.reactivex.functions.Predicate;
import zk.rx.demo.domain.Region;
import zk.rx.demo.domain.Robot;

public class RobotTracker {

	private static RobotBackend backend = new RobotBackend();

	static {
		backend.start();
	}

	public Observable<TrackEvent<Robot>> trackRobots(Region trackRegion, long interval) {

		Predicate<TrackEvent<Robot>> filter = event -> {
			boolean wasInside = trackRegion.contains(event.getOldPosition());
			boolean isInside = trackRegion.contains(event.getTarget().getPosition());
			return wasInside || isInside;
		};

		return backend.trackRobots()
				.filter(filter)
				.map(event -> {
					final Robot robot = event.getTarget();
					final boolean wasInside = trackRegion.contains(event.getOldPosition());
					boolean isInside = trackRegion.contains(robot.getPosition());
					if(wasInside && isInside) {
						return event; //stays inside just ON_UPDATE
					} else {
						if(!wasInside) {
							return new TrackEvent<>(TrackEvent.Name.ON_ENTER, robot, event.getOldPosition());
						} else {
							return new TrackEvent<>(TrackEvent.Name.ON_LEAVE, robot, event.getOldPosition());
						}
					}
				});
	}
}
