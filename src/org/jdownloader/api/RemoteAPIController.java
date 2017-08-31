package org.jdownloader.api;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.DefaultDocsPageFactory;
import org.appwork.remoteapi.InterfaceHandler;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPI.RemoteAPIMethod;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.SessionRemoteAPI;
import org.appwork.remoteapi.SessionRemoteAPIRequest;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.events.EventObject;
import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.EventsAPI;
import org.appwork.remoteapi.events.EventsAPIInterface;
import org.appwork.remoteapi.events.Subscriber;
import org.appwork.remoteapi.events.json.EventObjectStorable;
import org.appwork.remoteapi.exceptions.APIFileNotFoundException;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.HttpConnection.HttpConnectionType;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.appwork.utils.net.websocket.ReadWebSocketFrame;
import org.appwork.utils.net.websocket.WebSocketEndPoint;
import org.appwork.utils.net.websocket.WebSocketFrame;
import org.appwork.utils.net.websocket.WebSocketFrameHeader;
import org.appwork.utils.net.websocket.WebSocketFrameHeader.OP_CODE;
import org.appwork.utils.net.websocket.WriteWebSocketFrame;
import org.appwork.utils.reflection.Clazz;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.accounts.AccountAPIImpl;
import org.jdownloader.api.accounts.v2.AccountAPIImplV2;
import org.jdownloader.api.captcha.CaptchaAPISolver;
import org.jdownloader.api.cnl2.ExternInterfaceImpl;
import org.jdownloader.api.config.AdvancedConfigManagerAPIImpl;
import org.jdownloader.api.content.ContentAPIImpl;
import org.jdownloader.api.content.v2.ContentAPIImplV2;
import org.jdownloader.api.device.DeviceAPIImpl;
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
import org.jdownloader.api.linkcollector.v2.LinkCollectorAPIImplV2;
import org.jdownloader.api.linkcrawler.LinkCrawlerAPIImpl;
import org.jdownloader.api.linkcrawler.LinkCrawlerEventPublisher;
import org.jdownloader.api.logs.LogAPIImpl;
import org.jdownloader.api.myjdownloader.MyJDownloaderPostRequest;
import org.jdownloader.api.myjdownloader.MyJDownloaderRequestInterface;
import org.jdownloader.api.plugins.PluginsAPIImpl;
import org.jdownloader.api.polling.PollingAPIImpl;
import org.jdownloader.api.reconnect.ReconnectAPIImpl;
import org.jdownloader.api.system.SystemAPIImpl;
import org.jdownloader.api.toolbar.JDownloaderToolBarAPIImpl;
import org.jdownloader.api.ui.UIAPIImpl;
import org.jdownloader.api.useragent.UserAgentController;
import org.jdownloader.captcha.api.CaptchaForwarder;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;
import org.jdownloader.myjdownloader.client.bindings.ClientApiNameSpace;
import org.jdownloader.myjdownloader.client.bindings.events.json.MyJDEvent;
import org.jdownloader.myjdownloader.client.bindings.interfaces.EventsInterface;
import org.jdownloader.myjdownloader.client.bindings.interfaces.Linkable;
import org.jdownloader.myjdownloader.client.json.AbstractJsonData;
import org.jdownloader.myjdownloader.client.json.ObjectData;

import jd.nutils.DiffMatchPatch;
import jd.nutils.DiffMatchPatch.Diff;
import jd.nutils.DiffMatchPatch.Patch;

public class RemoteAPIController {
    private static RemoteAPIController INSTANCE = new RemoteAPIController();
    private final boolean              isJared  = Application.isJared(RemoteAPIController.class);

    public static RemoteAPIController getInstance() {
        return INSTANCE;
    }

    private final SessionRemoteAPI<RemoteAPISession> rapi;
    private RemoteAPISessionControllerImp            sessionc = null;
    private EventsAPI                                eventsapi;
    private final LogSource                          logger;
    private AdvancedConfigManagerAPIImpl             advancedConfigAPI;
    private ContentAPIImplV2                         contentAPI;
    private DownloadsAPIV2Impl                       downloadsAPIV2;
    private LinkCollectorAPIImplV2                   linkcollector;
    private final UserAgentController                uaController;
    private final HashMap<String, RIDArray>          rids;

