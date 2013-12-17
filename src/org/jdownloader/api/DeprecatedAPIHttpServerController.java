package org.jdownloader.api;

import org.appwork.utils.net.httpserver.HttpServer;
import org.appwork.utils.net.httpserver.HttpServerController;

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

}
