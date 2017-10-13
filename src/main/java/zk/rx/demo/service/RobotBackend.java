package zk.rx.demo.service;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
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

	/**
	 * called once to start the stream of events
	 */
	public void start() {
		allRobots = LongStream.range(0L, NUM_ROBOTS)
				.mapToObj(index -> new Robot(100 + index, new Position(0, 0), Robot.Mood.NEUTRAL))
				.collect(Collectors.toConcurrentMap(Robot::getId, robot -> robot));

		Observable<TrackEvent<Robot>> obs = Observable.create(this::backgroundThread);
		hotRobotObservable = obs.publish();
		disposable = hotRobotObservable.connect();
	}

	/**
	 * called by each subscriber to connect to the same stream of events
	 * @return Observable of {@link TrackEvent}
	 */
	public Observable<TrackEvent<Robot>> trackRobots() {
		Stream<TrackEvent<Robot>> currentRobots = allRobots.values().stream()
				.map(robot -> new TrackEvent<>(TrackEvent.Name.ON_ENTER, robot, robot));
		//prepend initial state for all robots to the hot stream of updates
		return hotRobotObservable
				.startWith(currentRobots::iterator);
	}

	public void stop() {
		disposable.dispose();
	}

	private void backgroundThread(ObservableEmitter<TrackEvent<Robot>> source) {
		new Thread(() -> {
			while (!source.isDisposed()) {
				try {
					Thread.sleep(TICK_INTERVAL);
//					Logger.log("tick after " + TICK_INTERVAL + "ms");
					//update random robots
					allRobots.values().stream()
							.filter(robot -> random.nextDouble() < CHANCE_TO_UPDATE)
							.map(this::updateRobot)
//							.peek(robot -> Logger.log("updated robot: " + robot))
							.forEach(event -> {
								allRobots.put(event.getCurrent().getId(), event.getCurrent());
								source.onNext(event);
							});
				} catch (InterruptedException e) {
					source.onComplete();
				}
			}
		}, "BackgroundThread").start();
	}

	private TrackEvent<Robot> updateRobot(Robot robot) {
		double time = (double) System.nanoTime() / TimeUnit.SECONDS.toNanos(1);
		long robotNum = robot.getId();
		double x = Math.sin(time * 14 / robotNum + robotNum * Math.PI * 0.2) * 45 + 50 + Math.sin(time * 8 + robotNum) * 3;
		double y = Math.cos(time * 13 / robotNum + robotNum * Math.PI * 0.2) * 45 + 50 + Math.cos(time * 8 + robotNum) * 3;

		Robot updatedRobot = robot.update(new Position(x, y), randomStatus(robot.getMood()));
		return new TrackEvent<>(TrackEvent.Name.ON_UPDATE, updatedRobot, robot);
	}

	private Robot.Mood randomStatus(Robot.Mood oldStatus) {
		return random.nextDouble() < CHANCE_TO_CHANGE_MOOD ? Robot.Mood.values()[random.nextInt(Robot.Mood.values().length)] : oldStatus;
	}
}
