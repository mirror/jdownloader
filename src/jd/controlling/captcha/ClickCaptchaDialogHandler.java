package jd.controlling.captcha;

import java.awt.Image;

import jd.gui.swing.dialog.ClickCaptchaDialog;
import jd.gui.swing.dialog.ClickCaptchaDialogInterface;
import jd.gui.swing.dialog.DialogType;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
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
    protected void showDialog(DialogType dialogType, int flag, Image[] images) throws DialogClosedException, DialogCanceledException, HideCaptchasByHostException, HideCaptchasByPackageException, StopDownloadsException, HideAllCaptchasException, RefreshException {

        ClickCaptchaDialog d = new ClickCaptchaDialog(flag, dialogType, getHost(), images, captchaChallenge.getExplain());
        d.setPlugin(captchaChallenge.getPlugin());
        d.setCountdownTime(CaptchaSettings.CFG.getCountdown());
        dialog = d;
        try {
            result = UIOManager.I().show(ClickCaptchaDialogInterface.class, d).getResult();
        } catch (DialogCanceledException e) {
            if (d.isHideCaptchasForHost()) throw new HideCaptchasByHostException();
            if (d.isHideCaptchasForPackage()) throw new HideCaptchasByPackageException();
            if (d.isStopDownloads()) throw new StopDownloadsException();
            if (d.isHideAllCaptchas()) throw new HideAllCaptchasException();
            if (d.isRefresh()) throw new RefreshException();
            throw e;
        }

    }

}