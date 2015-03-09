package org.jdownloader.extensions.extraction.bindings.crawledlink;

import java.awt.Color;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkProperty;
import jd.plugins.DownloadLink.AvailableStatus;

import org.jdownloader.controlling.FileCreationManager.DeleteOption;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionStatus;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;

public class CrawledLinkArchiveFile implements ArchiveFile {

    private final List<CrawledLink>        links;

    private final String                   name;
    private volatile long                  size;
    private final AtomicReference<Boolean> exists = new AtomicReference<Boolean>(null);
    private final int                      hashCode;

    public CrawledLinkArchiveFile(CrawledLink l) {
        links = new CopyOnWriteArrayList<CrawledLink>();
        links.add(l);
        name = l.getName();
        size = Math.max(0, l.getSize());
        hashCode = (getClass() + name).hashCode();
    }

    public java.util.List<CrawledLink> getLinks() {
        return links;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null) {
            if (obj instanceof CrawledLinkArchiveFile) {
                for (CrawledLink dl : ((CrawledLinkArchiveFile) obj).getLinks()) {
                    if (getLinks().contains(dl)) {
                        return true;
                    }
                }
            } else if (obj instanceof CrawledLink) {
                if (getLinks().contains(obj)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isComplete() {
        for (CrawledLink downloadLink : getLinks()) {
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

    public void deleteFile(DeleteOption option) {
        invalidateExists();
    }

    public String getName() {
        return name;
    }

    public void setStatus(ExtractionController controller, ExtractionStatus error) {
    }

    public void setMessage(ExtractionController controller, String plugins_optional_extraction_status_notenoughspace) {
    }

    public void setProgress(ExtractionController controller, long value, long max, Color color) {
    }

    @Override
    public long getFileSize() {
        if (exists()) {
            return Math.max(size, new File(LinkTreeUtils.getDownloadDirectory(getLinks().get(0)), getName()).length());
        }
        return Math.max(0, size);
    }

    public void addMirror(CrawledLink link) {
        getLinks().add(link);
        size = Math.max(Math.max(0, link.getSize()), size);
    }

    public AvailableStatus getAvailableStatus() {
        AvailableStatus ret = null;
        for (CrawledLink downloadLink : getLinks()) {
            switch (downloadLink.getLinkState()) {
            case ONLINE:
                return AvailableStatus.TRUE;
            case TEMP_UNKNOWN:
                ret = AvailableStatus.UNCHECKED;
                break;
            case UNKNOWN:
                if (ret != AvailableStatus.UNCHECKED) {
                    ret = AvailableStatus.UNCHECKABLE;
                }
                break;
            case OFFLINE:
                if (ret == null) {
                    ret = AvailableStatus.FALSE;
                }
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
        if (archive != null && archive.getFactory() != null) {
            for (CrawledLink link : getLinks()) {
                link.setArchiveID(archive.getFactory().getID());
            }
        }
    }

    @Override
    public boolean exists() {
        Boolean ret = exists.get();
        if (ret == null) {
            ret = new File(LinkTreeUtils.getDownloadDirectory(getLinks().get(0)), getName()).exists();
            exists.compareAndSet(null, ret);
        }
        return ret;
    }

    protected void setExists(boolean b) {
        exists.set(b);
    }

    @Override
    public void notifyChanges(Object type) {
        for (CrawledLink link : getLinks()) {
            link.firePropertyChanged(CrawledLinkProperty.Property.ARCHIVE, type);
        }
    }

    @Override
    public void removePluginProgress(ExtractionController controller) {
    }

    @Override
    public void invalidateExists() {
        exists.set(null);
    }
}
