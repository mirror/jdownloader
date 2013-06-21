package org.jdownloader.extensions.myjdownloader;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.awfc.AWFCUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.myjdownloader.api.MyJDownloaderAPI;
import org.jdownloader.myjdownloader.client.exceptions.AuthException;
import org.jdownloader.myjdownloader.client.exceptions.EmailInvalidException;
import org.jdownloader.myjdownloader.client.exceptions.EmailNotValidatedException;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderException;
import org.jdownloader.myjdownloader.client.json.DeviceConnectionStatus;
import org.jdownloader.myjdownloader.client.json.DeviceData;
import org.jdownloader.myjdownloader.client.json.NotificationRequestMessage.TYPE;

public class MyJDownloaderConnectThread extends Thread {

    private final AtomicLong                 THREADCOUNTER  = new AtomicLong(0);
    private MyJDownloaderExtension           myJDownloaderExtension;
    private MyDownloaderExtensionConfig      config;
    private Socket                           connectionSocket;
    private MyJDownloaderAPI                 api;
    private LogSource                        logger;
    private int                              backoffCounter = 0;
    private boolean                          sessionValid   = false;
    private AtomicLong                       syncMark       = new AtomicLong(-1);
    public final ScheduledThreadPoolExecutor THREADQUEUE    = new ScheduledThreadPoolExecutor(1);

    public MyJDownloaderConnectThread(MyJDownloaderExtension myJDownloaderExtension) {
        setName("MyJDownloaderConnectThread");
        this.setDaemon(true);
        this.myJDownloaderExtension = myJDownloaderExtension;
        config = myJDownloaderExtension.getSettings();
        api = new MyJDownloaderAPI(myJDownloaderExtension);
        logger = myJDownloaderExtension.getLogger();
        THREADQUEUE.setKeepAliveTime(10000, TimeUnit.MILLISECONDS);
        THREADQUEUE.allowCoreThreadTimeOut(true);
    }

    private void backoff() throws InterruptedException {
        long timeout = 300 * 1000l;
        if (backoffCounter <= 5) {
            timeout = ((long) Math.pow(3.0d, backoffCounter)) * 1000;
        }
        timeout = Math.min(300 * 1000l, timeout);
        timeout = timeout + new Random().nextInt(5000);
        logger.info("Backoff:" + backoffCounter + "->" + timeout);
        backoffCounter++;
        Thread.sleep(timeout);
    }

