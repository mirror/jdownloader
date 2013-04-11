package org.jdownloader.extensions.jdanywhere.api;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.FilePackage;

import org.jdownloader.api.linkcollector.LinkCollectorAPIImpl;
import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.jdanywhere.api.interfaces.ILinkCrawlerApi;
import org.jdownloader.extensions.jdanywhere.api.storable.CrawledLinkStoreable;
import org.jdownloader.extensions.jdanywhere.api.storable.CrawledPackageStorable;

public class LinkCrawlerApi implements ILinkCrawlerApi {

    LinkCollectorAPIImpl lcAPI = new LinkCollectorAPIImpl();

    public LinkCrawlerApi() {

    }

    public List<CrawledPackageStorable> list() {
        LinkCollector lc = LinkCollector.getInstance();
        boolean b = lc.readLock();
        try {
            java.util.List<CrawledPackageStorable> ret = new ArrayList<CrawledPackageStorable>(lc.size());
            for (CrawledPackage cpkg : lc.getPackages()) {
                CrawledPackageStorable pkg;
                ret.add(pkg = new CrawledPackageStorable(cpkg));
                // synchronized (cpkg) {
                // List<CrawledLinkStoreable> links = new ArrayList<CrawledLinkStoreable>(cpkg.getChildren().size());
                // for (CrawledLink link : cpkg.getChildren()) {
                // links.add(new CrawledLinkStoreable(link));
                // }
                // pkg.setLinks(links);
                // }
            }
            return ret;
        } finally {
            lc.readUnlock(b);
        }
    }

    public List<CrawledLinkStoreable> listLinks(long ID) {
        LinkCollector lc = LinkCollector.getInstance();
        boolean b = lc.readLock();
        try {
            List<CrawledLinkStoreable> links = null;
            for (CrawledPackage cpkg : lc.getPackages()) {
                if (cpkg.getUniqueID().getID() == ID) {
                    synchronized (cpkg) {
                        links = new ArrayList<CrawledLinkStoreable>(cpkg.getChildren().size());
                        for (CrawledLink link : cpkg.getChildren()) {
                            links.add(new CrawledLinkStoreable(link));
                        }
                        break;
                    }
                }
            }
            return links;

        } finally {
            lc.readUnlock(b);
        }
    }

    public CrawledPackageStorable getCrawledPackage(long crawledPackageID) {
        CrawledPackage cpkg = getCrawledPackageFromID(crawledPackageID);
        CrawledPackageStorable pkg = new CrawledPackageStorable(cpkg);
        List<CrawledLinkStoreable> links = new ArrayList<CrawledLinkStoreable>(cpkg.getChildren().size());
        for (CrawledLink link : cpkg.getChildren()) {
            links.add(new CrawledLinkStoreable(link));
        }
        pkg.setLinks(links);
        return pkg;
    }

    public String getPackageIDFromLinkID(long ID) {
        CrawledLink dl = getCrawledLinkFromID(ID);
        CrawledPackage fpk = dl.getParentNode();
        return fpk.getUniqueID().toString();
    }

    public CrawledLinkStoreable getCrawledLink(long crawledLinkID) {
        CrawledLink link = getCrawledLinkFromID(crawledLinkID);
        return new CrawledLinkStoreable(link);
    }

    public boolean AddCrawledPackageToDownloads(long crawledPackageID) {
        CrawledPackage cp = getCrawledPackageFromID(crawledPackageID);
        if (cp != null) {
            java.util.List<FilePackage> fpkgs = new ArrayList<FilePackage>();
            List<CrawledLink> links = new ArrayList<CrawledLink>(cp.getView().getItems());
            java.util.List<FilePackage> packages = LinkCollector.getInstance().convert(links, true);
            if (packages != null) fpkgs.addAll(packages);
            /* add the converted FilePackages to DownloadController */
            DownloadController.getInstance().addAllAt(fpkgs, -(fpkgs.size() + 10));
        }
        return true;
    }

    public boolean addCrawledLinkToDownloads(List<Long> linkIds) {
        return lcAPI.startDownloads(linkIds);
    }

    public boolean removeCrawledLink(List<Long> linkIds) {
        return lcAPI.removeLinks(linkIds);
    }

