package org.jdownloader.api.linkcollector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLink.LinkState;
import jd.controlling.linkcrawler.CrawledLinkModifier;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackageView;
import jd.controlling.linkcrawler.PackageInfo;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;

import org.appwork.remoteapi.APIQuery;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.settings.GeneralSettings;

public class LinkCollectorAPIImpl implements LinkCollectorAPI {

    @Override
    public List<CrawledPackageAPIStorable> queryPackages(APIQuery queryParams) {
        List<CrawledPackageAPIStorable> result = new ArrayList<CrawledPackageAPIStorable>();
        LinkCollector lc = LinkCollector.getInstance();

        int startWith = queryParams.getStartAt();
        int maxResults = queryParams.getMaxResults();

        // filter out packages, if specific packageUUIDs given, else return all packages
        List<CrawledPackage> packages = lc.getPackagesCopy();
        if (!queryParams._getQueryParam("packageUUIDs", ArrayList.class, new ArrayList<Long>()).isEmpty()) {
            List<Long> requestedIds = queryParams._getQueryParam("packageUUIDs", ArrayList.class, new ArrayList<Long>());
            List<CrawledPackage> toKeep = new ArrayList<CrawledPackage>();
            for (CrawledPackage pkg : packages) {
                for (Long uuid : requestedIds) {
                    if (uuid.equals(pkg.getUniqueID().getID())) {
                        toKeep.add(pkg);
                    }
                }
            }
            packages = toKeep;
        }

        if (startWith > lc.getPackages().size() - 1) return result;
        if (startWith < 0) startWith = 0;
        if (maxResults < 0) maxResults = lc.getPackages().size();

        for (int i = startWith; i < startWith + maxResults; i++) {

            final CrawledPackage pkg = packages.get(i);
            boolean readL = pkg.getModifyLock().readLock();
            try {
                CrawledPackageAPIStorable cps = new CrawledPackageAPIStorable(pkg);
                final CrawledPackageView view = new CrawledPackageView();
                view.setItems(pkg.getChildren());
                org.jdownloader.myjdownloader.client.json.JsonMap infomap = new org.jdownloader.myjdownloader.client.json.JsonMap();
                if (queryParams._getQueryParam("saveTo", Boolean.class, false)) {
                    infomap.put("saveTo", pkg.getRawDownloadFolder());
                }
                if (queryParams._getQueryParam("size", Boolean.class, false)) {
                    infomap.put("size", view.getFileSize());
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
                    // Does not make much sense?
                    String availabilityString = "";
                    int onlineCount = 0;
                    for (CrawledLink cl : pkg.getChildren()) {
                        if (LinkState.ONLINE.equals(cl.getLinkState())) {
                            onlineCount++;
                        }
                    }
                    if (onlineCount == pkg.getChildren().size()) {
                        availabilityString = "ONLINE";
                    } else {
                        availabilityString = "UNKNOWN";
                    }
                    infomap.put("availability", availabilityString);
                }
                if (queryParams._getQueryParam("availabilityCount", Boolean.class, false)) {
                    int onlineCount = 0;
                    for (CrawledLink cl : pkg.getChildren()) {
                        if (LinkState.ONLINE.equals(cl.getLinkState())) {
                            onlineCount++;
                        }
                        infomap.put("availabilityCount", onlineCount);
                    }
                }
                if (queryParams.fieldRequested("enabled")) {
                    boolean enabled = false;
                    for (CrawledLink dl : pkg.getChildren()) {
                        if (dl.isEnabled()) {
                            enabled = true;
                            break;
                        }
                    }
                    infomap.put("enabled", enabled);
                }
                cps.setInfoMap(infomap);

                result.add(cps);

                if (i == lc.getPackages().size() - 1) {
                    break;
                }
            } finally {
                pkg.getModifyLock().readUnlock(readL);
            }
        }

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
            boolean readL = pkg.getModifyLock().readLock();
            try {
                links.addAll(pkg.getChildren());
            } finally {
                pkg.getModifyLock().readUnlock(readL);
            }
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

            org.jdownloader.myjdownloader.client.json.JsonMap infomap = new org.jdownloader.myjdownloader.client.json.JsonMap();

            if (queryParams._getQueryParam("size", Boolean.class, false)) {
                infomap.put("size", cl.getSize());
            }
            if (queryParams._getQueryParam("host", Boolean.class, false)) {
                infomap.put("host", cl.getHost());
            }
            if (queryParams._getQueryParam("availability", Boolean.class, false)) {
                infomap.put("availability", cl.getLinkState().toString());
            }
            if (queryParams._getQueryParam("url", Boolean.class, false)) {
                infomap.put("url", cl.getURL());
            }
            if (queryParams.fieldRequested("enabled")) infomap.put("enabled", cl.isEnabled());
            infomap.put("packageUUID", cl.getParentNode().getUniqueID().getID());

            cls.setInfoMap(infomap);
            result.add(cls);
        }

