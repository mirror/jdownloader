package jd.controlling.captcha;

import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import jd.gui.swing.dialog.CaptchaDialog;
import jd.gui.swing.dialog.DialogType;

import org.appwork.uio.UserIODefinition.CloseReason;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;

public class BasicCaptchaDialogHandler extends ChallengeDialogHandler<BasicCaptchaChallenge> {

    private CaptchaDialog dialog;
    private String        result;
    private String        suggest;

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

        new EDTHelper<Object>() {

            @Override
            public Object edtRun() {
                dialog.displayDialog();
                dialog.getDialog().addWindowListener(new WindowListener() {

                    @Override
                    public void windowOpened(WindowEvent e) {
                    }

                    @Override
                    public void windowIconified(WindowEvent e) {
                    }

                    @Override
                    public void windowDeiconified(WindowEvent e) {
                    }

                    @Override
                    public void windowDeactivated(WindowEvent e) {
                    }

                    @Override
                    public void windowClosing(WindowEvent e) {
                        synchronized (BasicCaptchaDialogHandler.this) {
                            boolean v = dialog.getDialog().isVisible();
                            BasicCaptchaDialogHandler.this.notifyAll();
                        }

                    }

                    @Override
                    public void windowClosed(WindowEvent e) {
                        synchronized (BasicCaptchaDialogHandler.this) {
                            boolean v = dialog.getDialog().isVisible();
                            BasicCaptchaDialogHandler.this.notifyAll();
                        }
                    }

                    @Override
                    public void windowActivated(WindowEvent e) {
                    }
                });
                return null;
            }
        }.waitForEDT();
        try {
            while (dialog.getDialog().isVisible()) {
                synchronized (this) {

                    this.wait();

                }
            }

        } catch (InterruptedException e) {
            throw new DialogClosedException(Dialog.RETURN_INTERRUPT);
        }
        result = dialog.getResult();
        try {
            if (dialog.getCloseReason() != CloseReason.OK) {

                if (dialog.isHideCaptchasForHost()) throw new HideCaptchasByHostException();
                if (dialog.isHideCaptchasForPackage()) throw new HideCaptchasByPackageException();
                if (dialog.isStopDownloads()) throw new StopCurrentActionException();
                if (dialog.isHideAllCaptchas()) throw new HideAllCaptchasException();
                if (dialog.isStopCrawling()) throw new StopCurrentActionException();
                if (dialog.isStopShowingCrawlerCaptchas()) throw new HideAllCaptchasException();
                if (dialog.isRefresh()) throw new RefreshException();
                dialog.throwCloseExceptions();
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