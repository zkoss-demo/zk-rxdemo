package zk.rx.demo.vm;

import org.zkoss.bind.annotation.DependsOn;
import zk.rx.demo.domain.Robot;

public class UiRobot {
	private Robot robot;
	private boolean inside;

	public UiRobot(Robot robot) {
		this.robot = robot;
	}

	public Robot getRobot() {
		return robot;
	}

	public void setRobot(Robot robot) {
		this.robot = robot;
	}

	public boolean isInside() {
		return inside;
	}

	public void setInside(boolean inside) {
		this.inside = inside;
	}

	@DependsOn({"robot", "inside"})
	public String getStyleClasses() {
		return "robot " + (isInside() ? "inner" : "outer") + " " + robot.getStatus().toString();
	}
}
