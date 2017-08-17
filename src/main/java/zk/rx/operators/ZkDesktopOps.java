package zk.rx.operators;

import io.reactivex.functions.Action;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import zk.rx.demo.helper.Logger;

public class ZkDesktopOps {
	private Desktop desktop;
	public ZkDesktopOps(Desktop desktop) {
		this.desktop = desktop;
	}
	public Action activate() {
		return () -> {
			Executions.activate(desktop);
			Logger.log("activate");
		};
	}
	public Action deactivate() {
		return () -> {
			if(Executions.getCurrent() != null) {
				Executions.deactivate(desktop);
				Logger.log("deactivate");
			}
		};
	}
}