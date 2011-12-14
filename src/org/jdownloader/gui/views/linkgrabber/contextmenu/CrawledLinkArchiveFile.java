package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.Color;

import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.extensions.extraction.ArchiveFile;

public class CrawledLinkArchiveFile implements ArchiveFile {

    private CrawledLink link;

    public CrawledLinkArchiveFile(CrawledLink l) {
        this.link = l;
    }

    public boolean isComplete() {
        return true;
    }

    public String getFilePath() {
        return link.getName();
    }

    public boolean isValid() {
        return true;
    }

    public boolean delete() {
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

}
