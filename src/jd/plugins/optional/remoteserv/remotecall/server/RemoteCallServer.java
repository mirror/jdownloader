package jd.plugins.optional.remoteserv.remotecall.server;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jd.plugins.optional.remoteserv.remotecall.RemoteCallService;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class RemoteCallServer extends HttpServlet {

    private final int                                       port;
    private final HashMap<String, RemoteCallServiceWrapper> servicesMap;
    private Server                                          server;

    public RemoteCallServer(final int port) {
        this.port = port;
        this.servicesMap = new HashMap<String, RemoteCallServiceWrapper>();
    }

    public void addHandler(final RemoteCallService serviceImpl) {
        this.servicesMap.put(serviceImpl.getClass().getSimpleName(), RemoteCallServiceWrapper.create(serviceImpl));
    }

    /**
     * Got get request
     */
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

        this.handleRequest(request, response);
    }

    /**
     * Got post request
     */
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) {
        this.handleRequest(request, response);
    }

    protected void handleRequest(final HttpServletRequest request, final HttpServletResponse response) {

        final String data = request.getParameter("p");
        System.out.println(data);

    }

    public void start() throws Exception {
        this.server = new Server();

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(this), "/*");
        context.setMaxFormContentSize(3000000);
        final HandlerCollection handlers = new HandlerCollection();

        handlers.setHandlers(new Handler[] { context });
        this.server.setHandler(handlers);

        final SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(this.port);
        connector.setThreadPool(new QueuedThreadPool(20));

        this.server.addConnector(connector);

        this.server.start();
    }

}
