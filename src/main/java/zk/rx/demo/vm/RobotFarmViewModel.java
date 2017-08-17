package zk.rx.demo.vm;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.ListModelMap;
import zk.rx.demo.domain.Region;
import zk.rx.demo.domain.Robot;
import zk.rx.demo.helper.Logger;
import zk.rx.demo.service.RobotTracker;
import zk.rx.demo.service.TrackEvent;
import zk.rx.operators.ZkObservable;


public class RobotFarmViewModel {

	private RobotTracker robotTracker = new RobotTracker();

	private ListModelMap<Long, UiRobot> trackedRobots = new ListModelMap<>();
	private CompositeDisposable subscriptions;

	@Init
	public void init() {
		Executions.getCurrent().getDesktop().enableServerPush(true);
	}

	@Command
	public void start() {
		stop();

		Region innerTrackingRegion = new Region(20, 20, 80, 80, false);
		Region outerTrackingRegion = new Region(20, 20, 80, 80, true);

		subscriptions = new CompositeDisposable();

		Disposable outerSubscription = robotTracker.trackRobots(innerTrackingRegion, 10)
				.observeOn(Schedulers.newThread())
//				.compose(ZkObservable.activated())
//				.compose(ZkObservable.activatedThrottle(10))
				.compose(ZkObservable.activatedThrottleUnique(100, event -> event.getTarget().getId()))
				.subscribe(this::trackInnerRobot, this::handleError);

		Disposable innerSubscription = robotTracker.trackRobots(outerTrackingRegion, 10)
				.observeOn(Schedulers.newThread())
				.compose(ZkObservable.activatedThrottleUnique(1000, event -> event.getTarget().getId()))
				.subscribe(this::trackOuterRobot, this::handleError);

		subscriptions.addAll(innerSubscription, outerSubscription);
	}

	private void trackInnerRobot(TrackEvent<Robot> robotTrackEvent) {
		trackRobot(robotTrackEvent, true);
	}

	private void trackOuterRobot(TrackEvent<Robot> robotTrackEvent) {
		trackRobot(robotTrackEvent, false);
	}

	private void handleError(Throwable throwable) {
		if (throwable != null) {
			System.err.println("Error happened: dispose subscription: " + throwable);
		}
		stop();
	}

	@Command
	public void stop() {
		if (subscriptions != null && !subscriptions.isDisposed()) {
			System.out.println("Dispose subscriptions: " + subscriptions);
			subscriptions.dispose();
			subscriptions = null;
		}
	}

	public void trackRobot(TrackEvent<Robot> trackEvent, boolean inside) {
		Robot robot = trackEvent.getTarget();
		UiRobot uiRobot = trackedRobots.computeIfAbsent(robot.getId(), key -> new UiRobot(robot));

		uiRobot.setRobot(robot);
		switch (trackEvent.getName()) {
			case ON_ENTER:
			case ON_UPDATE:
				uiRobot.setInside(inside);
				break;
			case ON_LEAVE:
				if(uiRobot.isInside() == !inside) {
					return;
				}
				uiRobot.setInside(!inside);
				break;

		}
		Logger.log("update robot:" + robot);
		BindUtils.postNotifyChange(null, null, uiRobot, "robot");
	}

	public ListModelMap<Long, UiRobot> getTrackedRobots() {
		return trackedRobots;
	}
}
