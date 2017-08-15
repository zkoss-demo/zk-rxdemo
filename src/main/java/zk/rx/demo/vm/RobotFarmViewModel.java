package zk.rx.demo.vm;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.ListModelMap;
import zk.rx.demo.helper.Logger;
import zk.rx.demo.service.RobotTracker;
import zk.rx.demo.service.TrackEvent;
import zk.rx.operators.ZkObservable;

public class RobotFarmViewModel {

	private RobotTracker robotTracker = new RobotTracker();

	private ListModelMap<Long, UiRobot> trackedRobots = new ListModelMap<>();
	private Disposable subscription;

	@Init
	public void init() {
		Executions.getCurrent().getDesktop().enableServerPush(true);

		subscription = robotTracker.trackRobots()
				.observeOn(Schedulers.newThread())
//				.compose(ZkObservable.activated())
				.compose(ZkObservable.activatedThrottled(10))
//				.compose(ZkObservable.activatedBufferUnique(10, event -> event.getTarget().getId()))
				.subscribe(this::trackRobot);
	}

	public void trackRobot(TrackEvent<Robot> trackEvent) {
		Robot robot = trackEvent.getTarget();
		trackedRobots.compute(robot.getId(), (key, uiRobot) -> {
			if (uiRobot == null) {
				Logger.log("added: " + robot);
				return new UiRobot(robot);
			} else {
				Logger.log("updated: " + robot);
				uiRobot.setRobot(robot);
				BindUtils.postNotifyChange(null, null, uiRobot, "robot");
				return uiRobot;
			}
		});
	}

	public ListModelMap<Long, UiRobot> getTrackedRobots() {
		return trackedRobots;
	}
}
