package jd.controlling.captcha;

import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;

import javax.swing.SwingUtilities;

import jd.gui.swing.dialog.CaptchaDialog;
import jd.gui.swing.dialog.DialogType;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Exceptions;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogHandler;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.InternDialog;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.AbstractCaptchaDialog;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.settings.SilentModeSettings.CaptchaDuringSilentModeAction;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;

public abstract class ChallengeDialogHandler<T extends Challenge<ResultType>, ResultType> {
    private DomainInfo                          host;
    protected T                                 captchaChallenge;
    private CaptchaSettings                     config;
    private final UniqueAlltimeID               id = new UniqueAlltimeID();
    protected DialogHandler                     dialogHandler;
    private LogSource                           logger;
    protected volatile ResultType               result;
    protected AbstractCaptchaDialog<ResultType> dialog;

    public ChallengeDialogHandler(DomainInfo instance, T captchaChallenge2) {
        this.host = instance;
        this.captchaChallenge = captchaChallenge2;
        config = JsonConfig.create(CaptchaSettings.class);
        logger = JDGui.getInstance().getLogger();

        dialogHandler = new DialogHandler() {
            @Override
            public <T> T showDialog(final AbstractDialog<T> dialog) throws DialogClosedException, DialogCanceledException {
                // synchronized (this) {
                try {
                    dialog.forceDummyInit();
                    boolean silentModeActive = JDGui.getInstance().isSilentModeActive();
                    if (silentModeActive) {
                        // switch (CFG_SILENTMODE.CFG.getOnCaptchaDuringSilentModeAction()) {
                        // case WAIT_IN_BACKGROUND_UNTIL_WINDOW_GETS_FOCUS_OR_TIMEOUT
                        // // Cancel dialog
                        // throw new DialogClosedException(Dialog.RETURN_CLOSED);
                        // }
                        // if this is the edt, we should not block it.. NEVER
                        if (!SwingUtilities.isEventDispatchThread()) {
                            // block dialog calls... the shall appear as soon as isSilentModeActive is false.
                            long countdown = -1;
                            if (dialog.isCountdownFlagEnabled()) {
                                long countdownDif = dialog.getCountdown();
                                countdown = System.currentTimeMillis() + countdownDif;
                            }
                            if (countdown < 0 && CFG_SILENTMODE.CFG.getOnCaptchaDuringSilentModeAction() == CaptchaDuringSilentModeAction.WAIT_IN_BACKGROUND_UNTIL_WINDOW_GETS_FOCUS_OR_TIMEOUT) {
                                countdown = System.currentTimeMillis() + CFG_SILENTMODE.ON_DIALOG_DURING_SILENT_MODE_ACTION_TIMEOUT.getValue();
                            }
                            JDGui.getInstance().flashTaskbar();
                            while (JDGui.getInstance().isSilentModeActive()) {
                                if (countdown > 0) {
                                    Thread.sleep(Math.min(Math.max(1, countdown - System.currentTimeMillis()), 250));
                                    if (System.currentTimeMillis() > countdown) {
                                        dialog.onTimeout();
                                        // clear interrupt
                                        Thread.interrupted();
                                        throw new DialogCanceledException(dialog.getReturnmask());
                                    }
                                } else {
                                    Thread.sleep(250);
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    if (dialog.getReturnmask() == 0) {
                        dialog.setCloseReason(CloseReason.INTERRUPT);
                    }
                    throw new DialogClosedException(Dialog.RETURN_INTERRUPT, e);
                } catch (DialogCanceledException e) {
                    throw e;
                } catch (Exception e) {
                    logger.log(e);
                }
                dialog.resetDummyInit();
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        dialog.displayDialog();
                    }
                }.waitForEDT();
                return null;
                // }
            }
        };
    }

    public ResultType getResult() {
        return result;
    }

    protected abstract AbstractCaptchaDialog<ResultType> createDialog(DialogType dialogType, int flag);

    protected void setResultFrom(final AbstractCaptchaDialog<ResultType> dialog) {
        this.result = dialog.getReturnValue();
    }

    protected Image[] getImages(T challenge) {
        if (challenge instanceof ImageCaptchaChallenge) {
            Image images[] = null;
            try {
                images = CaptchaDialog.getGifImages(((ImageCaptchaChallenge<?>) captchaChallenge).getImageFile().toURI().toURL());
                if (images == null || images.length == 0) {
                    BufferedImage img = IconIO.getImage(((ImageCaptchaChallenge<?>) captchaChallenge).getImageFile().toURI().toURL(), false);
                    if (img != null) {
                        images = new Image[] { img };
                    }
                }
            } catch (MalformedURLException e) {
                throw new WTFException(e);
            }
            if (images == null || images.length == 0 || images[0] == null) {
                getLogger().severe("Could not load CaptchaImage! " + ((ImageCaptchaChallenge<?>) captchaChallenge).getImageFile().getAbsolutePath());
                return null;
            } else {
                return images;
            }
        }
        return null;
    }

    protected void waitForDialog(final AbstractCaptchaDialog<ResultType> dialog) throws DialogClosedException, DialogCanceledException, HideCaptchasByHostException, HideCaptchasByPackageException, StopCurrentActionException, HideAllCaptchasException, RefreshException {
        new EDTHelper<Object>() {
            @Override
            public Object edtRun() {
                dialog.getDialog().addWindowListener(new WindowListener() {
                    @Override
                    public void windowOpened(WindowEvent e) {
                    }

                    @Override
                    public void windowIconified(WindowEvent e) {
                    }

                    @Override
                    public void windowDeiconified(WindowEvent e) {
                    }

                    @Override
                    public void windowDeactivated(WindowEvent e) {
                    }

                    @Override
                    public void windowClosing(WindowEvent e) {
                        synchronized (ChallengeDialogHandler.this) {
                            ChallengeDialogHandler.this.notifyAll();
                        }
                    }

                    @Override
                    public void windowClosed(WindowEvent e) {
                        synchronized (ChallengeDialogHandler.this) {
                            boolean v = dialog.getDialog().isVisible();
                            ChallengeDialogHandler.this.notifyAll();
                        }
                    }

                    @Override
                    public void windowActivated(WindowEvent e) {
                    }
                });
                return null;
            }
        }.waitForEDT();
        try {
            while (dialog.getDialog().isDisplayable()) {
                synchronized (this) {
                    this.wait(1000);
                }
            }
        } catch (InterruptedException e) {
            if (dialog.getReturnmask() == 0) {
                dialog.setCloseReason(CloseReason.INTERRUPT);
            }
            throw new DialogClosedException(Dialog.RETURN_INTERRUPT, e);
        } finally {
            try {
                dialog.dispose();
            } catch (Exception e) {
            }
        }
        setResultFrom(dialog);
        try {
            if (dialog.getCloseReason() != CloseReason.OK) {
                if (dialog.isHideCaptchasForHost()) {
                    throw new HideCaptchasByHostException();
                } else if (dialog.isHideCaptchasForPackage()) {
                    throw new HideCaptchasByPackageException();
                } else if (dialog.isStopDownloads()) {
                    throw new StopCurrentActionException();
                } else if (dialog.isHideAllCaptchas()) {
                    throw new HideAllCaptchasException();
                } else if (dialog.isStopCrawling()) {
                    throw new StopCurrentActionException();
                } else if (dialog.isStopShowingCrawlerCaptchas()) {
                    throw new HideAllCaptchasException();
                } else if (dialog.isRefresh()) {
                    throw new RefreshException();
                } else {
                    dialog.throwCloseExceptions();
                    throw new DialogClosedException(Dialog.RETURN_CLOSED);
                }
            }
        } catch (IllegalStateException e) {
            // Captcha has been solved externally
        }
    }

    protected void showDialog(AbstractCaptchaDialog<ResultType> dialog) throws DialogClosedException, DialogCanceledException {
        this.dialog = dialog;
        dialogHandler.showDialog(dialog);
    }

    public DomainInfo getHost() {
        return host;
    }

    public void run() throws InterruptedException, SkipException {
        viaGUI();
    }

    protected volatile ResultType suggest;

    public void setSuggest(final ResultType value) {
        suggest = value;
    }

    public ResultType getSuggest() {
        return suggest;
    }

    public void requestFocus() {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                final AbstractCaptchaDialog<ResultType> d = dialog;
                if (d != null) {
                    final InternDialog<ResultType> win = d.getDialog();
                    if (win != null) {
                        WindowManager.getInstance().setZState(win, FrameState.TO_FRONT_FOCUSED);
                    }
                }
            }
        };
    }

