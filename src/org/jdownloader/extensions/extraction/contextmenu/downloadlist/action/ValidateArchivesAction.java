package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionAction;
import org.jdownloader.extensions.extraction.gui.DummyArchiveDialog;
import org.jdownloader.extensions.extraction.multi.CheckException;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.views.SelectionInfo;

public class ValidateArchivesAction extends AbstractExtractionAction {

    public ValidateArchivesAction(SelectionInfo<?, ?> selection) {
        super(selection);
        setName(T._.ValidateArchiveAction_ValidateArchiveAction_object_());

        setSmallIcon(merge("unpack", "ok"));

        setEnabled(false);

    }

    @Override
    protected void onAsyncInitDone() {
        if (archives != null && archives.size() > 0) {
            if (archives.size() > 1) {
                setName(T._.ValidateArchiveAction_ValidateArchiveAction_multi());
            } else {
                setName(T._.ValidateArchiveAction_ValidateArchiveAction(archives.get(0).getName()));
            }
        }
        super.onAsyncInitDone();
    }

    public void actionPerformed(ActionEvent e) {
        for (Archive archive : archives) {
            try {
                DummyArchive da = archive.createDummyArchive();

                DummyArchiveDialog d = new DummyArchiveDialog(da);

                try {
                    Dialog.getInstance().showDialog(d);
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }

            } catch (CheckException e1) {
                Dialog.getInstance().showExceptionDialog("Error", "Cannot Check Archive", e1);
            }
        }
    }

}
