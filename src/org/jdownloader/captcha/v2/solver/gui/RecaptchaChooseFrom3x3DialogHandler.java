package org.jdownloader.captcha.v2.solver.gui;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import jd.controlling.captcha.ChallengeDialogHandler;
import jd.controlling.captcha.HideAllCaptchasException;
import jd.controlling.captcha.HideCaptchasByHostException;
import jd.controlling.captcha.HideCaptchasByPackageException;
import jd.controlling.captcha.RefreshException;
import jd.controlling.captcha.StopCurrentActionException;
import jd.gui.swing.dialog.DialogType;

import org.appwork.uio.CloseReason;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.InternDialog;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptcha2FallbackChallenge;

public class RecaptchaChooseFrom3x3DialogHandler extends ChallengeDialogHandler<AbstractRecaptcha2FallbackChallenge> {
    private String                       result;
    private RecaptchaChooseFrom3x3Dialog dialog;

    public String getResult() {
        return result;
    }

    public RecaptchaChooseFrom3x3DialogHandler(AbstractRecaptcha2FallbackChallenge captchaChallenge) {
        super(captchaChallenge.getDomainInfo(), captchaChallenge);
    }

    @Override
    protected void showDialog(DialogType dialogType, int flag) throws DialogClosedException, DialogCanceledException, HideCaptchasByHostException, HideCaptchasByPackageException, StopCurrentActionException, HideAllCaptchasException, RefreshException {
        if (false) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            result = "7,8,9";
            return;
        }
        RecaptchaChooseFrom3x3Dialog d = new RecaptchaChooseFrom3x3Dialog(captchaChallenge, flag, dialogType, getHost(), captchaChallenge);
        d.setPlugin(captchaChallenge.getPlugin());
        d.setTimeout(getTimeoutInMS());
        if (!captchaChallenge.keepAlive()) {
            // no reason to let the user stop the countdown if the result cannot be used after the countdown anyway
            d.setCountdownPausable(false);
        }
        dialog = d;
        // don't put this in the edt
        showDialog(dialog);
        new EDTHelper<Object>() {
            @Override
            public Object edtRun() {
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
                        synchronized (RecaptchaChooseFrom3x3DialogHandler.this) {
                            boolean v = dialog.getDialog().isVisible();
                            RecaptchaChooseFrom3x3DialogHandler.this.notifyAll();
                        }
                    }

                    @Override
                    public void windowClosed(WindowEvent e) {
                        synchronized (RecaptchaChooseFrom3x3DialogHandler.this) {
                            boolean v = dialog.getDialog().isVisible();
                            RecaptchaChooseFrom3x3DialogHandler.this.notifyAll();
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
        } finally {
            try {
                if (!dialog.isDisposed()) {
                    dialog.dispose();
                }
            } catch (Exception e) {
            }
        }
        result = dialog.getResult();
        try {
            if (dialog.getCloseReason() != CloseReason.OK) {
                if (dialog.isHideCaptchasForHost()) {
                    throw new HideCaptchasByHostException();
                }
                if (dialog.isHideCaptchasForPackage()) {
                    throw new HideCaptchasByPackageException();
                }
                if (dialog.isStopDownloads()) {
                    throw new StopCurrentActionException();
                }
                if (dialog.isHideAllCaptchas()) {
                    throw new HideAllCaptchasException();
                }
                if (dialog.isStopCrawling()) {
                    throw new StopCurrentActionException();
                }
                if (dialog.isStopShowingCrawlerCaptchas()) {
                    throw new HideAllCaptchasException();
                }
                if (dialog.isRefresh()) {
                    throw new RefreshException();
                }
                dialog.throwCloseExceptions();
                throw new DialogClosedException(Dialog.RETURN_CLOSED);
            }
        } catch (IllegalStateException e) {
            // Captcha has been solved externally
        }
    }

    public void requestFocus() {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                RecaptchaChooseFrom3x3Dialog d = dialog;
                if (d != null) {
                    InternDialog<Object> win = d.getDialog();
                    if (win != null) {
                        WindowManager.getInstance().setZState(win, FrameState.TO_FRONT_FOCUSED);
                    }
                }
            }
        };
    }
}