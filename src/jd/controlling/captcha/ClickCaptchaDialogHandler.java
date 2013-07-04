package jd.controlling.captcha;

import java.awt.Image;

import jd.gui.swing.dialog.ClickCaptchaDialog;
import jd.gui.swing.dialog.ClickCaptchaDialogInterface;
import jd.gui.swing.dialog.DialogType;

import org.appwork.uio.UserIODefinition.CloseReason;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;

public class ClickCaptchaDialogHandler extends ChallengeDialogHandler<ClickCaptchaChallenge> {

    private ClickedPoint result;

    public ClickedPoint getPoint() {
        return result;
    }

    public ClickCaptchaDialogHandler(ClickCaptchaChallenge captchaChallenge) {
        super(DomainInfo.getInstance(captchaChallenge.getPlugin().getHost()), captchaChallenge);

    }

    @Override
    protected void showDialog(DialogType dialogType, int flag, Image[] images) throws DialogClosedException, DialogCanceledException, HideCaptchasByHostException, HideCaptchasByPackageException, StopCurrentActionException, HideAllCaptchasException, RefreshException {

        ClickCaptchaDialog d = new ClickCaptchaDialog(flag, dialogType, getHost(), images, captchaChallenge.getExplain());
        d.setPlugin(captchaChallenge.getPlugin());
        d.setTimeout(getTimeoutInMS());
        if (getTimeoutInMS() == captchaChallenge.getTimeout()) {
            // no reason to let the user stop the countdown if the result cannot be used after the countdown anyway
            d.setCountdownPausable(false);
        }
        ClickCaptchaDialogInterface io = d.show();
        d = null;
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

}