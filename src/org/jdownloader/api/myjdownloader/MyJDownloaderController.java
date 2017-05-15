package org.jdownloader.api.myjdownloader;

import org.appwork.console.AbstractConsole;
import org.appwork.console.ConsoleDialog;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.MyJDownloaderError;
import org.jdownloader.api.myjdownloader.event.MyJDownloaderEvent;
import org.jdownloader.api.myjdownloader.event.MyJDownloaderEventSender;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.exceptions.MyJDownloaderException;
import org.jdownloader.myjdownloader.client.exceptions.UnconnectedException;
import org.jdownloader.myjdownloader.client.json.MyCaptchaChallenge;
import org.jdownloader.myjdownloader.client.json.MyCaptchaSolution;
import org.jdownloader.myjdownloader.client.json.MyCaptchaSolution.RESULT;
import org.jdownloader.settings.staticreferences.CFG_MYJD;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.translate._JDT;

public class MyJDownloaderController implements ShutdownVetoListener, GenericConfigEventListener<Boolean> {
    private static final MyJDownloaderController INSTANCE = new MyJDownloaderController();

    public static MyJDownloaderController getInstance() {
        return INSTANCE;
    }

    private final NullsafeAtomicReference<MyJDownloaderConnectThread> thread = new NullsafeAtomicReference<MyJDownloaderConnectThread>(null);
    private LogSource                                                 logger;
    private MyJDownloaderEventSender                                  eventSender;

    public MyJDownloaderEventSender getEventSender() {
        return eventSender;
    }

    public boolean isConnected() {
        final MyJDownloaderConnectThread lThread = getConnectThread();
        return lThread != null && lThread.isAlive() && lThread.isConnected();
    }

    public boolean isActive() {
        final MyJDownloaderConnectThread lThread = getConnectThread();
        return lThread != null && lThread.isAlive();
    }

    public MyJDownloaderConnectionStatus getConnectionStatus() {
        final MyJDownloaderConnectThread lThread = getConnectThread();
        if (lThread != null) {
            return lThread.getConnectionStatus();
        } else {
            return MyJDownloaderConnectionStatus.UNCONNECTED;
        }
    }

    public int getEstablishedConnections() {
        final MyJDownloaderConnectThread lThread = getConnectThread();
        if (lThread != null) {
            return lThread.getEstablishedConnections();
        } else {
            return 0;
        }
    }

    protected void stop() {
        final MyJDownloaderConnectThread lThread = thread.getAndSet(null);
        if (lThread != null) {
            ShutdownController.getInstance().removeShutdownVetoListener(this);
            new Thread("MyJDownloaderController:Stop:" + lThread) {
                {
                    setDaemon(true);
                }

                public void run() {
                    lThread.disconnect();
                };
            }.start();
        }
    }

    private final boolean isAlwaysConnectEnabled() {
        return Application.isHeadless();
    }

    private MyJDownloaderController() {
        logger = LogController.getInstance().getLogger(MyJDownloaderController.class.getName());
        eventSender = new MyJDownloaderEventSender();
        if (isAlwaysConnectEnabled() || CFG_MYJD.AUTO_CONNECT_ENABLED.isEnabled()) {
            start();
        }
        CFG_MYJD.AUTO_CONNECT_ENABLED.getEventSender().addListener(this);
    }

    protected void start() {
        stop();
        String email = CFG_MYJD.CFG.getEmail();
        String password = CFG_MYJD.CFG.getPassword();
        if (!validateLogins(email, password) && isAlwaysConnectEnabled()) {
            synchronized (AbstractConsole.LOCK) {
                final ConsoleDialog cd = new ConsoleDialog("MyJDownloader Setup");
                cd.start();
                try {
                    cd.printLines(_JDT.T.MyJDownloaderController_onError_badlogins());
                    try {
                        while (true) {
                            cd.waitYesOrNo(0, "Enter Logins", "Exit JDownloader");
                            email = cd.ask("Please Enter your MyJDownloader Email:");
                            password = cd.askHidden("Please Enter your MyJDownloader Password(not visible):");
                            if (validateLogins(email, password)) {
                                CFG_MYJD.EMAIL.setValue(email);
                                CFG_MYJD.PASSWORD.setValue(password);
                                break;
                            } else {
                                cd.println("Invalid Logins");
                            }
                        }
                    } catch (DialogNoAnswerException e) {
                        ShutdownController.getInstance().requestShutdown();
                    }
                } finally {
                    cd.end();
                }
            }
        }
        if (validateLogins(email, password)) {
            MyJDownloaderConnectThread lthread = new MyJDownloaderConnectThread(this);
            lthread.setEmail(email);
            lthread.setPassword(password);
            lthread.setDeviceName(CFG_MYJD.CFG.getDeviceName());
            if (thread.compareAndSet(null, lthread)) {
                ShutdownController.getInstance().addShutdownVetoListener(this);
                lthread.start();
            }
        }
    }

