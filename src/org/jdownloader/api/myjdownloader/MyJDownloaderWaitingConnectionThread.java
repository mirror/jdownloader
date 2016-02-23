package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jd.controlling.proxy.ProxyController;
import jd.http.SocketConnectionFactory;

import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyException;
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

        public final DeviceConnectionStatus getConnectionStatus() {
            return connectionStatus;
        }

        public final Socket getConnectionSocket() {
            return connectionSocket;
        }

        public final Throwable getThrowable() {
            return throwable;
        }

        private final DeviceConnectionStatus               connectionStatus;
        private final Socket                               connectionSocket;
        private final Throwable                            throwable;
        private final MyJDownloaderWaitingConnectionThread thread;
        private final MyJDownloaderConnectionRequest       request;

        public final MyJDownloaderConnectionRequest getRequest() {
            return request;
        }

        protected MyJDownloaderConnectionResponse(MyJDownloaderWaitingConnectionThread thread, MyJDownloaderConnectionRequest request, DeviceConnectionStatus connectionStatus, Socket connectionSocket, Throwable e) {
            this.request = request;
            this.connectionStatus = connectionStatus;
            this.connectionSocket = connectionSocket;
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
                    DeviceConnectionStatus connectionStatus = null;
                    request.getConnectionHelper().backoff();
                    Socket connectionSocket = null;
                    HTTPProxy proxy = null;
                    final InetSocketAddress addr = request.getConnectionHelper().getAddr();
                    final URL url = new URL(null, "socket://" + SocketConnection.getHostName(addr) + ":" + addr.getPort(), ProxyController.SOCKETURLSTREAMHANDLER);
                    try {
                        connectThread.log("Connect " + addr);
                        final List<HTTPProxy> list = ProxyController.getInstance().getProxiesByURL(url, false, false);
                        if (list != null && list.size() > 0) {
                            proxy = list.get(0);
                            connectionSocket = SocketConnectionFactory.createSocket(proxy);
                            connectionSocket.setReuseAddress(true);
                            connectionSocket.setSoTimeout(180000);
                            connectionSocket.setTcpNoDelay(true);
                            connectionSocket.connect(addr, 30000);
                            connectionSocket.getOutputStream().write(("DEVICE" + request.getSession().getSessionToken()).getBytes("ISO-8859-1"));
                            connectionSocket.getOutputStream().flush();
                            int validToken = connectionSocket.getInputStream().read();
                            connectionStatus = DeviceConnectionStatus.parse(validToken);
                        } else {
                            synchronized (connectionRequest) {
                                try {
                                    connectionRequest.wait(5000);
                                } catch (final InterruptedException ignore) {
                                }
                                throw new ConnectException("No available connection for: " + url);
                            }
                        }
                    } catch (Throwable throwable) {
                        try {
                            connectThread.log(throwable);
                            e = throwable;
                            if (proxy != null && !proxy.isNone() && throwable instanceof HTTPProxyException) {
                                ProxyController.getInstance().reportHTTPProxyException(proxy, url, (IOException) throwable);
                            }
                        } finally {
                            try {
                                if (connectionSocket != null) {
                                    connectionSocket.close();
                                    connectionSocket = null;
                                }
                            } catch (final Throwable ignore) {
                            }
                        }
                    }
                    final MyJDownloaderConnectionResponse response = new MyJDownloaderConnectionResponse(this, request, connectionStatus, connectionSocket, e);
                    if (connectThread.putResponse(response) == false) {
                        connectThread.log("putResponse failed, maybe connectThread is closed/interrupted.");
                        try {
                            if (connectionSocket != null) {
                                connectionSocket.close();
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
