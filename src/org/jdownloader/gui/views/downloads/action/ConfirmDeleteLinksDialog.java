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
import org.jdownloader.settings.GraphicalUserInterfaceSettings.DeleteFileOptions;

public class ConfirmDeleteLinksDialog extends ConfirmDialog {

    private long              bytes;

    private JComboBox         toRecycleCb;
    private boolean           recycleSupported;

    private Image             image;
    private DeleteFileOptions mode;

    public ConfirmDeleteLinksDialog(String msg, long bytes) {
        super(UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.literally_are_you_sure(), msg, NewTheme.I().getIcon("robot_del", -1), _GUI._.lit_delete(), _GUI._.lit_cancel());
        this.bytes = bytes;

    }

    @Override
    public String getDontShowAgainKey() {
        return "ConfirmDeleteLinksDialog";
    }

    public ConfirmDeleteLinksDialog show() {

        return UIOManager.I().show(null, this);
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

            if (isRecycleSupported()) {

                switch (mode) {
                case REMOVE_LINKS_ONLY:
                    toRecycleCb.setSelectedIndex(0);
                    break;
                case REMOVE_LINKS_AND_DELETE_FILES:
                    toRecycleCb.setSelectedIndex(2);
                    break;

                case REMOVE_LINKS_AND_RECYCLE_FILES:
                    toRecycleCb.setSelectedIndex(1);
                    break;

                }
            } else {
                switch (mode) {
                case REMOVE_LINKS_ONLY:
                    toRecycleCb.setSelectedIndex(0);
                    break;
                case REMOVE_LINKS_AND_DELETE_FILES:
                    toRecycleCb.setSelectedIndex(1);
                    break;

                case REMOVE_LINKS_AND_RECYCLE_FILES:
                    toRecycleCb.setSelectedIndex(1);
                    break;

                }
            }
            ret.add(p, "newline,pushx,growx");
        }

        return ret;

    }

    public void setMode(DeleteFileOptions mode) {
        this.mode = mode;
    }

    public DeleteFileOptions getMode() {
        if (toRecycleCb != null) {
            if (toRecycleCb.getItemCount() == 3) {
                switch (toRecycleCb.getSelectedIndex()) {
                case 0:
                    return DeleteFileOptions.REMOVE_LINKS_ONLY;
                case 1:
                    return DeleteFileOptions.REMOVE_LINKS_AND_RECYCLE_FILES;
                default:
                    return DeleteFileOptions.REMOVE_LINKS_AND_DELETE_FILES;
                }
            } else {
                switch (toRecycleCb.getSelectedIndex()) {
                case 0:
                    return DeleteFileOptions.REMOVE_LINKS_ONLY;

                default:
                    return DeleteFileOptions.REMOVE_LINKS_AND_DELETE_FILES;
                }
            }

        }
        return mode;
    }

    public void setRecycleSupported(boolean windows) {
        recycleSupported = windows;
    }

    public boolean isRecycleSupported() {
        return recycleSupported;
    }

}
