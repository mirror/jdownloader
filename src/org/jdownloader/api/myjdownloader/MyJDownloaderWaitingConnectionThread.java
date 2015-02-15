package org.jdownloader.api.myjdownloader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jd.controlling.proxy.ProxyController;
import jd.http.SocketConnectionFactory;

import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyException;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectThread.DeviceConnectionHelper;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectThread.SessionInfoWrapper;
import org.jdownloader.myjdownloader.client.json.DeviceConnectionStatus;

public class MyJDownloaderWaitingConnectionThread extends Thread {

    protected static class MyJDownloaderConnectionRequest {
        private final InetSocketAddress addr;

        public final InetSocketAddress getAddr() {
            return addr;
        }

        public final SessionInfoWrapper getSession() {
            return session;
        }

        private final SessionInfoWrapper     session;
        private final DeviceConnectionHelper connectionHelper;

        public final DeviceConnectionHelper getConnectionHelper() {
            return connectionHelper;
        }

        protected MyJDownloaderConnectionRequest(SessionInfoWrapper session, DeviceConnectionHelper connectionHelper) {
            this.addr = connectionHelper.getAddr();
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
                    final String url = "http://" + request.getAddr().getHostString() + ":" + request.getAddr().getPort();
                    try {
                        connectThread.log("Connect " + request.getAddr());
                        final List<HTTPProxy> list = ProxyController.getInstance().getProxiesByUrl(url, false, false);
                        if (list != null && list.size() > 0) {
                            proxy = list.get(0);
                            connectionSocket = SocketConnectionFactory.createSocket(proxy);
                            connectionSocket.setReuseAddress(true);
                            connectionSocket.setSoTimeout(180000);
                            connectionSocket.setTcpNoDelay(true);
                            connectionSocket.connect(request.getAddr(), 30000);
                            connectionSocket.getOutputStream().write(("DEVICE" + request.getSession().getSessionToken()).getBytes("ISO-8859-1"));
                            connectionSocket.getOutputStream().flush();
                            int validToken = connectionSocket.getInputStream().read();
                            connectionStatus = DeviceConnectionStatus.parse(validToken);
                        } else {
                            connectThread.log("No connection available!");
                            synchronized (connectionRequest) {
                                connectionRequest.wait(5000);
                            }
                        }
                    } catch (Throwable throwable) {
                        connectThread.log(throwable);
                        e = throwable;
                        if (proxy != null && !proxy.isNone() && throwable instanceof HTTPProxyException) {
                            ProxyController.getInstance().reportHTTPProxyException(proxy, url, (IOException) throwable);
                        }
                        try {
                            if (connectionSocket != null) {
                                connectionSocket.close();
                                connectionSocket = null;
                            }
                        } catch (final Throwable ignore) {
                        }
                    }
                    MyJDownloaderConnectionResponse response = new MyJDownloaderConnectionResponse(this, request, connectionStatus, connectionSocket, e);
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
