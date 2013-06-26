package org.jdownloader.gui.views.downloads.action;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ConfirmDeleteLinksDialog extends ConfirmDialog implements ConfirmDeleteLinksDialogInterface {

    private long      bytes;
    private boolean   toRecycle;
    private JComboBox toRecycleCb;
    private boolean   recycleSupported;
    private boolean   shift;
    private Image     image;

    public ConfirmDeleteLinksDialog(String msg, long bytes) {
        super(UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.literally_are_you_sure(), msg, NewTheme.I().getIcon("robot_del", -1), _GUI._.lit_delete(), _GUI._.lit_cancel());
        this.bytes = bytes;

    }

    public ConfirmDeleteLinksDialogInterface show() {

        return UIOManager.I().show(ConfirmDeleteLinksDialogInterface.class, this);
    }

    @Override
    public JComponent layoutDialogContent() {
        image = NewTheme.I().getImage("botty_stop", 128);
        getDialog().setContentPane(new MigPanel("ins 5,wrap 1", "[grow,fill]", "[grow,fill][]") {

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                g.drawImage(image, 0, 0, null);
            }

            public Dimension getPreferredSize() {
                Dimension ret = super.getPreferredSize();
                ret.height = Math.max(ret.height, image.getHeight(null));
                return ret;
            }
        });

        JComponent ret = super.layoutDialogContent();
        ret.setBorder(BorderFactory.createEmptyBorder(0, image.getWidth(null), 0, 0));
        ret.setOpaque(false);
        if (isRecycleSupported()) {
            toRecycleCb = new JComboBox(new String[] { _GUI._.ConfirmDeleteLinksDialog_layoutDialogContent_no_filedelete2(), _GUI._.ConfirmDeleteLinksDialog_layoutDialogContent_Recycle_2(), _GUI._.ConfirmDeleteLinksDialog_layoutDialogContent_delete_2() });
        } else {
            toRecycleCb = new JComboBox(new String[] { _GUI._.ConfirmDeleteLinksDialog_layoutDialogContent_no_filedelete2(), _GUI._.ConfirmDeleteLinksDialog_layoutDialogContent_delete_2() });

        }

        if (bytes > 0) {
            MigPanel p = new MigPanel("ins 15 0 0 0", "[][]", "[]");
            p.setOpaque(false);
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