    public boolean removeCrawledPackage(String ID) {
        long id = Long.valueOf(ID);
        CrawledPackage cpkg = getCrawledPackageFromID(id);
        if (cpkg != null) {
            LinkCollector.getInstance().removePackage(cpkg);
        }
        return true;
    }

    private CrawledPackage getCrawledPackageFromID(long ID) {
        LinkCollector lc = LinkCollector.getInstance();
        boolean b = lc.readLock();
        try {
            for (CrawledPackage cpkg : lc.getPackages()) {
                if (cpkg.getUniqueID().getID() == ID) { return cpkg; }
            }
            return null;
        } finally {
            lc.readUnlock(b);
        }
    }

    private CrawledLink getCrawledLinkFromID(long ID) {
        LinkCollector lc = LinkCollector.getInstance();
        boolean b = lc.readLock();
        try {
            for (CrawledPackage cpkg : lc.getPackages()) {
                synchronized (cpkg) {
                    for (CrawledLink link : cpkg.getChildren()) {
                        if (link.getDownloadLink().getUniqueID().getID() == ID) { return link; }
                    }
                }
            }
            return null;
        } finally {
            lc.readUnlock(b);
        }
    }

    public boolean setCrawledLinkPriority(final List<Long> linkIds, int priority) {
        LinkCollector lc = LinkCollector.getInstance();
        lc.writeLock();
        try {
            List<CrawledLink> lks = lc.getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {
                @Override
                public int returnMaxResults() {
                    return -1;
                }

                @Override
                public boolean acceptNode(CrawledLink node) {
                    return linkIds != null && linkIds.contains(node.getUniqueID().getID());
                }
            });

            for (CrawledLink cl : lks) {
                setPriority(priority, cl);
            }
        } finally {
            lc.writeUnlock();
        }
        return true;
    }

    public boolean setCrawledPackagePriority(long ID, int priority) {
        CrawledPackage cp = getCrawledPackageFromID(ID);
        if (cp != null) {
            synchronized (cp) {
                for (CrawledLink link : cp.getChildren()) {
                    setPriority(priority, link);
                }
            }
        }
        return true;
    }

    public boolean setCrawledLinkEnabled(final List<Long> linkIds, boolean enabled) {
        LinkCollector lc = LinkCollector.getInstance();
        lc.writeLock();
        try {
            List<CrawledLink> lks = lc.getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {
                @Override
                public int returnMaxResults() {
                    return -1;
                }

                @Override
                public boolean acceptNode(CrawledLink node) {
                    return linkIds != null && linkIds.contains(node.getUniqueID().getID());
                }
            });

            for (CrawledLink cl : lks) {
                cl.setEnabled(enabled);
            }
        } finally {
            lc.writeUnlock();
        }
        return true;
    }

    public boolean setCrawledPackageEnabled(long ID, boolean enabled) {
        CrawledPackage cp = getCrawledPackageFromID(ID);
        if (cp != null) {
            synchronized (cp) {
                for (CrawledLink link : cp.getChildren()) {
                    link.setEnabled(enabled);
                }
            }
        }
        return true;
    }

    private void setPriority(int priority, CrawledLink cl) {
        switch (priority) {
        case -1:
            cl.setPriority(Priority.LOWER);
            break;
        case 0:
            cl.setPriority(Priority.DEFAULT);
            break;
        case 1:
            cl.setPriority(Priority.HIGH);
            break;
        case 2:
            cl.setPriority(Priority.HIGHER);
            break;
        case 3:
            cl.setPriority(Priority.HIGHEST);
            break;
        }
    }

    public boolean CrawlLink(String URL) {
        return lcAPI.addLinks(URL, "", "", "");
    }

    public boolean enableCrawledLink(final List<Long> linkIds, boolean enabled) {
        LinkCollector lc = LinkCollector.getInstance();

        lc.writeLock();
        try {
            List<CrawledLink> lks = lc.getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {
                @Override
                public int returnMaxResults() {
                    return -1;
                }

                @Override
                public boolean acceptNode(CrawledLink node) {
                    return linkIds != null && linkIds.contains(node.getUniqueID().getID());
                }
            });

            for (CrawledLink cl : lks) {
                cl.setEnabled(enabled);
            }
        } finally {
            lc.writeUnlock();
        }
        return true;

    }

}
