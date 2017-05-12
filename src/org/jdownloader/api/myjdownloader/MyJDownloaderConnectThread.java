package org.jdownloader.api.myjdownloader;

import java.io.File;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.controlling.reconnect.ipcheck.IPCheckException;
import jd.controlling.reconnect.ipcheck.OfflineException;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Hash;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.StringUtils;
import org.appwork.utils.awfc.AWFCUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.ProxyEndpointConnectException;
import org.appwork.utils.net.httpconnection.SocketStreamInterface;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.DIRECTMODE;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.MyJDownloaderError;
import org.jdownloader.api.myjdownloader.MyJDownloaderWaitingConnectionThread.MyJDownloaderConnectionRequest;
import org.jdownloader.api.myjdownloader.MyJDownloaderWaitingConnectionThread.MyJDownloaderConnectionResponse;
import org.jdownloader.api.myjdownloader.api.MyJDownloaderAPI;
import org.jdownloader.myjdownloader.client.MyJDCaptchasClient;
import org.jdownloader.myjdownloader.client.SessionInfo;
import org.jdownloader.myjdownloader.client.exceptions.AuthException;
import org.jdownloader.myjdownloader.client.exceptions.EmailInvalidException;
import org.jdownloader.myjdownloader.client.exceptions.EmailNotValidatedException;
import org.jdownloader.myjdownloader.client.exceptions.MaintenanceException;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderException;
import org.jdownloader.myjdownloader.client.exceptions.OutdatedException;
import org.jdownloader.myjdownloader.client.exceptions.OverloadException;
import org.jdownloader.myjdownloader.client.exceptions.TokenException;
import org.jdownloader.myjdownloader.client.exceptions.UnconnectedException;
import org.jdownloader.myjdownloader.client.json.DeviceConnectionStatus;
import org.jdownloader.myjdownloader.client.json.DeviceData;
import org.jdownloader.myjdownloader.client.json.MyCaptchaChallenge;
import org.jdownloader.myjdownloader.client.json.MyCaptchaSolution;
import org.jdownloader.myjdownloader.client.json.MyCaptchaSolution.RESULT;
import org.jdownloader.myjdownloader.client.json.NotificationRequestMessage;
import org.jdownloader.myjdownloader.client.json.NotificationRequestMessage.TYPE;
import org.jdownloader.settings.staticreferences.CFG_MYJD;
import org.jdownloader.statistics.StatsManager;

public class MyJDownloaderConnectThread extends Thread {
    public static class SessionInfoWrapper extends SessionInfo {
        public static enum STATE {
            VALID,
            INVALID,
            RECONNECT
        }

        private volatile NullsafeAtomicReference<STATE> state = new NullsafeAtomicReference<MyJDownloaderConnectThread.SessionInfoWrapper.STATE>(STATE.RECONNECT);

        public SessionInfoWrapper(byte[] deviceSecret, byte[] serverEncryptionToken, byte[] deviceEncryptionToken, String sessionToken, String regainToken) {
            super(deviceSecret, serverEncryptionToken, deviceEncryptionToken, sessionToken, regainToken);
        }

        public final STATE getState() {
            return state.get();
        }

        public final void setState(STATE set) {
            state.set(set);
        }

        public final boolean compareAndSetState(STATE expect, STATE set) {
            return state.compareAndSet(expect, set);
        }
    }

    protected class DeviceConnectionHelper {
        private final AtomicLong    backoffCounter = new AtomicLong(0);
        private final AtomicBoolean backOff        = new AtomicBoolean(false);
        private final String        host;

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        private final int                  port;
        private volatile InetSocketAddress addr;

        public InetSocketAddress getAddr() {
            return addr;
        }

        private void refresh() {
            addr = new InetSocketAddress(host, port);
        }

        private DeviceConnectionHelper(int port, String host) {
            this.host = host;
            this.port = port;
            refresh();
        }

        public void requestbackoff() {
            backOff.set(true);
        }

        public void requestbackoff(int backOffCounter) {
            backOff.set(true);
            backoffCounter.set(Math.max(1, Math.min(backOffCounter, 5)));
        }

        public void backoff() throws InterruptedException {
            if (api == null) {
                return;
            }
            if (backOff.get()) {
                synchronized (backOff) {
                    if (backOff.get()) {
                        refresh();
                        long currentBackOff = backoffCounter.get();
                        try {
                            long timeout = 120 * 1000l;
                            if (currentBackOff <= 5) {
                                timeout = ((long) Math.pow(3.0d, currentBackOff)) * 1000;
                            }
                            timeout = Math.min(180 * 1000l, timeout);
                            timeout = timeout + new Random().nextInt(10000);
                            log("Error #:" + currentBackOff + " next retry: " + timeout);
                            Thread.sleep(timeout);
                        } finally {
                            backoffCounter.compareAndSet(currentBackOff, currentBackOff + 1);
                            backOff.compareAndSet(true, false);
                        }
                    }
                }
            }
        }

        public boolean backoffrequested() {
            return backOff.get();
        }

