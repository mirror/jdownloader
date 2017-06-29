package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.channels.ClosedByInterruptException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jd.controlling.proxy.ProxyController;
import jd.http.SocketConnectionFactory;

import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.net.httpconnection.HTTPConnectionImpl;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyException;
import org.appwork.utils.net.httpconnection.SocketStreamInterface;
import org.appwork.utils.net.socketconnection.SocketConnection;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectThread.DeviceConnectionHelper;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectThread.SessionInfoWrapper;
import org.jdownloader.myjdownloader.client.json.DeviceConnectionStatus;

public class MyJDownloaderWaitingConnectionThread extends Thread {
    protected static class MyJDownloaderConnectionRequest {
        public final SessionInfoWrapper getSession() {
            return session;
        }

        private final SessionInfoWrapper     session;
        private final DeviceConnectionHelper connectionHelper;

        public final DeviceConnectionHelper getConnectionHelper() {
            return connectionHelper;
        }

        protected MyJDownloaderConnectionRequest(SessionInfoWrapper session, DeviceConnectionHelper connectionHelper) {
            this.session = session;
            this.connectionHelper = connectionHelper;
        }
    }

    protected static class MyJDownloaderConnectionResponse {
        public final DeviceConnectionStatus getStatus() {
            return status;
        }

        public final SocketStreamInterface getSocketStream() {
            return socket;
        }

        public final Throwable getThrowable() {
            return throwable;
        }

        private final DeviceConnectionStatus               status;
        private final SocketStreamInterface                socket;
        private final Throwable                            throwable;
        private final MyJDownloaderWaitingConnectionThread thread;
        private final MyJDownloaderConnectionRequest       request;

        public final MyJDownloaderConnectionRequest getRequest() {
            return request;
        }

        protected MyJDownloaderConnectionResponse(MyJDownloaderWaitingConnectionThread thread, MyJDownloaderConnectionRequest request, DeviceConnectionStatus connectionStatus, SocketStreamInterface connectionSocket, Throwable e) {
            this.request = request;
            this.status = connectionStatus;
            this.socket = connectionSocket;
            this.throwable = e;
            this.thread = thread;
        }

        /**
         * @return the thread
         */
        public final MyJDownloaderWaitingConnectionThread getThread() {
            return thread;
        }
    }

    protected final AtomicBoolean                                           running           = new AtomicBoolean(true);
    protected final NullsafeAtomicReference<MyJDownloaderConnectionRequest> connectionRequest = new NullsafeAtomicReference<MyJDownloaderConnectionRequest>();
    protected final MyJDownloaderConnectThread                              connectThread;
    private final static AtomicInteger                                      THREADID          = new AtomicInteger(0);

    public MyJDownloaderWaitingConnectionThread(MyJDownloaderConnectThread connectThread) {
        this.setDaemon(true);
        this.setName("MyJDownloaderWaitingConnectionThread:" + THREADID.incrementAndGet());
        this.connectThread = connectThread;
    }

    @Override
    public void run() {
        try {
            while (running.get()) {
                MyJDownloaderConnectionRequest request = null;
                synchronized (connectionRequest) {
                    if ((request = connectionRequest.getAndSet(null)) == null) {
                        if (running.get() == false) {
                            return;
                        }
                        connectionRequest.wait();
                        request = connectionRequest.getAndSet(null);
                    }
                }
                if (request != null) {
                    Throwable e = null;
                    DeviceConnectionStatus status = null;
                    request.getConnectionHelper().backoff();
                    HTTPProxy proxy = null;
                    final InetSocketAddress addr = request.getConnectionHelper().getAddr();
                    final URL url = new URL(null, "socket://" + SocketConnection.getHostName(addr) + ":" + addr.getPort(), ProxyController.SOCKETURLSTREAMHANDLER);
                    Socket socket = null;
                    SocketStreamInterface socketStream = null;
                    request.getConnectionHelper().mark();
                    try {
                        connectThread.log("Connect " + addr);
                        final List<HTTPProxy> list = ProxyController.getInstance().getProxiesByURL(url, false, false);
                        if (list != null && list.size() > 0) {
                            proxy = list.get(0);
                            socket = SocketConnectionFactory.createSocket(proxy);
                            socket.setReuseAddress(true);
                            socket.setSoTimeout(180000);
                            socket.setTcpNoDelay(true);
                            socket.connect(addr, 30000);
                            final Socket finalSocket = socket;
                            socketStream = new SocketStreamInterface() {
                                @Override
                                public Socket getSocket() {
                                    return finalSocket;
                                }

                                @Override
                                public OutputStream getOutputStream() throws IOException {
                                    return finalSocket.getOutputStream();
                                }

                                @Override
                                public InputStream getInputStream() throws IOException {
                                    return finalSocket.getInputStream();
                                }

                                @Override
                                public void close() throws IOException {
                                    finalSocket.close();
                                }
                            };
                            if (addr.getPort() == 443) {
                                socketStream = HTTPConnectionImpl.getDefaultSSLSocketStreamFactory().create(socketStream, SocketConnection.getHostName(addr), 443, true, true);
                            }
                            socketStream.getOutputStream().write(("DEVICE" + request.getSession().getSessionToken()).getBytes("ISO-8859-1"));
                            socketStream.getOutputStream().flush();
                            final int validToken = socketStream.getInputStream().read();
                            status = DeviceConnectionStatus.parse(validToken);
                        } else {
                            synchronized (connectionRequest) {
                                try {
                                    connectionRequest.wait(5000);
                                } catch (final InterruptedException ignore) {
                                }
                                throw new ConnectException("No available connection for: " + url);
                            }
                        }
                    } catch (ClosedByInterruptException ignore) {
                        // SocketChannel Socket-> Interrupted
                    } catch (Throwable throwable) {
                        try {
                            connectThread.log(throwable);
                            e = throwable;
                            if (proxy != null && !proxy.isNone() && throwable instanceof HTTPProxyException) {
                                ProxyController.getInstance().reportHTTPProxyException(proxy, url, (IOException) throwable);
                            }
                        } finally {
                            try {
                                if (socket != null) {
                                    socket.close();
                                    socket = null;
                                }
                            } catch (final Throwable ignore) {
                            }
                        }
                    } finally {
                        request.getConnectionHelper().unmark();
                    }
                    final MyJDownloaderConnectionResponse response = new MyJDownloaderConnectionResponse(this, request, status, socketStream, e);
                    if (connectThread.putResponse(response) == false) {
                        connectThread.log("putResponse failed, maybe connectThread is closed/interrupted.");
                        try {
                            if (socket != null) {
                                socket.close();
                            }
                        } catch (final Throwable ignore) {
                        }
                    }
                }
            }
        } catch (final Throwable e) {
            connectThread.log(e);
        } finally {
            abort();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean putRequest(MyJDownloaderConnectionRequest request) {
        synchronized (connectionRequest) {
            if (running.get() == false) {
                return false;
            }
            if (connectionRequest.compareAndSet(null, request)) {
                connectionRequest.notifyAll();
                return true;
            }
            return false;
        }
    }

    public void abort() {
        synchronized (connectionRequest) {
            running.set(false);
            connectionRequest.notifyAll();
        }
    }
}
