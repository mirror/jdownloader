package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;

import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.MissingArchiveFile;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkArchiveFile;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;

public class ArchiveFileSandbox {
    private final ArchiveFile archiveFile;

    public ArchiveFileSandbox(ArchiveFile archiveFile) {
        this.archiveFile = archiveFile;
    }

    public Boolean isComplete() {
        if (archiveFile != null) {
            return archiveFile.isComplete();
        } else {
            return null;
        }
    }

    public String getFilePath() {
        if (archiveFile != null) {
            return archiveFile.getFilePath();
        } else {
            return null;
        }
    }

    public FilePathSandbox getPath() {
        final String filePath = getFilePath();
        if (filePath != null) {
            return ScriptEnvironment.getPath(filePath);
        } else {
            return null;
        }
    }

    public boolean isMissingArchiveFile() {
        return archiveFile instanceof MissingArchiveFile;
    }

    public long getFileSize() {
        if (archiveFile != null) {
            return archiveFile.getFileSize();
        } else {
            return 0;
        }
    }

    public CrawledLinkSandbox[] getCrawledLinks() {
        if (archiveFile instanceof CrawledLinkArchiveFile) {
            final List<CrawledLinkSandbox> ret = new ArrayList<CrawledLinkSandbox>();
            for (final CrawledLink link : ((CrawledLinkArchiveFile) archiveFile).getLinks()) {
                ret.add(new CrawledLinkSandbox(link));
            }
            if (ret.size() > 0) {
                return ret.toArray(new CrawledLinkSandbox[0]);
            }
        }
        return null;
    }

    public DownloadLinkSandBox[] getDownloadLinks() {
        if (archiveFile instanceof DownloadLinkArchiveFile) {
            final List<DownloadLinkSandBox> ret = new ArrayList<DownloadLinkSandBox>();
            for (final DownloadLink link : ((DownloadLinkArchiveFile) archiveFile).getDownloadLinks()) {
                ret.add(new DownloadLinkSandBox(link));
            }
            if (ret.size() > 0) {
                return ret.toArray(new DownloadLinkSandBox[0]);
            }
        }
        return null;
    }

    public boolean exists() {
        return archiveFile != null && archiveFile.exists();
    }

    public boolean exists(boolean ignoreCache) {
        return archiveFile != null && archiveFile.exists(ignoreCache);
    }

    public void invalidateExists() {
        if (archiveFile != null) {
            archiveFile.invalidateExists();
        }
    }

    public String getName() {
        if (archiveFile != null) {
            return archiveFile.getName();
        } else {
            return null;
        }
    }

    public Boolean isPartOfAnArchive() {
        if (archiveFile != null) {
            return archiveFile.isPartOfAnArchive();
        } else {
            return null;
        }
    }

    public String getArchiveID() {
        if (archiveFile != null) {
            return archiveFile.getArchiveID();
        } else {
            return null;
        }
    }
}
