package org.jdownloader.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.InterfaceHandler;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.SessionRemoteAPI;
import org.appwork.remoteapi.SessionRemoteAPIRequest;
import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.EventsAPI;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;
import org.appwork.remoteapi.responsewrapper.DataObject;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.requests.JSonRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.api.accounts.AccountAPIImpl;
import org.jdownloader.api.captcha.CaptchaAPISolver;
import org.jdownloader.api.cnl2.ExternInterfaceImpl;
import org.jdownloader.api.config.AdvancedConfigManagerAPIImpl;
import org.jdownloader.api.content.ContentAPIImpl;
import org.jdownloader.api.dialog.RemoteAPIIOHandlerWrapper;
import org.jdownloader.api.downloads.DownloadControllerEventPublisher;
import org.jdownloader.api.downloads.DownloadWatchDogEventPublisher;
import org.jdownloader.api.downloads.DownloadsAPIImpl;
import org.jdownloader.api.extensions.ExtensionsAPIImpl;
import org.jdownloader.api.extraction.ExtractionAPIImpl;
import org.jdownloader.api.jd.JDAPIImpl;
import org.jdownloader.api.jdanywhere.JDAnywhereAPI;
import org.jdownloader.api.linkcollector.LinkCollectorAPIImpl;
import org.jdownloader.api.linkcollector.LinkCollectorEventPublisher;
import org.jdownloader.api.linkcrawler.LinkCrawlerAPIImpl;
import org.jdownloader.api.linkcrawler.LinkCrawlerEventPublisher;
import org.jdownloader.api.myjdownloader.MyJDownloaderPostRequest;
import org.jdownloader.api.myjdownloader.MyJDownloaderRequestInterface;
import org.jdownloader.api.plugins.PluginsAPIImpl;
import org.jdownloader.api.polling.PollingAPIImpl;
import org.jdownloader.api.toolbar.JDownloaderToolBarAPIImpl;
import org.jdownloader.logging.LogController;

public class RemoteAPIController {

    private static RemoteAPIController INSTANCE = new RemoteAPIController();

    public static RemoteAPIController getInstance() {
        return INSTANCE;
    }

    private SessionRemoteAPI<RemoteAPISession> rapi     = null;
    private RemoteAPISessionControllerImp      sessionc = null;
    private EventsAPI                          eventsapi;
    private LogSource                          logger;

