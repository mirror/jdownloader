package org.jdownloader.gui.views.downloads.action;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ConfirmDeleteLinksDialog extends ConfirmDialog implements ConfirmDeleteLinksDialogInterface {

    private long      bytes;
    private boolean   toRecycle;
    private JComboBox toRecycleCb;
    private boolean   recycleSupported;
    private boolean   shift;

    public ConfirmDeleteLinksDialog(String msg, long bytes) {
        super(Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.literally_are_you_sure(), msg, NewTheme.I().getIcon("robot_del", -1), _GUI._.lit_delete(), _GUI._.lit_cancel());
        this.bytes = bytes;

    }

    @Override
    public JComponent layoutDialogContent() {
        JComponent ret = super.layoutDialogContent();

        if (isRecycleSupported()) {
            toRecycleCb = new JComboBox(new String[] { _GUI._.ConfirmDeleteLinksDialog_layoutDialogContent_no_filedelete(), _GUI._.ConfirmDeleteLinksDialog_layoutDialogContent_Recycle_(), _GUI._.ConfirmDeleteLinksDialog_layoutDialogContent_delete_() });
        } else {
            toRecycleCb = new JComboBox(new String[] { _GUI._.ConfirmDeleteLinksDialog_layoutDialogContent_no_filedelete(), _GUI._.ConfirmDeleteLinksDialog_layoutDialogContent_delete_() });

        }

        if (bytes > 0) {
            MigPanel p = new MigPanel("ins 15 0 0 0", "[][]", "[]");

            p.add(toRecycleCb, "newline,spanx,pushx,growx");
            if (isRecycleSupported() && shift) {
                toRecycleCb.setSelectedIndex(2);
            } else if (shift) {
                toRecycleCb.setSelectedIndex(1);
            }

            ret.add(p, "newline,pushx,growx");
        }
        return ret;

    }

    @Override
    public boolean isDeleteFilesFromDiskEnabled() {

        return toRecycleCb.getSelectedIndex() > 0;
    }

    public void setDeleteFilesToRecycle(boolean shiftDown) {
        toRecycle = shiftDown;
    }

    @Override
    public boolean isDeleteFilesToRecycle() {
        return toRecycle && isRecycleSupported() && toRecycleCb.getSelectedIndex() == 1;
    }

    public void setRecycleSupported(boolean windows) {
        recycleSupported = windows;
    }

    public boolean isRecycleSupported() {
        return recycleSupported;
    }

    public void setDeleteFilesFromDiskEnabled(boolean shiftDown) {
        this.shift = shiftDown;
    }
}
