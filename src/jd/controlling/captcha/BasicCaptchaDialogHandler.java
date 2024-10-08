package jd.controlling.captcha;

import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;

import jd.gui.swing.dialog.CaptchaDialog;
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
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;

public class BasicCaptchaDialogHandler extends ChallengeDialogHandler<BasicCaptchaChallenge> {
    private CaptchaDialog dialog;
    private String        result;
    private String        suggest;

    public String getCaptchaCode() {
        return result;
    }

    public BasicCaptchaDialogHandler(BasicCaptchaChallenge captchaChallenge) {
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
            CaptchaDialog d = new CaptchaDialog(captchaChallenge, flag, dialogType, getHost(), images, captchaChallenge.getExplain()) {
                public void dispose() {
                    super.dispose();
                    synchronized (BasicCaptchaDialogHandler.this) {
                        BasicCaptchaDialogHandler.this.notifyAll();
                    }
                }
            };
            d.setPlugin(captchaChallenge.getPlugin());
            d.setTimeout(getTimeoutInMS());
            if (!captchaChallenge.keepAlive()) {
                // no reason to let the user stop the countdown if the result cannot be used after the countdown anyway
                d.setCountdownPausable(false);
            }
            dialog = d;
            if (suggest != null) {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        dialog.suggest(suggest);
                    }
                }.waitForEDT();
            }
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
                            synchronized (BasicCaptchaDialogHandler.this) {
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
                while (dialog.getDialog().isDisplayable()) {
                    synchronized (this) {
                        this.wait(1000);
                    }
                }
            } catch (InterruptedException e) {
                if (dialog.getReturnmask() == 0) {
                    dialog.setCloseReason(CloseReason.INTERRUPT);
                }
                throw new DialogClosedException(Dialog.RETURN_INTERRUPT, e);
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
                        //
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

    public void requestFocus() {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                CaptchaDialog d = dialog;
                if (d != null) {
                    InternDialog<Object> win = d.getDialog();
                    if (win != null) {
                        WindowManager.getInstance().setZState(win, FrameState.TO_FRONT_FOCUSED);
                    }
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