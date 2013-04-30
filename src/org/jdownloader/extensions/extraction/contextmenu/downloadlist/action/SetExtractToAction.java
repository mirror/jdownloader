package org.jdownloader.extensions.extraction.contextmenu.downloadlist.action;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.filechooser.FileFilter;

import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.AbstractExtractionAction;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.gui.views.SelectionInfo;

public class SetExtractToAction extends AbstractExtractionAction {

    public SetExtractToAction(final SelectionInfo<?, ?> selection) {
        super(selection);
        setName(_.contextmenu_extract_to());
        setSmallIcon(merge("folder", "edit"));

        setEnabled(false);

    }

    @Override
    protected void onAsyncInitDone() {
        super.onAsyncInitDone();
        if (archives!=null&&archives.size() > 0) setSelected(_getExtension().isAutoExtractEnabled(archives.get(0)));

    }

    public void actionPerformed(ActionEvent e) {

        FileFilter ff = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) return true;
                return false;
            }

            @Override
            public String getDescription() {
                return _.plugins_optional_extraction_filefilter_extractto();
            }
        };
        ArchiveFactory dlAF = archives.get(0).getFactory();
        File extractto = _getExtension().getFinalExtractToFolder(archives.get(0));

        while (extractto != null && !extractto.isDirectory()) {
            extractto = extractto.getParentFile();
        }
        try {
            File path = DownloadFolderChooserDialog.open(extractto, false, _.extract_to2());

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
