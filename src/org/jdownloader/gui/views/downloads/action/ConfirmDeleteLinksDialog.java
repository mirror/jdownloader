package org.jdownloader.gui.views.downloads.action;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ConfirmDeleteLinksDialog extends ConfirmDialog implements ConfirmDeleteLinksDialogInterface {

    private JCheckBox deletFilesCb;
    private String    bytes;

    public ConfirmDeleteLinksDialog(String msg, String bytes) {
        super(Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.literally_are_you_sure(), msg, NewTheme.I().getIcon("robot_del", -1), _GUI._.literally_yes(), _GUI._.literall_no());
        this.bytes = bytes;

    }

    @Override
    public JComponent layoutDialogContent() {
        JComponent ret = super.layoutDialogContent();
        deletFilesCb = new JCheckBox();
        MigPanel p = new MigPanel("ins 15 0 0 0", "[][]", "[]");
        p.add(deletFilesCb);
        p.add(new JLabel(_GUI._.ConfirmDeleteLinksDialog_layoutDialogContent_deletefiles_(bytes)));
        ret.add(p, "newline,alignx right");
        return ret;

    }

    @Override
    public boolean isDeleteFilesFromDiskEnabled() {
        return deletFilesCb.isSelected();
    }
}
