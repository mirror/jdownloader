package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionContextAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;

public class SetExtractToAction extends AbstractExtractionContextAction {

    public SetExtractToAction() {
        super();
        setName(org.jdownloader.extensions.extraction.translate.T.T.contextmenu_extract_to());

        setIconKey(IconKey.ICON_FOLDER);

    }

    @Override
    protected void onAsyncInitDone() {
        super.onAsyncInitDone();

    }

    public void actionPerformed(ActionEvent e) {
        final List<Archive> lArchives = getArchives();
        if (!isEnabled() || lArchives == null) {
            return;
        } else {
            File extractto = _getExtension().getFinalExtractToFolder(lArchives.get(0), true);
            while (extractto != null && !extractto.isDirectory() && !isTag(extractto.getName())) {
                extractto = extractto.getParentFile();
            }
            try {
                File path = DownloadFolderChooserDialog.open(extractto, true, org.jdownloader.extensions.extraction.translate.T.T.extract_to2());
                if (path == null) {
                    return;
                }
                for (Archive archive : lArchives) {
                    archive.getSettings().setExtractPath(path.getAbsolutePath());
                }
            } catch (DialogClosedException e1) {
                e1.printStackTrace();
            } catch (DialogCanceledException e1) {
                e1.printStackTrace();
            }
        }
    }

    private boolean isTag(String name) {
        return name.matches(".*\\<.+\\>.*");
    }
}
