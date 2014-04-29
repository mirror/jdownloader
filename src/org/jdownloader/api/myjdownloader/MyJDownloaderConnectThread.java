package org.jdownloader.api.myjdownloader;

import java.io.File;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.Hash;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.StringUtils;
import org.appwork.utils.awfc.AWFCUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogSource;
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
        private AtomicLong              backoffCounter = new AtomicLong(0);

        private AtomicBoolean           backOff        = new AtomicBoolean(false);
        private final InetSocketAddress addr;

        public InetSocketAddress getAddr() {
            return addr;
        }

        private DeviceConnectionHelper(int port, String url) {
            addr = new InetSocketAddress(url, port);
        }

        public void requestbackoff() {
            backOff.set(true);
        }

        public void backoff() throws InterruptedException {
            if (api == null) return;
            if (backOff.get()) {
                synchronized (backOff) {
                    if (backOff.get()) {
                        long currentBackOff = backoffCounter.get();
                        try {
                            long timeout = 300 * 1000l;
                            if (currentBackOff <= 5) {
                                timeout = ((long) Math.pow(3.0d, currentBackOff)) * 1000;
                            }
                            timeout = Math.min(300 * 1000l, timeout);
                            timeout = timeout + new Random().nextInt(5000);
                            logger.info("Backoff:" + currentBackOff + "->" + timeout);
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

    protected final AtomicLong      THREADCOUNTER = new AtomicLong(0);
    private MyJDownloaderController myJDownloaderController;

    private MyJDownloaderAPI        api;
    private final LogSource         logger;

    public LogSource getLogger() {
        return logger;
    }

    private AtomicLong                                             syncMark        = new AtomicLong(-1);
    private ScheduledExecutorService                               THREADQUEUE     = DelayedRunnable.getNewScheduledExecutorService();
    private final DeviceConnectionHelper[]                         deviceConnectionHelper;
    private int                                                    helperIndex     = 0;
    private NullsafeAtomicReference<MyJDownloaderConnectionStatus> connected       = new NullsafeAtomicReference<MyJDownloaderConnectionStatus>(MyJDownloaderConnectionStatus.UNCONNECTED);
    private String                                                 password;
    private String                                                 email;
    private String                                                 deviceName;
    private Set<TYPE>                                              notifyInterests;
    private final static HashMap<Thread, Socket>                   openConnections = new HashMap<Thread, Socket>();

    protected static HashMap<Thread, Socket> getOpenconnections() {
        return openConnections;
    }

    private final ArrayDeque<MyJDownloaderConnectionResponse>     responses                 = new ArrayDeque<MyJDownloaderWaitingConnectionThread.MyJDownloaderConnectionResponse>();
    private final ArrayList<MyJDownloaderWaitingConnectionThread> waitingConnections        = new ArrayList<MyJDownloaderWaitingConnectionThread>();
    private final int                                             minimumWaitingConnections = 1;
    private final int                                             maximumWaitingConnections = 4;
    private final File                                            sessionInfoCache;
    private final MyJDownloaderDirectServer                       directServer;

    public MyJDownloaderDirectServer getDirectServer() {
        return directServer;
    }

    private final static Object SESSIONLOCK = new Object();

    public MyJDownloaderConnectThread(MyJDownloaderController myJDownloaderExtension) {
        setName("MyJDownloaderConnectThread");
        this.setDaemon(true);
        this.myJDownloaderController = myJDownloaderExtension;
        logger = myJDownloaderExtension.getLogger();
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
        ArrayList<DeviceConnectionHelper> helper = new ArrayList<DeviceConnectionHelper>();
        for (int port : CFG_MYJD.CFG.getDeviceConnectPorts()) {
            helper.add(new DeviceConnectionHelper(port, CFG_MYJD.CFG.getConnectIP()));
        }
        deviceConnectionHelper = helper.toArray(new DeviceConnectionHelper[helper.size()]);
        notifyInterests = new CopyOnWriteArraySet<NotificationRequestMessage.TYPE>();
        sessionInfoCache = Application.getTempResource("myjd.session");
        loadSessionInfo();
        DIRECTMODE mode = CFG_MYJD.CFG.getDirectConnectMode();
        if (mode == null) mode = DIRECTMODE.NONE;
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
            if (waitingConnections.size() == 0) return false;
        }
        synchronized (responses) {
            responses.add(response);
            responses.notify();
        }
        return true;
    }

    private DeviceConnectionHelper getNextDeviceConnectionHelper() {
        DeviceConnectionHelper ret = deviceConnectionHelper[helperIndex];
        helperIndex = (helperIndex + 1) % deviceConnectionHelper.length;
        return ret;
    }

    public boolean isConnected() {
        return connected.get() == MyJDownloaderConnectionStatus.CONNECTED;
    }

    private DeviceConnectionStatus handleResponse(final MyJDownloaderConnectionResponse response, final SessionInfoWrapper currentSession) {
        boolean closeSocket = true;
        DeviceConnectionHelper currentHelper = null;
        try {
            currentHelper = response.getRequest().getConnectionHelper();
            if (response.getThrowable() != null) throw response.getThrowable();
            DeviceConnectionStatus connectionStatus = response.getConnectionStatus();
            final Socket socket = response.getConnectionSocket();
            if (connectionStatus != null) {
                setConnected(MyJDownloaderConnectionStatus.CONNECTED);
                switch (connectionStatus) {
                case OUTDATED:
                    currentHelper.reset();
                    logger.info("Outdated session");
                    response.getRequest().getSession().setState(SessionInfoWrapper.STATE.INVALID);
                    return connectionStatus;
                case UNBOUND:
                    currentHelper.reset();
                    logger.info("Unbound");
                    response.getRequest().getSession().setState(SessionInfoWrapper.STATE.INVALID);
                    return connectionStatus;
                case KEEPALIVE:
                    currentHelper.reset();
                    Thread keepAlivehandler = new Thread("KEEPALIVE_HANDLER") {
                        public void run() {
                            try {
                                socket.setSoTimeout(5000);
                                long syncMark = new AWFCUtils(socket.getInputStream()).readLongOptimized();
                                sync(syncMark, currentSession);
                            } catch (final Throwable e) {
                            } finally {
                                logger.info("KeepAlive " + syncMark);
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
                    logger.info("Invalid sessionToken");
                    response.getRequest().getSession().compareAndSetState(SessionInfoWrapper.STATE.VALID, SessionInfoWrapper.STATE.RECONNECT);
                    return connectionStatus;
                case OK:
                    currentHelper.reset();
                    logger.info("valid connection(old Ok)");
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
                                logger.info("valid connection (Ok: " + syncMark + ")");
                                response.getThread().putRequest(new MyJDownloaderConnectionRequest(currentSession, response.getRequest().getConnectionHelper()));
                                handleConnection(socket);
                                sync(syncMark, currentSession);
                                closeSocket = false;
                            } catch (final Throwable e) {
                            } finally {
                                try {
                                    if (closeSocket) socket.close();
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
                case OVERLOAD:
                    logger.info(connectionStatus.name());
                    currentHelper.requestbackoff();
                    return connectionStatus;
                }
            }
            logger.info("Something else!?!?! WTF!");
            currentHelper.requestbackoff();
            return null;
        } catch (ConnectException e) {
            currentHelper.requestbackoff();
            logger.info("Could not connect! Server down?");
            setConnected(MyJDownloaderConnectionStatus.PENDING);
            myJDownloaderController.onError(MyJDownloaderError.SERVER_DOWN);
            return null;
        } catch (SocketTimeoutException e) {
            currentHelper.requestbackoff();
            logger.info("ReadTimeout on server connect!");
            setConnected(MyJDownloaderConnectionStatus.PENDING);
            myJDownloaderController.onError(MyJDownloaderError.IO);
            return null;
        } catch (Throwable e) {
            currentHelper.requestbackoff();
            logger.log(e);
            myJDownloaderController.onError(MyJDownloaderError.UNKNOWN);
            return null;
        } finally {
            if (closeSocket) {
                try {
                    response.getConnectionSocket().close();
                } catch (final Throwable ignore) {
                }
            }
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
            if (directServer != null) directServer.start();
            while (myJDownloaderController.getConnectThread() == this && api != null) {
                try {
                    try {
                        if (currentHelper == null || currentHelper.backoffrequested()) {
                            currentHelper = getNextDeviceConnectionHelper();
                        }
                        SessionInfoWrapper currentSession = ensureValidSession(currentHelper);
                        if (connected.get() == MyJDownloaderConnectionStatus.UNCONNECTED) {
                            setConnected(MyJDownloaderConnectionStatus.PENDING);
                        }
                        MyJDownloaderConnectionRequest request = null;
                        boolean waitForResponse = false;
                        /* make sure we have at least one alive thread */
                        startWaitingConnections(false);
                        synchronized (waitingConnections) {
                            if (waitingConnections.size() == 0) {
                                logger.info("No WaitingConnection? Maybe disconnected!?");
                                return;
                            }
                            for (MyJDownloaderWaitingConnectionThread waitingThread : waitingConnections) {
                                if (request == null) request = new MyJDownloaderConnectionRequest(currentSession, currentHelper);
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
                                        logger.info("No WaitingConnection? Maybe disconnected!?");
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
                    } catch (final MyJDownloaderException e) {
                        setConnected(MyJDownloaderConnectionStatus.PENDING);
                        if (e instanceof MaintenanceException) {
                            logger.info("Maintenance!");
                            currentHelper.requestbackoff();
                        } else if (e instanceof OverloadException) {
                            logger.info("Overload!");
                            currentHelper.requestbackoff();
                        } else if (e instanceof OutdatedException) {
                            logger.info("Outdated Version, Please update!");
                            myJDownloaderController.onError(MyJDownloaderError.OUTDATED);
                            return;
                        } else if (e instanceof EmailInvalidException) {
                            logger.info("Invalid email!");
                            myJDownloaderController.onError(MyJDownloaderError.EMAIL_INVALID);
                            return;
                        } else if (e instanceof EmailNotValidatedException) {
                            logger.info("Account is not confirmed!");
                            myJDownloaderController.onError(MyJDownloaderError.ACCOUNT_UNCONFIRMED);
                            return;
                        } else if (e instanceof AuthException) {
                            logger.info("Wrong Username/Password!");
                            myJDownloaderController.onError(MyJDownloaderError.BAD_LOGINS);
                            return;
                        } else if (Exceptions.containsInstanceOf(e, ConnectException.class, SocketTimeoutException.class)) {
                            logger.info("Could not connect! Server down?");
                            myJDownloaderController.onError(MyJDownloaderError.SERVER_DOWN);
                            currentHelper.requestbackoff();
                        } else {

                            BalancedWebIPCheck onlineCheck = new BalancedWebIPCheck(true);
                            try {
                                onlineCheck.getExternalIP();

                            } catch (final OfflineException e2) {
                                logger.info("Could not connect! NO Internet!");

                                currentHelper.requestbackoff();
                                break;
                            } catch (final IPCheckException e2) {
                            }

                            logger.log(e);
                            currentHelper.requestbackoff();
                            if (unknownErrorSafeOff-- == 0) {
                                myJDownloaderController.onError(MyJDownloaderError.OUTDATED);
                                logger.severe("Unknown Error, SafetyOff!");
                                return;
                            }
                            myJDownloaderController.onError(MyJDownloaderError.UNKNOWN);

                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                        setConnected(MyJDownloaderConnectionStatus.UNCONNECTED);
                        if (myJDownloaderController.getConnectThread() != this || api == null) {
                            // external disconnect
                            return;
                        }
                        myJDownloaderController.onError(MyJDownloaderError.UNKNOWN);
                        currentHelper.requestbackoff();
                        if (unknownErrorSafeOff-- == 0) {
                            myJDownloaderController.onError(MyJDownloaderError.OUTDATED);
                            logger.severe("Unknown Error, SafetyOff!");
                            return;
                        }
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
        } finally {
            if (directServer != null) directServer.close();
            disconnect();
        }
    }

    protected void setConnected(MyJDownloaderConnectionStatus set) {
        if (connected.getAndSet(set) == set) return;
        myJDownloaderController.fireConnectionStatusChanged(set, getEstablishedConnections());
    }

    protected void setEstablishedConnections(int connections) {
        myJDownloaderController.fireConnectionStatusChanged(connected.get(), connections);
    }

    private void sync(final long nextSyncMark, final SessionInfoWrapper session) {
        if (this.syncMark.getAndSet(nextSyncMark) == nextSyncMark) return;
        ScheduledExecutorService lTHREADQUEUE = THREADQUEUE;
        if (lTHREADQUEUE != null) {
            lTHREADQUEUE.execute(new Runnable() {
                @Override
                public void run() {
                    boolean failed = true;
                    try {
                        MyJDownloaderAPI lapi = api;
                        if (lapi == null) return;
                        if (lapi.getSessionInfo() != session) return;
                        if (!SessionInfoWrapper.STATE.VALID.equals(session.getState())) return;
                        if (MyJDownloaderConnectThread.this.syncMark.get() != nextSyncMark) return;
                        TYPE[] types = lapi.listrequesteddevicesnotifications();
                        HashSet<TYPE> notifyTypes = new HashSet<TYPE>();
                        if (types != null) {
                            for (TYPE type : types) {
                                notifyTypes.add(type);
                            }
                        }
                        setNotifyTypes(notifyTypes);
                        failed = false;
                    } catch (final TokenException e) {
                        session.compareAndSetState(SessionInfoWrapper.STATE.VALID, SessionInfoWrapper.STATE.RECONNECT);
                    } catch (final UnconnectedException e) {
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                    if (failed) MyJDownloaderConnectThread.this.syncMark.compareAndSet(nextSyncMark, 0);
                }
            });
        }
    }

    protected void setNotifyTypes(HashSet<TYPE> notifyTypes) {
        notifyInterests.clear();
        if (notifyTypes != null) notifyInterests.addAll(notifyTypes);
    }

    private AtomicLong captchaSendMark = new AtomicLong(0);

    protected void pushCaptchaNotification(final boolean requested) {
        if (!notifyInterests.contains(TYPE.CAPTCHA) || api == null) return;
        final long currentMark = captchaSendMark.incrementAndGet();
        ScheduledExecutorService lTHREADQUEUE = THREADQUEUE;
        if (lTHREADQUEUE != null) {
            lTHREADQUEUE.execute(new Runnable() {
                @Override
                public void run() {
                    SessionInfoWrapper session = null;
                    try {
                        MyJDownloaderAPI lapi = api;
                        if (lapi == null) return;
                        if (!notifyInterests.contains(TYPE.CAPTCHA)) return;
                        session = (SessionInfoWrapper) lapi.getSessionInfo();
                        if (!SessionInfoWrapper.STATE.VALID.equals(session.getState())) return;
                        if (MyJDownloaderConnectThread.this.captchaSendMark.get() != currentMark) return;
                        NotificationRequestMessage message = new NotificationRequestMessage();
                        message.setType(TYPE.CAPTCHA);
                        message.setRequested(requested);
                        if (!lapi.pushNotification(message)) {
                            /* no devices are interested in captchas */
                            removeInterest(TYPE.CAPTCHA);

                        }
                    } catch (final TokenException e) {
                        if (session != null) session.compareAndSetState(SessionInfoWrapper.STATE.VALID, SessionInfoWrapper.STATE.RECONNECT);
                    } catch (final UnconnectedException e) {
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
            });
        }
    }

    protected void removeInterest(TYPE captcha) {
        notifyInterests.remove(captcha);
    }

    protected void handleConnection(final Socket clientSocket) {
        final long requestNumber = THREADCOUNTER.incrementAndGet();
        Thread connectionThread = new Thread("MyJDownloaderConnection:" + requestNumber) {
            @Override
            public void run() {
                try {
                    System.out.println("Handle a passthrough MyJDownloader connection:" + requestNumber);
                    MyJDownloaderHttpConnection httpConnection = new MyJDownloaderHttpConnection(clientSocket, api);
                    httpConnection.run();
                } catch (final Throwable e) {
                    logger.log(e);
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
                    next.getConnectionSocket().close();
                } catch (final Throwable e) {
                }
            }
            responses.notifyAll();
        }
    }

    private void disconnectSession(MyJDownloaderAPI api, SessionInfoWrapper session) {
        if (api == null) return;
        try {
            if (session == null) session = (SessionInfoWrapper) api.getSessionInfo();
            session.setState(SessionInfoWrapper.STATE.INVALID);
            if (api.getSessionInfo() != session) return;
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
            logger.log(e1);
        }
    }

    public void disconnect() {
        MyJDownloaderAPI lapi = api;
        api = null;
        terminateWaitingConnections();
        try {
            interrupt();
        } catch (final Throwable e) {
        }
        disconnectSession(lapi, null);
        synchronized (openConnections) {
            Iterator<Entry<Thread, Socket>> it = openConnections.entrySet().iterator();
            while (it.hasNext()) {
                Entry<Thread, Socket> next = it.next();
                try {
                    next.getValue().close();
                } catch (final Throwable e) {
                }
                try {
                    next.getKey().interrupt();
                } catch (final Throwable e) {
                }
            }
        }
        setConnected(MyJDownloaderConnectionStatus.UNCONNECTED);
        ScheduledExecutorService lTHREADQUEUE = THREADQUEUE;
        THREADQUEUE = null;
        if (lTHREADQUEUE != null) lTHREADQUEUE.shutdownNow();
        notifyInterests.clear();
    }

    private void startWaitingConnections(boolean minimumORmaximum) {
        int max = minimumWaitingConnections;
        if (minimumORmaximum) max = maximumWaitingConnections;
        synchronized (waitingConnections) {
            for (int index = waitingConnections.size() - 1; index >= 0; index--) {
                MyJDownloaderWaitingConnectionThread thread = waitingConnections.get(index);
                if (!thread.isRunning()) waitingConnections.remove(index);
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
                if (session == null) return;
                session.setState(SessionInfoWrapper.STATE.VALID);
                JSonStorage.saveTo(sessionInfoCache, false, HexFormatter.hexToByteArray(Hash.getMD5(CFG_MYJD.PASSWORD.getValue())), JSonStorage.serializeToJson(new SessionInfoStorable(session)));
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
    }

    private void loadSessionInfo() {
        synchronized (SESSIONLOCK) {
            try {
                if (!sessionInfoCache.exists()) return;
                SessionInfoStorable sessionInfoStorable = JSonStorage.restoreFrom(sessionInfoCache, false, HexFormatter.hexToByteArray(Hash.getMD5(CFG_MYJD.PASSWORD.getValue())), new TypeRef<SessionInfoStorable>() {
                }, null);
                if (sessionInfoStorable == null) return;
                SessionInfoWrapper sessionInfo = sessionInfoStorable._getSessionInfoWrapper();
                if (sessionInfo == null) return;
                sessionInfo.setState(SessionInfoWrapper.STATE.RECONNECT);
                api.setSessionInfo(sessionInfo);
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
    }

    protected SessionInfoWrapper ensureValidSession(DeviceConnectionHelper connectionHelper) throws MyJDownloaderException, InterruptedException {
        MyJDownloaderAPI lapi = api;
        if (lapi == null) throw new WTFException("api is null, disconnected?!");
        SessionInfoWrapper session = null;
        try {
            session = (SessionInfoWrapper) lapi.getSessionInfo();
            if (session != null && SessionInfoWrapper.STATE.VALID.equals(session.getState())) return session;
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
                if (session != null) return session;
            }
        } catch (UnconnectedException e) {
            /* let's connect first */
        } catch (MyJDownloaderException e) {
            if (session != null) session.setState(SessionInfoWrapper.STATE.INVALID);
            return ensureValidSession(connectionHelper);
        }
        boolean deviceBound = false;
        try {
            session = (SessionInfoWrapper) lapi.connect(getEmail(), getPassword());
            // Thread.sleep(1000);
            String uniqueID = getUniqueDeviceID();
            DeviceData device = lapi.bindDevice(new DeviceData(uniqueID, "jd", getDeviceName()));
            if (StringUtils.isNotEmpty(device.getId())) {
                if (!device.getId().equals(uniqueID)) {
                    setUniqueDeviceID(device.getId());
                }
                validateSession(session);
                deviceBound = true;
            }
            return session;
        } finally {
            if (deviceBound == false) {
                disconnectSession(lapi, session);
            }
        }
    }

    protected String getUniqueDeviceID() {
        String salt = Hash.getSHA256(CFG_MYJD.CFG._getStorageHandler().getPath().getAbsolutePath());
        if (salt.equals(CFG_MYJD.CFG.getUniqueDeviceIDSalt())) { return CFG_MYJD.CFG.getUniqueDeviceID(); }
        return null;
    }

    private void setUniqueDeviceID(String uniqueID) {
        CFG_MYJD.CFG.setUniqueDeviceIDSalt(Hash.getSHA256(CFG_MYJD.CFG._getStorageHandler().getPath().getAbsolutePath()));
        CFG_MYJD.CFG.setUniqueDeviceID(uniqueID);
        CFG_MYJD.CFG._getStorageHandler().write();
    }

    protected String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        if (StringUtils.isEmpty(deviceName)) deviceName = "JDownloader";
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

    public MyCaptchaSolution pushChallenge(final MyCaptchaChallenge ch) throws MyJDownloaderException {
        MyJDownloaderAPI lapi = api;
        if (lapi == null) return null;

        SessionInfoWrapper session = null;
        try {

            session = (SessionInfoWrapper) lapi.getSessionInfo();
            if (!SessionInfoWrapper.STATE.VALID.equals(session.getState())) return null;

            MyJDCaptchasClient<Type> captchaClient = new MyJDCaptchasClient<Type>(api);
            return captchaClient.solve(ch);

        } catch (final TokenException e) {
            if (session != null) session.compareAndSetState(SessionInfoWrapper.STATE.VALID, SessionInfoWrapper.STATE.RECONNECT);

        }
        return null;
    }

    public MyCaptchaSolution getChallengeResponse(String id) throws MyJDownloaderException {
        MyJDownloaderAPI lapi = api;
        if (lapi == null) return null;

        SessionInfoWrapper session = null;
        try {

            session = (SessionInfoWrapper) lapi.getSessionInfo();
            if (!SessionInfoWrapper.STATE.VALID.equals(session.getState())) return null;

            MyJDCaptchasClient<Type> captchaClient = new MyJDCaptchasClient<Type>(api);

            return captchaClient.get(id);

        } catch (final TokenException e) {
            if (session != null) session.compareAndSetState(SessionInfoWrapper.STATE.VALID, SessionInfoWrapper.STATE.RECONNECT);

        }
        return null;
    }

    public boolean sendChallengeFeedback(String id, RESULT correct) throws MyJDownloaderException {
        MyJDownloaderAPI lapi = api;
        if (lapi == null) return false;

        SessionInfoWrapper session = null;
        try {

            session = (SessionInfoWrapper) lapi.getSessionInfo();
            if (!SessionInfoWrapper.STATE.VALID.equals(session.getState())) return false;

            MyJDCaptchasClient<Type> captchaClient = new MyJDCaptchasClient<Type>(api);

            return captchaClient.remove(id, correct);

        } catch (final TokenException e) {
            if (session != null) session.compareAndSetState(SessionInfoWrapper.STATE.VALID, SessionInfoWrapper.STATE.RECONNECT);

        }
        return false;
    }

}
