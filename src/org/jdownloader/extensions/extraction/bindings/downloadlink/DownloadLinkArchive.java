package org.jdownloader.extensions.extraction.bindings.downloadlink;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jd.plugins.DownloadLink;

import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;

public class DownloadLinkArchive extends Archive {

    private java.util.List<DownloadLink> disabledLinks;

    public DownloadLinkArchive(ArchiveFactory link) {
        super(link);
    }

    /* TODO: change this to use conditionalSkipReason(avoid DownloadWatchDog to catchUp the link) while the archive is extracting */
    public void onControllerAssigned(ExtractionController extractionController) {
        disabledLinks = new CopyOnWriteArrayList<DownloadLink>();
        for (ArchiveFile af : getArchiveFiles()) {
            if (af instanceof DownloadLinkArchiveFile) {
                List<DownloadLink> dlinks = ((DownloadLinkArchiveFile) af).getDownloadLinks();
                for (DownloadLink dl : dlinks) {
                    if (dl.isEnabled() && dl.getFinalLinkState() == null) {
                        dl.setEnabled(false);
                        disabledLinks.add(dl);
                    }
                }
            }
        }
    }

    public void onStartExtracting() {
    }

    public void onCleanUp() {
        if (disabledLinks != null) {
            for (DownloadLink dl : disabledLinks) {
                dl.setEnabled(true);
            }
        }
    }
}
