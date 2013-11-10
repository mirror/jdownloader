package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.util.List;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.AbstractExtensionAction;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public abstract class AbstractExtractionAction extends AbstractExtensionAction<ExtractionExtension> {

    protected List<Archive> archives;

    public AbstractExtractionAction() {
        super();
        if (getSelection() != null && !getSelection().isEmpty()) {
            Thread thread = new Thread() {
                public void run() {
                    asynchInit();
                    new EDTRunner() {
                        @Override
                        protected void runInEDT() {

                            onAsyncInitDone();
                        }
                    };
                };
            };
            thread.setDaemon(true);
            thread.setName("SetEnabled: " + getClass().getName());
            thread.start();
        }
    }

    protected void onAsyncInitDone() {
        if (archives != null && archives.size() > 0) {
            super.setEnabled(true);
        } else {
            super.setEnabled(false);
        }

    }

    public void setEnabled(boolean newValue) {

        super.setEnabled(newValue);
    }

    private SelectionInfo<FilePackage, DownloadLink> getSelection() {
        return DownloadsTable.getInstance().getSelectionInfo(true, true);
    }

    protected void asynchInit() {
        archives = ArchiveValidator.validate(getSelection()).getArchives();
    }

}
