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
import org.jdownloader.images.NewTheme;

public class DummyArchiveDialog extends AbstractDialog<Object> {

    private DummyArchive archive;

    public DummyArchiveDialog(DummyArchive da) {
        super(Dialog.STYLE_HIDE_ICON | UIOManager.BUTTONS_HIDE_CANCEL, T._.dummyarchivedialog_title(da.getName()), null, T._.close(), null);
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
            JLabel lbl;

            d.add(lbl = new JLabel(), "pushx,growx");
            lbl.setIcon(NewTheme.I().getIcon("ok", 32));
            lbl.setText(T._.ValidateArchiveAction_actionPerformed_(archive.getSize()));

        } else {
            JLabel lbl;
            // )

            d.add(lbl = new JLabel(), "pushx,growx");
            lbl.setIcon(NewTheme.I().getIcon("stop", 32));
            lbl.setText(T._.ValidateArchiveAction_actionPerformed_bad(archive.getSize()));

        }
        DummyArchiveContentsTable table = new DummyArchiveContentsTable(archive);
        d.add(new JScrollPane(table), "spanx,pushx,growx");

        setPreferredSize(new Dimension(500, 500));
        return d;
    }

}
