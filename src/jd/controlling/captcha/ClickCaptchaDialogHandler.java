package jd.controlling.captcha;

import java.awt.Image;

import jd.gui.swing.dialog.ClickCaptchaDialog;
import jd.gui.swing.dialog.ClickCaptchaDialogInterface;
import jd.gui.swing.dialog.DialogType;

import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.gui.userio.NewUIO;

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
    protected void showDialog(DialogType dialogType, int flag, Image[] images) throws DialogClosedException, DialogCanceledException {

        ClickCaptchaDialog d = new ClickCaptchaDialog(flag, dialogType, getHost(), images, captchaChallenge.getExplain());
        d.setPlugin(captchaChallenge.getPlugin());
        d.setCountdownTime(CaptchaSettings.CFG.getCountdown());
        dialog = d;
        result = NewUIO.I().show(ClickCaptchaDialogInterface.class, d).getResult();

    }

}