    @Override
    public void run() {
        try {
            while (myJDownloaderExtension.getConnectThread() == this && api != null) {
                try {
                    boolean closeSocket = true;
                    try {
                        ensureValidSession();
                        connectionSocket = new Socket();
                        connectionSocket.setSoTimeout(120000);
                        connectionSocket.setTcpNoDelay(false);
                        InetSocketAddress ia = new InetSocketAddress(this.config.getAPIServerURL(), this.config.getAPIServerPort());
                        logger.info("Connect " + ia);
                        connectionSocket.connect(ia, 30000);
                        connectionSocket.getOutputStream().write(("DEVICE" + api.getSessionInfo().getSessionToken()).getBytes("ISO-8859-1"));
                        int validToken = connectionSocket.getInputStream().read();
                        DeviceConnectionStatus connectionStatus = DeviceConnectionStatus.parse(validToken);
                        if (connectionStatus != null) {
                            long syncMark = 0;
                            backoffCounter = 0;
                            switch (connectionStatus) {
                            case UNBOUND:
                                logger.info("Unbound");
                                sessionValid = false;
                                continue;
                            case KEEPALIVE:
                                try {
                                    syncMark = new AWFCUtils(connectionSocket.getInputStream()).readLongOptimized();
                                    sync(syncMark);
                                } catch (final IOException e) {
                                }
                                logger.info("KeepAlive " + syncMark);
                                continue;
                            case TOKEN:
                                logger.info("Invalid sessionToken");
                                sessionValid = false;
                                continue;
                            case OK:
                                logger.info("valid connection(old Ok)");
                                closeSocket = false;
                                handleConnection(connectionSocket);
                                continue;
                            case OK_SYNC:
                                syncMark = new AWFCUtils(connectionSocket.getInputStream()).readLongOptimized();
                                logger.info("valid connection (Ok: " + syncMark + ")");
                                closeSocket = false;
                                handleConnection(connectionSocket);
                                sync(syncMark);
                                continue;
                            }
                        }
                        logger.info("Something else!?!?! WTF!" + validToken);
                        backoff();
                    } catch (final MyJDownloaderException e) {
                        if (e instanceof EmailInvalidException) {
                            logger.info("Invalid email!");
                            Dialog.getInstance().showMessageDialog(0, "MyJDownloader", "Invalid email!\r\nMyJDownloader Extension is disabled now.");
                            myJDownloaderExtension.setEnabled(false);
                            return;
                        } else if (e instanceof EmailNotValidatedException) {
                            logger.info("Account is not confirmed!");
                            Dialog.getInstance().showMessageDialog(0, "MyJDownloader", "Account is not confirmed!\r\nMyJDownloader Extension is disabled now.");
                            myJDownloaderExtension.setEnabled(false);
                            return;
                        } else if (e instanceof AuthException) {
                            logger.info("Wrong Username/Password!");
                            Dialog.getInstance().showMessageDialog(0, "MyJDownloader", "Wrong Username/Password!\r\nMyJDownloader Extension is disabled now.");
                            myJDownloaderExtension.setEnabled(false);
                            return;
                        } else if (Exceptions.containsInstanceOf(e, ConnectException.class, SocketTimeoutException.class)) {
                            logger.info("Could not connect! Server down?");
                            backoff();
                        } else {
                            logger.log(e);
                            backoff();
                        }
                    } catch (ConnectException e) {
                        logger.info("Could not connect! Server down?");
                        backoff();
                    } catch (SocketTimeoutException e) {
                        logger.info("ReadTimeout on server connect!");
                        backoff();
                    } catch (final Throwable e) {
                        logger.log(e);
                        backoff();
                    } finally {
                        try {
                            if (closeSocket) connectionSocket.close();
                        } catch (final Throwable e) {
                        }
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
        } finally {
            disconnect();
        }
    }

    private void sync(final long nextSyncMark) {
        final long lastSyncMark = this.syncMark.get();
        if (lastSyncMark != nextSyncMark) {
            THREADQUEUE.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        MyJDownloaderAPI lapi = api;
                        if (MyJDownloaderConnectThread.this.syncMark.get() != lastSyncMark) return;
                        if (lapi != null) {
                            TYPE[] types = lapi.listrequesteddevicesnotifications();
                            int jj = 1;
                            MyJDownloaderConnectThread.this.syncMark.compareAndSet(lastSyncMark, nextSyncMark);
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
            });
        }
    }

    private void handleConnection(final Socket clientSocket) {
        Thread connectionThread = new Thread("MyJDownloaderConnection:" + THREADCOUNTER.incrementAndGet()) {
            @Override
            public void run() {
                try {
                    MyJDownloaderHttpConnection httpConnection = new MyJDownloaderHttpConnection(clientSocket, api);
                    httpConnection.run();
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
        };
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    public void disconnect() {
        MyJDownloaderAPI lapi = api;
        api = null;
        try {
            connectionSocket.close();
        } catch (final Throwable e) {
        }
        try {
            interrupt();
        } catch (final Throwable e) {
        }
        try {
            lapi.disconnect();
        } catch (final Throwable e) {
        }
    }

    protected void ensureValidSession() throws MyJDownloaderException {
        MyJDownloaderAPI lapi = api;
        if (sessionValid && lapi.getSessionInfo() != null) return;
        /* fetch new jdToken if needed */
        if (lapi.getSessionInfo() != null) {
            try {
                lapi.reconnect();
            } catch (MyJDownloaderException e) {
                sessionValid = false;
                lapi.setSessionInfo(null);
                ensureValidSession();
            }
        } else {
            lapi.connect(config.getEmail(), config.getPassword());
            boolean deviceBound = false;
            try {
                DeviceData device = lapi.bindDevice(new DeviceData(config.getUniqueDeviceID(), "jd", config.getDeviceName()));
                if (StringUtils.isNotEmpty(device.getId())) {
                    deviceBound = true;
                    config.setUniqueDeviceID(device.getId());
                }
            } finally {
                if (deviceBound == false) {
                    lapi.disconnect();
                }
            }
        }
        //
        sessionValid = true;
    }

}