    public String getCurrentDeviceName() {
        if (!isConnected()) {
            return null;
        } else {
            final MyJDownloaderConnectThread th = thread.get();
            if (th == null || !th.isAlive()) {
                return null;
            } else {
                return th.getDeviceName();
            }
        }
    }

    public String getCurrentEmail() {
        if (!isConnected()) {
            return null;
        } else {
            final MyJDownloaderConnectThread th = thread.get();
            if (th == null || !th.isAlive()) {
                return null;
            } else {
                return th.getEmail();
            }
        }
    }

    public String getCurrentPassword() {
        if (!isConnected()) {
            return null;
        } else {
            final MyJDownloaderConnectThread th = thread.get();
            if (th == null || !th.isAlive()) {
                return null;
            } else {
                return th.getPassword();
            }
        }
    }

    public MyJDownloaderConnectThread getConnectThread() {
        return thread.get();
    }

    @Override
    public void onShutdown(ShutdownRequest request) {
        try {
            stop();
        } catch (final Throwable e) {
        }
    }

    @Override
    public void onShutdownVeto(ShutdownRequest request) {
    }

    @Override
    public void onShutdownVetoRequest(ShutdownRequest request) throws ShutdownVetoException {
    }

    @Override
    public long getShutdownVetoPriority() {
        return 0;
    }

    public LogSource getLogger() {
        return logger;
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (isAlwaysConnectEnabled() || CFG_MYJD.AUTO_CONNECT_ENABLED.isEnabled()) {
            start();
        }
    }

