package org.jdownloader.api.jdanywhere.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollector.MoveLinksMode;
import jd.controlling.linkcollector.LinkCollector.MoveLinksSettings;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;

import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.jdownloader.api.jdanywhere.api.interfaces.ILinkCrawlerApi;
import org.jdownloader.api.jdanywhere.api.storable.CrawledLinkStoreable;
import org.jdownloader.api.jdanywhere.api.storable.CrawledPackageStorable;
import org.jdownloader.api.linkcollector.LinkCollectorAPIImpl;
import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.views.SelectionInfo;

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
            for (CrawledPackage cpkg : lc.getPackages()) {
                if (cpkg.getUniqueID().getID() == ID) {
                    final boolean readL = cpkg.getModifyLock().readLock();
                    try {
                        final List<CrawledLinkStoreable> links = new ArrayList<CrawledLinkStoreable>(cpkg.getChildren().size());
                        for (CrawledLink link : cpkg.getChildren()) {
                            links.add(new CrawledLinkStoreable(link));
                        }
                        return links;
                    } finally {
                        cpkg.getModifyLock().readUnlock(readL);
                    }
                }
            }
            return null;
        } finally {
            lc.readUnlock(b);
        }
    }

    public CrawledPackageStorable getCrawledPackage(long crawledPackageID) {
        CrawledPackage cpkg = getCrawledPackageFromID(crawledPackageID);
        if (cpkg != null) {
            final boolean readL = cpkg.getModifyLock().readLock();
            try {
                final CrawledPackageStorable pkg = new CrawledPackageStorable(cpkg);
                List<CrawledLinkStoreable> links = new ArrayList<CrawledLinkStoreable>(cpkg.getChildren().size());
                for (CrawledLink link : cpkg.getChildren()) {
                    links.add(new CrawledLinkStoreable(link));
                }
                pkg.setLinks(links);
                return pkg;
            } finally {
                cpkg.getModifyLock().readUnlock(readL);
            }
        }
        return null;
    }

    public String getPackageIDFromLinkID(long ID) {
        final CrawledLink dl = getCrawledLinkFromID(ID);
        if (dl != null) {
            final CrawledPackage fpk = dl.getParentNode();
            if (fpk != null) {
                return fpk.getUniqueID().toString();
            }
        }
        return null;
    }

    public CrawledLinkStoreable getCrawledLink(long crawledLinkID) {
        CrawledLink link = getCrawledLinkFromID(crawledLinkID);
        if (link != null) {
            return new CrawledLinkStoreable(link);
        }
        return null;
    }

    public boolean AddCrawledPackageToDownloads(long crawledPackageID) {
        final CrawledPackage cp = getCrawledPackageFromID(crawledPackageID);
        if (cp != null) {
            LinkCollector.getInstance().moveLinksToDownloadList(new MoveLinksSettings(MoveLinksMode.MANUAL, null, null, null), new SelectionInfo<CrawledPackage, CrawledLink>(cp));
            return true;
        }
        return false;
    }

    public boolean addCrawledLinkToDownloads(List<Long> linkIds) {
        return lcAPI.startDownloads(linkIds);
    }

    public boolean addDLC(String dlcContent) {
        try {
            if (dlcContent == null) {
                throw new IllegalArgumentException("no DLC Content available");
            }
            final String dlc = dlcContent.trim().replace(" ", "+");
            final File tmp = Application.getTempResource("jd_" + System.currentTimeMillis() + ".dlc");
            IO.writeToFile(tmp, dlc.getBytes("UTF-8"));
            final String url = tmp.toURI().toString();
            LinkCollectingJob job = new LinkCollectingJob(LinkOrigin.MYJD.getLinkOriginDetails(), url);
            LinkCollector.getInstance().addCrawlerJob(job);
        } catch (Throwable e) {
        }
        return true;
    }

    public boolean removeCrawledLink(List<Long> linkIds) {
        return lcAPI.removeLinks(linkIds);
    }

    public boolean removeCrawledPackage(String ID) {
        long id = Long.valueOf(ID);
        CrawledPackage cpkg = getCrawledPackageFromID(id);
        if (cpkg != null) {
            LinkCollector.getInstance().removePackage(cpkg);
            return true;
        }
        return false;
    }

    private CrawledPackage getCrawledPackageFromID(long ID) {
        LinkCollector lc = LinkCollector.getInstance();
        boolean b = lc.readLock();
        try {
            for (CrawledPackage cpkg : lc.getPackages()) {
                if (cpkg.getUniqueID().getID() == ID) {
                    return cpkg;
                }
            }
            return null;
        } finally {
            lc.readUnlock(b);
        }
    }

    private CrawledLink getCrawledLinkFromID(long ID) {
        LinkCollector lc = LinkCollector.getInstance();
        final boolean b = lc.readLock();
        try {
            for (CrawledPackage cpkg : lc.getPackages()) {
                final boolean readL = cpkg.getModifyLock().readLock();
                try {
                    for (CrawledLink link : cpkg.getChildren()) {
                        if (link.getDownloadLink().getUniqueID().getID() == ID) {
                            return link;
                        }
                    }

                } finally {
                    cpkg.getModifyLock().readUnlock(readL);
                }
            }
            return null;
        } finally {
            lc.readUnlock(b);
        }
    }

    public boolean setCrawledLinkPriority(final List<Long> linkIds, int priority) {
        if (linkIds != null && linkIds.size() > 0) {
            final int size = linkIds.size();
            final List<CrawledLink> lks = LinkCollector.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {
                @Override
                public int returnMaxResults() {
                    return size;
                }

                @Override
                public boolean acceptNode(CrawledLink node) {
                    return linkIds.contains(node.getUniqueID().getID());
                }
            });
            if (lks.size() > 0) {
                for (CrawledLink cl : lks) {
                    setPriority(priority, cl);
                }
                return true;
            }
        }
        return false;
    }

    public boolean setCrawledPackagePriority(long ID, final int priority) {
        final CrawledPackage cp = getCrawledPackageFromID(ID);
        if (cp != null) {
            cp.getModifyLock().runReadLock(new Runnable() {

                @Override
                public void run() {
                    for (CrawledLink link : cp.getChildren()) {
                        setPriority(priority, link);
                    }
                }
            });
            return true;
        }
        return false;
    }

    public boolean setCrawledLinkEnabled(final List<Long> linkIds, boolean enabled) {
        if (linkIds != null && linkIds.size() > 0) {
            final int size = linkIds.size();
            List<CrawledLink> lks = LinkCollector.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {
                @Override
                public int returnMaxResults() {
                    return size;
                }

                @Override
                public boolean acceptNode(CrawledLink node) {
                    return linkIds.contains(node.getUniqueID().getID());
                }
            });
            if (lks.size() > 0) {
                for (CrawledLink cl : lks) {
                    cl.setEnabled(enabled);
                }
                return true;
            }
        }
        return false;
    }

    public boolean setCrawledPackageEnabled(long ID, final boolean enabled) {
        final CrawledPackage cp = getCrawledPackageFromID(ID);
        if (cp != null) {
            cp.getModifyLock().runReadLock(new Runnable() {

                @Override
                public void run() {
                    for (CrawledLink link : cp.getChildren()) {
                        link.setEnabled(enabled);
                    }
                }
            });
            return true;
        }
        return true;
    }

    private void setPriority(int priority, CrawledLink cl) {
        cl.setPriority(Priority.getPriority(priority));
    }

    public boolean CrawlLink(String URL) {
        return lcAPI.addLinks(URL, "", "", "");
    }

    public boolean enableCrawledLink(final List<Long> linkIds, boolean enabled) {
        if (linkIds != null && linkIds.size() > 0) {
            final int size = linkIds.size();
            final List<CrawledLink> lks = LinkCollector.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {
                @Override
                public int returnMaxResults() {
                    return size;
                }

                @Override
                public boolean acceptNode(CrawledLink node) {
                    return linkIds.contains(node.getUniqueID().getID());
                }
            });
            if (lks.size() > 0) {
                for (CrawledLink cl : lks) {
                    cl.setEnabled(enabled);
                }
                return true;
            }
        }
        return false;

    }

}
