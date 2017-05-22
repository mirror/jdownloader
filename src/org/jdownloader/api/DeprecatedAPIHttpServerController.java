package org.jdownloader.api;

import java.io.IOException;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.HttpConnection.ConnectionHook;
import org.appwork.utils.net.httpserver.HttpServer;
import org.appwork.utils.net.httpserver.HttpServerController;
import org.appwork.utils.net.httpserver.handler.ExtendedHttpRequestHandler;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;

public class DeprecatedAPIHttpServerController extends HttpServerController {
   

    private static DeprecatedAPIHttpServerController INSTANCE = new DeprecatedAPIHttpServerController();

    public static DeprecatedAPIHttpServerController getInstance() {
        return INSTANCE;
    }

    @Override
    protected HttpServer createServer(int port) {
        return new DeprecatedAPIServer(port);
    }

    private DeprecatedAPIHttpServerController() {
    }

    public void registerSessionController(final RemoteAPISessionControllerImp sessionc) throws IOException {
        registerRequestHandler(JsonConfig.create(RemoteAPIConfig.class).getDeprecatedApiPort(), true, sessionc);
    }
}
