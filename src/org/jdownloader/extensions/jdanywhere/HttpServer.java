package org.jdownloader.extensions.jdanywhere;

import org.appwork.utils.net.httpserver.HttpServerController;

public class HttpServer extends HttpServerController {

    private static HttpServer INSTANCE = new HttpServer();

    public static HttpServer getInstance() {
        return INSTANCE;
    }

    private HttpServer() {
    }

}
