package org.jdownloader.extensions.extraction.gui;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.appwork.app.gui.MigPanel;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.images.NewTheme;

public class DummyArchiveDialog extends AbstractDialog<Object> {

    private DummyArchive archive;

    public DummyArchiveDialog(DummyArchive da) {
        super(Dialog.STYLE_HIDE_ICON | Dialog.BUTTONS_HIDE_CANCEL, T._.dummyarchivedialog_title(da.getName()), null, T._.close(), null);
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

        MigPanel d = new MigPanel("ins 5,wrap 1", "[][]", "[]");

        if (archive.isComplete()) {
            JLabel lbl;
            d.add(lbl = new JLabel(T._.ValidateArchiveAction_actionPerformed_(archive.getName(), archive.getSize())));
            lbl.setIcon(NewTheme.I().getIcon("ok", 20));
        } else {
            JLabel lbl;
            d.add(lbl = new JLabel(T._.ValidateArchiveAction_actionPerformed_bad(archive.getName(), archive.getSize())));
            lbl.setIcon(NewTheme.I().getIcon("stop", 20));

        }
        DummyArchiveContentsTable table = new DummyArchiveContentsTable(archive);
        d.add(new JScrollPane(table), "spanx");

        setPreferredSize(new Dimension(500, 400));
        return d;
    }

}
