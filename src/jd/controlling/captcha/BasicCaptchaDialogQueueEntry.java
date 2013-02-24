package jd.controlling.captcha;

import java.awt.Image;

import jd.gui.swing.dialog.CaptchaDialog;
import jd.gui.swing.dialog.CaptchaDialogInterface;
import jd.gui.swing.dialog.DialogType;

import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.gui.userio.NewUIO;

public class BasicCaptchaDialogQueueEntry extends ChallengeDialogQueueEntry<BasicCaptchaChallenge> {

    private CaptchaDialogInterface dialog;
    private String                 result;

    public String getCaptchaCode() {
        return result;
    }

    public BasicCaptchaDialogQueueEntry(BasicCaptchaChallenge captchaChallenge) {
        super(DomainInfo.getInstance(captchaChallenge.getPlugin().getHost()), captchaChallenge);

    }

    @Override
    protected void showDialog(DialogType dialogType, int flag, Image[] images) throws DialogClosedException, DialogCanceledException {
        CaptchaDialog d = new CaptchaDialog(flag, dialogType, getHost(), images, captchaChallenge.getExplain());
        d.setPlugin(captchaChallenge.getPlugin());
        d.setCountdownTime(CaptchaSettings.CFG.getCountdown());
        dialog = d;
        result = NewUIO.I().show(CaptchaDialogInterface.class, d).getResult();

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