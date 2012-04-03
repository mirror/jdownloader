package org.jdownloader.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.httpserver.HttpConnection;
import org.appwork.utils.net.httpserver.HttpHandlerInfo;
import org.appwork.utils.net.httpserver.requests.HttpRequest;

public class JDConnectClient {

    private static JDConnectClient INSTANCE = new JDConnectClient();

    public static JDConnectClient getInstace() {
        return INSTANCE;
    }

    private JDConnectClient() {
        boolean enabled = JsonConfig.create(JDConnectClientConfig.class).isClientEnabled();
        final int port = JsonConfig.create(JDConnectClientConfig.class).getPort();
        final String host = JsonConfig.create(JDConnectClientConfig.class).getHost();
        if (enabled && !StringUtils.isEmpty(host)) {
            final HttpHandlerInfo info = RemoteAPIController.getInstance().getHandlerInfo();
            if (info != null) {
                startJDConnect(host, port, info.getHttpServer());
            }
        }
    }

    private void startJDConnect(final String host, final int port, final org.appwork.utils.net.httpserver.HttpServer server) {
        new Thread() {
            @Override
            public void run() {
                try {
                    final Socket httpSocket = new Socket();
                    httpSocket.connect(new InetSocketAddress(host, port), 10000);
                    /*
                     * this injects the HTTPConnection from our JDConnect System
                     * into local running HTTPServer
                     */
                    new HttpConnection(server, httpSocket) {

                        @Override
                        protected void requestReceived(HttpRequest request) {
                            startJDConnect(host, port, server);
                        }

                    };
                } catch (final SocketTimeoutException e) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e1) {
                        return;
                    }
                } catch (IOException e) {
                    Log.exception(e);
                }
            }
        }.start();
    }
}
