package org.jdownloader.captcha.v2.solver.gui;

import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;

import org.appwork.exceptions.WTFException;
import org.appwork.uio.CloseReason;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.InternDialog;
import org.appwork.utils.swing.windowmanager.WindowManager;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge.Recaptcha2FallbackChallenge;

import jd.controlling.captcha.ChallengeDialogHandler;
import jd.controlling.captcha.HideAllCaptchasException;
import jd.controlling.captcha.HideCaptchasByHostException;
import jd.controlling.captcha.HideCaptchasByPackageException;
import jd.controlling.captcha.RefreshException;
import jd.controlling.captcha.StopCurrentActionException;
import jd.gui.swing.dialog.DialogType;

public class RecaptchaChooseFrom3x3DialogHandler extends ChallengeDialogHandler<Recaptcha2FallbackChallenge> {

    private String                       result;
    private RecaptchaChooseFrom3x3Dialog dialog;

    public String getResult() {
        return result;
    }

    public RecaptchaChooseFrom3x3DialogHandler(Recaptcha2FallbackChallenge captchaChallenge) {
        super(captchaChallenge.getDomainInfo(), captchaChallenge);

    }

    @Override
    protected void showDialog(DialogType dialogType, int flag) throws DialogClosedException, DialogCanceledException, HideCaptchasByHostException, HideCaptchasByPackageException, StopCurrentActionException, HideAllCaptchasException, RefreshException {
        Image[] images = null;
        try {
            BufferedImage img = IconIO.getImage(captchaChallenge.getImageFile().toURI().toURL(), false);
            if (img != null) {
                images = new Image[] { img };
            }
            RecaptchaChooseFrom3x3Dialog d = new RecaptchaChooseFrom3x3Dialog(flag, dialogType, getHost(), images, captchaChallenge.getExplain());
            d.setPlugin(captchaChallenge.getPlugin());
            d.setTimeout(getTimeoutInMS());
            if (getTimeoutInMS() == captchaChallenge.getTimeout()) {
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

        } catch (MalformedURLException e) {
            throw new WTFException(e);
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