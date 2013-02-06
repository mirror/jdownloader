package org.jdownloader.extensions.oliverremoteapi.api.linkcollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorApiEvent;
import jd.controlling.linkcollector.LinkCollectorApiEventListener;
import jd.controlling.linkcollector.LinkCollectorApiEventSender;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.plugins.FilePackage;

import org.appwork.remoteapi.EventsAPIEvent;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.linkcollector.LinkCollectorAPIImpl;

public class LinkCollectorMobileAPIImpl implements LinkCollectorMobileAPI, LinkCollectorApiEventListener {

    LinkCollectorAPIImpl lcAPI = new LinkCollectorAPIImpl();

    public LinkCollectorMobileAPIImpl() {
        LinkCollectorApiEventSender.getInstance().addListener(this);
    }

    public List<CrawledPackageAPIStorable> list() {
        LinkCollector lc = LinkCollector.getInstance();
        boolean b = lc.readLock();
        try {
            java.util.List<CrawledPackageAPIStorable> ret = new ArrayList<CrawledPackageAPIStorable>(lc.size());
            for (CrawledPackage cpkg : lc.getPackages()) {
                CrawledPackageAPIStorable pkg;
                ret.add(pkg = new CrawledPackageAPIStorable(cpkg));
                synchronized (cpkg) {
                    List<CrawledLinkAPIStorable> links = new ArrayList<CrawledLinkAPIStorable>(cpkg.getChildren().size());
                    for (CrawledLink link : cpkg.getChildren()) {
                        links.add(new CrawledLinkAPIStorable(link));
                    }
                    pkg.setLinks(links);
                }
            }
            return ret;
        } finally {
            lc.readUnlock(b);
        }
    }

    public CrawledPackageAPIStorable getCrawledPackage(long crawledPackageID) {
        CrawledPackage cpkg = getCrawledPackageFromID(crawledPackageID);
        CrawledPackageAPIStorable pkg = new CrawledPackageAPIStorable(cpkg);
        List<CrawledLinkAPIStorable> links = new ArrayList<CrawledLinkAPIStorable>(0);
        pkg.setLinks(links);
        return pkg;
    }

    public String getPackageIDFromLinkID(long ID) {
        CrawledLink dl = getCrawledLinkFromID(ID);
        CrawledPackage fpk = dl.getParentNode();
        return fpk.getUniqueID().toString();
    }

    public CrawledLinkAPIStorable getCrawledLink(long crawledLinkID) {
        CrawledLink link = getCrawledLinkFromID(crawledLinkID);
        return new CrawledLinkAPIStorable(link);
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

    public boolean AddCrawledLinkToDownloads(long crawledLinkID) {
        List<Long> crawledLinks = new ArrayList<Long>();
        crawledLinks.add(crawledLinkID);
        return lcAPI.startDownloads(crawledLinks);

        // CrawledLink cl = getCrawledLinkFromID(crawledLinkID);
        // if (cl != null) {
        // java.util.List<FilePackage> fpkgs = new ArrayList<FilePackage>();
        // java.util.List<CrawledLink> clinks = new ArrayList<CrawledLink>();
        // clinks.add(cl);
        // java.util.List<FilePackage> frets = LinkCollector.getInstance().convert(clinks, true);
        // if (frets != null) fpkgs.addAll(frets);
        // /* add the converted FilePackages to DownloadController */
        // DownloadController.getInstance().addAllAt(fpkgs, -(fpkgs.size() + 10));
        // }
        // return true;
    }

    public boolean removeCrawledLink(String ID) {
        LinkCollector lc = LinkCollector.getInstance();
        boolean b = lc.readLock();
        long id = Long.valueOf(ID);
        try {
            for (CrawledPackage cpkg : lc.getPackages()) {
                synchronized (cpkg) {
                    for (CrawledLink link : cpkg.getChildren()) {
                        if (link.getDownloadLink().getUniqueID().getID() == id) {
                            List<CrawledLink> children = new ArrayList<CrawledLink>(0);
                            children.add(link);
                            lc.removeChildren(cpkg, children, true);
                            return true;
                        }
                    }
                }
            }
            return true;
        } finally {
            lc.readUnlock(b);
        }
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

    public boolean CrawlLink(String URL) {
        return lcAPI.addLinks(URL, "", "", "");
    }

    @Override
    public void onLinkCollectorApiEvent(LinkCollectorApiEvent event) {
        switch (event.getType()) {
        case REMOVE_CONTENT:
            if (event.getParameter() instanceof CrawledLink) {
                linkCollectorApiLinkRemoved((CrawledLink) event.getParameter());
            } else if (event.getParameter() instanceof CrawledPackage) {
                linkCollectorApiPackageRemoved((CrawledPackage) event.getParameter());
            }
            break;
        case ADD_CONTENT:
            if (event.getParameter() instanceof CrawledLink) {
                linkCollectorApiLinkAdded((CrawledLink) event.getParameter());
            } else if (event.getParameter() instanceof CrawledPackage) {
                linkCollectorApiPackageAdded((CrawledPackage) event.getParameter());
            }
            break;
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }

    private void linkCollectorApiLinkAdded(CrawledLink link) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("action", "linkCollectorLinkAdded");
        data.put("message", link.getName());
        data.put("data", link.getDownloadLink().getUniqueID().toString());
        RemoteAPIController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("linkCollectorLinkAdded", data), null);
    }

    private void linkCollectorApiLinkRemoved(CrawledLink link) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("action", "linkCollectorLinkRemoved");
        data.put("message", link.getName());
        data.put("data", link.getDownloadLink().getUniqueID().toString());
        RemoteAPIController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("linkCollectorLinkRemoved", data), null);
    }

    private void linkCollectorApiPackageAdded(CrawledPackage cpkg) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("action", "linkCollectorPackageAdded");
        data.put("message", cpkg.getName());
        data.put("data", cpkg.getUniqueID().toString());
        RemoteAPIController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("linkCollectorPackageAdded", data), null);
    }

    private void linkCollectorApiPackageRemoved(CrawledPackage cpkg) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("action", "linkCollectorPackageRemoved");
        data.put("message", cpkg.getName());
        data.put("data", cpkg.getUniqueID().toString());
        RemoteAPIController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("linkCollectorPackageRemoved", data), null);
    }
}
