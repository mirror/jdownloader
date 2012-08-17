package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import java.util.List;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class PrepareEntry {

    private FilePackage filePackage;

    public PrepareEntry(FilePackage fp) {
        filePackage = fp;
    }

    public List<DownloadLink> getLinks() {
        return filePackage.getChildren();
    }

}
