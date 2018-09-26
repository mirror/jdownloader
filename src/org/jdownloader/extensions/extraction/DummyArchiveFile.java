package org.jdownloader.extensions.extraction;

import java.io.File;

import jd.plugins.DownloadLink.AvailableStatus;

import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkArchiveFile;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFile;

public class DummyArchiveFile {
    private final String      name;
    private final boolean     missing;
    private final ArchiveFile archiveFile;

    public ArchiveFile getArchiveFile() {
        return archiveFile;
    }

    public Boolean isIncomplete() {
        if (archiveFile == null) {
            return Boolean.TRUE;
        } else {
            final Boolean complete = archiveFile.isComplete();
            if (complete == null) {
                return null;
            }
            return complete.booleanValue() ? Boolean.FALSE : Boolean.TRUE;
        }
    }

    public boolean isMissing() {
        return missing;
    }

    public DummyArchiveFile(String miss, File folder) {
        name = miss;
        missing = true;
        this.archiveFile = null;
    }

    public String toString() {
        if (archiveFile != null) {
            return archiveFile.toString();
        } else {
            return name;
        }
    }

    public DummyArchiveFile(ArchiveFile af) {
        name = af.getName();
        if (af instanceof MissingArchiveFile) {
            missing = true;
        } else {
            missing = false;
        }
        archiveFile = af;
    }

    public String getName() {
        return name;
    }

    public AvailableStatus getOnlineStatus() {
        if (archiveFile != null) {
            if (archiveFile instanceof CrawledLinkArchiveFile) {
                return ((CrawledLinkArchiveFile) archiveFile).getAvailableStatus();
            } else if (archiveFile instanceof DownloadLinkArchiveFile) {
                return ((DownloadLinkArchiveFile) archiveFile).getAvailableStatus();
            } else if (archiveFile instanceof FileArchiveFile) {
                if (((FileArchiveFile) archiveFile).exists()) {
                    return AvailableStatus.TRUE;
                } else {
                    return AvailableStatus.FALSE;
                }
            }
        }
        return AvailableStatus.UNCHECKED;
    }

    public boolean isLocalFileAvailable() {
        return archiveFile != null && archiveFile.exists();
    }
}
