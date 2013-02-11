package org.jdownloader.gui.userio;

import javax.swing.ImageIcon;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.UserIODefinition;

public interface UserIOInterface {

    void showConfirmDialog(int flags, String title, String message, ImageIcon icon, String ok, String cancel) throws DialogClosedException, DialogCanceledException;

    void showConfirmDialog(int flag, String title, String message) throws DialogClosedException, DialogCanceledException;

    void showMessageDialog(String message);

    <T extends UserIODefinition> T show(Class<T> class1, AbstractDialog<?> defImpl) throws DialogClosedException, DialogCanceledException;

    void showErrorMessage(String message);

}
