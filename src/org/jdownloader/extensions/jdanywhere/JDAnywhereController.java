package org.jdownloader.extensions.jdanywhere;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.SessionRemoteAPI;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;

public class JDAnywhereController {

    private static JDAnywhereController INSTANCE = new JDAnywhereController();

    public static JDAnywhereController getInstance() {
        return INSTANCE;
    }

    private int                                 apiPort;

    private SessionRemoteAPI<JDAnywhereSession> rapi         = null;
    private JDAnywhereSessionControllerImp      sessionc     = null;
    private int                                 registered   = 0;
    private JDAnywhereEventsImpl                eventsapi;
    private boolean                             isIntialized = false;

    /**
     * @return the eventsapi
     */
    public JDAnywhereEventsImpl getEventsapi() {
        return eventsapi;
    }

    private JDAnywhereController() {

    }

    public HttpRequestHandler getRequestHandler() {
        return sessionc;
    }

    public synchronized void register(final RemoteAPIInterface x, int port, boolean forceRegister) {
        try {
            if (!isIntialized) {
                apiPort = port;
                rapi = new SessionRemoteAPI<JDAnywhereSession>();

                sessionc = new JDAnywhereSessionControllerImp(((JDAnywhereAPI) x).getUsername(), ((JDAnywhereAPI) x).getPassword());
                eventsapi = new JDAnywhereEventsImpl();
                try {
                    sessionc.registerSessionRequestHandler(rapi);
                    rapi.register(sessionc);
                    rapi.register(eventsapi);
                } catch (Throwable e) {
                    Log.exception(e);
                }
                isIntialized = true;
            }
            rapi.register(x);
            registered++;
            if (registered == 1) {
                /* we start httpServer when first interface gets registered */
                HttpServer.getInstance().registerRequestHandler(apiPort, false, sessionc);
            }
        } catch (final Throwable e) {
            Log.exception(e);
        }
    }

    public synchronized void register(final RemoteAPIInterface x, int port) {
        if (x == null) return;
        register(x, port, false);
    }

    public synchronized void unregister(final RemoteAPIInterface x) {
        if (x == null) return;
        try {
            rapi.unregister(x);
            registered--;
        } catch (final Throwable e) {
            Log.exception(e);
        }
    }

}
