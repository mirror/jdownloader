package org.jdownloader.controlling;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public interface DownloadLinkWalker {
    public boolean accept(DownloadLink link);

    public boolean accept(FilePackage fp);

    public void handle(DownloadLink link);
}
