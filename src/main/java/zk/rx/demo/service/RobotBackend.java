package zk.rx.demo.service;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observables.ConnectableObservable;
import zk.rx.demo.helper.Logger;
import zk.rx.demo.vm.Robot;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class RobotBackend {

	private Random random = new Random();

	private Map<Long, Robot> allRobots;
	private ConnectableObservable<Robot> hotRobotObservable;
	private Disposable disposable;

	public void start() {
		allRobots = LongStream.range(0L, 10L)
				.mapToObj(index -> new Robot(100 + index))
				.collect(Collectors.toConcurrentMap(Robot::getId, robot -> robot));

		Observable<Robot> obs = Observable.create(source -> {
			new Thread(() -> {
				while (!source.isDisposed()) {
					try {
						int millis = 10;
						Thread.sleep(millis);
						Logger.log("tick after " + millis + "ms");
						updateRandomRobots()
								.peek(robot -> Logger.log("updated robot: " + robot))
								.forEach(robot -> {
									allRobots.put(robot.getId(), robot);
									source.onNext(robot);
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
		return hotRobotObservable
				.startWith(allRobots.values())
				.map(robot -> new TrackEvent<>(TrackEvent.Name.ON_UPDATE, robot));
	}

	private Stream<Robot> updateRandomRobots() {
		return allRobots.values().stream()
				.filter(this::shallUpdate)
				.map(this::updateRobot);
	}

	private Robot updateRobot(Robot robot) {
		double time = (double)System.nanoTime() / TimeUnit.SECONDS.toNanos(1);

		double x = clamp(0, 200, Math.sin(time * 94 / robot.getId() + robot.getId() * Math.PI * 0.2) * 100 + 100);
		double y = clamp(0, 200, Math.cos(time * 100 / robot.getId() + robot.getId() * Math.PI * 0.2) * 100 + 100);
//		double x = clamp(0, 200, robot.getPosX() + Math.random() * 10 - 5);
//		double y = clamp(0, 200, robot.getPosY() + Math.random() * 10 - 5);
		return robot.update(x, y, randomStatus(robot.getStatus()));
	}

	private double clamp(double lower, double upper, double value) {
		return Math.min(upper, Math.max(lower, value));
	}

	private Robot.Status randomStatus(Robot.Status oldStatus) {
		return Math.random() < 0.02 ? Robot.Status.values()[random.nextInt(Robot.Status.values().length)] : oldStatus;
	}

	private boolean shallUpdate(Robot robot) {
		return Math.random() < 1.0;
	}

}
