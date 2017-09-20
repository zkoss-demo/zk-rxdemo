package zk.rx.demo.service;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;
import zk.rx.demo.domain.Position;
import zk.rx.demo.domain.Robot;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Backend service starting a hot observable generating a stream of {@link TrackEvent}s.
 */
public class RobotBackend {
	private static int NUM_ROBOTS = 20;
	//Wait 10ms between updates 100updates per second
	private static int TICK_INTERVAL = 10;
 	//At each tick a robot has a 30% chance to get updated (just to add some randomness).
	private static double CHANCE_TO_UPDATE = 0.3;
	private static double CHANCE_TO_CHANGE_MOOD = 0.02;
	//Contains all robots at their most recent status
	private Map<Long, Robot> allRobots;
	private ConnectableObservable<TrackEvent<Robot>> hotRobotObservable;
	private Disposable disposable;
	private Random random = new Random();

	public void start() {
		allRobots = LongStream.range(0L, NUM_ROBOTS)
				.mapToObj(index -> new Robot(100 + index, new Position(0, 0), Robot.Status.NEUTRAL))
				.collect(Collectors.toConcurrentMap(Robot::getId, robot -> robot));

		Observable<TrackEvent<Robot>> obs = Observable.create(source -> {
			new Thread(() -> {
				while (!source.isDisposed()) {
					try {
						Thread.sleep(TICK_INTERVAL);
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

	public void stop() {
		disposable.dispose();
	}

	public Observable<TrackEvent<Robot>> trackRobots() {
		Stream<TrackEvent<Robot>> currentRobots = allRobots.values()
				.stream()
				.map(robot -> new TrackEvent<>(TrackEvent.Name.ON_ENTER, robot, robot));
		//start with initial state for all robots prepended to the hot stream of updates
		return hotRobotObservable
				.startWith(currentRobots::iterator);
	}

	private Stream<TrackEvent<Robot>> updateRandomRobots() {
		return allRobots.values().stream()
				.filter(robot -> random.nextDouble() < CHANCE_TO_UPDATE)
				.map(this::updateRobot);
	}

	private TrackEvent<Robot> updateRobot(Robot robot) {
		double time = (double)System.nanoTime() / TimeUnit.SECONDS.toNanos(1);

		double x = Math.sin(time * 14 / robot.getId() + robot.getId() * Math.PI * 0.2) * 45 + 50 + Math.sin(time * 8 + robot.getId()) * 3;
		double y = Math.cos(time * 13 / robot.getId() + robot.getId() * Math.PI * 0.2) * 45 + 50 + Math.cos(time * 8 + robot.getId()) * 3;

		Robot updatedRobot = robot.update(new Position(x, y), randomStatus(robot.getStatus()));
		return new TrackEvent<>(TrackEvent.Name.ON_UPDATE, updatedRobot, robot);
	}

	private Robot.Status randomStatus(Robot.Status oldStatus) {
		return random.nextDouble() < CHANCE_TO_CHANGE_MOOD ? Robot.Status.values()[random.nextInt(Robot.Status.values().length)] : oldStatus;
	}
}
