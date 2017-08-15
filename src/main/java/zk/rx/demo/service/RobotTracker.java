package zk.rx.demo.service;

import io.reactivex.Observable;
import zk.rx.demo.vm.Robot;

public class RobotTracker {

	private static RobotBackend backend = new RobotBackend();

	static {
		backend.start();
	}

	public Observable<TrackEvent<Robot>> trackRobots() {
		//TODO implement range tracking logic
		return backend.trackRobots();
	}
}
