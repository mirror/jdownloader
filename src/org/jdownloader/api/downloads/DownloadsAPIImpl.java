package org.jdownloader.api.downloads;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.settings.GeneralSettings;

public class DownloadsAPIImpl implements DownloadsAPI {

    public List<FilePackageAPIStorable> list() {
        DownloadController dlc = DownloadController.getInstance();
        boolean b = dlc.readLock();
        try {
            ArrayList<FilePackageAPIStorable> ret = new ArrayList<FilePackageAPIStorable>(dlc.size());
            for (FilePackage fpkg : dlc.getPackages()) {
                FilePackageAPIStorable pkg;
                ret.add(pkg = new FilePackageAPIStorable(fpkg));
                synchronized (fpkg) {
                    List<DownloadLinkAPIStorable> links = new ArrayList<DownloadLinkAPIStorable>(fpkg.size());
                    for (DownloadLink link : fpkg.getChildren()) {
                        links.add(new DownloadLinkAPIStorable(link));
                    }
                    pkg.setLinks(links);
                }
            }
            return ret;
        } finally {
            dlc.readUnlock(b);
        }
    }

    public DownloadStatusAPIStorable status() {
        return new DownloadStatusAPIStorable();
    }

    public boolean stop() {
        DownloadWatchDog.getInstance().startDownloads();
        return true;
    }

    public boolean start() {
        DownloadWatchDog.getInstance().stopDownloads();
        return true;
    }

    public boolean reconnect() {
        try {
            ReconnectPluginController.getInstance().doReconnect();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean reconnectenabled(boolean enable) {
        org.jdownloader.settings.staticreferences.CFG_GENERAL.AUTO_RECONNECT_ENABLED.setValue(enable);
        return true;
    }

    public boolean speedlimit(boolean enable, int limit) {
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED.setValue(enable);
        org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT.setValue(limit);
        return true;
    }
}
