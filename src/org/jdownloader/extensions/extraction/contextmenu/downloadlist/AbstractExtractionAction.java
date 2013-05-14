package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import java.util.List;

import jd.controlling.IOEQ;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.AbstractExtensionAction;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.gui.views.SelectionInfo;

public abstract class AbstractExtractionAction extends AbstractExtensionAction<ExtractionExtension> {

    private SelectionInfo<?, ?> selection;
    protected List<Archive>     archives;

    public AbstractExtractionAction(SelectionInfo<?, ?> selection) {

        this.selection = selection;

    }

    protected void onAsyncInitDone() {
        if (archives != null && archives.size() > 0) {
            setEnabled(true);
        }
    }

    public void setEnabled(boolean newValue) {

        if (!newValue && selection != null) {
            IOEQ.add(new Runnable() {

                @Override
                public void run() {
                    asynchInit();

                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            onAsyncInitDone();

                        }

                    };

                }

            });
        }
        super.setEnabled(newValue);
    }

    protected void asynchInit() {
        archives = ArchiveValidator.validate((SelectionInfo<FilePackage, DownloadLink>) selection).getArchives();
    }

}
