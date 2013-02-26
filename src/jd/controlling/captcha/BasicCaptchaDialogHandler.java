package jd.controlling.captcha;

import java.awt.Image;

import jd.gui.swing.dialog.CaptchaDialog;
import jd.gui.swing.dialog.CaptchaDialogInterface;
import jd.gui.swing.dialog.DialogType;

import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.gui.userio.NewUIO;

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
    protected void showDialog(DialogType dialogType, int flag, Image[] images) throws DialogClosedException, DialogCanceledException {
        CaptchaDialog d = new CaptchaDialog(flag, dialogType, getHost(), images, captchaChallenge.getExplain());
        d.setPlugin(captchaChallenge.getPlugin());
        d.setCountdownTime(CaptchaSettings.CFG.getCountdown());
        dialog = d;
        if (suggest != null) dialog.suggest(suggest);
        result = NewUIO.I().show(CaptchaDialogInterface.class, d).getResult();

    }

    public void suggest(final String value) {
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