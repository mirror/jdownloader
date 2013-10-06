package org.jdownloader.extensions.streaming.upnp.clingext;

import java.io.IOException;
import java.net.InetAddress;

import org.fourthline.cling.model.message.Connection;
import org.fourthline.cling.transport.Router;
import org.fourthline.cling.transport.impl.HttpExchangeUpnpStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class CustomRequestHttpHandler implements HttpHandler {

    private final Router router;

    public CustomRequestHttpHandler(Router router) {
        this.router = router;
    }

    protected class HttpServerConnection implements Connection {

        protected HttpExchange exchange;

        public HttpServerConnection(HttpExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public boolean isOpen() {

            // log.warning("Can't check client connection, socket access impossible on JDK webserver!");
            return true;

        }

        @Override
        public InetAddress getRemoteAddress() {
            return exchange.getRemoteAddress() != null ? exchange.getRemoteAddress().getAddress() : null;
        }

        @Override
        public InetAddress getLocalAddress() {
            return exchange.getLocalAddress() != null ? exchange.getLocalAddress().getAddress() : null;
        }
    }

    // This is executed in the request receiving thread!
    public void handle(final HttpExchange httpExchange) throws IOException {
        // And we pass control to the service, which will (hopefully) start a new thread immediately so we can
        // continue the receiving thread ASAP
        // log.fine("Received HTTP exchange: " + httpExchange.getRequestMethod() + " " + httpExchange.getRequestURI());
        // headers=httpExchange.getHttpContext()
        router.received(new HttpExchangeUpnpStream(router.getProtocolFactory(), httpExchange) {
            @Override
            protected Connection createConnection() {
                return new HttpServerConnection(httpExchange);
            }
        });
    }
}