        return result;
    }

    @Override
    public int packageCount() {
        return LinkCollector.getInstance().getPackages().size();
    }

    @Override
    public Boolean addLinks(String links, String packageName, String extractPassword, String downloadPassword, String destinationFolder) {
        return addLinks(links, packageName, extractPassword, downloadPassword, destinationFolder, false);
    }

    @Override
    public Boolean addLinks(String links, String packageName, String extractPassword, String downloadPassword) {
        return addLinks(links, packageName, extractPassword, downloadPassword, null, false);
    }

    @Override
    public Boolean addLinksAndStartDownload(String links, String packageName, String extractPassword, String downloadPassword) {
        return addLinks(links, packageName, extractPassword, downloadPassword, null, true);
    }

    private Boolean addLinks(String links, final String finalPackageName, String extractPassword, final String downloadPassword, final String destinationFolder, final boolean autostart) {
        LinkCollector lc = LinkCollector.getInstance();
        LinkCollectingJob lcj = new LinkCollectingJob(LinkOrigin.MYJD, links);
        HashSet<String> extPws = null;
        if (StringUtils.isNotEmpty(extractPassword)) {
            extPws = new HashSet<String>();
            extPws.add(extractPassword);
        }
        final HashSet<String> finalExtPws = extPws;
        lcj.setCrawledLinkModifier(new CrawledLinkModifier() {
            private PackageInfo getPackageInfo(CrawledLink link) {
                PackageInfo packageInfo = link.getDesiredPackageInfo();
                if (packageInfo != null) return packageInfo;
                packageInfo = new PackageInfo();
                link.setDesiredPackageInfo(packageInfo);
                return packageInfo;
            }

            @Override
            public void modifyCrawledLink(CrawledLink link) {
                if (finalExtPws != null && finalExtPws.size() > 0) {
                    link.getArchiveInfo().getExtractionPasswords().addAll(finalExtPws);
                }
                if (StringUtils.isNotEmpty(finalPackageName)) {
                    getPackageInfo(link).setName(finalPackageName);
                    getPackageInfo(link).setUniqueId(null);
                }
                if (StringUtils.isNotEmpty(destinationFolder)) {
                    getPackageInfo(link).setDestinationFolder(destinationFolder);
                    getPackageInfo(link).setUniqueId(null);
                }
                DownloadLink dlLink = link.getDownloadLink();
                if (dlLink != null) {
                    if (StringUtils.isNotEmpty(downloadPassword)) dlLink.setDownloadPassword(downloadPassword);
                }
                if (autostart) {
                    link.setAutoConfirmEnabled(true);
                    link.setAutoStartEnabled(true);
                }
            }
        });
        lc.addCrawlerJob(lcj);
        return true;
    }

    // @Override
    // public Boolean uploadLinkContainer(RemoteAPIRequest request) {
    // if (request.getRequestType() == REQUESTTYPE.POST) {
    // PostRequest post = (PostRequest) request.getHttpRequest();
    // }
    // return false;
    // }

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
        return startDownloads(linkIds, null);
    }

    @Override
    public Boolean startDownloads(final List<Long> linkIds, final List<Long> packageIds) {
        LinkCollector lc = LinkCollector.getInstance();

        List<CrawledLink> lks;

        boolean lb = lc.readLock();
        try {
            lks = getAllTheLinks(linkIds, packageIds);
        } finally {
            lc.readUnlock(lb);
        }

        LinkCollector.getInstance().moveLinksToDownloadList(new SelectionInfo<CrawledPackage, CrawledLink>(null, lks, false));

        return true;
    }

    @Override
    public Boolean removeLinks(final List<Long> linkIds) {
        return removeLinks(linkIds, null);
    }

    @Override
    public Boolean removeLinks(final List<Long> linkIds, final List<Long> packageIds) {
        LinkCollector lc = LinkCollector.getInstance();
        lc.writeLock();
        try {
            lc.removeChildren(getAllTheLinks(linkIds, packageIds));
        } finally {
            lc.writeUnlock();
        }
        return true;
    }

    @Override
    public boolean renameLink(Long packageId, Long linkId, String newName) {
        LinkCollector lc = LinkCollector.getInstance();
        try {
            lc.writeLock();
            for (CrawledPackage fp : lc.getPackages()) {
                if (packageId.equals(fp.getUniqueID().getID())) {
                    for (CrawledLink cl : fp.getChildren()) {
                        if (linkId.equals(cl.getUniqueID().getID())) {
                            cl.setName(newName);
                        }
                        break;
                    }
                    break;
                }
            }
        } finally {
            lc.writeUnlock();
        }
        return true;
    }

    @Override
    public boolean renamePackage(Long packageId, String newName) {
        LinkCollector lc = LinkCollector.getInstance();
        try {
            lc.writeLock();
            for (CrawledPackage fp : lc.getPackages()) {
                if (packageId.equals(fp.getUniqueID().getID())) {
                    fp.setName(newName);
                    break;
                }
            }
        } finally {
            lc.writeUnlock();
        }
        return true;
    }

    @Override
    public boolean enableLinks(final List<Long> linkIds) {
        if (linkIds == null) return true;
        return enableLinks(linkIds, null);
    }

    @Override
    public boolean enableLinks(final List<Long> linkIds, final List<Long> packageIds) {
        try {
            LinkCollector.getInstance().writeLock();
            List<CrawledLink> sdl = getAllTheLinks(linkIds, packageIds);
            for (CrawledLink dl : sdl) {
                dl.setEnabled(true);
            }
        } finally {
            LinkCollector.getInstance().writeUnlock();
        }
        return true;
    }

    @Override
    public boolean disableLinks(final List<Long> linkIds) {
        if (linkIds == null) return true;
        return disableLinks(linkIds, null);
    }

    @Override
    public boolean disableLinks(final List<Long> linkIds, final List<Long> packageIds) {
        try {
            LinkCollector.getInstance().writeLock();
            List<CrawledLink> sdl = getAllTheLinks(linkIds, packageIds);
            for (CrawledLink dl : sdl) {
                dl.setEnabled(false);
            }
        } finally {
            LinkCollector.getInstance().writeUnlock();
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean movePackages(APIQuery query) {
        List<Long> packageUUIDs = query._getQueryParam("packageUUIDs", List.class, new ArrayList<Long>());
        Long afterDestPackageUUID = query._getQueryParam("afterDestPackageUUID", Long.class, null);

        LinkCollector dlc = LinkCollector.getInstance();

        List<CrawledPackage> selectedPackages = new ArrayList<CrawledPackage>();
        CrawledPackage afterDestPackage = null;

        boolean b = dlc.readLock();
        try {
            for (CrawledPackage fp : dlc.getPackages()) {
                if (packageUUIDs.contains(fp.getUniqueID().getID())) {
                    selectedPackages.add(fp);
                }
                if (afterDestPackageUUID != null && afterDestPackageUUID.equals(fp.getUniqueID().getID())) {
                    afterDestPackage = fp;
                }
            }
        } finally {
            dlc.readUnlock(b);
        }

        dlc.move(selectedPackages, afterDestPackage);
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean moveLinks(APIQuery query) {
        List<Long> selectedUUIDs = query._getQueryParam("linkUUIDs", List.class, new ArrayList<Long>());
        Long afterDestLinkUUID = query._getQueryParam("afterDestLinkUUID", Long.class, new Long(-1));
        Long targetPackageUUID = query._getQueryParam("destPackageUUID", Long.class, new Long(-1));

        LinkCollector dlc = LinkCollector.getInstance();

        List<CrawledLink> selectedLinks = new ArrayList<CrawledLink>();
        CrawledLink afterDestLink = null;
        CrawledPackage destPackage = null;

        boolean b = dlc.readLock();
        try {
            final List<CrawledLink> allLinks = new ArrayList<CrawledLink>();
            for (final CrawledPackage cpkg : dlc.getPackages()) {
                cpkg.getModifyLock().runReadLock(new Runnable() {
                    @Override
                    public void run() {
                        allLinks.addAll(cpkg.getChildren());
                    }
                });
            }
            for (CrawledLink dl : allLinks) {
                if (selectedUUIDs.contains(dl.getUniqueID().getID())) {
                    selectedLinks.add(dl);
                }
                if (afterDestLink == null && afterDestLinkUUID.equals(dl.getUniqueID().getID())) {
                    afterDestLink = dl;
                }
            }
            for (CrawledPackage fp : dlc.getPackages()) {
                if (targetPackageUUID.equals(fp.getUniqueID().getID())) {
                    destPackage = fp;
                    break;
                }
            }
        } finally {
            dlc.readUnlock(b);
        }

        dlc.move(selectedLinks, destPackage, afterDestLink);
        return true;
    }

    /*
     * UTIL to break down package and links selections into links only
     */
    private List<CrawledLink> getAllTheLinks(final List<Long> linkIds, final List<Long> packageIds) {
        LinkCollector lc = LinkCollector.getInstance();
        final List<CrawledLink> lks = lc.getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {
            @Override
            public int returnMaxResults() {
                return -1;
            }

            @Override
            public boolean acceptNode(CrawledLink node) {
                return linkIds != null && linkIds.contains(node.getUniqueID().getID());
            }
        });
        if (packageIds != null) {
            for (final CrawledPackage cp : lc.getPackages()) {
                if (packageIds.contains(cp.getUniqueID().getID())) {
                    cp.getModifyLock().runReadLock(new Runnable() {
                        @Override
                        public void run() {
                            lks.addAll(cp.getChildren());
                        }
                    });
                }
            }
        }
        return lks;
    }

    @Override
    public List<String> getDownloadFolderHistorySelectionBase() {

        return DownloadPathHistoryManager.getInstance().listPathes(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder());

    }
}