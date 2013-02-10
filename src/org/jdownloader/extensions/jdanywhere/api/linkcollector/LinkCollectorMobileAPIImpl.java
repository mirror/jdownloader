package org.jdownloader.extensions.jdanywhere.api.linkcollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.plugins.FilePackage;

import org.appwork.remoteapi.EventsAPIEvent;
import org.jdownloader.api.linkcollector.LinkCollectorAPIImpl;
import org.jdownloader.extensions.jdanywhere.CheckUser;
import org.jdownloader.extensions.jdanywhere.JDAnywhereAPI;
import org.jdownloader.extensions.jdanywhere.JDAnywhereController;

public class LinkCollectorMobileAPIImpl implements LinkCollectorMobileAPI, LinkCollectorListener, JDAnywhereAPI {

    LinkCollectorAPIImpl lcAPI = new LinkCollectorAPIImpl();
    private String       user;
    private String       pass;
    private CheckUser    checkUser;

    public LinkCollectorMobileAPIImpl(String user, String pass) {
        LinkCollector.getInstance().getEventsender().addListener(this, true);
        this.user = user;
        this.pass = pass;
        checkUser = new CheckUser(user, pass);
    }

    public List<CrawledPackageAPIStorable> list(final String username, final String password) {
        if (!checkUser.check(username, password)) return null;
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

    public CrawledPackageAPIStorable getCrawledPackage(long crawledPackageID, final String username, final String password) {
        if (!checkUser.check(username, password)) return null;
        CrawledPackage cpkg = getCrawledPackageFromID(crawledPackageID);
        CrawledPackageAPIStorable pkg = new CrawledPackageAPIStorable(cpkg);
        List<CrawledLinkAPIStorable> links = new ArrayList<CrawledLinkAPIStorable>(0);
        pkg.setLinks(links);
        return pkg;
    }

    public String getPackageIDFromLinkID(long ID, final String username, final String password) {
        if (!checkUser.check(username, password)) return null;
        CrawledLink dl = getCrawledLinkFromID(ID);
        CrawledPackage fpk = dl.getParentNode();
        return fpk.getUniqueID().toString();
    }

    public CrawledLinkAPIStorable getCrawledLink(long crawledLinkID, final String username, final String password) {
        if (!checkUser.check(username, password)) return null;
        CrawledLink link = getCrawledLinkFromID(crawledLinkID);
        return new CrawledLinkAPIStorable(link);
    }

    public boolean AddCrawledPackageToDownloads(long crawledPackageID, final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
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

    public boolean AddCrawledLinkToDownloads(long crawledLinkID, final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        List<Long> crawledLinks = new ArrayList<Long>();
        crawledLinks.add(crawledLinkID);
        return lcAPI.startDownloads(crawledLinks);
    }

    public boolean removeCrawledLink(String ID, final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
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

    public boolean removeCrawledPackage(String ID, final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
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

    public boolean CrawlLink(String URL, final String username, final String password) {
        if (!checkUser.check(username, password)) return false;
        return lcAPI.addLinks(URL, "", "", "");
    }

    private void linkCollectorApiLinkAdded(CrawledLink link) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("action", "linkCollectorLinkAdded");
        data.put("message", link.getName());
        data.put("data", link.getDownloadLink().getUniqueID().toString());
        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("linkCollectorLinkAdded", data), null);
    }

    private void linkCollectorApiLinkRemoved(CrawledLink link) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("action", "linkCollectorLinkRemoved");
        data.put("message", link.getName());
        data.put("data", link.getDownloadLink().getUniqueID().toString());
        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("linkCollectorLinkRemoved", data), null);
    }

    private void linkCollectorApiPackageAdded(CrawledPackage cpkg) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("action", "linkCollectorPackageAdded");
        data.put("message", cpkg.getName());
        data.put("data", cpkg.getUniqueID().toString());
        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("linkCollectorPackageAdded", data), null);
    }

    private void linkCollectorApiPackageRemoved(CrawledPackage cpkg) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("action", "linkCollectorPackageRemoved");
        data.put("message", cpkg.getName());
        data.put("data", cpkg.getUniqueID().toString());
        JDAnywhereController.getInstance().getEventsapi().publishEvent(new EventsAPIEvent("linkCollectorPackageRemoved", data), null);
    }

    @Override
    public void onLinkCollectorAbort(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
        if (event.getParameters() != null) {
            for (Object object : event.getParameters()) {
                if (object instanceof CrawledLink) linkCollectorApiLinkRemoved((CrawledLink) object);
                if (object instanceof CrawledPackage) linkCollectorApiPackageRemoved((CrawledPackage) event.getParameter());
            }
        }
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
        if (event.getParameters() != null) {
            for (Object object : event.getParameters()) {
                if (object instanceof CrawledLink) linkCollectorApiLinkAdded((CrawledLink) object);
                if (object instanceof CrawledPackage) linkCollectorApiPackageAdded((CrawledPackage) event.getParameter());
            }
        }
    }

    @Override
    public void onLinkCollectorContentModified(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    public String getUsername() {
        return user;
    }

    public String getPassword() {
        return pass;
    }
}
