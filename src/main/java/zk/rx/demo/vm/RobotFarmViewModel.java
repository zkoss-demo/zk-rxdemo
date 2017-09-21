package zk.rx.demo.vm;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.ToServerCommand;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.ListModelMap;
import zk.rx.demo.domain.Region;
import zk.rx.demo.domain.Robot;
import zk.rx.demo.service.RobotTracker;
import zk.rx.demo.service.TrackEvent;
import zk.rx.operators.ZkObservable;

import java.util.*;

@ToServerCommand({"start", "stop"})
public class RobotFarmViewModel {

	public static final String NONE = "None";
	public static final String ALL_ROBOTS = "All Robots";
	public static final String HAPPY_ROBOTS = "Happy Robots";
	public static final String ANGRY_ROBOTS = "Angry Robots";
	public static final String CENTER_REGION = "Center Region";
	private RobotTracker robotTracker = new RobotTracker();

	private ListModelMap<Long, UiRobot> trackedRobots = new ListModelMap<>();
	private CompositeDisposable subscriptions;

	private Predicate<Robot> currentFilter;
	private Map<String, Predicate<Robot>> availableFilters;
	private ListModelList<String> filterNamesModel;

	@Init
	public void init() {
		Executions.getCurrent().getDesktop().enableServerPush(true);

		availableFilters = new LinkedHashMap<>();
		availableFilters.put(NONE, robot -> false);
		availableFilters.put(ALL_ROBOTS, robot -> true);
		availableFilters.put(HAPPY_ROBOTS, robot -> robot.getMood() == Robot.Mood.HAPPY);
		availableFilters.put(ANGRY_ROBOTS, robot -> robot.getMood() == Robot.Mood.ANGRY);
		Region centerRegion = new Region(20, 20, 80, 80);
		availableFilters.put(CENTER_REGION, robot -> centerRegion.contains(robot.getPosition()));

		currentFilter = availableFilters.get(CENTER_REGION);
		filterNamesModel = new ListModelList<>(availableFilters.keySet());
		filterNamesModel.addToSelection(CENTER_REGION);

		start();
	}


	@Command
	public void selectFilter() {
		currentFilter = availableFilters.get(filterNamesModel.getSelection().iterator().next());
		if(isRunning()) {
			start();
		}
	}

	@Command
	public void toggleRunning() {
		if(isRunning()) {
			stop();
		} else {
			start();
		}
	}

	@Command
	public void testServerResponse() {
		Clients.showNotification("Response from Server");
	}

	@Command
	public void start() {
		stop();
		subscriptions = new CompositeDisposable();

		Disposable innerSubscription = robotTracker.trackRobots(this.currentFilter)
//				.compose(ZkObservable.activated())
//				.compose(ZkObservable.activatedThrottle(100))
				.compose(ZkObservable.activatedThrottleUnique(100, event -> event.getCurrent().getId()))
				.subscribe(this::trackRealtimeRobot, this::handleError);

		Disposable outerSubscription = robotTracker.trackRobots(negate(this.currentFilter))
				.compose(ZkObservable.activatedThrottleUnique(1000, event -> event.getCurrent().getId()))
				.subscribe(this::trackDelayedRobot, this::handleError);

		subscriptions.addAll(innerSubscription, outerSubscription);
		notifyRunning();
	}

	@Command
	public void stop() {
		if (subscriptions != null && !subscriptions.isDisposed()) {
			System.out.println("Dispose subscriptions: " + subscriptions);
			subscriptions.dispose();
			subscriptions = null;
		}
		notifyRunning();
	}

	private void notifyRunning() {
		BindUtils.postNotifyChange(null, null, this, "running");
	}

	private void trackRealtimeRobot(TrackEvent<Robot> robotTrackEvent) {
		trackRobot(robotTrackEvent, true);
	}

	private void trackDelayedRobot(TrackEvent<Robot> robotTrackEvent) {
		trackRobot(robotTrackEvent, false);
	}

	private void trackRobot(TrackEvent<Robot> trackEvent, boolean realtime) {
		Robot robot = trackEvent.getCurrent();
		UiRobot uiRobot = trackedRobots.computeIfAbsent(robot.getId(), key -> new UiRobot(robot));

		uiRobot.setRobot(robot);
		switch (trackEvent.getName()) {
			case ON_ENTER:
			case ON_UPDATE:
				uiRobot.setRealTime(realtime);
				break;
			case ON_LEAVE:
				if (uiRobot.isRealTime() == !realtime) {
					return;
				}
				uiRobot.setRealTime(!realtime);
				break;

		}
//		Logger.log("update robot:" + robot);
		BindUtils.postNotifyChange(null, null, uiRobot, "robot");
	}

	private Predicate<Robot> negate(Predicate<Robot> filter) {
		return robot -> !filter.test(robot);
	}

	private void handleError(Throwable throwable) {
		if (throwable != null) {
			System.err.println("Error happened: dispose subscription: " + throwable);
		}
		stop();
	}

	public ListModelMap<Long, UiRobot> getTrackedRobots() {
		return trackedRobots;
	}

	public ListModelList<String> getFilterNamesModel() {
		return filterNamesModel;
	}

	public boolean isRunning() {
		return !(subscriptions == null || subscriptions.isDisposed());
	}
}
