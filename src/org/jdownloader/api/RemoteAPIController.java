package org.jdownloader.api;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.SessionRemoteAPI;
import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.EventsAPI;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.UIOManager;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.jdownloader.api.accounts.AccountAPIImpl;
import org.jdownloader.api.captcha.CaptchaAPISolver;
import org.jdownloader.api.config.AdvancedConfigManagerAPIImpl;
import org.jdownloader.api.content.ContentAPIImpl;
import org.jdownloader.api.dialog.RemoteAPIIOHandlerWrapper;
import org.jdownloader.api.downloads.DownloadControllerEventPublisher;
import org.jdownloader.api.downloads.DownloadWatchDogEventPublisher;
import org.jdownloader.api.downloads.DownloadsAPIImpl;
import org.jdownloader.api.extensions.ExtensionsAPIImpl;
import org.jdownloader.api.jd.JDAPIImpl;
import org.jdownloader.api.jdanywhere.JDAnywhereAPI;
import org.jdownloader.api.linkcollector.LinkCollectorAPIImpl;
import org.jdownloader.api.linkcollector.LinkCollectorEventPublisher;
import org.jdownloader.api.linkcollector.LinkCrawlerEventPublisher;
import org.jdownloader.api.polling.PollingAPIImpl;
import org.jdownloader.api.toolbar.JDownloaderToolBarAPIImpl;

public class RemoteAPIController {

    private static RemoteAPIController INSTANCE = new RemoteAPIController();

    public static RemoteAPIController getInstance() {
        return INSTANCE;
    }

    private SessionRemoteAPI<RemoteAPISession> rapi     = null;
    private RemoteAPISessionControllerImp      sessionc = null;
    private EventsAPI                          eventsapi;

    private RemoteAPIController() {

        rapi = new SessionRemoteAPI<RemoteAPISession>();
        sessionc = new RemoteAPISessionControllerImp();

        try {
            sessionc.registerSessionRequestHandler(rapi);
            rapi.register(sessionc);
            if (JsonConfig.create(RemoteAPIConfig.class).isDeprecatedApiEnabled()) {
                HttpServer.getInstance().registerRequestHandler(3128, true, sessionc);
            }
        } catch (Throwable e) {
            Log.exception(e);
        }
        register(eventsapi = new EventsAPI());
        register(CaptchaAPISolver.getInstance());
        register(CaptchaAPISolver.getInstance().getEventPublisher());
        register(new JDAPIImpl());
        register(new DownloadsAPIImpl());
        register(new AdvancedConfigManagerAPIImpl());
        register(new JDownloaderToolBarAPIImpl());
        register(new AccountAPIImpl());
        register(new LinkCollectorAPIImpl());
        register(new ContentAPIImpl());
        register(new PollingAPIImpl());

        register(new DownloadWatchDogEventPublisher());

        register(new LinkCollectorEventPublisher());
        register(new DownloadControllerEventPublisher());
        register(new ExtensionsAPIImpl());

        register(new LinkCrawlerEventPublisher());
        RemoteAPIIOHandlerWrapper wrapper;
        UIOManager.setUserIO(wrapper = new RemoteAPIIOHandlerWrapper(UIOManager.I()));
        register(wrapper.getEventPublisher());
        register(wrapper.getApi());

        JDAnywhereAPI.getInstance().init(this);
    }

    public HttpRequestHandler getRequestHandler() {
        return sessionc;
    }

    public boolean register(final RemoteAPIInterface x) {
        if (x == null) return false;
        try {
            rapi.register(x);
            return true;
        } catch (final Throwable e) {
            Log.exception(e);
            return false;
        }
    }

    public boolean unregister(final RemoteAPIInterface x) {
        if (x == null) return false;
        try {
            rapi.unregister(x);
            return true;
        } catch (final Throwable e) {
            Log.exception(e);
            return false;
        }
    }

    public boolean register(EventPublisher publisher) {
        return eventsapi.register(publisher);
    }

    public boolean unregister(EventPublisher publisher) {
        return eventsapi.unregister(publisher);
    }

}
