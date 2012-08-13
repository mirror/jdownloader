package org.jdownloader.extensions.streaming.upnp;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.appwork.utils.logging2.LogSource;
import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.model.message.StreamRequestMessage;
import org.fourthline.cling.model.message.StreamResponseMessage;
import org.fourthline.cling.transport.Router;
import org.fourthline.cling.transport.impl.StreamClientConfigurationImpl;
import org.fourthline.cling.transport.impl.StreamClientImpl;
import org.fourthline.cling.transport.impl.StreamServerConfigurationImpl;
import org.fourthline.cling.transport.impl.StreamServerImpl;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.MulticastReceiver;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;
import org.fourthline.cling.transport.spi.StreamClient;
import org.fourthline.cling.transport.spi.StreamServer;
import org.jdownloader.logging.LogController;
import org.seamless.http.Headers;

import com.sun.net.httpserver.HttpServer;

public class Configuration extends DefaultUpnpServiceConfiguration {
    private static LogSource LOGGER = LogController.getInstance().getLogger(Configuration.class.getName());

    public Configuration() {
        super(0);

    }

    // Override using Apache Http instead of sun http
    // This could be used to implement our own http stack instead
    @Override
    public StreamClient<StreamClientConfigurationImpl> createStreamClient() {
        return new StreamClientImpl(new StreamClientConfigurationImpl()) {

            @Override
            public StreamResponseMessage sendRequest(StreamRequestMessage requestMessage) {
                return super.sendRequest(requestMessage);
            }

            @Override
            protected void applyRequestProperties(HttpURLConnection urlConnection, StreamRequestMessage requestMessage) {
                super.applyRequestProperties(urlConnection, requestMessage);
            }

            @Override
            protected void applyHeaders(HttpURLConnection urlConnection, Headers headers) {
                super.applyHeaders(urlConnection, headers);
            }

            @Override
            protected void applyRequestBody(HttpURLConnection urlConnection, StreamRequestMessage requestMessage) throws IOException {
                super.applyRequestBody(urlConnection, requestMessage);
            }

            @Override
            protected StreamResponseMessage createResponse(HttpURLConnection urlConnection, InputStream inputStream) throws Exception {
                LOGGER.info(urlConnection.toString());
                StreamResponseMessage ret = super.createResponse(urlConnection, inputStream);
                LOGGER.info(ret + "\r\n" + ret.getBodyString());
                return ret;
            }

        };
    }

    @Override
    public MulticastReceiver createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
        return new MulticastReceiverImpl2(networkAddressFactory);
    }

    @Override
    public StreamServer<StreamServerConfigurationImpl> createStreamServer(NetworkAddressFactory networkAddressFactory) {
        return new StreamServerImpl(new StreamServerConfigurationImpl(networkAddressFactory.getStreamListenPort())) {
            synchronized public void init(InetAddress bindAddress, Router router) throws InitializationException {
                try {
                    InetSocketAddress socketAddress = new InetSocketAddress(bindAddress, configuration.getListenPort());
                    LOGGER.info("HTTPServer: " + bindAddress + ":" + configuration.getListenPort());
                    server = HttpServer.create(socketAddress, configuration.getTcpConnectionBacklog());
                    server.createContext("/", new ServerRequestHttpHandler(router, LOGGER));

                    LOGGER.info("Created server (for receiving TCP streams) on: " + server.getAddress());

                } catch (Exception ex) {
                    throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex.toString(), ex);
                }
            }
        };
    }

    protected NetworkAddressFactory createNetworkAddressFactory(int streamListenPort) {
        return new FixedNetworkAddressFactoryImpl(streamListenPort);
    }

}
