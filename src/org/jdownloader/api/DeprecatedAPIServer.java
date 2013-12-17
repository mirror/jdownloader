package org.jdownloader.api;

import java.io.IOException;
import java.net.Socket;

import org.appwork.utils.net.httpserver.HttpConnection;
import org.appwork.utils.net.httpserver.HttpServer;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.HeadRequest;
import org.appwork.utils.net.httpserver.requests.OptionsRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;

public class DeprecatedAPIServer extends HttpServer {
    public static final class DeprecatedPostRequest extends PostRequest implements DeprecatedAPIRequestInterface {
        public DeprecatedPostRequest(HttpConnection connection) {
            super(connection);
        }
    }

    public static final class DeprecatedOptionsRequest extends OptionsRequest implements DeprecatedAPIRequestInterface {
        public DeprecatedOptionsRequest(HttpConnection connection) {
            super(connection);
        }
    }

    public static final class DeprecatedHeadRequest extends HeadRequest implements DeprecatedAPIRequestInterface {
        public DeprecatedHeadRequest(HttpConnection connection) {
            super(connection);
        }
    }

    public static final class DeprecatedGetRequest extends GetRequest implements DeprecatedAPIRequestInterface {
        public DeprecatedGetRequest(HttpConnection connection) {
            super(connection);
        }
    }

    public DeprecatedAPIServer(int port) {
        super(port);
    }

    public class CustomHttpConnection extends HttpConnection {

        protected GetRequest buildGetRequest() {
            return new DeprecatedGetRequest(this);
        }

        protected HeadRequest buildHeadRequest() {
            return new DeprecatedHeadRequest(this);
        }

        protected OptionsRequest buildOptionsRequest() {
            return new DeprecatedOptionsRequest(this);
        }

        protected PostRequest buildPostRequest() {
            return new DeprecatedPostRequest(this);
        }

        public CustomHttpConnection(HttpServer server, Socket clientSocket) throws IOException {
            super(server, clientSocket);
        }

    }

    protected HttpConnection createConnectionInstance(final Socket clientSocket) throws IOException {
        return new CustomHttpConnection(this, clientSocket);
    }
}
