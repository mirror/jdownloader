package jd.controlling.captcha;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.util.logging.Logger;

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
            throw new SkipException(SkipRequest.SINGLE);
            // switch (dialogType) {
            // case CRAWLER:
            //
            // break;
            // case HOSTER:
            // ((PluginForHost) captchaChallenge.getPlugin()).getDownloadLink().setSkipped(true);
            //
            // HelpDialog.show(false, true, MouseInfo.getPointerInfo().getLocation(), "SKIPPED", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN,
            // _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(),
            // NewTheme.I().getIcon("skipped", 32));
            // break;
            // }
        } catch (HideCaptchasByHostException e) {
            // if (captchaChallenge.getIoPermission() != null) {
            // captchaChallenge.getIoPermission().setCaptchaAllowed(getHost().getTld(), CAPTCHA.BLOCKHOSTER);
            // }
            throw new SkipException(SkipRequest.BLOCK_HOSTER);
            // switch (dialogType) {
            // case CRAWLER:
            //
            // break;
            // case HOSTER:
            // DownloadController.getInstance().set(new DownloadLinkWalker() {
            //
            // @Override
            // public boolean accept(DownloadLink link) {
            // boolean ret = link.getHost().equals(getHost().getTld());
            //
            // ret &= ((PluginForHost) link.getDefaultPlugin()).hasCaptcha(link, null);
            // return ret;
            // }
            //
            // @Override
            // public boolean accept(FilePackage fp) {
            // return true;
            // }
            //
            // @Override
            // public void handle(DownloadLink link) {
            // link.setSkipped(true);
            // }
            //
            // });
            // HelpDialog.show(false, true, MouseInfo.getPointerInfo().getLocation(), "SKIPPED", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN,
            // _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(),
            // NewTheme.I().getIcon("skipped", 32));
            //
            // break;
            //
            // }

        } catch (HideCaptchasByPackageException e) {

            throw new SkipException(SkipRequest.BLOCK_PACKAGE);
            // switch (dialogType) {
            // case CRAWLER:
            //
            // break;
            // case HOSTER:
            // DownloadController.getInstance().set(new DownloadLinkWalker() {
            //
            // @Override
            // public boolean accept(DownloadLink link) {
            // boolean ret = link.getFilePackage() == ((PluginForHost) captchaChallenge.getPlugin()).getDownloadLink().getFilePackage();
            // ret &= ((PluginForHost) link.getDefaultPlugin()).hasCaptcha(link, null);
            // return ret;
            // }
            //
            // @Override
            // public boolean accept(FilePackage fp) {
            // return true;
            // }
            //
            // @Override
            // public void handle(DownloadLink link) {
            // link.setSkipped(true);
            // }
            //
            // });
            //
            // HelpDialog.show(false, true, MouseInfo.getPointerInfo().getLocation(), "SKIPPED", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN,
            // _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(),
            // NewTheme.I().getIcon("skipped", 32));
            //
            // break;
            // }

        } catch (StopDownloadsException e) {
            switch (dialogType) {
            case CRAWLER:

                break;
            case HOSTER:

                break;
            }

            DownloadWatchDog.getInstance().stopDownloads();
        } catch (HideAllCaptchasException e) {
            throw new SkipException(SkipRequest.BLOCK_ALL_CAPTCHAS);
            // switch (dialogType) {
            // case CRAWLER:
            //
            // break;
            // case HOSTER:
            // DownloadController.getInstance().set(new DownloadLinkWalker() {
            //
            // @Override
            // public boolean accept(DownloadLink link) {
            //
            // boolean ret = ((PluginForHost) link.getDefaultPlugin()).hasCaptcha(link, null);
            // return ret;
            // }
            //
            // @Override
            // public boolean accept(FilePackage fp) {
            // return true;
            // }
            //
            // @Override
            // public void handle(DownloadLink link) {
            // link.setSkipped(true);
            // }
            //
            // });
            // HelpDialog.show(false, true, MouseInfo.getPointerInfo().getLocation(), "SKIPPED", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN,
            // _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(),
            // NewTheme.I().getIcon("skipped", 32));
            //
            // break;
            // }

        } catch (MalformedURLException e) {
            throw new WTFException();
        } catch (RuntimeException e) {
            LogSource.exception(getLogger(), e);
        } catch (RefreshException e) {
            // just continue. plugin will retry
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
     * @throws RefreshException
     */
    abstract protected void showDialog(DialogType dialogType, int flag, Image[] images) throws DialogClosedException, DialogCanceledException, HideCaptchasByHostException, HideCaptchasByPackageException, StopDownloadsException, HideAllCaptchasException, RefreshException;

    /**
     * @return the iD
     */
    public UniqueAlltimeID getID() {
        return id;
    }
}
