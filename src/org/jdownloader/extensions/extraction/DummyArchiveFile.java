package org.jdownloader.extensions.extraction;

import java.io.File;

import jd.plugins.DownloadLink.AvailableStatus;

import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkArchiveFile;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;

public class DummyArchiveFile {

    private String      name;
    private boolean     missing = false;
    private ArchiveFile archiveFile;

    public ArchiveFile getArchiveFile() {
        return archiveFile;
    }

    public boolean isIncomplete() {
        return archiveFile == null || !archiveFile.isComplete();
    }

    public boolean isMissing() {
        return missing;
    }

    public DummyArchiveFile setMissing(boolean exists) {
        this.missing = exists;
        return this;
    }

    public DummyArchiveFile(String miss) {
        name = miss;
        setMissing(true);
    }

    public String toString() {
        return name;
    }

    public DummyArchiveFile(ArchiveFile af) {
        name = af.getName();
        archiveFile = af;

    }

    public String getName() {
        return name;
    }

    public boolean isOnline() {
        return false;
    }

    public AvailableStatus getOnlineStatus() {
        if (archiveFile == null) return AvailableStatus.UNCHECKED;
        if (archiveFile instanceof CrawledLinkArchiveFile) {
            return ((CrawledLinkArchiveFile) archiveFile).getLink().getDownloadLink().getAvailableStatus();
        } else if (archiveFile instanceof DownloadLinkArchiveFile) {
            //
            return ((DownloadLinkArchiveFile) archiveFile).getDownloadLink().getAvailableStatus();
        }
        return AvailableStatus.UNCHECKED;
    }

    public boolean isLocalFileAvailable() {
        if (archiveFile == null) return false;
        if (archiveFile instanceof CrawledLinkArchiveFile) {
            return new File(((CrawledLinkArchiveFile) archiveFile).getLink().getDownloadLink().getFileOutput()).exists();
        } else if (archiveFile instanceof DownloadLinkArchiveFile) {
            //
            return new File(((DownloadLinkArchiveFile) archiveFile).getDownloadLink().getFileOutput()).exists();
        }
        return false;
    }

}
