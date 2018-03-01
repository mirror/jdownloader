package jd.controlling.captcha;

import javax.swing.SwingUtilities;

import jd.gui.swing.dialog.DialogType;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.UIOManager;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogHandler;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.settings.SilentModeSettings.CaptchaDuringSilentModeAction;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;

public abstract class ChallengeDialogHandler<T extends Challenge<?>> {
    private DomainInfo            host;
    protected T                   captchaChallenge;
    private CaptchaSettings       config;
    private final UniqueAlltimeID id = new UniqueAlltimeID();
    protected DialogHandler       dialogHandler;
    private LogSource             logger;

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

    protected void showDialog(AbstractDialog<?> dialog2) throws DialogClosedException, DialogCanceledException {
        dialogHandler.showDialog(dialog2);
    }

    public DomainInfo getHost() {
        return host;
    }

    public void run() throws InterruptedException, SkipException {
        viaGUI();
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
            if (captchaChallenge.getPlugin() instanceof PluginForHost) {
                dialogType = DialogType.HOSTER;
                if (countdown > 0) {
                    f = f | UIOManager.LOGIC_COUNTDOWN;
                }
            } else if (captchaChallenge.getPlugin() instanceof PluginForDecrypt) {
                dialogType = DialogType.CRAWLER;
                if (countdown > 0) {
                    f = f | UIOManager.LOGIC_COUNTDOWN;
                }
            }
            showDialog(dialogType, f);
            return;
        } catch (DialogNoAnswerException e) {
            /* no external response available */
            if (e.isCausedByInterrupt()) {
                throw new InterruptedException("Dialog Interrupted");
            }
            if (e.isCausedByTimeout()) {
                throw new SkipException(captchaChallenge, SkipRequest.TIMEOUT);
            }
            throw new SkipException(captchaChallenge, SkipRequest.SINGLE);
        } catch (HideCaptchasByHostException e) {
            throw new SkipException(captchaChallenge, SkipRequest.BLOCK_HOSTER);
        } catch (HideCaptchasByPackageException e) {
            throw new SkipException(captchaChallenge, SkipRequest.BLOCK_PACKAGE);
        } catch (StopCurrentActionException e) {
            throw new SkipException(captchaChallenge, SkipRequest.STOP_CURRENT_ACTION);
        } catch (HideAllCaptchasException e) {
            throw new SkipException(captchaChallenge, SkipRequest.BLOCK_ALL_CAPTCHAS);
        } catch (RuntimeException e) {
            LogSource.exception(getLogger(), e);
        } catch (RefreshException e) {
            throw new SkipException(captchaChallenge, SkipRequest.REFRESH);
        }
    }

    protected int getTimeoutInMS() {
        int countdown = -1;
        if (config.isDialogCountdownForDownloadsEnabled()) {
            if (captchaChallenge.getPlugin() instanceof PluginForHost) {
                countdown = config.getCaptchaDialogDefaultCountdown();
            } else if (captchaChallenge.getPlugin() instanceof PluginForDecrypt) {
                countdown = config.getCaptchaDialogDefaultCountdown();
            }
        }
        int pluginTimeout = captchaChallenge.getTimeout();
        if (pluginTimeout > 0) {
            if (countdown <= 0 || pluginTimeout < countdown) {
                countdown = pluginTimeout;
            }
        }
        int pluginCaptchaChallengeTimout = captchaChallenge.getPlugin() == null ? 0 : captchaChallenge.getPlugin().getChallengeTimeout(captchaChallenge);
        if (pluginCaptchaChallengeTimout > 0 && pluginCaptchaChallengeTimout < countdown) {
            countdown = pluginCaptchaChallengeTimout;
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
    abstract protected void showDialog(DialogType dialogType, int flag) throws DialogClosedException, DialogCanceledException, HideCaptchasByHostException, HideCaptchasByPackageException, StopCurrentActionException, HideAllCaptchasException, RefreshException;

    /**
     * @return the iD
     */
    public UniqueAlltimeID getID() {
        return id;
    }
}
