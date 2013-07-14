package org.jdownloader.extensions.extraction.bindings.crawledlink;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink.AvailableStatus;

import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionStatus;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;

public class CrawledLinkArchiveFile implements ArchiveFile {

    private java.util.List<CrawledLink> links;
    private String                      name;
    private long                        size;
    private Archive                     archive;

    public CrawledLinkArchiveFile(CrawledLink l) {
        links = new ArrayList<CrawledLink>();
        links.add(l);
        name = l.getName();
        size = l.getSize();

    }

    public java.util.List<CrawledLink> getLinks() {
        return links;
    }

    @Override
    public int hashCode() {
        return links.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CrawledLinkArchiveFile)) return false;
        // this equals is used by the build method of ExtractionExtension. If we have one matching link, the archivefile matches as well
        for (CrawledLink dl : ((CrawledLinkArchiveFile) obj).links) {
            if (links.contains(dl)) return true;

        }

        return false;
    }

    public boolean isComplete() {
        for (CrawledLink downloadLink : links) {
            if (isLinkComplete(downloadLink)) return true;
        }
        return false;
    }

    private boolean isLinkComplete(CrawledLink link) {
        switch (link.getLinkState()) {
        case OFFLINE:
            return false;
        default:
            return true;
        }
    }

    public String toString() {
        return getName();
    }

    public String getFilePath() {
        return name;
    }

    public boolean isValid() {
        return true;
    }

    public boolean deleteFile() {
        return false;
    }

    public boolean exists() {
        return false;
    }

    public String getName() {
        return name;
    }

    public void setStatus(ExtractionStatus error) {
    }

    public void setMessage(String plugins_optional_extraction_status_notenoughspace) {
    }

    public void setProgress(long value, long max, Color color) {
    }

    @Override
    public long getFileSize() {
        return Math.max(0, size);
    }

    public void addMirror(CrawledLink link) {
        links.add(link);
        size = Math.max(link.getSize(), size);

    }

    @Override
    public void deleteLink() {
    }

    public AvailableStatus getAvailableStatus() {
        for (CrawledLink downloadLink : links) {
            switch (downloadLink.getDownloadLink().getAvailableStatus()) {
            case TRUE:
                return downloadLink.getDownloadLink().getAvailableStatus();
            }
        }
        for (CrawledLink downloadLink : links) {
            switch (downloadLink.getDownloadLink().getAvailableStatus()) {
            case UNCHECKED:
                return downloadLink.getDownloadLink().getAvailableStatus();
            }
        }
        for (CrawledLink downloadLink : links) {
            switch (downloadLink.getDownloadLink().getAvailableStatus()) {
            case UNCHECKABLE:
                return downloadLink.getDownloadLink().getAvailableStatus();
            }
        }
        return links.get(0).getDownloadLink().getAvailableStatus();
    }

    public boolean existsLocalFile() {
        return new File(LinkTreeUtils.getDownloadDirectory(links.get(0)), links.get(0).getName()).exists();

    }

    @Override
    public void onCleanedUp(ExtractionController controller) {
    }

    @Override
    public void setArchive(Archive archive) {
        this.archive = archive;

        if (archive != null && archive.getFactory() != null) {
            for (CrawledLink link : links) {

                link.setArchiveID(archive.getFactory().getID());
            }
        }
    }

    public Archive getArchive() {
        return archive;
    }
}
