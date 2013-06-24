package org.jdownloader.extensions.myjdownloader;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.awfc.AWFCUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.captcha.event.ChallengeResponseListener;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.extensions.myjdownloader.api.MyJDownloaderAPI;
import org.jdownloader.myjdownloader.client.exceptions.AuthException;
import org.jdownloader.myjdownloader.client.exceptions.EmailInvalidException;
import org.jdownloader.myjdownloader.client.exceptions.EmailNotValidatedException;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderException;
import org.jdownloader.myjdownloader.client.json.DeviceConnectionStatus;
import org.jdownloader.myjdownloader.client.json.DeviceData;
import org.jdownloader.myjdownloader.client.json.NotificationRequestMessage;
import org.jdownloader.myjdownloader.client.json.NotificationRequestMessage.TYPE;

public class MyJDownloaderConnectThread extends Thread implements ChallengeResponseListener {

    private class DeviceConnectionHelper {
        private int       backoffCounter = 0;
        private final int port;

        private DeviceConnectionHelper(int port) {
            this.port = port;
        }

        public int getPort() {
            return port;
        }

        public void backoff(AtomicBoolean errorNotify) throws InterruptedException {
            if (errorNotify != null) errorNotify.set(true);
            if (api == null) return;
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

        public void reset() {
            backoffCounter = 0;
        }
    }

    private final AtomicLong               THREADCOUNTER   = new AtomicLong(0);
    private MyJDownloaderExtension         myJDownloaderExtension;
    private MyDownloaderExtensionConfig    config;
    private Socket                         connectionSocket;
    private MyJDownloaderAPI               api;
    private LogSource                      logger;
    private boolean                        sessionValid    = false;
    private AtomicLong                     syncMark        = new AtomicLong(-1);
    private AtomicLong                     captchaSendMark = new AtomicLong(0);
    private AtomicBoolean                  captchaInterest = new AtomicBoolean(false);
    public ScheduledThreadPoolExecutor     THREADQUEUE     = new ScheduledThreadPoolExecutor(1);
    private final DeviceConnectionHelper[] deviceConnectionHelper;
    private int                            helperIndex     = 0;

    public MyJDownloaderConnectThread(MyJDownloaderExtension myJDownloaderExtension) {
        setName("MyJDownloaderConnectThread");
        this.setDaemon(true);
        this.myJDownloaderExtension = myJDownloaderExtension;
        config = myJDownloaderExtension.getSettings();
        api = new MyJDownloaderAPI(myJDownloaderExtension);
        logger = myJDownloaderExtension.getLogger();
        THREADQUEUE.setKeepAliveTime(10000, TimeUnit.MILLISECONDS);
        THREADQUEUE.allowCoreThreadTimeOut(true);
        ArrayList<DeviceConnectionHelper> helper = new ArrayList<DeviceConnectionHelper>();
        for (int port : config.getDeviceConnectPorts()) {
            helper.add(new DeviceConnectionHelper(port));
        }
        deviceConnectionHelper = helper.toArray(new DeviceConnectionHelper[helper.size()]);
    }

    private DeviceConnectionHelper getNextDeviceConnectionHelper() {
        DeviceConnectionHelper ret = deviceConnectionHelper[helperIndex];
        helperIndex = (helperIndex + 1) % deviceConnectionHelper.length;
        return ret;
    }

