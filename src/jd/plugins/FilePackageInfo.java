package jd.plugins;

import java.util.HashSet;
import java.util.Iterator;

public class FilePackageInfo {

    private FilePackage fp                = null;
    protected long      structureVersion  = 0;
    protected long      lastIconVersion   = -1;

    protected long      statusVersion     = 0;
    protected long      lastStatusVersion = -1;

    protected FilePackageInfo(FilePackage fp) {
        this.fp = fp;
    }

    private PluginForHost[] icons = new PluginForHost[0];
    private long            size  = 0;

    public PluginForHost[] getIcons() {
        if (lastIconVersion == structureVersion) return icons;
        synchronized (this) {
            if (lastIconVersion == structureVersion) return icons;
            HashSet<PluginForHost> hosts = new HashSet<PluginForHost>();
            synchronized (fp) {
                for (DownloadLink link : fp.getChildren()) {
                    hosts.add(link.getDefaultPlugin());
                }
            }
            icons = new PluginForHost[hosts.size()];
            Iterator<PluginForHost> it = hosts.iterator();
            int i = 0;
            while (it.hasNext()) {
                icons[i++] = it.next();
            }
            lastIconVersion = structureVersion;
        }
        return icons;
    }

    public void changeStructure() {
        structureVersion++;
        statusVersion++;
    }

    public long getSize() {
        if (lastStatusVersion == statusVersion) return size;
        synchronized (this) {
            if (lastStatusVersion == statusVersion) return size;
            updateStatus();
        }
        return size;
    }

    private synchronized void updateStatus() {
        long newSize = 0;
        synchronized (fp) {
            for (DownloadLink link : fp.getChildren()) {
                if (!link.isEnabled()) continue;
                newSize += link.getDownloadSize();
            }
        }
        size = newSize;
        lastStatusVersion = statusVersion;
    }
}