        public void reset() {
            synchronized (backOff) {
                backoffCounter.set(0);
                backOff.set(false);
            }
        }
    }

    protected final AtomicLong            THREADCOUNTER = new AtomicLong(0);
    private final MyJDownloaderController myJDownloaderController;
    private volatile MyJDownloaderAPI     api           = null;
    private final LogSource               logger;

    public LogSource getLogger() {
        return logger;
    }

    private final AtomicLong                                             syncMark                  = new AtomicLong(Integer.MIN_VALUE);
    private ScheduledExecutorService                                     THREADQUEUE               = DelayedRunnable.getNewScheduledExecutorService();
    private final DeviceConnectionHelper[]                               deviceConnectionHelper;
    private int                                                          helperIndex               = 0;
    private final NullsafeAtomicReference<MyJDownloaderConnectionStatus> connected                 = new NullsafeAtomicReference<MyJDownloaderConnectionStatus>(MyJDownloaderConnectionStatus.UNCONNECTED);
    private String                                                       password;
    private String                                                       email;
    private String                                                       deviceName;
    private final Set<TYPE>                                              notifyInterests           = new CopyOnWriteArraySet<NotificationRequestMessage.TYPE>();
    private final static HashMap<Thread, SocketStreamInterface>          openConnections           = new HashMap<Thread, SocketStreamInterface>();
    private final static HashSet<String>                                 KILLEDSESSIONS            = new HashSet<String>();
    private final ArrayDeque<MyJDownloaderConnectionResponse>            responses                 = new ArrayDeque<MyJDownloaderWaitingConnectionThread.MyJDownloaderConnectionResponse>();
    private final ArrayList<MyJDownloaderWaitingConnectionThread>        waitingConnections        = new ArrayList<MyJDownloaderWaitingConnectionThread>();
    private final int                                                    minimumWaitingConnections = 1;
    private final int                                                    maximumWaitingConnections = 4;
    private final File                                                   sessionInfoCache          = Application.getTempResource("myjd.session");
    private final MyJDownloaderDirectServer                              directServer;
    private final AtomicBoolean                                          challengeExchangeEnabled  = new AtomicBoolean(false);
    private final boolean                                                debugEnabled;

    public MyJDownloaderDirectServer getDirectServer() {
        return directServer;
    }

    public boolean isSessionTokenKilled(final String sessionToken) {
        synchronized (KILLEDSESSIONS) {
            return KILLEDSESSIONS.contains(sessionToken);
        }
    }

    private final static Object SESSIONLOCK = new Object();

    protected static HashMap<Thread, SocketStreamInterface> getOpenconnections() {
        return openConnections;
    }

    public MyJDownloaderConnectThread(MyJDownloaderController myJDownloaderExtension) {
        setName("MyJDownloaderConnectThread");
        this.setDaemon(true);
        this.myJDownloaderController = myJDownloaderExtension;
        debugEnabled = CFG_MYJD.CFG.isDebugEnabled();
        if (debugEnabled) {
            logger = myJDownloaderExtension.getLogger();
        } else {
            logger = new LogSource("Dummy") {
                @Override
                public synchronized void clear() {
                }

                public synchronized void log(java.util.logging.LogRecord record) {
                };
            };
        }
        api = new MyJDownloaderAPI() {
            @Override
            protected SessionInfo createSessionInfo(byte[] deviceSecret, byte[] serverEncryptionToken, byte[] deviceEncryptionToken, String sessionToken, String regainToken) {
                return new SessionInfoWrapper(deviceSecret, serverEncryptionToken, deviceEncryptionToken, sessionToken, regainToken);
            }

            @Override
            public LogSource getLogger() {
                return MyJDownloaderConnectThread.this.getLogger();
            }
        };
        final ArrayList<DeviceConnectionHelper> helper = new ArrayList<DeviceConnectionHelper>();
        for (int port : CFG_MYJD.CFG.getDeviceConnectPorts()) {
            helper.add(new DeviceConnectionHelper(port, CFG_MYJD.CFG.getServerHost()));
        }
        deviceConnectionHelper = helper.toArray(new DeviceConnectionHelper[helper.size()]);
        loadSessionInfo();
        DIRECTMODE mode = CFG_MYJD.CFG.getDirectConnectMode();
        if (mode == null) {
            mode = DIRECTMODE.NONE;
        }
        switch (CFG_MYJD.CFG.getDirectConnectMode()) {
        case LAN:
        case LAN_WAN_MANUAL:
        case LAN_WAN_UPNP:
            directServer = new MyJDownloaderDirectServer(this, mode);
            break;
        default:
            directServer = null;
            break;
        }
    }

    protected MyJDownloaderAPI getApi() {
        return api;
    }

    public boolean putResponse(MyJDownloaderConnectionResponse response) {
        synchronized (waitingConnections) {
            if (waitingConnections.size() == 0) {
                return false;
            }
        }
        synchronized (responses) {
            responses.add(response);
            responses.notify();
        }
        return true;
    }

    protected void log(String message) {
        if (debugEnabled) {
            logger.info(message);
        }
    }

