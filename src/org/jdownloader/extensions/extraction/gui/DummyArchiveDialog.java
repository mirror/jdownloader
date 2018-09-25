package org.jdownloader.extensions.extraction.gui;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;

public class DummyArchiveDialog extends AbstractDialog<Object> {
    private final DummyArchive archive;

    public DummyArchiveDialog(DummyArchive da) {
        super(Dialog.STYLE_HIDE_ICON | UIOManager.BUTTONS_HIDE_CANCEL, T.T.dummyarchivedialog_title(da.getName()), null, T.T.close(), null);
        archive = da;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel d = new MigPanel("ins 5,wrap 1", "[]", "[grow,fill]");
        if (archive.isComplete()) {
            JLabel lbl = new JLabel();
            d.add(lbl, "pushx,growx");
            lbl.setIcon(new AbstractIcon(IconKey.ICON_OK, 32));
            lbl.setText(T.T.ValidateArchiveAction_actionPerformed_(archive.getSize()));
            lbl = new JLabel();
            d.add(lbl, "pushx,growx");
            lbl.setIcon(new AbstractIcon(IconKey.ICON_INFO, 32));
            lbl.setText(T.T.ValidateArchiveAction_actionPerformed_information(archive.getType()));
        } else {
            JLabel lbl = new JLabel();
            d.add(lbl, "pushx,growx");
            lbl.setIcon(new AbstractIcon(IconKey.ICON_STOP, 32));
            lbl.setText(T.T.ValidateArchiveAction_actionPerformed_bad(archive.getSize()));
            lbl = new JLabel();
            d.add(lbl, "pushx,growx");
            lbl.setIcon(new AbstractIcon(IconKey.ICON_INFO, 32));
            lbl.setText(T.T.ValidateArchiveAction_actionPerformed_information(archive.getType()));
        }
        DummyArchiveContentsTable table = new DummyArchiveContentsTable(archive);
        d.add(new JScrollPane(table), "spanx,pushx,growx");
        setPreferredSize(new Dimension(500, 500));
        return d;
    }
}