    protected LogInterface getLogger() {
        LogInterface logger = null;
        final Plugin plg = captchaChallenge.getPlugin();
        if (plg != null) {
            if (plg instanceof PluginForHost) {
                logger = plg.getLogger();
            } else if (plg instanceof PluginForDecrypt) {
                logger = plg.getLogger();
            }
        }
        if (logger == null) {
            logger = org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger();
        }
        return logger;
    }

    private void viaGUI() throws InterruptedException, SkipException {
        DialogType dialogType = DialogType.OTHER;
        try {
            int f = 0;
            int countdown = getTimeoutInMS();
            if (captchaChallenge.isAccountLogin()) {
                // dialogType = DialogType.ACCOUNT; //TODO
                dialogType = DialogType.HOSTER;
            } else if (captchaChallenge.getPlugin() instanceof PluginForHost) {
                dialogType = DialogType.HOSTER;
            } else if (captchaChallenge.getPlugin() instanceof PluginForDecrypt) {
                dialogType = DialogType.CRAWLER;
            }
            if (countdown > 0) {
                f = f | UIOManager.LOGIC_COUNTDOWN;
            }
            showDialog(dialogType, f);
            return;
        } catch (DialogNoAnswerException e) {
            /* no external response available */
            if (e.isCausedByInterrupt()) {
                throw Exceptions.addSuppressed(new InterruptedException("Dialog Interrupted"), e);
            } else if (e.isCausedByTimeout()) {
                throw new SkipException(captchaChallenge, SkipRequest.TIMEOUT, e);
            } else {
                throw new SkipException(captchaChallenge, SkipRequest.SINGLE, e);
            }
        } catch (HideCaptchasByHostException e) {
            throw new SkipException(captchaChallenge, SkipRequest.BLOCK_HOSTER, e);
        } catch (HideCaptchasByPackageException e) {
            throw new SkipException(captchaChallenge, SkipRequest.BLOCK_PACKAGE, e);
        } catch (StopCurrentActionException e) {
            throw new SkipException(captchaChallenge, SkipRequest.STOP_CURRENT_ACTION, e);
        } catch (HideAllCaptchasException e) {
            throw new SkipException(captchaChallenge, SkipRequest.BLOCK_ALL_CAPTCHAS, e);
        } catch (RuntimeException e) {
            LogSource.exception(getLogger(), e);
        } catch (RefreshException e) {
            throw new SkipException(captchaChallenge, SkipRequest.REFRESH, e);
        }
    }