    protected void log(String message, Throwable throwable) {
        if (debugEnabled) {
            logger.info(message);
            logger.log(throwable);
        }
    }

    protected void log(Throwable throwable) {
        if (debugEnabled) {
            logger.log(throwable);
        }
    }

    private synchronized DeviceConnectionHelper getNextDeviceConnectionHelper() {
        final DeviceConnectionHelper ret = deviceConnectionHelper[helperIndex];
        helperIndex = (helperIndex + 1) % deviceConnectionHelper.length;
        if (ret.backoffrequested()) {
            final DeviceConnectionHelper ret2 = deviceConnectionHelper[helperIndex];
            if (!ret2.backoffrequested()) {
                return ret2;
            }
        }
        return ret;
    }

    public boolean isConnected() {
        return connected.get() == MyJDownloaderConnectionStatus.CONNECTED && isAlive();
    }

    private boolean isConnectionOffline(DeviceConnectionHelper connectionHelper) {
        final BalancedWebIPCheck onlineCheck = new BalancedWebIPCheck();
        try {
            onlineCheck.getExternalIP();
        } catch (final OfflineException e2) {
            log("Could not connect! NO Internet!");
            return true;
        } catch (final IPCheckException e2) {
        }
        return false;
    }

    private DeviceConnectionStatus handleResponse(final MyJDownloaderConnectionResponse response, final SessionInfoWrapper currentSession) {
        boolean closeSocket = true;
        DeviceConnectionHelper currentHelper = null;
        try {
            currentHelper = response.getRequest().getConnectionHelper();
            if (response.getThrowable() != null) {
                throw response.getThrowable();
            }
            final DeviceConnectionStatus connectionStatus = response.getStatus();
            final SocketStreamInterface socket = response.getSocketStream();
            if (socket != null && connectionStatus != null) {
                setConnectionStatus(MyJDownloaderConnectionStatus.CONNECTED, MyJDownloaderError.NONE);
                switch (connectionStatus) {
                case UNBOUND:
                    currentHelper.reset();
                    log("Unbound");
                    response.getRequest().getSession().setState(SessionInfoWrapper.STATE.INVALID);
                    return connectionStatus;
                case KEEPALIVE:
                    currentHelper.reset();
                    Thread keepAlivehandler = new Thread("KEEPALIVE_HANDLER") {
                        public void run() {
                            try {
                                socket.getSocket().setSoTimeout(5000);
                                long syncMark = new AWFCUtils(socket.getInputStream()).readLongOptimized();
                                sync(syncMark, currentSession);
                            } catch (final Throwable e) {
                            } finally {
                                log("KeepAlive " + syncMark);
                                try {
                                    socket.close();
                                } catch (final Throwable e) {
                                }
                            }
                        };
                    };
                    keepAlivehandler.setDaemon(true);
                    keepAlivehandler.start();
                    closeSocket = false;
                    return connectionStatus;
                case TOKEN:
                    currentHelper.reset();
                    log("Invalid sessionToken");
                    response.getRequest().getSession().compareAndSetState(SessionInfoWrapper.STATE.VALID, SessionInfoWrapper.STATE.RECONNECT);
                    return connectionStatus;
                case OK:
                    currentHelper.reset();
                    log("valid connection(old Ok)");
                    response.getThread().putRequest(new MyJDownloaderConnectionRequest(currentSession, currentHelper));
                    handleConnection(socket);
                    closeSocket = false;
                    return connectionStatus;
                case OK_SYNC:
                    currentHelper.reset();
                    Thread okHandler = new Thread("KEEPALIVE_HANDLER") {
                        public void run() {
                            boolean closeSocket = true;
                            try {
                                long syncMark = new AWFCUtils(socket.getInputStream()).readLongOptimized();
                                log("valid connection (Ok: " + syncMark + ")");
                                response.getThread().putRequest(new MyJDownloaderConnectionRequest(currentSession, response.getRequest().getConnectionHelper()));
                                handleConnection(socket);
                                sync(syncMark, currentSession);
                                closeSocket = false;
                            } catch (final Throwable e) {
                            } finally {
                                try {
                                    if (closeSocket) {
                                        socket.close();
                                    }
                                } catch (final Throwable e) {
                                }
                            }
                        };
                    };
                    okHandler.setDaemon(true);
                    okHandler.start();
                    closeSocket = false;
                    return connectionStatus;
                case MAINTENANCE:
                    log(connectionStatus.name());
                    currentHelper.requestbackoff(5);
                    setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.SERVER_MAINTENANCE);
                    return connectionStatus;
                case OVERLOAD:
                    log(connectionStatus.name());
                    currentHelper.requestbackoff(2);
                    setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.SERVER_OVERLOAD);
                    return connectionStatus;
                case OUTDATED:
                    currentHelper.reset();
                    log("Outdated session");
                    setConnectionStatus(MyJDownloaderConnectionStatus.UNCONNECTED, MyJDownloaderError.OUTDATED);
                    return connectionStatus;
                }
            }
            log("Something else!?!?! WTF!");
            currentHelper.requestbackoff();
            return null;
        } catch (ProxyEndpointConnectException e) {
            currentHelper.requestbackoff();
            log("Could not connect! Server down?", e);
            setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.SERVER_DOWN);
            return null;
        } catch (ConnectException e) {
            currentHelper.requestbackoff();
            log("Could not connect! Server down?", e);
            if (isConnectionOffline(currentHelper)) {
                setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.NO_INTERNET_CONNECTION);
            } else {
                setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.SERVER_DOWN);
            }
            return null;
        } catch (SocketTimeoutException e) {
            currentHelper.requestbackoff();
            log("ReadTimeout on server connect!", e);
            if (isConnectionOffline(currentHelper)) {
                setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.NO_INTERNET_CONNECTION);
            } else {
                setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.IO);
            }
            return null;
        } catch (Throwable e) {
            currentHelper.requestbackoff();
            log(e);
            if (isConnectionOffline(currentHelper)) {
                setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.NO_INTERNET_CONNECTION);
            } else {
                setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.UNKNOWN);
            }
            return null;
        } finally {
            if (closeSocket) {
                try {
                    final SocketStreamInterface socket = response.getSocketStream();
                    if (socket != null) {
                        socket.close();
                    }
                } catch (final Throwable ignore) {
                }
            }
        }
    }

    private void setConnectionStatus(MyJDownloaderConnectionStatus status, MyJDownloaderError error) {
        if (status != null && connected.getAndSet(status) != status) {
            System.out.println(" set  " + status);
            StatsManager.I().track(1000, "myjd/connection/" + status);
            myJDownloaderController.fireConnectionStatusChanged(status, getEstablishedConnections());
        }
        if (error != null) {
            myJDownloaderController.onError(error);
        }
    }

    private MyJDownloaderConnectionResponse pollResponse(boolean wait) throws InterruptedException {
        MyJDownloaderConnectionResponse response = null;
        synchronized (responses) {
            if ((response = responses.poll()) == null && wait) {
                responses.wait();
                response = responses.poll();
            }
        }
        return response;
    }

    @Override
    public void run() {
        DeviceConnectionHelper currentHelper = null;
        int unknownErrorSafeOff = 10;
        try {
            if (directServer != null) {
                directServer.start();
            }
            while (myJDownloaderController.getConnectThread() == this && api != null) {
                try {
                    try {
                        if (currentHelper == null || currentHelper.backoffrequested()) {
                            currentHelper = getNextDeviceConnectionHelper();
                        }
                        final SessionInfoWrapper currentSession = ensureValidSession(currentHelper);
                        if (connected.get() == MyJDownloaderConnectionStatus.UNCONNECTED) {
                            setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, null);
                        }
                        if (syncMark.get() == Integer.MIN_VALUE) {
                            sync(0, currentSession);
                        }
                        MyJDownloaderConnectionRequest request = null;
                        boolean waitForResponse = false;
                        /* make sure we have at least one alive thread */
                        startWaitingConnections(false);
                        synchronized (waitingConnections) {
                            if (waitingConnections.size() == 0) {
                                log("No WaitingConnection? Maybe disconnected!?");
                                return;
                            }
                            for (MyJDownloaderWaitingConnectionThread waitingThread : waitingConnections) {
                                if (request == null) {
                                    request = new MyJDownloaderConnectionRequest(currentSession, currentHelper);
                                }
                                if (waitingThread.putRequest(request)) {
                                    waitForResponse = true;
                                    request = null;
                                    continue;
                                } else if (waitingThread.isRunning()) {
                                    waitForResponse = true;
                                }
                            }
                        }
                        MyJDownloaderConnectionResponse response = pollResponse(waitForResponse);
                        while (response != null) {
                            DeviceConnectionStatus status = handleResponse(response, currentSession);
                            if (status == null || (!DeviceConnectionStatus.OK.equals(status) && !DeviceConnectionStatus.OK_SYNC.equals(status))) {
                                synchronized (waitingConnections) {
                                    if (waitingConnections.size() == 0) {
                                        log("No WaitingConnection? Maybe disconnected!?");
                                        return;
                                    }
                                    if (waitingConnections.size() > minimumWaitingConnections) {
                                        response.getThread().interrupt();
                                        waitingConnections.remove(response.getThread());
                                    }
                                }
                            } else {
                                startWaitingConnections(true);
                            }
                            response = pollResponse(false);
                        }
                        unknownErrorSafeOff = 10;
                    } catch (final MyJDownloaderException e) {
                        if (e instanceof MaintenanceException) {
                            log("Maintenance!");
                            currentHelper.requestbackoff(5);
                            setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.SERVER_MAINTENANCE);
                        } else if (e instanceof OverloadException) {
                            log("Overload!");
                            currentHelper.requestbackoff(2);
                            setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.SERVER_OVERLOAD);
                        } else if (e instanceof OutdatedException) {
                            log("Outdated version, please update!");
                            setConnectionStatus(MyJDownloaderConnectionStatus.UNCONNECTED, MyJDownloaderError.OUTDATED);
                            return;
                        } else if (e instanceof EmailInvalidException) {
                            log("Invalid email!");
                            setConnectionStatus(MyJDownloaderConnectionStatus.UNCONNECTED, MyJDownloaderError.EMAIL_INVALID);
                            return;
                        } else if (e instanceof EmailNotValidatedException) {
                            log("Account is not confirmed!");
                            setConnectionStatus(MyJDownloaderConnectionStatus.UNCONNECTED, MyJDownloaderError.ACCOUNT_UNCONFIRMED);
                            return;
                        } else if (e instanceof AuthException) {
                            log("Wrong Username/Password!");
                            setConnectionStatus(MyJDownloaderConnectionStatus.UNCONNECTED, MyJDownloaderError.BAD_LOGINS);
                            return;
                        } else if (Exceptions.containsInstanceOf(e, ConnectException.class, SocketTimeoutException.class)) {
                            log("Could not connect! Server down?", e);
                            currentHelper.requestbackoff();
                            if (isConnectionOffline(currentHelper)) {
                                setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.NO_INTERNET_CONNECTION);
                            } else {
                                setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.SERVER_DOWN);
                            }
                        } else {
                            log(e);
                            currentHelper.requestbackoff();
                            if (isConnectionOffline(currentHelper)) {
                                setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.NO_INTERNET_CONNECTION);
                            } else {
                                log(e);
                                if (unknownErrorSafeOff-- == 0) {
                                    setConnectionStatus(MyJDownloaderConnectionStatus.UNCONNECTED, MyJDownloaderError.OUTDATED);
                                    log("Unknown Error, SafetyOff!");
                                    return;
                                }
                                setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.UNKNOWN);
                            }
                        }
                    } catch (final Throwable e) {
                        log(e);
                        setConnectionStatus(MyJDownloaderConnectionStatus.PENDING, MyJDownloaderError.UNKNOWN);
                        if (myJDownloaderController.getConnectThread() != this || api == null) {
                            // external disconnect
                            return;
                        }
                        currentHelper.requestbackoff();
                        if (unknownErrorSafeOff-- == 0) {
                            setConnectionStatus(MyJDownloaderConnectionStatus.UNCONNECTED, MyJDownloaderError.OUTDATED);
                            log("Unknown Error, SafetyOff!");
                            return;
                        }
                    }
                } catch (final Throwable e) {
                    log(e);
                }
            }
        } finally {
            if (directServer != null) {
                directServer.close();
            }
            disconnect();
        }
    }

    protected void setEstablishedConnections(final int connections) {
        myJDownloaderController.fireConnectionStatusChanged(connected.get(), connections);
    }

    private void sync(final long nextSyncMark, final SessionInfoWrapper session) {
        if (this.syncMark.getAndSet(nextSyncMark) == nextSyncMark) {
            return;
        }
        ScheduledExecutorService lTHREADQUEUE = THREADQUEUE;
        if (lTHREADQUEUE != null) {
            lTHREADQUEUE.execute(new Runnable() {
                @Override
                public void run() {
                    boolean failed = true;
                    try {
                        MyJDownloaderAPI lapi = api;
                        if (lapi == null) {
                            return;
                        }
                        if (lapi.getSessionInfo() != session) {
                            return;
                        }
                        if (!SessionInfoWrapper.STATE.VALID.equals(session.getState())) {
                            return;
                        }
                        if (MyJDownloaderConnectThread.this.syncMark.get() != nextSyncMark) {
                            return;
                        }
                        final MyJDCaptchasClient<Type> captchaClient = new MyJDCaptchasClient<Type>(lapi);
                        challengeExchangeEnabled.set(captchaClient.isEnabled());
                        final TYPE[] deviceNotifications = lapi.listrequesteddevicesnotifications();
                        final HashSet<TYPE> notifyTypes;
                        if (deviceNotifications != null) {
                            notifyTypes = new HashSet<TYPE>((Arrays.asList(deviceNotifications)));
                        } else {
                            notifyTypes = new HashSet<TYPE>();
                        }
                        setNotifyTypes(notifyTypes);
                        failed = false;
                    } catch (final TokenException e) {
                        session.compareAndSetState(SessionInfoWrapper.STATE.VALID, SessionInfoWrapper.STATE.RECONNECT);
                    } catch (final UnconnectedException e) {
                    } catch (final Throwable e) {
                        log(e);
                    }
                    if (failed) {
                        MyJDownloaderConnectThread.this.syncMark.compareAndSet(nextSyncMark, Integer.MIN_VALUE);
                    }
                }
            });
        }
    }

    protected void setNotifyTypes(HashSet<TYPE> notifyTypes) {
        notifyInterests.clear();
        if (notifyTypes != null) {
            notifyInterests.addAll(notifyTypes);
        }
    }

    private final HashMap<NotificationRequestMessage.TYPE, AtomicLong> notificationMarks = new HashMap<NotificationRequestMessage.TYPE, AtomicLong>();

    private long getNotificationMark(NotificationRequestMessage.TYPE type) {
        synchronized (notificationMarks) {
            AtomicLong mark = notificationMarks.get(type);
            if (mark == null) {
                mark = new AtomicLong();
                notificationMarks.put(type, mark);
            }
            return mark.incrementAndGet();
        }
    }

    private boolean checkNotificationMark(NotificationRequestMessage.TYPE type, final long notificationMark) {
        synchronized (notificationMarks) {
            final AtomicLong mark = notificationMarks.get(type);
            if (mark != null) {
                return notificationMark == mark.get();
            }
            return false;
        }
    }

    protected void pushNotification(final NotificationRequestMessage.TYPE type, final boolean requested) {
        if (!notifyInterests.contains(type) || api == null) {
            return;
        }
        final long currentMark = getNotificationMark(type);
        ScheduledExecutorService lTHREADQUEUE = THREADQUEUE;
        if (lTHREADQUEUE != null) {
            lTHREADQUEUE.execute(new Runnable() {
                @Override
                public void run() {
                    SessionInfoWrapper session = null;
                    try {
                        MyJDownloaderAPI lapi = api;
                        if (lapi == null) {
                            return;
                        }
                        if (!notifyInterests.contains(type)) {
                            return;
                        }
                        session = (SessionInfoWrapper) lapi.getSessionInfo();
                        if (!SessionInfoWrapper.STATE.VALID.equals(session.getState())) {
                            return;
                        }
                        if (!checkNotificationMark(type, currentMark)) {
                            return;
                        }
                        NotificationRequestMessage message = new NotificationRequestMessage();
                        message.setType(type);
                        message.setRequested(requested);
                        if (!lapi.pushNotification(message)) {
                            /* no devices are interested in captchas */
                            removeInterest(type);
                        }
                    } catch (final TokenException e) {
                        if (session != null) {
                            session.compareAndSetState(SessionInfoWrapper.STATE.VALID, SessionInfoWrapper.STATE.RECONNECT);
                        }
                    } catch (final UnconnectedException e) {
                    } catch (final Throwable e) {
                        log(e);
                    }
                }
            });
        }
    }

    protected void pushCaptchaNotification(final boolean requested) {
        pushNotification(TYPE.CAPTCHA, requested);
    }

    protected void removeInterest(TYPE captcha) {
        notifyInterests.remove(captcha);
    }

    protected void handleConnection(final SocketStreamInterface clientSocket) {
        final long requestNumber = THREADCOUNTER.incrementAndGet();
        Thread connectionThread = new Thread("MyJDownloaderConnection:" + requestNumber) {
            @Override
            public void run() {
                try {
                    System.out.println("Handle a passthrough MyJDownloader connection:" + requestNumber);
                    final MyJDownloaderHttpConnection httpConnection = new MyJDownloaderHttpConnection(clientSocket, api);
                    httpConnection.run();
                } catch (final Throwable e) {
                    log(e);
                } finally {
                    try {
                        clientSocket.close();
                    } catch (final Throwable e) {
                    }
                    synchronized (openConnections) {
                        openConnections.remove(Thread.currentThread());
                    }
                    setEstablishedConnections(getEstablishedConnections());
                }
            }
        };
        synchronized (openConnections) {
            openConnections.put(connectionThread, clientSocket);
        }
        setEstablishedConnections(getEstablishedConnections());
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    private void terminateWaitingConnections() {
        ArrayList<MyJDownloaderWaitingConnectionThread> copy = null;
        synchronized (waitingConnections) {
            copy = new ArrayList<MyJDownloaderWaitingConnectionThread>(waitingConnections);
            waitingConnections.clear();
        }
        for (MyJDownloaderWaitingConnectionThread thread : copy) {
            thread.interrupt();
        }
        synchronized (responses) {
            MyJDownloaderConnectionResponse next = null;
            while ((next = responses.poll()) != null) {
                try {
                    final SocketStreamInterface socket = next.getSocketStream();
                    if (socket != null) {
                        socket.close();
                    }
                } catch (final Throwable e) {
                }
            }
            responses.notifyAll();
        }
    }

    private void disconnectSession(MyJDownloaderAPI api, SessionInfoWrapper session) {
        if (api == null) {
            return;
        }
        try {
            if (session == null) {
                session = (SessionInfoWrapper) api.getSessionInfo();
            }
            session.setState(SessionInfoWrapper.STATE.INVALID);
            if (api.getSessionInfo() != session) {
                return;
            }
        } catch (UnconnectedException e) {
            return;
        }
        try {
            try {
                api.disconnect(false);
            } catch (UnconnectedException e) {
            } catch (TokenException e) {
                api.reconnect();
                api.disconnect(true);
            }
        } catch (MyJDownloaderException e1) {
            log(e1);
        }
    }

    public void disconnect() {
        notifyInterests.clear();
        challengeExchangeEnabled.set(false);
        try {
            final MyJDownloaderAPI lapi = api;
            api = null;
            synchronized (responses) {
                responses.notifyAll();
            }
            terminateWaitingConnections();
            disconnectSession(lapi, null);
            synchronized (openConnections) {
                final Iterator<Entry<Thread, SocketStreamInterface>> it = openConnections.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<Thread, SocketStreamInterface> next = it.next();
                    try {
                        next.getValue().close();
                    } catch (final Throwable e) {
                    }
                }
            }
            setConnectionStatus(MyJDownloaderConnectionStatus.UNCONNECTED, MyJDownloaderError.NONE);
            final ScheduledExecutorService lTHREADQUEUE = THREADQUEUE;
            THREADQUEUE = null;
            if (lTHREADQUEUE != null) {
                lTHREADQUEUE.shutdownNow();
            }
        } finally {
            notifyInterests.clear();
            challengeExchangeEnabled.set(false);
        }
    }

    private void startWaitingConnections(boolean minimumORmaximum) {
        int max = minimumWaitingConnections;
        if (minimumORmaximum) {
            max = maximumWaitingConnections;
        }
        synchronized (waitingConnections) {
            for (int index = waitingConnections.size() - 1; index >= 0; index--) {
                MyJDownloaderWaitingConnectionThread thread = waitingConnections.get(index);
                if (!thread.isRunning()) {
                    waitingConnections.remove(index);
                }
            }
            for (int index = waitingConnections.size(); index < max; index++) {
                MyJDownloaderWaitingConnectionThread thread = new MyJDownloaderWaitingConnectionThread(this);
                waitingConnections.add(thread);
                thread.start();
            }
        }
    }

    private void validateSession(SessionInfoWrapper session) {
        saveSessionInfo(session);
        startWaitingConnections(false);
    }

    private void saveSessionInfo(SessionInfoWrapper session) {
        synchronized (SESSIONLOCK) {
            try {
                if (session == null) {
                    return;
                }
                session.setState(SessionInfoWrapper.STATE.VALID);
                final byte[] key = HexFormatter.hexToByteArray(Hash.getMD5(CFG_MYJD.PASSWORD.getValue()));
                final byte[] json = JSonStorage.getMapper().objectToByteArray(new SessionInfoStorable(session));
                final Runnable run = new Runnable() {
                    @Override
                    public void run() {
                        JSonStorage.saveTo(sessionInfoCache, false, key, json);
                    }
                };
                StorageHandler.enqueueWrite(run, sessionInfoCache.getAbsolutePath(), true);
            } catch (final Throwable e) {
                log(e);
            }
        }
    }

    private void loadSessionInfo() {
        synchronized (SESSIONLOCK) {
            try {
                if (!sessionInfoCache.exists()) {
                    return;
                }
                SessionInfoStorable sessionInfoStorable = JSonStorage.restoreFrom(sessionInfoCache, false, HexFormatter.hexToByteArray(Hash.getMD5(CFG_MYJD.PASSWORD.getValue())), new TypeRef<SessionInfoStorable>() {
                }, null);
                if (sessionInfoStorable == null) {
                    return;
                }
                SessionInfoWrapper sessionInfo = sessionInfoStorable._getSessionInfoWrapper();
                if (sessionInfo == null) {
                    return;
                }
                sessionInfo.setState(SessionInfoWrapper.STATE.RECONNECT);
                api.setSessionInfo(sessionInfo);
            } catch (final Throwable e) {
                log(e);
            }
        }
    }

    protected SessionInfoWrapper ensureValidSession(DeviceConnectionHelper connectionHelper) throws MyJDownloaderException, InterruptedException {
        MyJDownloaderAPI lapi = api;
        if (lapi == null) {
            throw new WTFException("api is null, disconnected?!");
        }
        SessionInfoWrapper session = null;
        try {
            session = (SessionInfoWrapper) lapi.getSessionInfo();
            if (session != null && SessionInfoWrapper.STATE.VALID.equals(session.getState())) {
                return session;
            }
        } catch (UnconnectedException e) {
            /* not connected yet */
        }
        /* fetch new jdToken if needed */
        connectionHelper.backoff();
        try {
            if (session != null && SessionInfoWrapper.STATE.RECONNECT.equals(session.getState())) {
                session = (SessionInfoWrapper) lapi.reconnect();
                /* we need an additional call that will activate the new session */
                lapi.keepalive();
                validateSession(session);
                if (session != null) {
                    return session;
                }
            }
        } catch (UnconnectedException e) {
            /* let's connect first */
        } catch (MyJDownloaderException e) {
            if (session != null) {
                session.setState(SessionInfoWrapper.STATE.INVALID);
            }
            return ensureValidSession(connectionHelper);
        }
        boolean deviceBound = false;
        try {
            session = (SessionInfoWrapper) lapi.connect(getEmail(), getPassword());
            final String uniqueID = getUniqueDeviceID();
            final DeviceData device = lapi.bindDevice(new DeviceData(uniqueID, "jd", getDeviceName()));
            if (StringUtils.isNotEmpty(device.getId())) {
                if (!device.getId().equals(uniqueID)) {
                    setUniqueDeviceID(device.getId());
                }
                validateSession(session);
                CFG_MYJD.DEVICE_NAME.setValue(device.getName());
                deviceBound = true;
            }
            return session;
        } finally {
            if (deviceBound == false) {
                disconnectSession(lapi, session);
            }
        }
    }

    protected String getUniqueDeviceIDSalt() {
        return Hash.getSHA256(CFG_MYJD.CFG._getStorageHandler().getPath().getAbsolutePath() + CrossSystem.getOS().name() + CrossSystem.getARCHFamily().name() + CrossSystem.is64BitArch() + CrossSystem.is64BitOperatingSystem() + System.getProperty("user.name"));
    }

    protected String getUniqueDeviceID() {
        final String uuid = CFG_MYJD.CFG.getUniqueDeviceID();
        if (StringUtils.isNotEmpty(uuid)) {
            CFG_MYJD.CFG.setUniqueDeviceID(null);
            CFG_MYJD.CFG.setUniqueDeviceIDV2(uuid);
            CFG_MYJD.CFG.setUniqueDeviceIDSaltV2(getUniqueDeviceIDSalt());
            /* convert */
            return uuid;
        }
        if (getUniqueDeviceIDSalt().equals(CFG_MYJD.CFG.getUniqueDeviceIDSaltV2())) {
            return CFG_MYJD.CFG.getUniqueDeviceIDV2();
        }
        return null;
    }

    private void setUniqueDeviceID(String uniqueID) {
        CFG_MYJD.CFG.setUniqueDeviceIDSaltV2(getUniqueDeviceIDSalt());
        CFG_MYJD.CFG.setUniqueDeviceIDV2(uniqueID);
        CFG_MYJD.CFG._getStorageHandler().write();
    }

    protected String getDeviceName() {
        final String ret = this.deviceName;
        if (StringUtils.isEmpty(ret)) {
            return CFG_MYJD.DEVICE_NAME.getDefaultValue();
        } else {
            return ret;
        }
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    protected String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    protected String getEmail() {
        return email;
    }

    public MyJDownloaderConnectionStatus getConnectionStatus() {
        return connected.get();
    }

    protected int getEstablishedConnections() {
        synchronized (openConnections) {
            return openConnections.size();
        }
    }

    public boolean isChallengeExchangeEnabled() {
        final MyJDownloaderAPI lapi = api;
        if (lapi != null && challengeExchangeEnabled.get()) {
            try {
                final SessionInfoWrapper session = (SessionInfoWrapper) lapi.getSessionInfo();
                return SessionInfoWrapper.STATE.VALID.equals(session.getState());
            } catch (UnconnectedException e) {
            }
        }
        return false;
    }

    public MyCaptchaSolution pushChallenge(final MyCaptchaChallenge ch) throws MyJDownloaderException {
        MyJDownloaderAPI lapi = api;
        if (lapi == null) {
            return null;
        }
        SessionInfoWrapper session = null;
        try {
            session = (SessionInfoWrapper) lapi.getSessionInfo();
            if (!SessionInfoWrapper.STATE.VALID.equals(session.getState())) {
                return null;
            }
            MyJDCaptchasClient<Type> captchaClient = new MyJDCaptchasClient<Type>(lapi);
            return captchaClient.solve(ch);
        } catch (final TokenException e) {
            if (session != null) {
                session.compareAndSetState(SessionInfoWrapper.STATE.VALID, SessionInfoWrapper.STATE.RECONNECT);
            }
        }
        return null;
    }

    public MyCaptchaSolution getChallengeResponse(String id) throws MyJDownloaderException {
        MyJDownloaderAPI lapi = api;
        if (lapi == null) {
            return null;
        }
        SessionInfoWrapper session = null;
        try {
            session = (SessionInfoWrapper) lapi.getSessionInfo();
            if (!SessionInfoWrapper.STATE.VALID.equals(session.getState())) {
                return null;
            }
            MyJDCaptchasClient<Type> captchaClient = new MyJDCaptchasClient<Type>(lapi);
            return captchaClient.get(id);
        } catch (final TokenException e) {
            if (session != null) {
                session.compareAndSetState(SessionInfoWrapper.STATE.VALID, SessionInfoWrapper.STATE.RECONNECT);
            }
        }
        return null;
    }

    public boolean sendChallengeFeedback(String id, RESULT correct) throws MyJDownloaderException {
        MyJDownloaderAPI lapi = api;
        if (lapi == null) {
            return false;
        }
        SessionInfoWrapper session = null;
        try {
            session = (SessionInfoWrapper) lapi.getSessionInfo();
            if (!SessionInfoWrapper.STATE.VALID.equals(session.getState())) {
                return false;
            }
            MyJDCaptchasClient<Type> captchaClient = new MyJDCaptchasClient<Type>(lapi);
            return captchaClient.remove(id, correct);
        } catch (final TokenException e) {
            if (session != null) {
                session.compareAndSetState(SessionInfoWrapper.STATE.VALID, SessionInfoWrapper.STATE.RECONNECT);
            }
        }
        return false;
    }

    public void killSession(String connectToken) throws MyJDownloaderException {
        final MyJDownloaderAPI api = getApi();
        if (api != null && connectToken != null) {
            api.kill(getEmail(), getPassword(), connectToken);
            synchronized (KILLEDSESSIONS) {
                KILLEDSESSIONS.add(connectToken);
            }
        }
    }
}
