package jd.controlling.captcha;

import java.awt.Image;

import jd.gui.swing.dialog.ClickCaptchaDialog;
import jd.gui.swing.dialog.DialogType;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.OKCancelCloseUserIODefinition.CloseReason;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;

public class ClickCaptchaDialogHandler extends ChallengeDialogHandler<ClickCaptchaChallenge> {

    private ClickCaptchaDialog dialog;
    private ClickedPoint       result;

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
        d.setCountdownTime(CaptchaSettings.CFG.getCountdown());
        dialog = d;
        result = UIOManager.I().show(ClickCaptchaDialog.class, d).getResult();
        try {
            if (d.getCloseReason() != CloseReason.OK) {

                if (d.isHideCaptchasForHost()) throw new HideCaptchasByHostException();
                if (d.isHideCaptchasForPackage()) throw new HideCaptchasByPackageException();
                if (d.isStopDownloads()) throw new StopCurrentActionException();
                if (d.isHideAllCaptchas()) throw new HideAllCaptchasException();
                if (d.isStopCrawling()) throw new StopCurrentActionException();
                if (d.isStopShowingCrawlerCaptchas()) throw new HideAllCaptchasException();
                if (d.isRefresh()) throw new RefreshException();
                d.checkCloseReason();
            }
        } catch (IllegalStateException e) {
            // Captcha has been solved externally

        }

    }

}