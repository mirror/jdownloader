package org.jdownloader.extensions.extraction;

import java.io.File;

import jd.plugins.DownloadLink.AvailableStatus;

import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkArchiveFile;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFile;

public class DummyArchiveFile {

    private String      name;
    private boolean     missing = false;
    private ArchiveFile archiveFile;
    private File        file;

    public ArchiveFile getArchiveFile() {
        return archiveFile;
    }

    public boolean isIncomplete() {
        if (file != null && file.exists()) return false;
        return archiveFile == null || !archiveFile.isComplete();
    }

    public boolean isMissing() {
        return missing;
    }

    public DummyArchiveFile setMissing(boolean exists) {
        this.missing = exists;
        return this;
    }

    public DummyArchiveFile(String miss, File folder) {
        name = miss;
        this.file = new File(folder, miss);
        setMissing(!file.exists());
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
            return ((CrawledLinkArchiveFile) archiveFile).getAvailableStatus();
        } else if (archiveFile instanceof DownloadLinkArchiveFile) {
            //
            return ((DownloadLinkArchiveFile) archiveFile).getAvailableStatus();
        } else if (archiveFile instanceof FileArchiveFile) {

            if (((FileArchiveFile) archiveFile).getFile().exists()) {
                return AvailableStatus.TRUE;
            } else {
                return AvailableStatus.FALSE;
            }
        }
        return AvailableStatus.UNCHECKED;
    }

    public boolean isLocalFileAvailable() {
        if (file != null && file.exists()) return true;
        if (archiveFile == null) return false;
        if (archiveFile instanceof CrawledLinkArchiveFile) {
            return ((CrawledLinkArchiveFile) archiveFile).existsLocalFile();
        } else if (archiveFile instanceof DownloadLinkArchiveFile) {
            //
            return new File(((DownloadLinkArchiveFile) archiveFile).getFilePath()).exists();
        } else if (archiveFile instanceof FileArchiveFile) { return ((FileArchiveFile) archiveFile).getFile().exists(); }
        return false;
    }

}
