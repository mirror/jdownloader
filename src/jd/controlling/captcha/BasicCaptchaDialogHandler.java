package jd.controlling.captcha;

import java.awt.Image;

import jd.gui.swing.dialog.CaptchaDialog;
import jd.gui.swing.dialog.CaptchaDialogInterface;
import jd.gui.swing.dialog.DialogType;

import org.appwork.uio.UserIODefinition.CloseReason;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;

public class BasicCaptchaDialogHandler extends ChallengeDialogHandler<BasicCaptchaChallenge> {

    private CaptchaDialogInterface dialog;
    private String                 result;
    private String                 suggest;

    public String getCaptchaCode() {
        return result;
    }

    public BasicCaptchaDialogHandler(BasicCaptchaChallenge captchaChallenge) {
        super(DomainInfo.getInstance(captchaChallenge.getPlugin().getHost()), captchaChallenge);

    }

    @Override
    protected void showDialog(DialogType dialogType, int flag, Image[] images) throws DialogClosedException, DialogCanceledException, HideCaptchasByHostException, HideCaptchasByPackageException, StopCurrentActionException, HideAllCaptchasException, RefreshException {
        CaptchaDialog d = new CaptchaDialog(flag, dialogType, getHost(), images, captchaChallenge.getExplain());
        d.setPlugin(captchaChallenge.getPlugin());

        d.setTimeout(getTimeoutInMS());
        if (getTimeoutInMS() == captchaChallenge.getTimeout()) {
            // no reason to let the user stop the countdown if the result cannot be used after the countdown anyway
            d.setCountdownPausable(false);
        }
        dialog = d;
        if (suggest != null) dialog.suggest(suggest);

        CaptchaDialogInterface io = d.show();
        result = io.getResult();
        try {
            if (io.getCloseReason() != CloseReason.OK) {

                if (io.isHideCaptchasForHost()) throw new HideCaptchasByHostException();
                if (io.isHideCaptchasForPackage()) throw new HideCaptchasByPackageException();
                if (io.isStopDownloads()) throw new StopCurrentActionException();
                if (io.isHideAllCaptchas()) throw new HideAllCaptchasException();
                if (io.isStopCrawling()) throw new StopCurrentActionException();
                if (io.isStopShowingCrawlerCaptchas()) throw new HideAllCaptchasException();
                if (io.isRefresh()) throw new RefreshException();
                io.throwCloseExceptions();
                throw new DialogClosedException(Dialog.RETURN_CLOSED);
            }
        } catch (IllegalStateException e) {
            // Captcha has been solved externally

        }

    }

    public void setSuggest(final String value) {
        suggest = value;
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (dialog != null) {
                    dialog.suggest(suggest);
                }
            }
        };

    }

    public String getSuggest() {
        return suggest;
    }

    // public void setResponse(CaptchaResult resp) {
    // externalSet = true;
    // this.resp = resp;
    // new EDTRunner() {
    // @Override
    // protected void runInEDT() {
    // try {
    // if (textDialog != null) textDialog.dispose();
    // } catch (final Throwable e) {
    // LogSource.exception(getLogger(), e);
    // }
    // }
    // };
    // }

}