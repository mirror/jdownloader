package org.jdownloader.gui.userio;

import javax.swing.ImageIcon;

import org.appwork.utils.locale._AWU;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;

public class MessageDialogImpl extends ConfirmDialog implements MessageDialogInterface {

    public MessageDialogImpl(int flags, String msg) {
        this(Dialog.BUTTONS_HIDE_CANCEL | flags, _AWU.T.DIALOG_MESSAGE_TITLE(), msg, null, null);
    }

    public MessageDialogImpl(int flag, String title, String msg, ImageIcon icon, String okText) {
        super(flag, title, msg, icon == null ? Dialog.getIconByText(title + msg) : icon, okText, null);

    }

}
