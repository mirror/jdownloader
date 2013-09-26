package org.jdownloader.api.dialog;

import javax.swing.ImageIcon;

import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.resources.AWUTheme;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.MessageDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.uio.UserIODefinition;
import org.appwork.uio.UserIODefinition.CloseReason;
import org.appwork.uio.UserIOHandlerInterface;
import org.appwork.utils.Application;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.MessageDialogImpl;
import org.jdownloader.logging.LogController;

public class RemoteAPIIOHandlerWrapper implements UserIOHandlerInterface {

    private DialogApiImpl remoteHandler;
    private LogSource     logger;

    public RemoteAPIIOHandlerWrapper(UserIOHandlerInterface i) {
        remoteHandler = new DialogApiImpl(this);
        logger = LogController.getInstance().getLogger(RemoteAPIIOHandlerWrapper.class.getName());
    }

    @Override
    public boolean showConfirmDialog(int flags, String title, String message, ImageIcon icon, String ok, String cancel) {
        ConfirmDialog ret = new ConfirmDialog(flags, title, message, icon, ok, cancel);
        ConfirmDialogInterface io = show(ConfirmDialogInterface.class, ret);
        return io.getCloseReason() == CloseReason.OK;
    }

    @Override
    public boolean showConfirmDialog(int flag, String title, String message) {
        ConfirmDialog ret = new ConfirmDialog(flag, title, message, null, null, null);
        ConfirmDialogInterface io = show(ConfirmDialogInterface.class, ret);
        return io.getCloseReason() == CloseReason.OK;
    }

    @Override
    public void showMessageDialog(String message) {
        show(MessageDialogInterface.class, new MessageDialogImpl(0, message));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends UserIODefinition> T show(Class<T> class1, T impl) {
        // evaluate do not show again flag
        if (impl instanceof AbstractDialog) {
            AbstractDialog<?> dialog = (AbstractDialog<?>) impl;

            try {
                dialog.forceDummyInit();
                if (dialog.evaluateDontShowAgainFlag()) {
                    // final int mask = dialog.getReturnmask();
                    // if (BinaryLogic.containsSome(mask, Dialog.RETURN_CLOSED)) { throw new DialogClosedException(mask); }
                    // if (BinaryLogic.containsSome(mask, Dialog.RETURN_CANCEL)) { throw new DialogCanceledException(mask); }
                    // if (!BinaryLogic.containsSome(mask, Dialog.RETURN_OK)) { throw new DialogCanceledException(mask |
                    // Dialog.RETURN_CLOSED); }
                    return impl;
                }
                // } catch (DialogNoAnswerException e) {
                // throw e;
            } catch (Exception e) {
                // Dialogs are not initialized.... nullpointers are very likly
                // These nullpointers should be fixed
                // in this case, we should continue normal
                logger.log(e);
            }

            dialog.resetDummyInit();
        }

        ApiHandle handle = remoteHandler.enqueue(class1, impl);
        try {
            try {
                if (impl instanceof AbstractDialog) {
                    if (!Application.isJared(RemoteAPIIOHandlerWrapper.class)) {
                        ((AbstractDialog<?>) impl).setTitle(((AbstractDialog<?>) impl).getTitle() + " DialogID: " + handle.getId());
                    }

                    Dialog.getInstance().showDialog((AbstractDialog<?>) impl);
                } else {
                    throw new WTFException("Not Supported Dialog Type!: " + impl);
                }
            } catch (final DialogClosedException e) {
                if (e.isCausedByInterrupt()) {

                }
                // no Reason to log here
            } catch (final DialogCanceledException e) {
                // no Reason to log here
            } finally {
                handle.dispose();
            }
            return (T) (handle.getAnswer() != null ? handle.getAnswer() : impl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return impl;

    }

    @Override
    public void showErrorMessage(String message) {
        this.showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _AWU.T.DIALOG_ERROR_TITLE(), message, AWUTheme.I().getIcon(Dialog.ICON_ERROR, 32), null, null);
    }

    public void onHandlerDone(final ApiHandle ret) {
        if (ret.getImpl() instanceof AbstractDialog) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    try {
                        if (!((AbstractDialog) ret.getImpl()).isDisposed()) {
                            ((AbstractDialog) ret.getImpl()).interrupt();
                        }
                    } catch (Exception e) {

                    }
                }
            };
        }
    }

    public RemoteAPIInterface getApi() {
        return remoteHandler;
    }

    public EventPublisher getEventPublisher() {
        return remoteHandler;
    }

}
