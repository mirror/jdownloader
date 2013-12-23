package org.jdownloader.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.InterfaceHandler;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.SessionRemoteAPI;
import org.appwork.remoteapi.SessionRemoteAPIRequest;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.EventsAPI;
import org.appwork.remoteapi.events.EventsAPIInterface;
import org.appwork.remoteapi.events.json.EventObjectStorable;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;
import org.appwork.remoteapi.responsewrapper.DataObject;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.appwork.utils.reflection.Clazz;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.accounts.AccountAPIImpl;
import org.jdownloader.api.accounts.v2.AccountAPIImplV2;
import org.jdownloader.api.captcha.CaptchaAPISolver;
import org.jdownloader.api.cnl2.ExternInterfaceImpl;
import org.jdownloader.api.config.AdvancedConfigManagerAPIImpl;
import org.jdownloader.api.content.ContentAPIImpl;
import org.jdownloader.api.content.v2.ContentAPIImplV2;
import org.jdownloader.api.dialog.RemoteAPIIOHandlerWrapper;
import org.jdownloader.api.downloads.DownloadControllerEventPublisher;
import org.jdownloader.api.downloads.DownloadWatchDogEventPublisher;
import org.jdownloader.api.downloads.DownloadsAPIImpl;
import org.jdownloader.api.downloads.v2.DownloadWatchdogAPIImpl;
import org.jdownloader.api.downloads.v2.DownloadsAPIV2Impl;
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
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;
import org.jdownloader.myjdownloader.client.bindings.ClientApiNameSpace;
import org.jdownloader.myjdownloader.client.bindings.events.json.MyJDEvent;
import org.jdownloader.myjdownloader.client.bindings.interfaces.EventsInterface;
import org.jdownloader.myjdownloader.client.bindings.interfaces.Linkable;
import org.jdownloader.myjdownloader.client.json.AbstractJsonData;

public class RemoteAPIController {

    private static RemoteAPIController INSTANCE = new RemoteAPIController();
    private final boolean              isJared  = Application.isJared(RemoteAPIController.class);

    public static RemoteAPIController getInstance() {
        return INSTANCE;
    }

    private SessionRemoteAPI<RemoteAPISession> rapi     = null;
    private RemoteAPISessionControllerImp      sessionc = null;
    private EventsAPI                          eventsapi;
    private LogSource                          logger;

    public static class MyJDownloaderEvent extends MyJDEvent implements Storable {
        public MyJDownloaderEvent() {

        }
    }

