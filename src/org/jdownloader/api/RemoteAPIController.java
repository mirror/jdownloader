package org.jdownloader.api;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.SessionRemoteAPI;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging.Log;
import org.jdownloader.api.captcha.CaptchaAPIImpl;

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

    private RemoteAPIController() {
        apiEnabled = JsonConfig.create(RemoteAPIConfig.class).getAPIEnabled();
        apiPort = JsonConfig.create(RemoteAPIConfig.class).getAPIPort();
        apiLocal = JsonConfig.create(RemoteAPIConfig.class).getAPIlocalhost();
        rapi = new SessionRemoteAPI<RemoteAPISession>();
        sessionc = new RemoteAPISessionControllerImp();
        try {
            sessionc.registerSessionRequestHandler(rapi);
            rapi.register(sessionc);
        } catch (Throwable e) {
            Log.exception(e);
            apiEnabled = false;
        }
        register(new CaptchaAPIImpl());
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