    public static class MyJDownloaderEvent extends MyJDEvent implements Storable {
        public MyJDownloaderEvent() {
        }
    }

    private RemoteAPIController() {
        this.uaController = new UserAgentController();
        logger = LogController.getInstance().getLogger(RemoteAPIController.class.getName());
        rids = new HashMap<String, RIDArray>();
        rapi = new SessionRemoteAPI<RemoteAPISession>() {
            @Override
            protected void _handleRemoteAPICall(RemoteAPIRequest request, RemoteAPIResponse response) throws BasicRemoteAPIException {
                uaController.handle(request);
                super._handleRemoteAPICall(request, response);
            }

            @Override
            protected DefaultDocsPageFactory createHelpBuilder() throws NoSuchMethodException {
                return new DocsPageFactoryImpl(this);
            }

            @Override
            public String toString(RemoteAPIRequest request, RemoteAPIResponse response, Object responseData) {
                if (((SessionRemoteAPIRequest) request).getApiRequest() instanceof DeprecatedRemoteAPIRequest) {
                    return JSonStorage.serializeToJson(responseData);
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
            public void sendText(RemoteAPIRequest request, RemoteAPIResponse response, String text) throws UnsupportedEncodingException, IOException {
                try {
                    if (!isJared) {
                        logger.info("\r\n===========API Call Result:==============\r\n" + request.toString() + "\r\nResponse:\r\n" + text + "\r\n" + "=========================================");
                    }
                    if (request instanceof SessionRemoteAPIRequest) {
                        if (((SessionRemoteAPIRequest) request).getApiRequest() instanceof DeprecatedRemoteAPIRequest) {
                            //
                            super.sendText(request, response, JSonStorage.serializeToJson(new ObjectData(JSonStorage.restoreFromString(text, new TypeRef<Object>() {
                            }, null), -1)));
                            return;
                        }
                        MyJDownloaderRequestInterface ri = ((MyJDRemoteAPIRequest) ((SessionRemoteAPIRequest) request).getApiRequest()).getRequest();
                        if (ri.getApiVersion() > 0) {
                            long dka = ri.getDiffKeepAlive();
                            String type = ri.getDiffType();
                            if (dka > 0 && type != null) {
                                String diffID = ri.getDiffID();
                                String old = null;
                                if (StringUtils.isNotEmpty(diffID)) {
                                    File tmp = Application.getTempResource("apidiffs/" + ri.getDiffID() + ".dat");
                                    if (tmp.exists()) {
                                        old = IO.readFileToString(tmp);
                                    }
                                }
                                File tmp = Application.getTempResource("apidiffs/" + Hash.getMD5(text) + ".dat");
                                IO.secureWrite(tmp, text.getBytes("UTF-8"));
                                if (old == null) {
                                    ObjectData od = new ObjectData(text, ri.getRid());
                                    od.setDiffID(Hash.getMD5(text));
                                    super.sendText(request, response, JSonStorage.serializeToJson(od));
                                } else if ("patch".equalsIgnoreCase(type)) {
                                    DiffMatchPatch differ = new DiffMatchPatch();
                                    LinkedList<Diff> diff = differ.diffMain(old, text);
                                    differ.diffEditCost = 10;
                                    differ.diffCleanupEfficiency(diff);
                                    LinkedList<Patch> patches = differ.patchMake(diff);
                                    String md5 = Hash.getMD5(text);
                                    String difftext = differ.patchToText(patches);
                                    if (difftext.length() >= text.length()) {
                                        // diff longer than the actual content.
                                        ObjectData od = new ObjectData(text, ri.getRid());
                                        od.setDiffID(Hash.getMD5(text));
                                        super.sendText(request, response, JSonStorage.serializeToJson(od));
                                    } else {
                                        ObjectData od = new ObjectData(difftext, ri.getRid());
                                        od.setDiffID(md5);
                                        od.setDiffType("patch");
                                        super.sendText(request, response, JSonStorage.serializeToJson(od));
                                    }
                                } else {
                                    ObjectData od = new ObjectData(text, ri.getRid());
                                    od.setDiffID(Hash.getMD5(text));
                                    super.sendText(request, response, JSonStorage.serializeToJson(od));
                                }
                            } else {
                                super.sendText(request, response, JSonStorage.serializeToJson(new ObjectData(JSonStorage.restoreFromString(text, new TypeRef<Object>() {
                                }, null), ri.getRid())));
                            }
                        } else {
                            super.sendText(request, response, text);
                        }
                    } else {
                        throw new WTFException();
                    }
                } catch (Throwable e) {
                    throw new WTFException(e);
                }
            }

            @Override
            protected RemoteAPIResponse createRemoteAPIResponseObject(RemoteAPIRequest request, HttpResponse response) {
                final RemoteAPIResponse ret = new MyJDRmoteAPIResponse(response, this);
                return ret;
            }

            @Override
            public RemoteAPIRequest createRemoteAPIRequestObject(HttpRequest request, Object extractedData, final String methodName, InterfaceHandler<?> interfaceHandler, ParsedParameters parsedParameters) throws IOException, BasicRemoteAPIException {
                if (request instanceof DeprecatedAPIRequestInterface) {
                    return new DeprecatedRemoteAPIRequest(interfaceHandler, methodName, parsedParameters.parameters.toArray(new String[] {}), (DeprecatedAPIRequestInterface) request, parsedParameters.jqueryCallback);
                    //
                }
                return new MyJDRemoteAPIRequest(interfaceHandler, methodName, parsedParameters.parameters.toArray(new String[] {}), (MyJDownloaderRequestInterface) request);
            }

            @Override
            protected void validateRequest(HttpRequest request) throws BasicRemoteAPIException {
                super.validateRequest(request);
                if (request instanceof DeprecatedAPIRequestInterface) {
                    return;
                }
                if (request instanceof MyJDownloaderPostRequest) {
                    org.jdownloader.myjdownloader.client.json.JSonRequest jsonRequest;
                    try {
                        jsonRequest = ((MyJDownloaderPostRequest) request).getJsonRequest();
                        if (jsonRequest == null) {
                            throw new BasicRemoteAPIException("no JSONRequest", ResponseCode.ERROR_BAD_REQUEST);
                        }
                        if (!isJared) {
                            // logger.info(JSonStorage.toString(jsonRequest));
                        }
                        if (StringUtils.isEmpty(jsonRequest.getUrl())) {
                            throw new BasicRemoteAPIException("JSonRequest URL is empty", ResponseCode.ERROR_BAD_REQUEST);
                        }
                        if (!StringUtils.equals(request.getRequestedURL(), jsonRequest.getUrl())) {
                            throw new BasicRemoteAPIException("JSonRequest URL=" + jsonRequest.getUrl() + " does not match " + request.getRequestedURL(), ResponseCode.ERROR_BAD_REQUEST);
                        }
                        if (!validateRID(jsonRequest.getRid(), ((MyJDownloaderPostRequest) request).getRequestConnectToken())) {
                            throw new BasicRemoteAPIException("JSonRequest URL=" + jsonRequest.getUrl() + " has duplicated RID=" + jsonRequest.getRid(), ResponseCode.ERROR_BAD_REQUEST);
                        }
                    } catch (IOException e) {
                        throw new BasicRemoteAPIException(e);
                    }
                }
                MyJDownloaderRequestInterface ri = (MyJDownloaderRequestInterface) request;
                try {
                    if (!validateRID(ri.getRid(), ri.getRequestConnectToken())) {
                        throw new BasicRemoteAPIException("JSonRequest URL=" + request.getRequestedURL() + " has duplicated RID=" + ri.getRid(), ResponseCode.ERROR_BAD_REQUEST);
                    }
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
                DeprecatedAPIHttpServerController.getInstance().registerRequestHandler(JsonConfig.create(RemoteAPIConfig.class).getDeprecatedApiPort(), true, sessionc);
            }
        } catch (Throwable e) {
            logger.log(e);
        }
        register(eventsapi = new EventsAPI() {
            @Override
            public void listen(RemoteAPIRequest request, RemoteAPIResponse response, long subscriptionid) throws APIFileNotFoundException, InternalApiException {
                final Subscriber subscriber = getSubscriber(subscriptionid);
                if (subscriber == null) {
                    throw new APIFileNotFoundException();
                }
                if (!wrapWebSocket(request, response, subscriber)) {
                    super.listen(request, response, subscriptionid);
                }
            }

            private boolean wrapWebSocket(RemoteAPIRequest request, RemoteAPIResponse response, Subscriber subscriber) throws InternalApiException {
                final String upgradeHeader = request.getRequestHeaders().getValue("Upgrade");
                final String connectionHeader = request.getRequestHeaders().getValue(HTTPConstants.HEADER_REQUEST_CONNECTION);
                final String secWebSocketKey = request.getRequestHeaders().getValue("Sec-WebSocket-Key");
                if (StringUtils.isNotEmpty(secWebSocketKey) && StringUtils.equalsIgnoreCase(upgradeHeader, "websocket") && StringUtils.containsIgnoreCase(connectionHeader, "upgrade")) {
                    response.setResponseCode(ResponseCode.SWITCHING_PROTOCOLS);
                    response.getResponseHeaders().add(new HTTPHeader("Connection", "Upgrade"));
                    response.getResponseHeaders().add(new HTTPHeader("Upgrade", "websocket"));
                    try {
                        final MessageDigest md = MessageDigest.getInstance("SHA1");
                        final byte[] digest = md.digest((secWebSocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8"));
                        response.getResponseHeaders().add(new HTTPHeader("Sec-WebSocket-Accept", Base64.encodeToString(digest, false)));
                        final OutputStream outputStream = response.getOutputStream(true);
                        final InputStream is = request.getHttpRequest().getConnection().getInputStream();
                        final WebSocketEndPoint wsc = new WebSocketEndPoint() {
                            @Override
                            public ReadWebSocketFrame readWebSocketFrame(InputStream is) throws IOException {
                                if (is.available() > 0) {
                                    return super.readWebSocketFrame(is);
                                } else {
                                    if (true) {
                                        return null;
                                    } else {
                                        final byte[] temp = new byte[1];
                                        final int read;
                                        try {
                                            read = is.read(temp, 0, 1);
                                        } catch (IOException e) {
                                            return null;
                                        }
                                        if (read == 1) {
                                            final PushbackInputStream pbis = new PushbackInputStream(is);
                                            pbis.unread(temp, 0, 1);
                                            return super.readWebSocketFrame(pbis);
                                        } else if (read == 0) {
                                            return null;
                                        } else {
                                            throw new EOFException();
                                        }
                                    }
                                }
                            };

                            @Override
                            protected void log(WebSocketFrame webSocketFrame) {
                            }

                            @Override
                            protected OutputStream getOutputStream() throws IOException {
                                return outputStream;
                            }

                            @Override
                            protected InputStream getInputStream() throws IOException {
                                return is;
                            }
                        };
                        long lastActivity = System.currentTimeMillis();
                        boolean waitForPong = false;
                        eventLoop: while (true) {
                            if (!subscriber.isAlive()) {
                                wsc.writeFrame(wsc.buildCloseFrame());
                                break eventLoop;
                            }
                            final EventObject event;
                            if (!waitForPong) {
                                event = pollEvent(subscriber, 0);
                                if (event != null) {
                                    final byte[] jsonBytes = JSonStorage.getMapper().objectToByteArray(new EventObjectStorable(event));
                                    try {
                                        wsc.writeFrame(new WriteWebSocketFrame(new WebSocketFrameHeader(true, OP_CODE.UTF8_TEXT, jsonBytes.length, null), jsonBytes));
                                    } catch (IOException e) {
                                        pushBackEvent(subscriber, Arrays.asList(new EventObject[] { event }));
                                        throw e;
                                    }
                                }
                            } else {
                                event = null;
                            }
                            final ReadWebSocketFrame frame = wsc.readNextFrame();
                            if (frame != null) {
                                waitForPong = false;
                                lastActivity = System.currentTimeMillis();
                                System.out.println(frame);
                                switch (frame.getFrameHeader().getOpcode()) {
                                case PING:
                                    wsc.writeFrame(wsc.buildPongFrame(frame));
                                    break;
                                case CLOSE:
                                    break eventLoop;
                                default:
                                    break;
                                }
                            } else if (event == null) {
                                if (System.currentTimeMillis() - lastActivity > 15 * 1000) {
                                    waitForPong = true;
                                    wsc.writeFrame(wsc.buildPingFrame());
                                }
                                Thread.sleep(50);
                            }
                        }
                    } catch (Exception e) {
                        throw new InternalApiException(e);
                    }
                    return true;
                }
                return false;
            }
        });
        validateInterfaces(EventsAPIInterface.class, EventsInterface.class);
        register(CaptchaAPISolver.getInstance());
        register(CaptchaForwarder.getInstance());
        register(CaptchaAPISolver.getInstance().getEventPublisher());
        register(new JDAPIImpl());
        DownloadWatchDogEventPublisher downloadWatchDogEventPublisher = new DownloadWatchDogEventPublisher();
        DownloadsAPIImpl downloadsAPI;
        register(downloadsAPI = new DownloadsAPIImpl());
        register(downloadsAPIV2 = new DownloadsAPIV2Impl());
        register(new DownloadWatchdogAPIImpl());
        register(downloadWatchDogEventPublisher);
        register(advancedConfigAPI = new AdvancedConfigManagerAPIImpl());
        register(new JDownloaderToolBarAPIImpl());
        register(new AccountAPIImpl());
        register(new AccountAPIImplV2());
        register(new SystemAPIImpl());
        register(new LinkCollectorAPIImpl());
        register(linkcollector = new LinkCollectorAPIImplV2());
        register(new ContentAPIImpl());
        register(contentAPI = new ContentAPIImplV2());
        register(new UIAPIImpl());
        register(new PollingAPIImpl());
        register(new ExtractionAPIImpl());
        register(new LinkCrawlerAPIImpl());
        register(new PluginsAPIImpl());
        register(new ExternInterfaceImpl());
        register(new DownloadControllerEventPublisher(eventsapi));
        register(new LinkCollectorEventPublisher());
        register(new ExtensionsAPIImpl());
        register(new UpdateAPIImpl());
        register(new LinkCrawlerEventPublisher());
        register(new DeviceAPIImpl());
        register(new ReconnectAPIImpl());
        register(new LogAPIImpl());
        RemoteAPIIOHandlerWrapper wrapper;
        UIOManager.setUserIO(wrapper = new RemoteAPIIOHandlerWrapper(UIOManager.I()));
        register(wrapper.getRemoteHandler());
        JDAnywhereAPI.getInstance().init(this, downloadsAPI);
    }

    public UserAgentController getUaController() {
        return uaController;
    }

    public LinkCollectorAPIImplV2 getLinkcollector() {
        return linkcollector;
    }

    public DownloadsAPIV2Impl getDownloadsAPIV2() {
        return downloadsAPIV2;
    }

    public ContentAPIImplV2 getContentAPI() {
        return contentAPI;
    }

    public AdvancedConfigManagerAPIImpl getAdvancedConfigAPI() {
        return advancedConfigAPI;
    }

    /* TODO: add session support, currently all sessions share the same validateRID */
    public synchronized boolean validateRID(long rid, String sessionToken) {
        if (true) {
            return true;
        }
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

    public boolean register(final Object x) {
        if (x == null) {
            return false;
        }
        boolean ret = false;
        if (x instanceof EventPublisher) {
            ret = ret || eventsapi.register((EventPublisher) x);
        }
        if (x instanceof RemoteAPIInterface) {
            try {
                rapi.register((RemoteAPIInterface) x);
                ret = true;
            } catch (final Throwable e) {
                logger.log(e);
                Dialog.getInstance().showExceptionDialog("Bad API Interface", e.getMessage(), e);
                return false;
            }
        }
        return ret;
    }

    public EventsAPI getEventsapi() {
        return eventsapi;
    }

    public boolean unregister(final RemoteAPIInterface x) {
        if (x == null) {
            return false;
        }
        try {
            rapi.unregister(x);
            return true;
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        }
    }

    public boolean unregister(EventPublisher publisher) {
        if (publisher == null) {
            return false;
        }
        return eventsapi.unregister(publisher);
    }

    public static void validateInterfaces(Class<? extends RemoteAPIInterface> deviceInterface, Class<? extends Linkable> clientInterface) {
        if (Application.isJared(RemoteAPIController.class)) {
            return;
        }
        try {
            String deviceNameSpace = deviceInterface.getSimpleName();
            String clientNameSpace = clientInterface.getSimpleName();
            ApiNamespace deviceNameSpaceAnnotation = deviceInterface.getAnnotation(ApiNamespace.class);
            ClientApiNameSpace clientNameSpaceAnnotation = clientInterface.getAnnotation(ClientApiNameSpace.class);
            if (deviceNameSpaceAnnotation != null) {
                deviceNameSpace = deviceNameSpaceAnnotation.value();
            }
            if (clientNameSpaceAnnotation != null) {
                clientNameSpace = clientNameSpaceAnnotation.value();
            }
            if (!StringUtils.equals(deviceNameSpace, clientNameSpace)) {
                throw new Exception("DeviceNameSpace: " + deviceNameSpace + " != Clientnamespace " + clientNameSpace);
            }
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
            Dialog.getInstance().showExceptionDialog("Error In API Interface Declaration ", e.getMessage(), e);
        }
    }

    public static HashMap<String, Method> createMethodMap(Method[] deviceMethods) throws Exception {
        HashMap<String, Method> deviceMap = new HashMap<String, Method>();
        for (Method m : deviceMethods) {
            String name = m.getName();
            Class<?>[] actualTypes = m.getParameterTypes();
            ArrayList<Class<?>> params = new ArrayList<Class<?>>();
            for (Class<?> c : actualTypes) {
                if (Clazz.isInstanceof(c, RemoteAPIRequest.class)) {
                    continue;
                }
                if (Clazz.isInstanceof(c, RemoteAPIResponse.class)) {
                    continue;
                }
                Class<?> sc = c.getSuperclass();
                Package pkg = AbstractMyJDClient.class.getPackage();
                if (sc != null && sc != AbstractJsonData.class && sc.getName().startsWith(pkg.getName()) && !c.getName().startsWith(pkg.getName())) {
                    c = sc;
                }
                params.add(c);
            }
            String id = name + "(" + params + ")";
            Method oldMethod;
            if ((oldMethod = deviceMap.put(id, m)) != null) {
                throw new Exception("Dupe Method definition: " + m + " - " + oldMethod);
            }
        }
        return deviceMap;
    }

    public Object call(final String namespace, final String methodName, Object... params) throws BasicRemoteAPIException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        RemoteAPIMethod dummyMethod = this.rapi.getRemoteAPIMethod(new HttpRequest(null) {
            @Override
            public String getRequestedPath() {
                return "/" + namespace + "/" + methodName;
            }

            @Override
            public HttpConnectionType getHttpConnectionType() {
                return HttpConnectionType.GET;
            }

            @Override
            public String getParameterbyKey(String key) throws IOException {
                return null;
            }

            @Override
            public String[] getParametersbyKey(String key) throws IOException {
                return null;
            }
        });
        InterfaceHandler<?> iface = dummyMethod.getInterfaceHandler();
        Method method = iface.getMethod(methodName, params.length);
        ArrayList<String> stringParams = new ArrayList<String>();
        for (Object o : params) {
            stringParams.add(JSonStorage.serializeToJson(o));
        }
        final Object[] parameters = new Object[method.getParameterTypes().length];
        int count = 0;
        for (int i = 0; i < parameters.length; i++) {
            if (RemoteAPIRequest.class.isAssignableFrom(method.getParameterTypes()[i])) {
                throw new BasicRemoteAPIException("Not Found", ResponseCode.ERROR_NOT_FOUND);
            } else if (RemoteAPIResponse.class.isAssignableFrom(method.getParameterTypes()[i])) {
                throw new BasicRemoteAPIException("Not Found", ResponseCode.ERROR_NOT_FOUND);
            } else {
                try {
                    parameters[i] = RemoteAPI.convert(stringParams.get(count), method.getGenericParameterTypes()[i]);
                } catch (final Throwable e) {
                    try {
                        // maybe the parameter has been a Bla....Parameter array. let's try to evaluate the whole array as one
                        if (parameters.length == 1 && method.getGenericParameterTypes()[i] instanceof Class && ((Class) method.getGenericParameterTypes()[i]).isArray()) {
                            parameters[i] = RemoteAPI.convert(JSonStorage.serializeToJson(params), method.getGenericParameterTypes()[i]);
                        } else {
                            throw new BadParameterException(e, stringParams.get(count));
                        }
                    } catch (final Throwable e1) {
                        throw new BadParameterException(e, stringParams.get(count));
                    }
                }
                count++;
            }
        }
        return iface.invoke(method, parameters);
    }
}
