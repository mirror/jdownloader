package org.jdownloader.api.myjdownloader;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings.MyJDownloaderError;
import org.jdownloader.api.myjdownloader.event.MyJDownloaderEvent;
import org.jdownloader.api.myjdownloader.event.MyJDownloaderEventSender;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_MYJD;
import org.jdownloader.translate._JDT;

public class MyJDownloaderController implements ShutdownVetoListener, GenericConfigEventListener<Boolean> {
    private static final MyJDownloaderController INSTANCE = new MyJDownloaderController();

    public static MyJDownloaderController getInstance() {
        return INSTANCE;
    }

    private NullsafeAtomicReference<MyJDownloaderConnectThread> thread = new NullsafeAtomicReference<MyJDownloaderConnectThread>(null);
    private LogSource                                           logger;
    private MyJDownloaderEventSender                            eventSender;

    public MyJDownloaderEventSender getEventSender() {
        return eventSender;
    }

    public boolean isConnected() {
        MyJDownloaderConnectThread lThread = thread.get();
        return lThread != null && lThread.isAlive() && lThread.isConnected();
    }

    public MyJDownloaderConnectionStatus getConnectionStatus() {
        MyJDownloaderConnectThread lThread = thread.get();
        if (lThread != null) {
            return lThread.getConnectionStatus();
        } else {
            return MyJDownloaderConnectionStatus.UNCONNECTED;
        }
    }

    public int getEstablishedConnections() {
        MyJDownloaderConnectThread lThread = thread.get();
        if (lThread != null) {
            return lThread.getEstablishedConnections();
        } else {
            return 0;
        }
    }

    protected void stop() {
        MyJDownloaderConnectThread lThread = thread.getAndSet(null);
        if (lThread != null) {
            lThread.disconnect();
        }
    }

    private MyJDownloaderController() {
        logger = LogController.getInstance().getLogger(MyJDownloaderController.class.getName());
        eventSender = new MyJDownloaderEventSender();
        if (CFG_MYJD.AUTO_CONNECT_ENABLED.isEnabled()) {
            start();
        }
        CFG_MYJD.AUTO_CONNECT_ENABLED.getEventSender().addListener(this);

        GenericConfigEventListener<String> loginsChangedListener = new GenericConfigEventListener<String>() {

            @Override
            public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
            }

            @Override
            public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {

                // stop();
            }
        };
        CFG_MYJD.EMAIL.getEventSender().addListener(loginsChangedListener);
        CFG_MYJD.PASSWORD.getEventSender().addListener(loginsChangedListener);
    }

    protected void start() {
        stop();
        String email;
        String password;
        if (!validateLogins(email = CFG_MYJD.CFG.getEmail(), password = CFG_MYJD.CFG.getPassword())) return;

        MyJDownloaderConnectThread lthread = new MyJDownloaderConnectThread(this);

        lthread.setEmail(email);
        lthread.setPassword(password);
        lthread.setDeviceName(CFG_MYJD.CFG.getDeviceName());
        if (thread.compareAndSet(null, lthread)) {
            lthread.start();
        }
        ShutdownController.getInstance().addShutdownVetoListener(this);
    }

    public String getCurrentDeviceName() {
        if (!isConnected()) return null;
        MyJDownloaderConnectThread th = thread.get();
        if (th == null) return null;
        return th.getDeviceName();
    }

    public String getCurrentEmail() {
        if (!isConnected()) return null;
        MyJDownloaderConnectThread th = thread.get();
        if (th == null) return null;
        return th.getEmail();
    }

    public String getCurrentPassword() {
        if (!isConnected()) return null;
        MyJDownloaderConnectThread th = thread.get();
        if (th == null) return null;
        return th.getPassword();
    }

    protected MyJDownloaderConnectThread getConnectThread() {
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

        if (CFG_MYJD.AUTO_CONNECT_ENABLED.isEnabled()) {
            start();
        }
        // if (keyHandler == CFG_MYJD.ENABLED) {
        // if (newValue) {
        // start();
        // } else {
        // stop();
        // }
        // }
    }

    public void onError(MyJDownloaderError error) {
        CFG_MYJD.CFG.setLatestError(error);
        switch (error) {
        case NONE:
            break;
        case ACCOUNT_UNCONFIRMED:
            Dialog.getInstance().showMessageDialog(0, "MyJDownloader", _JDT._.MyJDownloaderController_onError_account_unconfirmed());
            CFG_MYJD.CFG.setAutoConnectEnabled(false);
            stop();
            break;
        case BAD_LOGINS:
            Dialog.getInstance().showMessageDialog(0, "MyJDownloader", _JDT._.MyJDownloaderController_onError_badlogins());
            CFG_MYJD.CFG.setAutoConnectEnabled(false);
            stop();
            break;
        case EMAIL_INVALID:
            Dialog.getInstance().showMessageDialog(0, "MyJDownloader", _JDT._.MyJDownloaderController_onError_badlogins());
            CFG_MYJD.CFG.setAutoConnectEnabled(false);
            stop();
            break;
        case IO:
            break;
        case SERVER_DOWN:
            break;
        case UNKNOWN:
            break;

        default:
            Dialog.getInstance().showMessageDialog(0, "MyJDownloader", _JDT._.MyJDownloaderController_onError_unknown(error.toString()));
        }

    }

    public void fireConnectionStatusChanged(MyJDownloaderConnectionStatus status, int connections) {
        CFG_MYJD.CFG.setLatestError(MyJDownloaderError.NONE);
        eventSender.fireEvent(new MyJDownloaderEvent(this, MyJDownloaderEvent.Type.CONNECTION_STATUS_UPDATE, status, connections));
    }

    public void disconnect() {
        stop();
    }

    public void connect() {
        start();
    }

    public static boolean validateLogins(String email, String password) {

        if (!new Regex(email, "..*?@.*?\\..+").matches()) return false;
        if (StringUtils.isEmpty(password)) return false;
        return true;
    }

    /**
     * Call this method to send a push request
     * 
     * @param captchasPending
     */
    public void pushCaptchaFlag(boolean captchasPending) {
        if (!isConnected()) return;
        MyJDownloaderConnectThread th = thread.get();
        if (th == null) return;
        th.pushCaptchaNotification(captchasPending);
    }

}
