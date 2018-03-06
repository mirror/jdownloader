package org.jdownloader.captcha.v2;

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
import org.jdownloader.DomainInfo;

public abstract class AbstractDialogHandler<DialogClass extends AbstractCaptchaDialog<ReturnCode>, ChallengeType extends Challenge<?>, ReturnCode> extends ChallengeDialogHandler<ChallengeType> {
    protected DialogClass dialog;

    public DialogClass getDialog() {
        return dialog;
    }

    public ReturnCode getResponse() {
        return getDialog().getReturnValue();
    }

    private Object suggest;

    public AbstractDialogHandler(DomainInfo instance, ChallengeType captchaChallenge2) {
        super(instance, captchaChallenge2);
    }

    @Override
    protected void showDialog(DialogType dialogType, int flag) throws DialogClosedException, DialogCanceledException, HideCaptchasByHostException, HideCaptchasByPackageException, StopCurrentActionException, HideAllCaptchasException, RefreshException {
        DialogClass d = createDialog(dialogType, flag, new Runnable() {
            @Override
            public void run() {
                synchronized (AbstractDialogHandler.this) {
                    AbstractDialogHandler.this.notifyAll();
                }
            }
        });
        d.setPlugin(captchaChallenge.getPlugin());
        d.setTimeout(getTimeoutInMS());
        if (!captchaChallenge.keepAlive()) {
            // no reason to let the user stop the countdown if the result cannot be used after the countdown anyway
            d.setCountdownPausable(false);
        }
        dialog = d;
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
                        synchronized (AbstractDialogHandler.this) {
                            AbstractDialogHandler.this.notifyAll();
                        }
                    }

                    @Override
                    public void windowClosed(WindowEvent e) {
                        synchronized (AbstractDialogHandler.this) {
                            boolean v = dialog.getDialog().isVisible();
                            AbstractDialogHandler.this.notifyAll();
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
            throw new DialogClosedException(Dialog.RETURN_INTERRUPT);
        } finally {
            try {
                dialog.dispose();
            } catch (Exception e) {
            }
        }
        // dialog.getReturnValue();
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
    }

    protected abstract DialogClass createDialog(DialogType dialogType, int flag, final Runnable runnable);

    public void setSuggest(final Object responseList) {
        suggest = responseList;
        // new EDTRunner() {
        //
        // @Override
        // protected void runInEDT() {
        // captchaChallenge.suggest(suggest);
        //
        // }
        // };
    }

    public void requestFocus() {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                DialogClass d = dialog;
                if (d != null) {
                    InternDialog<?> win = d.getDialog();
                    if (win != null) {
                        WindowManager.getInstance().setZState(win, FrameState.TO_FRONT_FOCUSED);
                    }
                }
            }
        };
    }
}
