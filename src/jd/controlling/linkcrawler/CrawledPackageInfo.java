package jd.controlling.linkcrawler;

import java.util.HashSet;
import java.util.Iterator;

import jd.plugins.PluginForHost;

public class CrawledPackageInfo {
    private CrawledPackage fp               = null;
    protected long         structureVersion = 0;
    protected long         lastIconVersion  = -1;

    protected CrawledPackageInfo(CrawledPackage fp) {
        this.fp = fp;
    }

    private PluginForHost[] icons = new PluginForHost[0];

    public PluginForHost[] getIcons() {
        if (lastIconVersion == structureVersion) return icons;
        synchronized (this) {
            if (lastIconVersion == structureVersion) return icons;
            System.out.println("create new iconlist");
            HashSet<PluginForHost> hosts = new HashSet<PluginForHost>();
            synchronized (fp) {
                for (CrawledLink link : fp.getChildren()) {
                    hosts.add(link.gethPlugin());
                }
            }
            icons = new PluginForHost[hosts.size()];
            Iterator<PluginForHost> it = hosts.iterator();
            int i = 0;
            while (it.hasNext()) {
                PluginForHost n = it.next();
                if (n != null) icons[i++] = n;
            }
            lastIconVersion = structureVersion;
        }
        return icons;
    }
}
