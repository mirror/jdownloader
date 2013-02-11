package org.jdownloader.gui.userio;

import javax.swing.ImageIcon;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.UserIODefinition;

public class JDSwingUserIO implements UserIOInterface {
    private static final Dialog D = Dialog.I();

    public void showConfirmDialog(int flags, String title, String message, ImageIcon icon, String ok, String cancel) throws DialogClosedException, DialogCanceledException {
        D.showConfirmDialog(flags, title, message, icon, ok, cancel);
    }

    public void showConfirmDialog(int flags, String title, String message) throws DialogClosedException, DialogCanceledException {
        showConfirmDialog(flags, title, message, null, null, null);
    }

    public void showMessageDialog(String message) {
        D.showMessageDialog(message);
    }

    @SuppressWarnings("unchecked")
    public <T extends UserIODefinition> T show(Class<T> class1, AbstractDialog<?> defImpl) throws DialogClosedException, DialogCanceledException {
        T ret = (T) defImpl;
        D.showDialog(defImpl);
        return ret;
    }

    public void showErrorMessage(String message) {
        D.showErrorDialog(message);
    }
}