    @Override
    public void run() {
        DeviceConnectionHelper currentHelper = null;
        AtomicBoolean errorNotify = new AtomicBoolean(false);
        try {
            while (myJDownloaderExtension.getConnectThread() == this && api != null) {
                try {
                    boolean closeSocket = true;
                    try {
                        if (currentHelper == null || errorNotify.get()) {
                            errorNotify.set(false);
                            currentHelper = getNextDeviceConnectionHelper();
                        }
                        ensureValidSession();
                        connectionSocket = new Socket();
                        connectionSocket.setSoTimeout(120000);
                        connectionSocket.setTcpNoDelay(false);
                        InetSocketAddress ia = new InetSocketAddress(this.config.getConnectIP(), currentHelper.getPort());
                        logger.info("Connect " + ia);
                        connectionSocket.connect(ia, 30000);
                        connectionSocket.getOutputStream().write(("DEVICE" + api.getSessionInfo().getSessionToken()).getBytes("ISO-8859-1"));
                        int validToken = connectionSocket.getInputStream().read();
                        DeviceConnectionStatus connectionStatus = DeviceConnectionStatus.parse(validToken);
                        if (connectionStatus != null) {
                            long syncMark = 0;
                            currentHelper.reset();
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
                        currentHelper.backoff(errorNotify);
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
                            currentHelper.backoff(errorNotify);
                        } else {
                            logger.log(e);
                            currentHelper.backoff(errorNotify);
                        }
                    } catch (ConnectException e) {
                        logger.info("Could not connect! Server down?");
                        logger.log(e);
                        currentHelper.backoff(errorNotify);
                    } catch (SocketTimeoutException e) {
                        logger.info("ReadTimeout on server connect!");
                        logger.log(e);
                        currentHelper.backoff(errorNotify);
                    } catch (final Throwable e) {
                        logger.log(e);
                        currentHelper.backoff(errorNotify);
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
        if (this.syncMark.getAndSet(nextSyncMark) == nextSyncMark) return;
        ScheduledThreadPoolExecutor lTHREADQUEUE = THREADQUEUE;
        if (lTHREADQUEUE != null) {
            lTHREADQUEUE.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        MyJDownloaderAPI lapi = api;
                        if (lapi == null) return;
                        if (MyJDownloaderConnectThread.this.syncMark.get() != nextSyncMark) return;
                        TYPE[] types = lapi.listrequesteddevicesnotifications();
                        boolean captchaInterest = false;
                        if (types != null) {
                            for (TYPE type : types) {
                                if (TYPE.CAPTCHA.equals(type)) {
                                    captchaInterest = true;
                                    break;
                                }
                            }
                        }
                        captchaInterest(captchaInterest);
                    } catch (final Throwable e) {
                        MyJDownloaderConnectThread.this.syncMark.set(0);
                        logger.log(e);
                    }
                }
            });
        }
    }

    private void pushCaptchaNotification(final boolean requested) {
        if (captchaInterest.get() == false) return;
        final long currentMark = captchaSendMark.incrementAndGet();
        ScheduledThreadPoolExecutor lTHREADQUEUE = THREADQUEUE;
        if (lTHREADQUEUE != null) {
            lTHREADQUEUE.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        MyJDownloaderAPI lapi = api;
                        if (lapi == null) return;
                        if (captchaInterest.get() == false) return;
                        if (MyJDownloaderConnectThread.this.captchaSendMark.get() != currentMark) return;
                        NotificationRequestMessage message = new NotificationRequestMessage();
                        message.setType(TYPE.CAPTCHA);
                        message.setRequested(requested);
                        if (!lapi.pushNotification(message)) {
                            /* no devices are interested in captchas */
                            captchaInterest(false);
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
        ScheduledThreadPoolExecutor lTHREADQUEUE = THREADQUEUE;
        THREADQUEUE = null;
        if (lTHREADQUEUE != null) lTHREADQUEUE.shutdownNow();
        captchaInterest(false);
    }

    /**
     * changes current interest in captcha events
     * 
     * will add/remove listener and set captchaInterest
     * 
     * @param interest
     */
    private void captchaInterest(boolean interest) {
        if (interest && captchaInterest.get() != interest && THREADQUEUE != null) {
            captchaInterest.set(true);
            ChallengeResponseController.getInstance().getEventSender().addListener(this, true);
        } else if (!interest) {
            captchaInterest.set(false);
            ChallengeResponseController.getInstance().getEventSender().removeListener(this);
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
                    if (!device.getId().equals(config.getUniqueDeviceID())) {
                        config.setUniqueDeviceID(device.getId());
                        config.getStorageHandler().write();
                    }
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

    @Override
    public void onNewJobAnswer(SolverJob<?> job, AbstractResponse<?> response) {
    }

    @Override
    public void onJobDone(SolverJob<?> job) {
        /* check if we have other captchas pending */
        pushCaptchaNotification(ChallengeResponseController.getInstance().hasPendingJobs());
    }

    @Override
    public void onNewJob(SolverJob<?> job) {
        /* we have a new captcha available */
        pushCaptchaNotification(true);
    }

    @Override
    public void onJobSolverEnd(ChallengeSolver<?> solver, SolverJob<?> job) {
    }

    @Override
    public void onJobSolverStart(ChallengeSolver<?> solver, SolverJob<?> job) {
    }
}
