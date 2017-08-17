package zk.rx.demo.vm;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.ListModelMap;
import zk.rx.demo.domain.Region;
import zk.rx.demo.domain.Robot;
import zk.rx.demo.service.RobotTracker;
import zk.rx.demo.service.TrackEvent;
import zk.rx.operators.ZkObservable;

import java.util.*;


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
		availableFilters.put(HAPPY_ROBOTS, robot -> robot.getStatus() == Robot.Status.HAPPY);
		availableFilters.put(ANGRY_ROBOTS, robot -> robot.getStatus() == Robot.Status.ANGRY);
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
		start();
	}

	@Command
	public void start() {
		stop();
		subscriptions = new CompositeDisposable();

		Disposable outerSubscription = robotTracker.trackRobots(this.currentFilter)
				.observeOn(Schedulers.newThread())
//				.compose(ZkObservable.activated())
//				.compose(ZkObservable.activatedThrottle(10))
				.compose(ZkObservable.activatedThrottleUnique(100, event -> event.getCurrent().getId()))
				.subscribe(this::trackInnerRobot, this::handleError);

		Disposable innerSubscription = robotTracker.trackRobots(negate(this.currentFilter))
				.observeOn(Schedulers.newThread())
				.compose(ZkObservable.activatedThrottleUnique(1000, event -> event.getCurrent().getId()))
				.subscribe(this::trackOuterRobot, this::handleError);

		subscriptions.addAll(innerSubscription, outerSubscription);
	}

	private Predicate<Robot> negate(Predicate<Robot> filter) {
		return robot -> !filter.test(robot);
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

	public void trackRobot(TrackEvent<Robot> trackEvent, boolean realtime) {
		Robot robot = trackEvent.getCurrent();
		UiRobot uiRobot = trackedRobots.computeIfAbsent(robot.getId(), key -> new UiRobot(robot));

		uiRobot.setRobot(robot);
		switch (trackEvent.getName()) {
			case ON_ENTER:
			case ON_UPDATE:
				uiRobot.setRealTime(realtime);
				break;
			case ON_LEAVE:
				if(uiRobot.isRealTime() == !realtime) {
					return;
				}
				uiRobot.setRealTime(!realtime);
				break;

		}
//		Logger.log("update robot:" + robot);
		BindUtils.postNotifyChange(null, null, uiRobot, "robot");
	}

	public ListModelMap<Long, UiRobot> getTrackedRobots() {
		return trackedRobots;
	}

	public ListModelList<String> getFilterNamesModel() {
		return filterNamesModel;
	}
}