    private RemoteAPIController() {
        logger = LogController.getInstance().getLogger(RemoteAPIController.class.getName());
        rids = new HashMap<String, RIDArray>();
        rapi = new SessionRemoteAPI<RemoteAPISession>() {
            @Override
            public String toString(RemoteAPIRequest request, RemoteAPIResponse response, Object responseData) {
                if (((SessionRemoteAPIRequest) request).getApiRequest() instanceof DeprecatedRemoteAPIRequest) { return JSonStorage.serializeToJson(responseData);
                // ((DeprecatedRemoteAPIRequest)((SessionRemoteAPIRequest) request).getApiRequest()).getRequest()

                }
                MyJDownloaderRequestInterface ri = ((MyJDRemoteAPIRequest) ((SessionRemoteAPIRequest) request).getApiRequest()).getRequest();

                // Convert EventData Objects
                if (responseData instanceof List && ((List) responseData).size() > 0 && ((List) responseData).get(0) instanceof EventObjectStorable) {
                    if (ri.getApiVersion() <= 0) {
                        // old API events do not use any wrapping.
                        return JSonStorage.serializeToJson(responseData);
                    }
                    ArrayList<MyJDownloaderEvent> newResponse = new ArrayList<MyJDownloaderEvent>();
                    for (Object o : ((List) responseData)) {
                        MyJDownloaderEvent ret = new MyJDownloaderEvent();
                        ret.setEventData(((EventObjectStorable) o).getEventdata());
                        ret.setEventid(((EventObjectStorable) o).getEventid());
                        ret.setPublisher(((EventObjectStorable) o).getPublisher());
                        newResponse.add(ret);
                    }
                    responseData = newResponse;
                }

                if (ri.getApiVersion() > 0) {
                    return JSonStorage.serializeToJson(responseData);
                } else {
                    return super.toString(request, response, responseData);
                }

            }

            @Override
            public void sendText(RemoteAPIRequest request, RemoteAPIResponse response, String text, boolean chunked) throws UnsupportedEncodingException, IOException {
                try {
                    if (request instanceof SessionRemoteAPIRequest) {
                        if (((SessionRemoteAPIRequest) request).getApiRequest() instanceof DeprecatedRemoteAPIRequest) {
                            //
                            super.sendText(request, response, JSonStorage.serializeToJson(new DataObject(JSonStorage.restoreFromString(text, new TypeRef<Object>() {
                            }, null), -1)), chunked);
                            return;

                        }
                        MyJDownloaderRequestInterface ri = ((MyJDRemoteAPIRequest) ((SessionRemoteAPIRequest) request).getApiRequest()).getRequest();
                        if (ri.getApiVersion() > 0) {
                            super.sendText(request, response, JSonStorage.serializeToJson(new DataObject(JSonStorage.restoreFromString(text, new TypeRef<Object>() {
                            }, null), ri.getRid())), chunked);
                        } else {
                            super.sendText(request, response, text, chunked);
                        }
                    } else {
                        throw new WTFException();
                    }
                } catch (Throwable e) {
                    throw new WTFException(e);
                }

            }

            @Override
            protected RemoteAPIResponse createRemoteAPIResponseObject(HttpResponse response) {
                return new MyJDRmoteAPIResponse(response, this);
            }

            @Override
            protected RemoteAPIRequest createRemoteAPIRequestObject(HttpRequest request, String[] intf, InterfaceHandler<?> interfaceHandler, List<String> parameters, String jqueryCallback) throws IOException {
                if (request instanceof DeprecatedAPIRequestInterface) {

                return new DeprecatedRemoteAPIRequest(interfaceHandler, intf[2], parameters.toArray(new String[] {}), (DeprecatedAPIRequestInterface) request, jqueryCallback);
                //
                }
                return new MyJDRemoteAPIRequest(interfaceHandler, intf[2], parameters.toArray(new String[] {}), (MyJDownloaderRequestInterface) request);
            }

            @Override
            protected void validateRequest(HttpRequest request) throws BasicRemoteAPIException {
                super.validateRequest(request);
                if (request instanceof DeprecatedAPIRequestInterface) {

                return; }
                if (request instanceof MyJDownloaderPostRequest) {
                    org.jdownloader.myjdownloader.client.json.JSonRequest jsonRequest;
                    try {
                        jsonRequest = ((MyJDownloaderPostRequest) request).getJsonRequest();
                        if (jsonRequest == null) throw new BasicRemoteAPIException("no JSONRequest", ResponseCode.ERROR_BAD_REQUEST);
                        if (!isJared) logger.info(JSonStorage.toString(jsonRequest));
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
                DeprecatedAPIHttpServerController.getInstance().registerRequestHandler(3128, true, sessionc);
            }
        } catch (Throwable e) {
            logger.log(e);
        }
        register(eventsapi = new EventsAPI());
        validateInterfaces(EventsAPIInterface.class, EventsInterface.class);
        register(CaptchaAPISolver.getInstance());
        register(CaptchaAPISolver.getInstance().getEventPublisher());
        register(new JDAPIImpl());
        DownloadWatchDogEventPublisher downloadWatchDogEventPublisher = new DownloadWatchDogEventPublisher();
        DownloadsAPIImpl downloadsAPI;
        register(downloadsAPI = new DownloadsAPIImpl());
        register(new DownloadsAPIV2Impl());
        register(new DownloadWatchdogAPIImpl());
        register(downloadWatchDogEventPublisher);
        register(new AdvancedConfigManagerAPIImpl());
        register(new JDownloaderToolBarAPIImpl());
        register(new AccountAPIImpl());
        register(new AccountAPIImplV2());
        register(new LinkCollectorAPIImpl());
        register(new ContentAPIImpl());
        register(new ContentAPIImplV2());
        register(new PollingAPIImpl());
        register(new ExtractionAPIImpl());
        register(new LinkCrawlerAPIImpl());
        register(new PluginsAPIImpl());
        register(new ExternInterfaceImpl());
        register(new DownloadControllerEventPublisher());
        register(new LinkCollectorEventPublisher());
        register(new ExtensionsAPIImpl());
        register(new UpdateAPIImpl());
        register(new LinkCrawlerEventPublisher());
        RemoteAPIIOHandlerWrapper wrapper;
        UIOManager.setUserIO(wrapper = new RemoteAPIIOHandlerWrapper(UIOManager.I()));
        register(wrapper.getEventPublisher());
        register(wrapper.getApi());

        JDAnywhereAPI.getInstance().init(this, downloadsAPI);
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
            logger.log(e);
            Dialog.getInstance().showExceptionDialog("Bad API Interface", e.getMessage(), e);
            return false;
        }
    }

    public boolean unregister(final RemoteAPIInterface x) {
        if (x == null) return false;
        try {
            rapi.unregister(x);
            return true;
        } catch (final Throwable e) {
            logger.log(e);
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

    public static void validateInterfaces(Class<? extends RemoteAPIInterface> deviceInterface, Class<? extends Linkable> clientInterface) {
        if (Application.isJared(RemoteAPIController.class)) return;
        try {
            String deviceNameSpace = deviceInterface.getSimpleName();
            String clientNameSpace = clientInterface.getSimpleName();
            ApiNamespace deviceNameSpaceAnnotation = deviceInterface.getAnnotation(ApiNamespace.class);
            ClientApiNameSpace clientNameSpaceAnnotation = clientInterface.getAnnotation(ClientApiNameSpace.class);

            if (deviceNameSpaceAnnotation != null) deviceNameSpace = deviceNameSpaceAnnotation.value();
            if (clientNameSpaceAnnotation != null) clientNameSpace = clientNameSpaceAnnotation.value();

            if (!StringUtils.equals(deviceNameSpace, clientNameSpace)) { throw new Exception("DeviceNameSpace: " + deviceNameSpace + " != Clientnamespace " + clientNameSpace); }

            HashMap<String, Method> deviceMap = createMethodMap(deviceInterface.getDeclaredMethods());

            HashMap<String, Method> clientMap = createMethodMap(clientInterface.getDeclaredMethods());

            for (Entry<String, Method> e : deviceMap.entrySet()) {
                Method device = e.getValue();
                Method client = clientMap.get(e.getKey());
                if (client == null) {

                    //
                    throw new Exception(e.getKey() + " Missing in " + clientInterface);
                }

            }

            for (Entry<String, Method> e : clientMap.entrySet()) {
                Method client = e.getValue();
                Method device = clientMap.get(e.getKey());
                if (device == null) {
                    //
                    throw new Exception(e.getKey() + " Missing in " + clientInterface);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            Dialog.getInstance().showExceptionDialog("Error In API Interface Declaration", e.getMessage(), e);
        }
    }

    public static HashMap<String, Method> createMethodMap(Method[] deviceMethods) throws Exception {
        HashMap<String, Method> deviceMap = new HashMap<String, Method>();
        for (Method m : deviceMethods) {
            String name = m.getName();
            Class<?>[] actualTypes = m.getParameterTypes();

            ArrayList<Class<?>> params = new ArrayList<Class<?>>();
            for (Class<?> c : actualTypes) {
                if (Clazz.isInstanceof(c, RemoteAPIRequest.class)) continue;
                if (Clazz.isInstanceof(c, RemoteAPIResponse.class)) continue;
                Class<?> sc = c.getSuperclass();
                Package pkg = AbstractMyJDClient.class.getPackage();
                if (sc != null && sc != AbstractJsonData.class && sc.getName().startsWith(pkg.getName()) && !c.getName().startsWith(pkg.getName())) {
                    c = sc;
                }
                params.add(c);
            }
            String id = name + "(" + params + ")";

            Method oldMethod;
            if ((oldMethod = deviceMap.put(id, m)) != null) { throw new Exception("Dupe Method definition: " + m + " - " + oldMethod); }
        }
        return deviceMap;
    }

}
