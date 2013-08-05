package org.jdownloader.api.myjdownloader;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.api.myjdownloader.MyJDownloaderConnectThread.DeviceConnectionHelper;
import org.jdownloader.myjdownloader.client.json.DeviceConnectionStatus;

public class MyJDownloaderWaitingConnectionThread extends Thread {

    protected static class MyJDownloaderConnectionRequest {
        private final InetSocketAddress addr;

        public InetSocketAddress getAddr() {
            return addr;
        }

        public String getSessionToken() {
            return sessionToken;
        }

        private final String                 sessionToken;
        private final DeviceConnectionHelper connectionHelper;

        public DeviceConnectionHelper getConnectionHelper() {
            return connectionHelper;
        }

        protected MyJDownloaderConnectionRequest(String sessionToken, DeviceConnectionHelper connectionHelper) {
            this.addr = connectionHelper.getAddr();
            this.sessionToken = sessionToken;
            this.connectionHelper = connectionHelper;
        }
    }

    protected static class MyJDownloaderConnectionResponse {

        private final DeviceConnectionHelper connectionHelper;

        public DeviceConnectionHelper getConnectionHelper() {
            return connectionHelper;
        }

        public DeviceConnectionStatus getConnectionStatus() {
            return connectionStatus;
        }

        public Socket getConnectionSocket() {
            return connectionSocket;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        private final DeviceConnectionStatus               connectionStatus;
        private final Socket                               connectionSocket;
        private final Throwable                            throwable;
        private final MyJDownloaderWaitingConnectionThread thread;

        protected MyJDownloaderConnectionResponse(MyJDownloaderWaitingConnectionThread thread, DeviceConnectionHelper connectionHelper, DeviceConnectionStatus connectionStatus, Socket connectionSocket, Throwable e) {
            this.connectionHelper = connectionHelper;
            this.connectionStatus = connectionStatus;
            this.connectionSocket = connectionSocket;
            this.throwable = e;
            this.thread = thread;
        }

        /**
         * @return the thread
         */
        public MyJDownloaderWaitingConnectionThread getThread() {
            return thread;
        }

    }

    protected AtomicBoolean                                           running           = new AtomicBoolean(false);
    protected NullsafeAtomicReference<MyJDownloaderConnectionRequest> connectionRequest = new NullsafeAtomicReference<MyJDownloaderConnectionRequest>();
    private final LogSource                                           logger;
    protected Socket                                                  connectionSocket  = null;
    protected final MyJDownloaderConnectThread                        connectThread;

    public MyJDownloaderWaitingConnectionThread(MyJDownloaderConnectThread connectThread) {
        this.setDaemon(true);
        this.setName("MyJDownloaderWaitingConnectionThread");
        logger = connectThread.getLogger();
        this.connectThread = connectThread;
        running.set(true);
    }

    @Override
    public void run() {
        try {
            while (running.get()) {
                MyJDownloaderConnectionRequest request = null;
                synchronized (connectionRequest) {
                    if (running.get() == false) return;
                    if ((request = connectionRequest.getAndSet(null)) == null) {
                        connectionRequest.wait();
                        if (running.get() == false) return;
                        request = connectionRequest.getAndSet(null);
                    }
                }
                if (request != null) {
                    Throwable e = null;
                    DeviceConnectionStatus connectionStatus = null;
                    request.getConnectionHelper().backoff();
                    try {
                        connectionSocket = new Socket();
                        connectionSocket.setSoTimeout(120000);
                        connectionSocket.setTcpNoDelay(true);
                        logger.info("Connect " + request.getAddr());
                        connectionSocket.connect(request.getAddr(), 30000);
                        connectionSocket.getOutputStream().write(("DEVICE" + request.getSessionToken()).getBytes("ISO-8859-1"));
                        connectionSocket.getOutputStream().flush();
                        int validToken = connectionSocket.getInputStream().read();
                        connectionStatus = DeviceConnectionStatus.parse(validToken);
                    } catch (final Throwable throwable) {
                        e = throwable;
                    }
                    MyJDownloaderConnectionResponse response = new MyJDownloaderConnectionResponse(this, request.getConnectionHelper(), connectionStatus, connectionSocket, e);
                    if (connectThread.putResponse(response) == false) {
                        logger.info("Could not putResponse to connectThread. Close responseSocket");
                        try {
                            connectionSocket.close();
                        } catch (final Throwable ignore) {
                        }
                    }
                }
            }
        } catch (final Throwable e) {
            logger.log(e);
        } finally {
            try {
                connectionSocket.close();
            } catch (final Throwable ignore) {
            }
            running.set(false);
        }
    }

    public boolean putRequest(MyJDownloaderConnectionRequest request) {
        synchronized (connectionRequest) {
            if (running.get() == false) return false;
            if (connectionRequest.compareAndSet(null, request)) {
                connectionRequest.notifyAll();
                return true;
            }
            return false;
        }
    }

    @Override
    public void interrupt() {
        synchronized (connectionRequest) {
            running.set(false);
            connectionRequest.notifyAll();
        }
        try {
            connectionSocket.close();
        } catch (final Throwable ignore) {
        }
        super.interrupt();
    }
}
