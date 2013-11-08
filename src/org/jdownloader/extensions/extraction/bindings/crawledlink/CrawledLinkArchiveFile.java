package org.jdownloader.extensions.extraction.bindings.crawledlink;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkProperty;
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
        size = Math.max(0, l.getSize());
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
        if (obj == this) return true;
        // this equals is used by the build method of ExtractionExtension. If we have one matching link, the archivefile matches as well
        for (CrawledLink dl : ((CrawledLinkArchiveFile) obj).links) {
            if (links.contains(dl)) return true;
        }
        return false;
    }

    public boolean isComplete() {
        for (CrawledLink downloadLink : links) {
            switch (downloadLink.getLinkState()) {
            case ONLINE:
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return "CrawledLink: " + name + " Complete:" + isComplete();
    }

    public String getFilePath() {
        return name;
    }

    public void deleteFile() {
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
        size = Math.max(Math.max(0, link.getSize()), size);
    }

    @Override
    public void deleteLink() {
    }

    public AvailableStatus getAvailableStatus() {
        AvailableStatus ret = null;
        for (CrawledLink downloadLink : links) {
            switch (downloadLink.getLinkState()) {
            case ONLINE:
                return AvailableStatus.TRUE;
            case TEMP_UNKNOWN:
                ret = AvailableStatus.UNCHECKED;
                break;
            case UNKNOWN:
                if (ret != AvailableStatus.UNCHECKED) ret = AvailableStatus.UNCHECKABLE;
                break;
            case OFFLINE:
                if (ret == null) ret = AvailableStatus.FALSE;
                break;
            }
        }
        return ret;
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

    @Override
    public boolean exists() {
        return new File(LinkTreeUtils.getDownloadDirectory(links.get(0)), name).exists();
    }

    @Override
    public void notifyChanges(Object type) {

        for (CrawledLink link : getLinks()) {
            link.firePropertyChanged(CrawledLinkProperty.Property.ARCHIVE, type);
        }

    }
}
