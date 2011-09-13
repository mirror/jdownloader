package org.jdownloader.api;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.SessionRemoteAPI;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging.Log;
import org.jdownloader.api.captcha.CaptchaAPIImpl;
import org.jdownloader.api.downloads.DownloadsAPIImpl;
import org.jdownloader.api.jd.JDAPIImpl;

public class RemoteAPIController {

    private static RemoteAPIController INSTANCE = new RemoteAPIController();

    public static RemoteAPIController getInstance() {
        return INSTANCE;
    }

    private boolean                            apiEnabled;
    private int                                apiPort;
    private boolean                            apiLocal;

    private SessionRemoteAPI<RemoteAPISession> rapi       = null;
    private RemoteAPISessionControllerImp      sessionc   = null;
    private int                                registered = 0;
    private EventsAPIImpl                      eventsapi;

    /**
     * @return the eventsapi
     */
    public EventsAPIImpl getEventsapi() {
        return eventsapi;
    }

    private RemoteAPIController() {
        apiEnabled = JsonConfig.create(RemoteAPIConfig.class).getAPIEnabled();
        apiPort = JsonConfig.create(RemoteAPIConfig.class).getAPIPort();
        apiLocal = JsonConfig.create(RemoteAPIConfig.class).getAPIlocalhost();
        rapi = new SessionRemoteAPI<RemoteAPISession>();
        sessionc = new RemoteAPISessionControllerImp();
        eventsapi = new EventsAPIImpl();
        try {
            sessionc.registerSessionRequestHandler(rapi);
            rapi.register(sessionc);
            rapi.register(eventsapi);
        } catch (Throwable e) {
            Log.exception(e);
            apiEnabled = false;
        }
        register(new CaptchaAPIImpl());
        register(new JDAPIImpl());
        register(new DownloadsAPIImpl());
    }

    public synchronized void register(final RemoteAPIInterface x, boolean forceRegister) {
        if (apiEnabled || forceRegister) {
            try {
                rapi.register(x);
                registered++;
                if (registered == 1) {
                    /* we start httpServer when first interface gets registered */
                    HttpServer.getInstance().registerRequestHandler(apiPort, apiLocal, sessionc);
                }
            } catch (final Throwable e) {
                Log.exception(e);
            }
        }
    }

    public synchronized void register(final RemoteAPIInterface x) {
        register(x, false);
    }

    public synchronized void unregister(final RemoteAPIInterface x) {
        try {
            rapi.unregister(x);
            registered--;
        } catch (final Throwable e) {
            Log.exception(e);
        }
    }

}
