package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionContextAction;
import org.jdownloader.gui.IconKey;

public class CleanupAutoDeleteFilesEnabledToggleAction extends AbstractExtractionContextAction {

    public CleanupAutoDeleteFilesEnabledToggleAction() {
        super();
        setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_autodeletefiles());
        setIconKey(IconKey.ICON_FILE);
        setSelected(false);

    }

    @Override
    protected void onAsyncInitDone() {
        super.onAsyncInitDone();
        if (archives != null && archives.size() > 0) setSelected(_getExtension().isRemoveFilesAfterExtractEnabled(archives.get(0)));
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        for (Archive archive : archives) {
            archive.getSettings().setRemoveFilesAfterExtraction(isSelected() ? BooleanStatus.TRUE : BooleanStatus.FALSE);
        }
        Dialog.getInstance().showMessageDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, isSelected() ? org.jdownloader.extensions.extraction.translate.T._.set_autoremovefiles_true() : org.jdownloader.extensions.extraction.translate.T._.set_autoremovefiles_false());

    }
}
