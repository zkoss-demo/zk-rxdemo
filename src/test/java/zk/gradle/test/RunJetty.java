package zk.gradle.test;
import javax.websocket.WebSocketContainer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.zkoss.zk.au.http.DHtmlUpdateServlet;
import org.zkoss.zk.ui.http.DHtmlLayoutServlet;
import org.zkoss.zkmax.ui.comet.CometAsyncServlet;

/**
 * helper class to run the zk application in an embedded jetty to minimize startup time
 * @author Robert
 *
 */
public class RunJetty {

	public static void main(String[] args) throws Exception {
		if(args.length < 2) {
			System.out.println("2 arguments required: contextPath webappFolder [java|webxml]");
			System.out.println("e.g. /zk-gradle src/main/webapp        (defaults to 'webxml')");
			System.out.println("e.g. /zk-gradle src/main/webapp java");
			System.exit(1);
		}

		String contextPath = args[0];
		String webappFolder = args[1];
		String configType = "webxml";
		if(args.length == 3) {
			configType = args[2];
		}

		
		Server server = new Server(8080);
		ServletContextHandler handler = null;
		if("java".equals(configType)) {
			handler = initZkProgrammatically(webappFolder, contextPath);
		} else if ("webxml".equals(configType)) {
			handler = new WebAppContext(webappFolder, contextPath);
		}
		server.setHandler(handler);
		WebSocketServerContainerInitializer.configureContext(handler);
		
		server.start();
		System.out.println("Press ENTER to exit ......");
		System.in.read();
		server.stop();
	}

	private static ServletContextHandler initZkProgrammatically(String resourceBase, String contextPath) {
		ServletContextHandler servletContextHandler = new ServletContextHandler(/*ServletContextHandler.SESSIONS*/);

		servletContextHandler.setContextPath(contextPath);
		servletContextHandler.setResourceBase(resourceBase);

		SessionHandler sessionHandler = new SessionHandler();
		sessionHandler.setMaxInactiveInterval(300);
		servletContextHandler.setSessionHandler(sessionHandler);
		
		ServletHolder zkComet = servletContextHandler.addServlet(CometAsyncServlet.class, "/zkcomet");
		zkComet.setInitOrder(1);
		ServletHolder zkLoader = servletContextHandler.addServlet(DHtmlLayoutServlet.class, "*.zul");
		zkLoader.setInitParameter("update-uri", "/zkau");
		zkLoader.setInitOrder(2);
		ServletHolder zkAjax = servletContextHandler.addServlet(DHtmlUpdateServlet.class, "/zkau/*");
		zkAjax.setInitOrder(3);
		return servletContextHandler;
	}
}
