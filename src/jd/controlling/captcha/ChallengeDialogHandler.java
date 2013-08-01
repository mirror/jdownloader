package jd.controlling.captcha;

import java.awt.Dialog.ModalExclusionType;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.dialog.AbstractCaptchaDialog;
import jd.gui.swing.dialog.CaptchaDialog;
import jd.gui.swing.dialog.DialogType;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.UIOManager;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogHandler;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.SilentModeSettings.DialogDuringSilentModeAction;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;

public abstract class ChallengeDialogHandler<T extends ImageCaptchaChallenge<?>> {

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

                        if (CFG_SILENTMODE.ON_DIALOG_DURING_SILENT_MODE_ACTION.getValue() == DialogDuringSilentModeAction.CANCEL_DIALOG) {
                            // Cancel dialog
                            throw new DialogClosedException(Dialog.RETURN_CLOSED);
                        }

                        // if this is the edt, we should not block it.. NEVER
                        if (!SwingUtilities.isEventDispatchThread()) {
                            // block dialog calls... the shall appear as soon as isSilentModeActive is false.
                            long countdown = -1;

                            if (dialog.isCountdownFlagEnabled()) {
                                long countdownDif = dialog.getCountdown() * 1000;
                                countdown = System.currentTimeMillis() + countdownDif;
                            }
                            if (countdown < 0 && CFG_SILENTMODE.ON_DIALOG_DURING_SILENT_MODE_ACTION.getValue() == DialogDuringSilentModeAction.WAIT_IN_BACKGROUND_UNTIL_WINDOW_GETS_FOCUS_OR_TIMEOUT) {
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
                                        final int mask = dialog.getReturnmask();
                                        if (BinaryLogic.containsSome(mask, Dialog.RETURN_CLOSED)) { throw new DialogClosedException(mask); }
                                        if (BinaryLogic.containsSome(mask, Dialog.RETURN_CANCEL)) { throw new DialogCanceledException(mask); }
                                        try {
                                            return dialog.getReturnValue();

                                        } catch (Exception e) {
                                            // dialogs have not been initialized. so the getReturnValue might fail.
                                            logger.log(e);
                                            throw new DialogClosedException(Dialog.RETURN_CLOSED | Dialog.RETURN_TIMEOUT);
                                        }
                                    }
                                } else {
                                    Thread.sleep(250);
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    throw new DialogClosedException(Dialog.RETURN_INTERRUPT, e);
                } catch (DialogClosedException e) {
                    throw e;
                } catch (DialogCanceledException e) {
                    throw e;
                } catch (Exception e) {
                    logger.log(e);
                } finally {
                    dialog.resetDummyInit();
                }

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

    protected void showDialog(AbstractCaptchaDialog dialog2) {
        try {
            dialogHandler.showDialog(dialog2);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    public DomainInfo getHost() {
        return host;
    }

    public void run() throws InterruptedException, SkipException {

        viaGUI();

    }

    private Logger getLogger() {
        Logger logger = null;
        if (captchaChallenge.getPlugin() instanceof PluginForHost) {
            logger = captchaChallenge.getPlugin().getLogger();
        } else if (captchaChallenge.getPlugin() instanceof PluginForDecrypt) {
            logger = captchaChallenge.getPlugin().getLogger();
        }
        if (logger == null) logger = LogController.GL;
        return logger;
    }

    private void viaGUI() throws InterruptedException, SkipException {
        DialogType dialogType = null;
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

            Image[] images = CaptchaDialog.getGifImages(captchaChallenge.getImageFile().toURI().toURL());
            if (images == null || images.length == 0) {
                BufferedImage img = IconIO.getImage(captchaChallenge.getImageFile().toURI().toURL(), false);
                if (img != null) images = new Image[] { img };
            }

            if (images == null || images.length == 0 || images[0] == null) {
                getLogger().severe("Could not load CaptchaImage! " + captchaChallenge.getImageFile().getAbsolutePath());
                return;
            }

            // }

            final ModalExclusionType orgEx = new EDTHelper<ModalExclusionType>() {

                @Override
                public ModalExclusionType edtRun() {
                    return JDGui.getInstance().getMainFrame().getModalExclusionType();
                }
            }.getReturnValue();

            try {

                showDialog(dialogType, f, images);
            } finally {

            }

            return;
        } catch (DialogNoAnswerException e) {

            /* no external response available */
            if (e.isCausedByInterrupt()) throw new InterruptedException("Dialog Interrupted");

            if (e.isCausedByTimeout()) { throw new SkipException(SkipRequest.TIMEOUT); }
            throw new SkipException(SkipRequest.SINGLE);

        } catch (HideCaptchasByHostException e) {

            throw new SkipException(SkipRequest.BLOCK_HOSTER);

        } catch (HideCaptchasByPackageException e) {

            throw new SkipException(SkipRequest.BLOCK_PACKAGE);

        } catch (StopCurrentActionException e) {
            switch (dialogType) {
            case CRAWLER:

                break;
            case HOSTER:

                break;
            }

            DownloadWatchDog.getInstance().stopDownloads();

            throw new SkipException(SkipRequest.STOP_CURRENT_ACTION);
        } catch (HideAllCaptchasException e) {
            throw new SkipException(SkipRequest.BLOCK_ALL_CAPTCHAS);

        } catch (MalformedURLException e) {
            throw new WTFException();
        } catch (RuntimeException e) {
            LogSource.exception(getLogger(), e);
        } catch (RefreshException e) {
            throw new SkipException(SkipRequest.REFRESH);
        }
    }

    protected int getTimeoutInMS() {

        int countdown = -1;

        if (captchaChallenge.getPlugin() instanceof PluginForHost) {

            if (config.isDialogCountdownForDownloadsEnabled()) {
                countdown = config.getCaptchaDialogDefaultCountdown();
            }
        } else if (captchaChallenge.getPlugin() instanceof PluginForDecrypt) {

            if (config.isDialogCountdownForCrawlerEnabled()) {
                countdown = config.getCaptchaDialogDefaultCountdown();
            }

        }
        int pluginTimeout = captchaChallenge.getTimeout();
        if (pluginTimeout > 0) {

            if (countdown <= 0 || pluginTimeout < countdown) {
                countdown = pluginTimeout;
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
    abstract protected void showDialog(DialogType dialogType, int flag, Image[] images) throws DialogClosedException, DialogCanceledException, HideCaptchasByHostException, HideCaptchasByPackageException, StopCurrentActionException, HideAllCaptchasException, RefreshException;

    /**
     * @return the iD
     */
    public UniqueAlltimeID getID() {
        return id;
    }
}
