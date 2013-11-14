package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;
import java.io.File;

import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionContextAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;

public class SetExtractToAction extends AbstractExtractionContextAction {

    public SetExtractToAction() {
        super();
        setName(org.jdownloader.extensions.extraction.translate.T._.contextmenu_extract_to());
        setIconKey(IconKey.ICON_FOLDER);

    }

    @Override
    protected void onAsyncInitDone() {
        super.onAsyncInitDone();

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        File extractto = _getExtension().getFinalExtractToFolder(archives.get(0));

        while (extractto != null && !extractto.isDirectory()) {
            extractto = extractto.getParentFile();
        }
        try {
            File path = DownloadFolderChooserDialog.open(extractto, false, org.jdownloader.extensions.extraction.translate.T._.extract_to2());
            if (path == null) return;
            for (Archive archive : archives) {
                archive.getSettings().setExtractPath(path.getAbsolutePath());
            }
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }
}
