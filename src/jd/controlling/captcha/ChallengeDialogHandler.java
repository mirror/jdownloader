package jd.controlling.captcha;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import jd.controlling.IOPermission.CAPTCHA;
import jd.gui.swing.dialog.CaptchaDialog;
import jd.gui.swing.dialog.CaptchaDialogInterface;
import jd.gui.swing.dialog.DialogType;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.storage.StorageException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.ComboBoxDialogInterface;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.gui.userio.NewUIO;
import org.jdownloader.logging.LogController;
import org.jdownloader.translate._JDT;

public abstract class ChallengeDialogHandler<T extends ImageCaptchaChallenge<?>> {
    private CaptchaDialogInterface textDialog;

    private DomainInfo             host;
    protected T                    captchaChallenge;
    private CaptchaSettings        config;
    private final UniqueAlltimeID  id = new UniqueAlltimeID();

    public ChallengeDialogHandler(DomainInfo instance, T captchaChallenge2) {
        this.host = instance;
        this.captchaChallenge = captchaChallenge2;
        config = JsonConfig.create(CaptchaSettings.class);

    }

    public DomainInfo getHost() {
        return host;
    }

    public void run() throws InterruptedException {

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

    private void viaGUI() throws InterruptedException {
        if (captchaChallenge.getIoPermission() != null && !captchaChallenge.getIoPermission().isCaptchaAllowed(getHost().getTld())) { return; }
        try {
            DialogType dialogType = null;
            if (captchaChallenge.getPlugin() instanceof PluginForHost) {
                dialogType = DialogType.HOSTER;
            } else if (captchaChallenge.getPlugin() instanceof PluginForDecrypt) {
                dialogType = DialogType.CRAWLER;
            }
            int f = 0;
            if (config.isCountdownEnabled() && config.getCountdown() > 0) {
                f = f | Dialog.LOGIC_COUNTDOWN;
            }
            Image[] images = CaptchaDialog.getGifImages(captchaChallenge.getImageFile().toURI().toURL());
            if (images == null || images.length == 0) {
                BufferedImage img = IconIO.getImage(captchaChallenge.getImageFile().toURI().toURL());
                if (img != null) images = new Image[] { img };
            }

            if (images == null || images.length == 0 || images[0] == null) {
                getLogger().severe("Could not load CaptchaImage! " + captchaChallenge.getImageFile().getAbsolutePath());
                return;
            }

            // }
            showDialog(dialogType, f, images);

            return;
        } catch (DialogNoAnswerException e) {

            /* no external response available */
            if (e.isCausedByInterrupt()) throw new InterruptedException("Dialog Interrupted");
            if (!e.isCausedByTimeout()) {
                String[] options = new String[] { _JDT._.captchacontroller_cancel_dialog_allorhost_next(), _JDT._.captchacontroller_cancel_dialog_allorhost_cancelhost(getHost().getTld()), _JDT._.captchacontroller_cancel_dialog_allorhost_all() };
                try {
                    int defSelection = CaptchaSettings.CFG.getLastCancelOption();
                    ComboBoxDialog combo = new ComboBoxDialog(Dialog.LOGIC_COUNTDOWN | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _JDT._.captchacontroller_cancel_dialog_allorhost(), _JDT._.captchacontroller_cancel_dialog_allorhost_msg(), options, defSelection, null, null, null, null);

                    switch (NewUIO.I().show(ComboBoxDialogInterface.class, combo).getSelectedIndex()) {
                    case 0:
                        // nothing
                        CaptchaSettings.CFG.setLastCancelOption(0);
                        break;
                    case 1:
                        if (captchaChallenge.getIoPermission() != null) {
                            captchaChallenge.getIoPermission().setCaptchaAllowed(getHost().getTld(), CAPTCHA.BLOCKHOSTER);
                        }
                        CaptchaSettings.CFG.setLastCancelOption(1);
                        break;
                    case 2:
                        if (captchaChallenge.getIoPermission() != null) {
                            captchaChallenge.getIoPermission().setCaptchaAllowed(null, CAPTCHA.BLOCKALL);
                        }
                        CaptchaSettings.CFG.setLastCancelOption(2);
                        break;
                    }

                } catch (DialogNoAnswerException e1) {
                } catch (StorageException e1) {
                    LogSource.exception(getLogger(), e1);
                }
            }

        } catch (Throwable e) {
            LogSource.exception(getLogger(), e);
        }
    }

    /**
     * @param dialogType
     * @param f
     * @param images
     * @throws DialogClosedException
     * @throws DialogCanceledException
     */
    abstract protected void showDialog(DialogType dialogType, int flag, Image[] images) throws DialogClosedException, DialogCanceledException;

    /**
     * @return the iD
     */
    public UniqueAlltimeID getID() {
        return id;
    }
}
