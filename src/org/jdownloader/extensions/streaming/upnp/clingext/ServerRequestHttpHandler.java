package org.jdownloader.extensions.streaming.upnp.clingext;

import java.io.IOException;
import java.util.logging.Logger;

import org.appwork.storage.JSonStorage;
import org.fourthline.cling.transport.Router;
import org.fourthline.cling.transport.impl.HttpExchangeUpnpStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ServerRequestHttpHandler implements HttpHandler {

    private final Router router;
    private Logger       logger;

    public ServerRequestHttpHandler(Router router, Logger logger) {
        this.router = router;
        this.logger = logger;
    }

    // This is executed in the request receiving thread!
    public void handle(final HttpExchange httpExchange) throws IOException {
        // And we pass control to the service, which will (hopefully) start a new thread immediately so we can
        // continue the receiving thread ASAP
        logger.fine("Received HTTP exchange: " + httpExchange.getRequestMethod() + " " + httpExchange.getRequestURI());
        logger.fine("Received HTTP exchange: " + httpExchange.getRequestBody());
        router.received(new HttpExchangeUpnpStream(router.getProtocolFactory(), httpExchange));
        logger.fine(httpExchange.getResponseCode() + "");
        logger.fine(JSonStorage.toString(httpExchange.getResponseHeaders()));

    }
}
