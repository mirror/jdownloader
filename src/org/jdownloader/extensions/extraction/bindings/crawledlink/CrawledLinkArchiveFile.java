package org.jdownloader.extensions.extraction.bindings.crawledlink;

import java.awt.Color;

import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.extensions.extraction.ArchiveFile;

public class CrawledLinkArchiveFile implements ArchiveFile {

    private CrawledLink link;

    public CrawledLinkArchiveFile(CrawledLink l) {
        this.link = l;
    }

    public CrawledLink getLink() {
        return link;
    }

    @Override
    public int hashCode() {
        return link.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CrawledLinkArchiveFile)) return false;
        return link.getName().equals(((CrawledLinkArchiveFile) obj).link.getName());
    }

    public boolean isComplete() {
        switch (link.getLinkState()) {
        case OFFLINE:
            return false;
        default:
            return true;
        }
    }

    public String toString() {
        return link.getName();
    }

    public String getFilePath() {
        return link.getName();
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
        return link.getName();
    }

    public void setStatus(Status error) {
    }

    public void setMessage(String plugins_optional_extraction_status_notenoughspace) {
    }

    public void setProgress(long value, long max, Color color) {
    }

    @Override
    public void deleteLink() {
    }

}
