package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionContextAction;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.extensions.extraction.gui.DummyArchiveDialog;
import org.jdownloader.extensions.extraction.multi.CheckException;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.IconKey;

public class ValidateArchivesAction extends AbstractExtractionContextAction {

    public ValidateArchivesAction() {
        super();
        setName(T._.ValidateArchiveAction_ValidateArchiveAction_object_());

        setIconKey(IconKey.ICON_VALIDATE_ARCHIVE);

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
        if (!isEnabled()) return;
        for (Archive archive : archives) {
            try {
                DummyArchive da = ArchiveValidator.EXTENSION.createDummyArchive(archive);

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