    protected int getTimeoutInMS() {
        int countdown = -1;
        final Plugin plugin = captchaChallenge.getPlugin();
        if (plugin instanceof PluginForHost && config.isDialogCountdownForDownloadsEnabled()) {
            countdown = CFG_CAPTCHA.CAPTCHA_DIALOG_DEFAULT_COUNTDOWN.getValue().intValue();
        } else if (plugin instanceof PluginForDecrypt && config.isDialogCountdownForCrawlerEnabled()) {
            countdown = CFG_CAPTCHA.CAPTCHA_DIALOG_DEFAULT_COUNTDOWN.getValue().intValue();
        }
        final int remainingTimeout = captchaChallenge.getRemainingTimeout();
        if (remainingTimeout > 0) {
            if (countdown <= 0 || remainingTimeout < countdown) {
                countdown = remainingTimeout;
            }
        }
        return countdown;
    }

    /**
     * @param dialogType
     * @param f
     * @param images
     * @throws DialogClosedException
     * @throws DialogCanceledException
     * @throws HideCaptchasByHostException
     * @throws StopCurrentActionException
     * @throws HideCaptchasByPackageException
     * @throws HideAllCaptchasException
     * @throws RefreshException
     */
    protected void showDialog(DialogType dialogType, int flag) throws DialogClosedException, DialogCanceledException, HideCaptchasByHostException, HideCaptchasByPackageException, StopCurrentActionException, HideAllCaptchasException, RefreshException {
        final AbstractCaptchaDialog<ResultType> dialog = createDialog(dialogType, flag);
        if (dialog == null) {
            return;
        }
        // don't put this in the edt
        showDialog(dialog);
        waitForDialog(dialog);
    }

    /**
     * @return the iD
     */
    public UniqueAlltimeID getID() {
        return id;
    }
}
