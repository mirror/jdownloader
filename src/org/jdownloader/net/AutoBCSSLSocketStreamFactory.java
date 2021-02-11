package org.jdownloader.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.appwork.utils.net.httpconnection.JavaSSLSocketStreamFactory;
import org.appwork.utils.net.httpconnection.SSLSocketStreamFactory;
import org.appwork.utils.net.httpconnection.SSLSocketStreamInterface;
import org.appwork.utils.net.httpconnection.SSLSocketStreamOptions;
import org.appwork.utils.net.httpconnection.SocketStreamInterface;

public class AutoBCSSLSocketStreamFactory implements SSLSocketStreamFactory {
    private final static String              BC_FACTORY = "BC_Factory";
    private final BCSSLSocketStreamFactory   bc;
    private final JavaSSLSocketStreamFactory jsse;

    public AutoBCSSLSocketStreamFactory() {
        bc = new BCSSLSocketStreamFactory();
        jsse = new JavaSSLSocketStreamFactory();
    }

    public interface AutoSwitchSSLSocketStreamInterface extends SSLSocketStreamInterface {
        public SSLSocketStreamInterface getInternalSSLSocketStreamInterface();

        public SSLSocketStreamFactory getInternalSSLSocketStreamFactory();
    }

    @Override
    public SSLSocketStreamInterface create(final SocketStreamInterface socketStream, final String host, final int port, final boolean autoclose, final SSLSocketStreamOptions options) throws IOException {
        final SSLSocketStreamInterface ret;
        if (options.getCustomFactorySettings().contains(BC_FACTORY)) {
            ret = bc.create(socketStream, host, port, autoclose, options);
        } else {
            ret = jsse.create(socketStream, host, port, autoclose, options);
        }
        return new AutoSwitchSSLSocketStreamInterface() {
            @Override
            public Socket getSocket() {
                return getInternalSSLSocketStreamInterface().getSocket();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return getInternalSSLSocketStreamInterface().getInputStream();
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return getInternalSSLSocketStreamInterface().getOutputStream();
            }

            @Override
            public void close() throws IOException {
                getInternalSSLSocketStreamInterface().close();
            }

            @Override
            public String getCipherSuite() {
                return "AutoSwitch|" + getInternalSSLSocketStreamInterface().getCipherSuite();
            }

            @Override
            public SocketStreamInterface getParentSocketStream() {
                return getInternalSSLSocketStreamInterface().getParentSocketStream();
            }

            @Override
            public SSLSocketStreamOptions getOptions() {
                return getInternalSSLSocketStreamInterface().getOptions();
            }

            @Override
            public SSLSocketStreamFactory getInternalSSLSocketStreamFactory() {
                return getInternalSSLSocketStreamInterface().getSSLSocketStreamFactory();
            }

            @Override
            public SSLSocketStreamInterface getInternalSSLSocketStreamInterface() {
                return ret;
            }

            @Override
            public SSLSocketStreamFactory getSSLSocketStreamFactory() {
                return AutoBCSSLSocketStreamFactory.this;
            }
        };
    }

    @Override
    public String retry(SSLSocketStreamOptions options, Exception e) {
        final String jsseRetry = jsse.retry(options, e);
        if (jsseRetry != null) {
            options.getCustomFactorySettings().remove(BC_FACTORY);
            return jsseRetry;
        }
        final String bcRetry = bc.retry(options, e);
        if (bcRetry != null) {
            options.getCustomFactorySettings().add(BC_FACTORY);
            return bcRetry;
        }
        return null;
    }
}
