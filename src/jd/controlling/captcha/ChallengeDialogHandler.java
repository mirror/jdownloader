package jd.controlling.captcha;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.util.logging.Logger;

import jd.controlling.IOPermission.CAPTCHA;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.dialog.CaptchaDialog;
import jd.gui.swing.dialog.CaptchaDialogInterface;
import jd.gui.swing.dialog.DialogType;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.logging.LogController;

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
                BufferedImage img = IconIO.getImage(captchaChallenge.getImageFile().toURI().toURL(), false);
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

        }

        catch (HideCaptchasByHostException e) {
            if (captchaChallenge.getIoPermission() != null) {
                captchaChallenge.getIoPermission().setCaptchaAllowed(getHost().getTld(), CAPTCHA.BLOCKHOSTER);
            }
        } catch (HideCaptchasByPackageException e) {
            Dialog.getInstance().showExceptionDialog("Error", "Not Implemented Yet", new WTFException("Not Implemented yet"));
        } catch (StopDownloadsException e) {
            if (captchaChallenge.getIoPermission() != null) {
                captchaChallenge.getIoPermission().setCaptchaAllowed(null, CAPTCHA.BLOCKALL);
            }

            DownloadWatchDog.getInstance().stopDownloads();
        } catch (HideAllCaptchasException e) {
            if (captchaChallenge.getIoPermission() != null) {
                captchaChallenge.getIoPermission().setCaptchaAllowed(null, CAPTCHA.BLOCKALL);
            }
        } catch (MalformedURLException e) {
            throw new WTFException();
        } catch (RuntimeException e) {
            LogSource.exception(getLogger(), e);
        }
    }

    /**
     * @param dialogType
     * @param f
     * @param images
     * @throws DialogClosedException
     * @throws DialogCanceledException
     * @throws HideCaptchasByHostException
     * @throws StopDownloadsException
     * @throws HideCaptchasByPackageException
     * @throws HideAllCaptchasException
     */
    abstract protected void showDialog(DialogType dialogType, int flag, Image[] images) throws DialogClosedException, DialogCanceledException, HideCaptchasByHostException, HideCaptchasByPackageException, StopDownloadsException, HideAllCaptchasException;

    /**
     * @return the iD
     */
    public UniqueAlltimeID getID() {
        return id;
    }
}
