package zk.rx.demo.helper;

public class Logger {
	public static void log(String message) {
		System.out.println(threadInfo() + message);
	}
	
	public static String threadInfo() {
		return Thread.currentThread() + " ........... ";
	}
}
