package org.jdownloader.api.downloads;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class DownloadsAPIImpl implements DownloadsAPI {

    public List<FilePackageStorable> list() {
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        try {
            ArrayList<FilePackageStorable> ret = new ArrayList<FilePackageStorable>(dlc.size());
            for (FilePackage fpkg : dlc.getPackages()) {
                FilePackageStorable pkg;
                ret.add(pkg = new FilePackageStorable(fpkg));
                synchronized (fpkg) {
                    List<DownloadLinkStorable> links = new ArrayList<DownloadLinkStorable>(fpkg.size());
                    for (DownloadLink link : fpkg.getChildren()) {
                        links.add(new DownloadLinkStorable(link));
                    }
                    pkg.setLinks(links);
                }
            }
            return ret;
        } finally {
            dlc.readUnlock(b);
        }
    }

}