    public void onError(MyJDownloaderError error) {
        if (error == null) {
            error = MyJDownloaderError.NONE;
        }
        StatsManager.I().track(1000, "myjd/error/" + error.name());
        CFG_MYJD.CFG.setLatestError(error);
        switch (error) {
        case ACCOUNT_UNCONFIRMED:
            stop();
            if (Application.isHeadless()) {
                synchronized (AbstractConsole.LOCK) {
                    final ConsoleDialog cd = new ConsoleDialog("MyJDownloader");
                    cd.start();
                    try {
                        cd.printLines(_JDT.T.MyJDownloaderController_onError_account_unconfirmed());
                        cd.waitToContinue();
                        ShutdownController.getInstance().requestShutdown();
                    } finally {
                        cd.end();
                    }
                }
            } else {
                UIOManager.I().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, "MyJDownloader", _JDT.T.MyJDownloaderController_onError_account_unconfirmed());
            }
            break;
        case OUTDATED:
            stop();
            if (Application.isHeadless()) {
                synchronized (AbstractConsole.LOCK) {
                    final ConsoleDialog cd = new ConsoleDialog("MyJDownloader");
                    cd.start();
                    try {
                        cd.printLines(_JDT.T.MyJDownloaderController_onError_outdated());
                        cd.waitToContinue();
                        ShutdownController.getInstance().requestShutdown();
                    } finally {
                        cd.end();
                    }
                }
            } else {
                UIOManager.I().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, "MyJDownloader", _JDT.T.MyJDownloaderController_onError_outdated());
            }
            break;
        case EMAIL_INVALID:
        case BAD_LOGINS:
            stop();
            if (Application.isHeadless()) {
                synchronized (AbstractConsole.LOCK) {
                    final ConsoleDialog cd = new ConsoleDialog("MyJDownloader");
                    cd.start();
                    try {
                        cd.printLines(_JDT.T.MyJDownloaderController_onError_badlogins());
                        try {
                            while (true) {
                                cd.waitYesOrNo(0, "Enter Logins", "Exit JDownloader");
                                final String email = cd.ask("Please Enter your MyJDownloader Email:");
                                final String password = cd.askHidden("Please Enter your MyJDownloader Password:");
                                if (validateLogins(email, password)) {
                                    CFG_MYJD.EMAIL.setValue(email);
                                    CFG_MYJD.PASSWORD.setValue(password);
                                    new Thread() {
                                        public void run() {
                                            connect();
                                        }
                                    }.start();
                                    return;
                                } else {
                                    cd.println("Invalid Logins");
                                }
                            }
                        } catch (DialogNoAnswerException e) {
                            ShutdownController.getInstance().requestShutdown();
                        }
                    } finally {
                        cd.end();
                    }
                }
            } else {
                UIOManager.I().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, "MyJDownloader", _JDT.T.MyJDownloaderController_onError_badlogins());
            }
            break;
        default:
            if (false) {
                // debug only
                if (Application.isHeadless()) {
                    synchronized (AbstractConsole.LOCK) {
                        final ConsoleDialog cd = new ConsoleDialog("MyJDownloader");
                        cd.start();
                        try {
                            cd.printLines(_JDT.T.MyJDownloaderController_onError_unknown(error.toString()));
                            cd.waitToContinue();
                            ShutdownController.getInstance().requestShutdown();
                            return;
                        } finally {
                            cd.end();
                        }
                    }
                } else {
                    UIOManager.I().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, "MyJDownloader", _JDT.T.MyJDownloaderController_onError_unknown(error.toString()));
                }
            }
            break;
        }
    }

    public void fireConnectionStatusChanged(MyJDownloaderConnectionStatus status, int connections) {
        switch (status) {
        case CONNECTED:
        case PENDING:
            CFG_MYJD.CFG.setLatestError(MyJDownloaderError.NONE);
            break;
        default:
            break;
        }
        eventSender.fireEvent(new MyJDownloaderEvent(this, MyJDownloaderEvent.Type.CONNECTION_STATUS_UPDATE, status, connections));
    }

    public void disconnect() {
        stop();
    }

    public void connect() {
        start();
    }

    public static boolean validateLogins(String email, String password) {
        if (!new Regex(email, "..*?@.*?\\..+").matches()) {
            return false;
        }
        if (StringUtils.isEmpty(password)) {
            return false;
        }
        return true;
    }

    public boolean isLoginValid() {
        return validateLogins(CFG_MYJD.CFG.getEmail(), CFG_MYJD.CFG.getPassword());
    }

    /**
     * Call this method to send a push request
     *
     * @param captchasPending
     */
    public void pushCaptchaFlag(boolean captchasPending) {
        if (!isConnected()) {
            return;
        }
        final MyJDownloaderConnectThread th = getConnectThread();
        if (th != null) {
            th.pushCaptchaNotification(captchasPending);
        }
    }

    public MyCaptchaSolution pushChallenge(MyCaptchaChallenge ch) throws MyJDownloaderException {
        final MyJDownloaderConnectThread th = getConnectThread();
        if (th == null) {
            throw new UnconnectedException();
        }
        if (th.isAlive()) {
            switch (th.getConnectionStatus()) {
            case CONNECTED:
            case PENDING:
                return th.pushChallenge(ch);
            }
        }
        return null;
    }

    public boolean isRemoteCaptchaServiceEnabled() {
        final MyJDownloaderConnectThread th = getConnectThread();
        if (th != null) {
            return th.isChallengeExchangeEnabled();
        }
        return false;
    }

    public MyCaptchaSolution getChallengeResponse(String id) throws MyJDownloaderException {
        final MyJDownloaderConnectThread th = getConnectThread();
        if (th == null) {
            throw new UnconnectedException();
        }
        if (th.isAlive()) {
            switch (th.getConnectionStatus()) {
            case CONNECTED:
            case PENDING:
                return th.getChallengeResponse(id);
            }
        }
        return null;
    }

    public boolean sendChallengeFeedback(String id, RESULT correct) throws MyJDownloaderException {
        final MyJDownloaderConnectThread th = getConnectThread();
        if (th == null) {
            throw new UnconnectedException();
        }
        if (th.isAlive()) {
            switch (th.getConnectionStatus()) {
            case CONNECTED:
            case PENDING:
                return th.sendChallengeFeedback(id, correct);
            }
        }
        return false;
    }

    public void terminateSession(String connectToken) throws MyJDownloaderException {
        final MyJDownloaderConnectThread ct = getConnectThread();
        if (ct != null) {
            ct.killSession(connectToken);
        }
    }
}