    private RemoteAPIController() {
        logger = LogController.getInstance().getLogger(RemoteAPIController.class.getName());
        rids = new HashMap<String, RIDArray>();
        rapi = new SessionRemoteAPI<RemoteAPISession>() {
            @Override
            public String toString(RemoteAPIRequest request, RemoteAPIResponse response, Object responseData) {
                try {
                    if (request instanceof SessionRemoteAPIRequest) {
                        MyJDownloaderRequestInterface ri = ((MyJDRemoteAPIRequest) ((SessionRemoteAPIRequest) request).getApiRequest()).getRequest();
                        return JSonStorage.serializeToJson(new DataObject(responseData, ri.getRid()));

                    }
                } catch (Throwable e) {
                    throw new WTFException(e);
                }
                throw new WTFException();

            }

            @Override
            public void sendText(RemoteAPIRequest request, RemoteAPIResponse response, String text) throws UnsupportedEncodingException, IOException {
                super.sendText(request, response, text);
            }

            @Override
            protected RemoteAPIResponse createRemoteAPIResponseObject(HttpResponse response) {
                return super.createRemoteAPIResponseObject(response);
            }

            @Override
            protected RemoteAPIRequest createRemoteAPIRequestObject(HttpRequest request, String[] intf, InterfaceHandler<?> interfaceHandler, List<String> parameters, String jqueryCallback) throws IOException {

                return new MyJDRemoteAPIRequest(interfaceHandler, intf[2], parameters.toArray(new String[] {}), (MyJDownloaderRequestInterface) request);
            }

            @Override
            protected void validateRequest(HttpRequest request) throws BasicRemoteAPIException {
                super.validateRequest(request);

                if (request instanceof MyJDownloaderPostRequest) {
                    JSonRequest jsonRequest;
                    try {
                        jsonRequest = ((MyJDownloaderPostRequest) request).getJsonRequest();

                        if (jsonRequest == null) throw new BasicRemoteAPIException("no JSONRequest", ResponseCode.ERROR_BAD_REQUEST);
                        if (StringUtils.isEmpty(jsonRequest.getUrl())) throw new BasicRemoteAPIException("JSonRequest URL is empty", ResponseCode.ERROR_BAD_REQUEST);
                        if (!StringUtils.equals(request.getRequestedURL(), jsonRequest.getUrl())) throw new BasicRemoteAPIException("JSonRequest URL=" + jsonRequest.getUrl() + " does not match " + request.getRequestedURL(), ResponseCode.ERROR_BAD_REQUEST);
                        if (!validateRID(jsonRequest.getRid(), ((MyJDownloaderPostRequest) request).getRequestConnectToken())) throw new BasicRemoteAPIException("JSonRequest URL=" + jsonRequest.getUrl() + " has duplicated RID=" + jsonRequest.getRid(), ResponseCode.ERROR_BAD_REQUEST);
                    } catch (IOException e) {
                        throw new BasicRemoteAPIException(e);
                    }
                }
                MyJDownloaderRequestInterface ri = (MyJDownloaderRequestInterface) request;

                try {
                    if (!validateRID(ri.getRid(), ri.getRequestConnectToken())) throw new BasicRemoteAPIException("JSonRequest URL=" + request.getRequestedURL() + " has duplicated RID=" + ri.getRid(), ResponseCode.ERROR_BAD_REQUEST);
                } catch (IOException e) {
                    throw new BasicRemoteAPIException(e, e.getMessage(), ResponseCode.SERVERERROR_INTERNAL, null);
                }

            }

            @Override
            protected void validateRequest(RemoteAPIRequest ret) throws BasicRemoteAPIException {
                super.validateRequest(ret);

            }
        };
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
        register(new ExtractionAPIImpl());
        register(new LinkCrawlerAPIImpl());
        register(new PluginsAPIImpl());

        register(new ExternInterfaceImpl());

        register(new DownloadWatchDogEventPublisher());

        register(new LinkCollectorEventPublisher());
        register(new DownloadControllerEventPublisher());
        register(new ExtensionsAPIImpl());
        register(new UpdateAPIImpl());
        register(new LinkCrawlerEventPublisher());
        RemoteAPIIOHandlerWrapper wrapper;
        UIOManager.setUserIO(wrapper = new RemoteAPIIOHandlerWrapper(UIOManager.I()));
        register(wrapper.getEventPublisher());
        register(wrapper.getApi());

        JDAnywhereAPI.getInstance().init(this);
    }

    private HashMap<String, RIDArray> rids;

    /* TODO: add session support, currently all sessions share the same validateRID */
    public synchronized boolean validateRID(long rid, String sessionToken) {
        if (true) return true;
        // TODO CLeanup
        RIDArray ridList = rids.get(sessionToken);
        if (ridList == null) {
            ridList = new RIDArray();
            rids.put(sessionToken, ridList);
        }

        // lowest RID
        System.out.println("RID " + rid + " " + sessionToken);
        long lowestRid = Long.MIN_VALUE;
        RIDEntry next;
        for (Iterator<RIDEntry> it = ridList.iterator(); it.hasNext();) {
            next = it.next();
            if (next.getRid() == rid) {
                // dupe rid is always bad
                logger.warning("received an RID Dupe. Possible Replay Attack avoided");
                return false;
            }
            if (System.currentTimeMillis() - next.getTimestamp() > 15000) {
                it.remove();
                if (next.getRid() > lowestRid) {
                    lowestRid = next.getRid();
                }

            }
        }
        if (lowestRid > ridList.getMinAcceptedRID()) {
            ridList.setMinAcceptedRID(lowestRid);
        }
        if (rid <= ridList.getMinAcceptedRID()) {
            // rid too low
            logger.warning("received an outdated RID. Possible Replay Attack avoided");
            return false;
        }
        RIDEntry ride = new RIDEntry(rid);
        ridList.add(ride);

        return true;
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
        if (publisher == null) return false;
        return eventsapi.register(publisher);
    }

    public boolean unregister(EventPublisher publisher) {
        if (publisher == null) return false;
        return eventsapi.unregister(publisher);
    }

}
