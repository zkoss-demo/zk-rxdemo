package zk.rx.demo.service;

import io.reactivex.Observable;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import org.zkoss.zk.ui.DesktopUnavailableException;
import zk.rx.demo.domain.Region;
import zk.rx.demo.domain.Robot;

import java.io.IOException;
import java.net.SocketException;

public class RobotTracker {

	private static RobotBackend backend = new RobotBackend();

	static {
		initRxErrorHandler();
		backend.start();
	}

	public Observable<TrackEvent<Robot>> trackRobots(Predicate<Robot> filter) {
		return backend.trackRobots()
				.observeOn(Schedulers.computation())
				.map(event -> new TrackingMatch(event, filter))
				.filter(TrackingMatch::anyMatch)
				.map(result -> {
					TrackEvent<Robot> event = result.getEvent();
					Robot current = event.getCurrent();
					Robot previous = event.getPrevious();

					if (result.allMatch()) {
						return event; //stays inside filter contraint just ON_UPDATE
					} else {
						if (result.isPreviousMatch()) {
							return new TrackEvent<>(TrackEvent.Name.ON_LEAVE, current, previous);
						} else {
							return new TrackEvent<>(TrackEvent.Name.ON_ENTER, current, previous);
						}
					}
				});
	}

	private static class TrackingMatch {
		TrackEvent<Robot> event;
		boolean currentMatch;
		boolean previousMatch;

		public TrackingMatch(TrackEvent<Robot> event, Predicate<Robot> filter) throws Exception {
			this.event = event;
			this.currentMatch = filter.test(event.getCurrent());
			this.previousMatch = filter.test(event.getPrevious());
		}

		public boolean isCurrentMatch() {
			return currentMatch;
		}

		public boolean isPreviousMatch() {
			return previousMatch;
		}

		public boolean anyMatch() {
			return isCurrentMatch() || isPreviousMatch();
		}

		public boolean allMatch() {
			return isCurrentMatch() && isPreviousMatch();
		}

		public TrackEvent<Robot> getEvent() {
			return event;
		}
	}

	/**
	 * deal with UndeliverableExceptions
	 * adapted from: https://github.com/ReactiveX/RxJava/wiki/What%27s-different-in-2.0#error-handling
	 */
	private static void initRxErrorHandler() {
		RxJavaPlugins.setErrorHandler(e -> {
			if (e instanceof UndeliverableException) {
				e = e.getCause();
			}
			if ((e instanceof IOException) || (e instanceof SocketException)) {
				// fine, irrelevant network problem or API that throws on cancellation
				return;
			}
			if (e instanceof InterruptedException) {
				// fine, some blocking code was interrupted by a dispose call
				return;
			}
			if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
				// that's likely a bug in the application
				Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
				return;
			}
			if (e instanceof IllegalStateException) {
				// that's a bug in RxJava or in a custom operator
				Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
				return;
			}
			System.err.println("Undeliverable exception received, not sure what to do: ");
		});
	}
}
