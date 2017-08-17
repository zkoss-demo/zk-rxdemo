package zk.rx.demo.vm;

import org.zkoss.bind.annotation.DependsOn;
import zk.rx.demo.domain.Robot;

public class UiRobot {
	private Robot robot;
	private boolean realTime;

	public UiRobot(Robot robot) {
		this.robot = robot;
	}

	public Robot getRobot() {
		return robot;
	}

	public void setRobot(Robot robot) {
		this.robot = robot;
	}

	public boolean isRealTime() {
		return realTime;
	}

	public void setRealTime(boolean realTime) {
		this.realTime = realTime;
	}

	@DependsOn({"robot", "realTime"})
	public String getStyleClasses() {
		return "robot " + (isRealTime() ? "realtime" : "delayed") + " " + robot.getStatus().toString();
	}
}
