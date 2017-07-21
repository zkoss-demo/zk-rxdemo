package zk.gradle.test;
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

	private static final String JETTY_HTTP_PORT = "org.zkoss.RunJetty.httpPort";
	private static final String JETTY_CONFIG_TYPE = "org.zkoss.RunJetty.configType";
	private static final String JETTY_CONTEXT_PATH = "org.zkoss.RunJetty.contextPath";
	private static final String JETTY_WEBAPP_FOLDER = "org.zkoss.RunJetty.webappFolder";
	private static final String JETTY_ENABLE_WEBSOCKET = "org.zkoss.RunJetty.enableWebsocket";

	public static void main(String[] args) throws Exception {
		int port = Integer.valueOf(System.getProperty(JETTY_HTTP_PORT, "8080"));
		String configType = System.getProperty(JETTY_CONFIG_TYPE, "webxml");
		String contextPath = System.getProperty(JETTY_CONTEXT_PATH, "/");
		String webappFolder = System.getProperty(JETTY_WEBAPP_FOLDER, "src/main/webapp");
		boolean enableWebsocket = Boolean.valueOf(System.getProperty(JETTY_ENABLE_WEBSOCKET, "true"));

		Server server = new Server(port);
		ServletContextHandler handler = configureContext(configType, contextPath, webappFolder);
		server.setHandler(handler);
		if(enableWebsocket) {
			WebSocketServerContainerInitializer.configureContext(handler);
		}
		
		server.start();
		System.err.println("ZK Application listening on: " + server.getURI());
		System.err.println("Press ENTER to exit ......");
		System.in.read();
		server.stop();
	}

	private static ServletContextHandler configureContext(String configType, String contextPath, String webappFolder) {
		if("java".equals(configType)) {
			return initZkProgrammatically(webappFolder, contextPath);
		} else if ("webxml".equals(configType)) {
			return new WebAppContext(webappFolder, contextPath);
		}
		throw new IllegalArgumentException("incorrect config type only 'java' or 'webxml' are supported");
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
