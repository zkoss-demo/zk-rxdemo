package zk.rx.demo.service;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;
import zk.rx.demo.domain.Position;
import zk.rx.demo.domain.Robot;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class RobotBackend {

	private Random random = new Random();

	private Map<Long, Robot> allRobots;
	private ConnectableObservable<TrackEvent<Robot>> hotRobotObservable;
	private Disposable disposable;



	public void start() {
		allRobots = LongStream.range(0L, 20L)
				.mapToObj(index -> new Robot(100 + index))
				.collect(Collectors.toConcurrentMap(Robot::getId, robot -> robot));

		Observable<TrackEvent<Robot>> obs = Observable.create(source -> {
			new Thread(() -> {
				while (!source.isDisposed()) {
					try {
						int millis = 10;
						Thread.sleep(millis);
//						Logger.log("tick after " + millis + "ms");
						updateRandomRobots()
//								.peek(robot -> Logger.log("updated robot: " + robot))
								.forEach(event -> {
									allRobots.put(event.getCurrent().getId(), event.getCurrent());
									source.onNext(event);
								});
					} catch (InterruptedException e) {
						source.onComplete();
					}
				}
			}, "BackgroundThread").start();
		});
		hotRobotObservable = obs.publish();
		disposable = hotRobotObservable.connect();
	}

	public Observable<TrackEvent<Robot>> trackRobots() {
		Stream<TrackEvent<Robot>> currentRobots = allRobots.values()
				.stream()
				.map(robot -> new TrackEvent<>(TrackEvent.Name.ON_ENTER, robot, robot));
		return hotRobotObservable
				.startWith(currentRobots::iterator);
	}

	private Stream<TrackEvent<Robot>> updateRandomRobots() {
		return allRobots.values().stream()
				.filter(this::shallUpdate)
				.map(this::updateRobot);
	}

	private TrackEvent<Robot> updateRobot(Robot robot) {
		double time = (double)System.nanoTime() / TimeUnit.SECONDS.toNanos(1);

		double x = Math.sin(time * 14 / robot.getId() + robot.getId() * Math.PI * 0.2) * 45 + 50 + Math.sin(time * 8 + robot.getId()) * 3;
		double y = Math.cos(time * 13 / robot.getId() + robot.getId() * Math.PI * 0.2) * 45 + 50 + Math.cos(time * 8 + robot.getId()) * 3;

		Robot updatedRobot = robot.update(new Position(x, y), randomStatus(robot.getStatus()));
		return new TrackEvent<>(TrackEvent.Name.ON_UPDATE, updatedRobot, robot);
	}

	private double clamp(double lower, double upper, double value) {
		return Math.min(upper, Math.max(lower, value));
	}

	private Robot.Status randomStatus(Robot.Status oldStatus) {
		return Math.random() < 0.02 ? Robot.Status.values()[random.nextInt(Robot.Status.values().length)] : oldStatus;
	}

	private boolean shallUpdate(Robot robot) {
		return Math.random() < 0.3;
	}

}
