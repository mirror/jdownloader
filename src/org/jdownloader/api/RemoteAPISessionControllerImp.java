package org.jdownloader.api;

import java.util.HashMap;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.HttpConnection;
import org.appwork.utils.net.httpserver.HttpConnection.ConnectionHook;
import org.appwork.utils.net.httpserver.HttpConnection.HttpConnectionType;
import org.appwork.utils.net.httpserver.handler.ExtendedHttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.HTTPBridge;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.requests.OptionsRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.appwork.utils.net.httpserver.session.HttpSessionController;
import org.jdownloader.api.myjdownloader.MyJDownloaderAPISession;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectThread;
import org.jdownloader.api.myjdownloader.MyJDownloaderController;
import org.jdownloader.api.myjdownloader.MyJDownloaderDirectHttpConnection;
import org.jdownloader.api.myjdownloader.MyJDownloaderHttpConnection;
import org.jdownloader.myjdownloader.client.json.ServerErrorType;

public class RemoteAPISessionControllerImp extends HttpSessionController<RemoteAPISession> implements ExtendedHttpRequestHandler, ConnectionHook {
    public RemoteAPISessionControllerImp() {
    }

    @Override
    public void onBeforeRequest(HttpRequest request, HttpResponse response) throws BasicRemoteAPIException {
        response.setHook(this);
        HTTPBridge bridge = request.getBridge();
        checkDirectConnectionToken(request);
        if (bridge != null && bridge instanceof MyJDownloaderConnectThread) {
            // no origin evaluation for my.jdownloader
            return;
        }
        HTTPHeader originHeader = request.getRequestHeaders().get(HTTPConstants.HEADER_REQUEST_ORIGIN);
        if (originHeader != null) {
            String origin = originHeader.getValue().replaceAll("^https?://", "");
            String value = JsonConfig.create(RemoteAPIConfig.class).getLocalAPIServerHeaderAccessControllAllowOrigin();
            if (StringUtils.isNotEmpty(origin) && !"*".equals(value)) {
                throw new org.appwork.remoteapi.exceptions.AuthException("Bad Origin");
            }
            // TODO Validate origin
        }
    }

    protected void checkDirectConnectionToken(HttpRequest request) throws BasicRemoteAPIException {
        final HttpConnection connection = request.getConnection();
        if (connection instanceof MyJDownloaderDirectHttpConnection && request.getHttpConnectionType() != HttpConnectionType.OPTIONS) {
            final MyJDownloaderDirectHttpConnection myConnection = (MyJDownloaderDirectHttpConnection) connection;
            final String sessionToken = myConnection.getRequestConnectToken();
            if (!MyJDownloaderController.getInstance().isSessionValid(sessionToken)) {
                final ServerErrorType type = ServerErrorType.TOKEN_INVALID;
                final ResponseCode code = ResponseCode.get(type.getCode());
                throw new BasicRemoteAPIException(null, type.name(), code, null);
            }
        }
    }

    @Override
    public void onAfterRequest(HttpRequest request, HttpResponse response, boolean handled) {
    }

    @Override
    public boolean onGetRequest(GetRequest request, HttpResponse response) throws BasicRemoteAPIException {
        if (request instanceof OptionsRequest) {
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH, "0"));
            response.setResponseCode(ResponseCode.SUCCESS_OK);
            return true;
        }
        return super.onGetRequest(request, response);
    }

    @Override
    public void onBeforeSendHeaders(HttpResponse response) {
        HttpRequest request = response.getConnection().getRequest();
        HTTPBridge bridge = request.getBridge();
        // https://scotthelme.co.uk/hardening-your-http-response-headers/#x-content-type-options
        if (bridge != null && bridge instanceof MyJDownloaderConnectThread) {
            response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
            response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_MAX_AGE, "1800"));
            response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS, GET, POST"));
            final String headers = request.getRequestHeaders().getValue("Access-Control-Request-Headers");
            if (headers != null) {
                response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_HEADERS, headers));
            }
            response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_X_FRAME_OPTIONS, "DENY"));
            response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_X_XSS_PROTECTION, "1; mode=block"));
            response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_REFERRER_POLICY, JsonConfig.create(RemoteAPIConfig.class).getLocalAPIServerHeaderReferrerPolicy()));
            response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_X_CONTENT_TYPE_OPTIONS, "nosniff"));
            return;
        }
        String value = JsonConfig.create(RemoteAPIConfig.class).getLocalAPIServerHeaderAccessControllAllowOrigin();
        if (StringUtils.isNotEmpty(value)) {
            response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_ORIGIN, value));
        }
        response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_MAX_AGE, "1800"));
        response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS, GET, POST"));
        final String headers = request.getRequestHeaders().getValue("Access-Control-Request-Headers");
        if (headers != null) {
            response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_ACCESS_CONTROL_ALLOW_HEADERS, headers));
        }
        response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_SECURITY_POLICY, JsonConfig.create(RemoteAPIConfig.class).getLocalAPIServerHeaderContentSecurityPolicy()));
        response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_X_FRAME_OPTIONS, JsonConfig.create(RemoteAPIConfig.class).getLocalAPIServerHeaderXFrameOptions()));
        response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_X_XSS_PROTECTION, JsonConfig.create(RemoteAPIConfig.class).getLocalAPIServerHeaderXXssProtection()));
        response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_REFERRER_POLICY, JsonConfig.create(RemoteAPIConfig.class).getLocalAPIServerHeaderReferrerPolicy()));
        response.getResponseHeaders().addIfAbsent(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_X_CONTENT_TYPE_OPTIONS, JsonConfig.create(RemoteAPIConfig.class).getLocalAPIServerHeaderXContentTypeOptions()));
    }

    @Override
    public void onAfterRequestException(HttpRequest request, HttpResponse response, Throwable e) {
    }

    private final HashMap<String, RemoteAPISession> sessions = new HashMap<String, RemoteAPISession>();

    @Override
    public RemoteAPISession getSession(final org.appwork.utils.net.httpserver.requests.HttpRequest request, final String id) {
        if (request.getConnection() instanceof MyJDownloaderHttpConnection) {
            return new MyJDownloaderAPISession(this, ((MyJDownloaderHttpConnection) (request.getConnection())));
        }
        synchronized (this.sessions) {
            return this.sessions.get(id);
        }
    }

    @Override
    protected RemoteAPISession newSession(RemoteAPIRequest request, final String username, final String password) {
        if (!"wron".equals(password)) {
            RemoteAPISession session = new RemoteAPISession(this);
            synchronized (this.sessions) {
                this.sessions.put(session.getSessionID(), session);
            }
            return session;
        } else {
            return null;
        }
    }

    @Override
    protected boolean removeSession(final RemoteAPISession session) {
        if (session == null) {
            return false;
        }
        synchronized (this.sessions) {
            final RemoteAPISession ret = this.sessions.remove(session.getSessionID());
            if (ret == null) {
                return false;
            }
            ret.setAlive(false);
            return true;
        }
    }
}
