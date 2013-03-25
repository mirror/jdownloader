package org.jdownloader.api;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.SessionRemoteAPI;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.jdownloader.api.accounts.AccountAPIImpl;
import org.jdownloader.api.captcha.CaptchaAPIImpl;
import org.jdownloader.api.config.AdvancedConfigManagerAPIImpl;
import org.jdownloader.api.content.ContentAPIImpl;
import org.jdownloader.api.downloads.DownloadsAPIImpl;
import org.jdownloader.api.jd.JDAPIImpl;
import org.jdownloader.api.linkcollector.LinkCollectorAPIImpl;
import org.jdownloader.api.polling.PollingAPIImpl;
import org.jdownloader.api.toolbar.JDownloaderToolBarAPIImpl;

public class RemoteAPIController {

    private static RemoteAPIController INSTANCE = new RemoteAPIController();

    public static RemoteAPIController getInstance() {
        return INSTANCE;
    }

    private SessionRemoteAPI<RemoteAPISession> rapi     = null;
    private RemoteAPISessionControllerImp      sessionc = null;
    private EventsAPIImpl                      eventsapi;

    /**
     * @return the eventsapi
     */
    public EventsAPIImpl getEventsapi() {
        return eventsapi;
    }

    private RemoteAPIController() {
        rapi = new SessionRemoteAPI<RemoteAPISession>();
        sessionc = new RemoteAPISessionControllerImp();
        eventsapi = new EventsAPIImpl();
        try {
            sessionc.registerSessionRequestHandler(rapi);
            rapi.register(sessionc);
            rapi.register(eventsapi);
        } catch (Throwable e) {
            Log.exception(e);
        }
        register(new CaptchaAPIImpl());
        register(new JDAPIImpl());
        register(new DownloadsAPIImpl());
        register(new AdvancedConfigManagerAPIImpl());
        register(new JDownloaderToolBarAPIImpl());
        register(new AccountAPIImpl());
        register(new LinkCollectorAPIImpl());
        register(new ContentAPIImpl());
        register(new PollingAPIImpl());
    }

    public HttpRequestHandler getRequestHandler() {
        return sessionc;
    }

    public synchronized void register(final RemoteAPIInterface x) {
        if (x == null) return;
        try {
            rapi.register(x);
        } catch (final Throwable e) {
            Log.exception(e);
        }

    }

    public synchronized void unregister(final RemoteAPIInterface x) {
        if (x == null) return;
        try {
            rapi.unregister(x);
        } catch (final Throwable e) {
            Log.exception(e);
        }
    }

}
