package org.jdownloader.api.linkcollector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.FilePackage;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.QueryResponseMap;

public class LinkCollectorAPIImpl implements LinkCollectorAPI {

    @Override
    public List<CrawledPackageAPIStorable> queryPackages(APIQuery queryParams) {
        List<CrawledPackageAPIStorable> result = new ArrayList<CrawledPackageAPIStorable>();
        LinkCollector lc = LinkCollector.getInstance();

        int startWith = queryParams.getStartAt();
        int maxResults = queryParams.getMaxResults();

        boolean b = lc.readLock();
        try {
            if (startWith > lc.getPackages().size() - 1) return result;
            if (startWith < 0) startWith = 0;
            if (maxResults < 0) maxResults = lc.getPackages().size();

            for (int i = startWith; i < startWith + maxResults; i++) {

                CrawledPackage pkg = lc.getPackages().get(i);
                CrawledPackageAPIStorable cps = new CrawledPackageAPIStorable(pkg);

                QueryResponseMap infomap = new QueryResponseMap();
                if (queryParams._getQueryParam("saveTo", Boolean.class, false)) {
                    infomap.put("saveTo", pkg.getRawDownloadFolder());
                }
                if (queryParams._getQueryParam("size", Boolean.class, false)) {
                    long size = 0;
                    for (CrawledLink cl : pkg.getChildren()) {
                        size = size + cl.getSize();
                    }
                    infomap.put("size", pkg.getView().getFileSize());
                }
                if (queryParams._getQueryParam("childCount", Boolean.class, false)) {
                    infomap.put("childCount", pkg.getChildren().size());
                }
                if (queryParams._getQueryParam("hosts", Boolean.class, false)) {
                    Set<String> hosts = new HashSet<String>();
                    for (CrawledLink cl : pkg.getChildren()) {
                        hosts.add(cl.getHost());
                    }
                    infomap.put("hosts", hosts);
                }
                if (queryParams._getQueryParam("availability", Boolean.class, false)) {
                    infomap.put("availability", pkg.getChildren().get(0).getLinkState());
                }
                cps.setInfoMap(infomap);

                result.add(cps);

                if (i == lc.getPackages().size() - 1) {
                    break;
                }
            }
        } finally {
            lc.readUnlock(b);
        }

        simulateLatency();

        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<CrawledLinkAPIStorable> queryLinks(APIQuery queryParams) {
        List<CrawledLinkAPIStorable> result = new ArrayList<CrawledLinkAPIStorable>();
        LinkCollector lc = LinkCollector.getInstance();

        List<Long> packageUUIDs = new ArrayList<Long>();
        if (!queryParams._getQueryParam("packageUUIDs", List.class, new ArrayList()).isEmpty()) {
            List uuidsFromQuery = queryParams._getQueryParam("packageUUIDs", List.class, new ArrayList());
            for (Object o : uuidsFromQuery) {
                try {
                    packageUUIDs.add((Long) o);
                } catch (ClassCastException e) {
                    continue;
                }
            }
        }

        List<CrawledPackage> matched = new ArrayList<CrawledPackage>();

        // if no specific uuids are specified collect all packages
        boolean b = lc.readLock();
        try {
            if (packageUUIDs.isEmpty()) {
                matched = lc.getPackages();
            } else {
                for (CrawledPackage pkg : lc.getPackages()) {
                    if (packageUUIDs.contains(pkg.getUniqueID().getID())) {
                        matched.add(pkg);
                    }
                }
            }
        } finally {
            lc.readUnlock(b);
        }

        // collect children of the selected packages and convert to storables for response
        List<CrawledLink> links = new ArrayList<CrawledLink>();
        for (CrawledPackage pkg : matched) {
            links.addAll(pkg.getChildren());
        }

        if (links.isEmpty()) return result;

        int startWith = queryParams.getStartAt();
        int maxResults = queryParams.getMaxResults();

        if (startWith > links.size() - 1) return result;
        if (startWith < 0) startWith = 0;
        if (maxResults < 0) maxResults = links.size();

        for (int i = startWith; i < Math.min(startWith + maxResults, links.size()); i++) {

            CrawledLink cl = links.get(i);
            CrawledLinkAPIStorable cls = new CrawledLinkAPIStorable(cl);

            QueryResponseMap infomap = new QueryResponseMap();

            if (queryParams._getQueryParam("size", Boolean.class, false)) {
                infomap.put("size", cl.getSize());
            }
            if (queryParams._getQueryParam("host", Boolean.class, false)) {
                infomap.put("host", cl.getHost());
            }
            if (queryParams._getQueryParam("availability", Boolean.class, false)) {
                infomap.put("availability", cl.getLinkState().toString());
            }

            infomap.put("packageUUID", cl.getParentNode().getUniqueID().getID());

            cls.setInfoMap(infomap);
            result.add(cls);
        }

        simulateLatency();

        return result;
    }

    @Override
    public Boolean addLinks(String links) {
        LinkCollector lc = LinkCollector.getInstance();
        lc.addCrawlerJob(new LinkCollectingJob(links));
        return true;
    }

    @Override
    public Long getChildrenChanged(Long structureWatermark) {
        LinkCollector lc = LinkCollector.getInstance();
        if (lc.getChildrenChanges() != structureWatermark) {
            return lc.getChildrenChanges();
        } else {
            return -1l;
        }
    }

    @Override
    public Boolean startDownloads(final List<Long> linkIds) {

        LinkCollector lc = LinkCollector.getInstance();
        DownloadController dc = DownloadController.getInstance();

        List<CrawledLink> lks;

        boolean lb = lc.readLock();
        try {
            lks = lc.getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {
                @Override
                public int returnMaxResults() {
                    return -1;
                }

                @Override
                public boolean isChildrenNodeFiltered(CrawledLink node) {
                    return linkIds != null && linkIds.contains(node.getUniqueID().getID());
                }
            });
        } finally {
            lc.readUnlock(lb);
        }

        List<FilePackage> frets = LinkCollector.getInstance().convert(lks, true);
        dc.addAll(frets);

        return true;
    }

    @Override
    public Boolean removeLinks(final List<Long> linkIds) {
        LinkCollector lc = LinkCollector.getInstance();

        lc.writeLock();
        try {
            List<CrawledLink> lks = lc.getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {
                @Override
                public int returnMaxResults() {
                    return -1;
                }

                @Override
                public boolean isChildrenNodeFiltered(CrawledLink node) {
                    return linkIds != null && linkIds.contains(node.getUniqueID().getID());
                }
            });

            lc.removeChildren(lks);
        } finally {
            lc.writeUnlock();
        }
        return true;
    }

    // should be commented out in production
    public void simulateLatency() {
        // try {
        // Thread.sleep(1000l);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
    }
}