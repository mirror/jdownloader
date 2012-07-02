package org.jdownloader.extensions.extraction.bindings.downloadlink;

import java.util.ArrayList;
import java.util.List;

import jd.plugins.DownloadLink;

import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFactory;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.translate.T;

public class DownloadLinkArchive extends Archive {

    private ArrayList<DownloadLink> disabledLinks;

    public DownloadLinkArchive(ArchiveFactory link) {
        super(link);
    }

    public void onControllerAssigned(ExtractionController extractionController) {
        disabledLinks = new ArrayList<DownloadLink>();
        for (ArchiveFile af : getArchiveFiles()) {
            if (af instanceof DownloadLinkArchiveFile) {
                List<DownloadLink> dlinks = ((DownloadLinkArchiveFile) af).getDownloadLinks();
                for (DownloadLink dl : dlinks) {
                    if (dl.isEnabled() && !dl.getLinkStatus().isFinished()) {
                        dl.getLinkStatus().setStatusText(T._.extracting());
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
                if (dl.getLinkStatus().getStatusText() != null && dl.getLinkStatus().getStatusText().equals(T._.extracting())) {
                    dl.getLinkStatus().setStatusText(null);
                }
                dl.setEnabled(true);
            }
        }
    }
}
