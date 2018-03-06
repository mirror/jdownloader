package jd.controlling.captcha;

import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;

import jd.gui.swing.dialog.CaptchaDialog;
import jd.gui.swing.dialog.ClickCaptchaDialog;
import jd.gui.swing.dialog.DialogType;

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
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;

public class ClickCaptchaDialogHandler extends ChallengeDialogHandler<ClickCaptchaChallenge> {
    private ClickedPoint       result;
    private ClickCaptchaDialog dialog;

    public ClickedPoint getPoint() {
        return result;
    }

    public ClickCaptchaDialogHandler(ClickCaptchaChallenge captchaChallenge) {
        super(captchaChallenge.getDomainInfo(), captchaChallenge);
    }

    @Override
    protected void showDialog(DialogType dialogType, int flag) throws DialogClosedException, DialogCanceledException, HideCaptchasByHostException, HideCaptchasByPackageException, StopCurrentActionException, HideAllCaptchasException, RefreshException {
        Image[] images = null;
        try {
            if (captchaChallenge instanceof ImageCaptchaChallenge) {
                images = CaptchaDialog.getGifImages(((ImageCaptchaChallenge<?>) captchaChallenge).getImageFile().toURI().toURL());
                if (images == null || images.length == 0) {
                    BufferedImage img = IconIO.getImage(((ImageCaptchaChallenge<?>) captchaChallenge).getImageFile().toURI().toURL(), false);
                    if (img != null) {
                        images = new Image[] { img };
                    }
                }
                if (images == null || images.length == 0 || images[0] == null) {
                    getLogger().severe("Could not load CaptchaImage! " + ((ImageCaptchaChallenge<?>) captchaChallenge).getImageFile().getAbsolutePath());
                    return;
                }
            } else {
                return;
            }
            ClickCaptchaDialog d = new ClickCaptchaDialog(captchaChallenge, flag, dialogType, getHost(), images, captchaChallenge.getExplain());
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
                            synchronized (ClickCaptchaDialogHandler.this) {
                                boolean v = dialog.getDialog().isVisible();
                                ClickCaptchaDialogHandler.this.notifyAll();
                            }
                        }

                        @Override
                        public void windowClosed(WindowEvent e) {
                            synchronized (ClickCaptchaDialogHandler.this) {
                                boolean v = dialog.getDialog().isVisible();
                                ClickCaptchaDialogHandler.this.notifyAll();
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
                    dialog.dispose();
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
                ClickCaptchaDialog d = dialog;
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