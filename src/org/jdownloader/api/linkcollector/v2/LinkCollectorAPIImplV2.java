package org.jdownloader.api.linkcollector.v2;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcollector.LinkOriginDetails;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkModifier;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackageView;
import jd.controlling.linkcrawler.PackageInfo;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.net.Base64InputStream;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.downloads.v2.DownloadsAPIV2Impl;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.myjdownloader.client.bindings.interfaces.LinkgrabberInterface;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class LinkCollectorAPIImplV2 implements LinkCollectorAPIV2 {
    public LinkCollectorAPIImplV2() {
        RemoteAPIController.validateInterfaces(LinkCollectorAPIV2.class, LinkgrabberInterface.class);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractNode> List<T> convertIdsToObjects(final List<T> ret, long[] linkIds, long[] packageIds) {
        final HashSet<Long> linklookUp = DownloadsAPIV2Impl.createLookupSet(linkIds);
        final HashSet<Long> packageLookup = DownloadsAPIV2Impl.createLookupSet(packageIds);

        LinkCollector dlc = LinkCollector.getInstance();

        if (linklookUp != null || packageLookup != null) {

            boolean readL = dlc.readLock();
            try {
                main: for (CrawledPackage pkg : dlc.getPackages()) {
                    if (packageLookup != null && packageLookup.remove(pkg.getUniqueID().getID())) {
                        ret.add((T) pkg);
                        if ((packageLookup == null || packageLookup.size() == 0) && (linklookUp == null || linklookUp.size() == 0)) {
                            break main;
                        }

                    }
                    if (linklookUp != null) {
                        boolean readL2 = pkg.getModifyLock().readLock();
                        try {
                            for (CrawledLink child : pkg.getChildren()) {

                                if (linklookUp.remove(child.getUniqueID().getID())) {
                                    ret.add((T) child);
                                    if ((packageLookup == null || packageLookup.size() == 0) && (linklookUp == null || linklookUp.size() == 0)) {
                                        break main;
                                    }
                                }

                            }
                        } finally {
                            pkg.getModifyLock().readUnlock(readL2);
                        }
                    }

                }
            } finally {
                dlc.readUnlock(readL);
            }

        }
        return ret;
    }

    @Override
    public ArrayList<CrawledPackageAPIStorableV2> queryPackages(CrawledPackageQueryStorable queryParams) throws BadParameterException {

        ArrayList<CrawledPackageAPIStorableV2> result = new ArrayList<CrawledPackageAPIStorableV2>();
        LinkCollector lc = LinkCollector.getInstance();

        int startWith = queryParams.getStartAt();
        int maxResults = queryParams.getMaxResults();

        // filter out packages, if specific packageUUIDs given, else return all packages
        List<CrawledPackage> packages;
        if (queryParams.getPackageUUIDs() != null && queryParams.getPackageUUIDs().length > 0) {

            packages = getPackagesByID(queryParams.getPackageUUIDs());

        } else {
            packages = lc.getPackagesCopy();
        }

        if (startWith > lc.getPackages().size() - 1) return result;
        if (startWith < 0) startWith = 0;
        if (maxResults < 0) maxResults = lc.getPackages().size();

        for (int i = startWith; i < startWith + maxResults; i++) {

            final CrawledPackage pkg = packages.get(i);
            boolean readL = pkg.getModifyLock().readLock();
            try {
                CrawledPackageAPIStorableV2 cps = new CrawledPackageAPIStorableV2(pkg);
                final CrawledPackageView view = new CrawledPackageView();
                view.setItems(pkg.getChildren());

                if (queryParams.isSaveTo()) {
                    cps.setSaveTo(pkg.getRawDownloadFolder());

                }
                if (queryParams.isBytesTotal()) {
                    cps.setBytesTotal(view.getFileSize());

                }
                if (queryParams.isChildCount()) {
                    cps.setChildCount(view.getItems().size());

                }
                if (queryParams.isHosts()) {
                    Set<String> hosts = new HashSet<String>();
                    for (CrawledLink cl : pkg.getChildren()) {
                        hosts.add(cl.getHost());
                    }
                    cps.setHosts(hosts.toArray(new String[] {}));

                }

                if (queryParams.isComment()) {
                    cps.setComment(pkg.getComment());
                }
                if (queryParams.isAvailableOfflineCount() || queryParams.isAvailableOnlineCount() || queryParams.isAvailableTempUnknownCount() || queryParams.isAvailableUnknownCount()) {
                    int onlineCount = 0;
                    int offlineCount = 0;
                    int tempUnknown = 0;
                    int unknown = 0;
                    for (CrawledLink cl : pkg.getChildren()) {
                        switch (cl.getLinkState()) {
                        case OFFLINE:
                            offlineCount++;
                            break;
                        case ONLINE:
                            onlineCount++;
                            break;
                        case TEMP_UNKNOWN:
                            tempUnknown++;
                            break;
                        case UNKNOWN:
                            unknown++;
                            break;

                        }
                        if (queryParams.isAvailableOfflineCount()) {
                            cps.setOfflineCount(offlineCount);
                        }
                        if (queryParams.isAvailableOnlineCount()) {
                            cps.setOnlineCount(onlineCount);
                        }
                        if (queryParams.isAvailableTempUnknownCount()) {
                            cps.setTempUnknownCount(tempUnknown);
                        }
                        if (queryParams.isAvailableUnknownCount()) {
                            cps.setUnknownCount(unknown);
                        }

                    }
                }

                if (queryParams.isEnabled()) {
                    boolean enabled = false;
                    for (CrawledLink dl : pkg.getChildren()) {
                        if (dl.isEnabled()) {
                            enabled = true;
                            break;
                        }
                    }
                    cps.setEnabled(enabled);

                }

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
    public ArrayList<CrawledLinkAPIStorableV2> queryLinks(CrawledLinkQueryStorable queryParams) throws BadParameterException {
        ArrayList<CrawledLinkAPIStorableV2> result = new ArrayList<CrawledLinkAPIStorableV2>();
        LinkCollector lc = LinkCollector.getInstance();

        List<CrawledPackage> matched = null;

        if (queryParams.getPackageUUIDs() != null && queryParams.getPackageUUIDs().length > 0) {

            matched = getPackagesByID(queryParams.getPackageUUIDs());

        } else {
            matched = lc.getPackagesCopy();
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
            CrawledLinkAPIStorableV2 cls = new CrawledLinkAPIStorableV2(cl);

            if (queryParams.isVariants()) {
                cls.setVariants(cl.hasVariantSupport());
            }
            if (queryParams.isBytesTotal()) {
                cls.setBytesTotal(cl.getSize());
            }
            if (queryParams.isHost()) {
                cls.setHost(cl.getHost());
            }
            if (queryParams.isAvailability()) {
                cls.setAvailability(cl.getLinkState());

            }
            if (queryParams.isUrl()) {
                cls.setUrl(cl.getURL());

            }
            if (queryParams.isEnabled()) {
                cls.setEnabled(cl.isEnabled());

            }
            cls.setPackageUUID(cl.getParentNode().getUniqueID().getID());

            result.add(cls);
        }

        return result;
    }

    @Override
    public int getPackageCount() {
        return LinkCollector.getInstance().getPackages().size();
    }

    @Override
    public void addLinks(final AddLinksQueryStorable query) {

        LinkCollector lc = LinkCollector.getInstance();
        LinkCollectingJob lcj = new LinkCollectingJob(new LinkOriginDetails(LinkOrigin.MYJD, null/* add useragent? */), query.getLinks());
        HashSet<String> extPws = null;
        if (StringUtils.isNotEmpty(query.getExtractPassword())) {
            extPws = new HashSet<String>();
            extPws.add(query.getExtractPassword());
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
                if (StringUtils.isNotEmpty(query.getPackageName())) {
                    getPackageInfo(link).setName(query.getPackageName());
                    getPackageInfo(link).setUniqueId(null);
                }
                if (StringUtils.isNotEmpty(query.getDestinationFolder())) {
                    getPackageInfo(link).setDestinationFolder(query.getDestinationFolder());
                    getPackageInfo(link).setUniqueId(null);
                }
                DownloadLink dlLink = link.getDownloadLink();
                if (dlLink != null) {
                    if (StringUtils.isNotEmpty(query.getDownloadPassword())) dlLink.setDownloadPassword(query.getDownloadPassword());
                }
                if (query.isAutostart()) {
                    link.setAutoConfirmEnabled(true);
                    link.setAutoStartEnabled(true);
                }
            }
        });
        lc.addCrawlerJob(lcj);

    }

    // @Override
    // public boolean uploadLinkContainer(RemoteAPIRequest request) {
    // if (request.getRequestType() == REQUESTTYPE.POST) {
    // PostRequest post = (PostRequest) request.getHttpRequest();
    // }
    // return false;
    // }

    @Override
    public long getChildrenChanged(long structureWatermark) {
        LinkCollector lc = LinkCollector.getInstance();
        if (lc.getChildrenChanges() != structureWatermark) {
            return lc.getChildrenChanges();
        } else {
            return -1l;
        }
    }

    /**
     * the SelectionInfo Class is actually used for the GUI downloadtable. it generates a logic selection out of selected links and
     * packages.
     * 
     * example: if a package is selected, and non if it's links - all its links will be in the selection info<br>
     * example2: if a package is selected AND SOME of it's children. The packge will not be considered as fully selected. only the actual
     * selected links.
     * 
     * @param linkIds
     * @param packageIds
     * @return
     * @throws BadParameterException
     */
    public static SelectionInfo<CrawledPackage, CrawledLink> getSelectionInfo(long[] linkIds, long[] packageIds) throws BadParameterException {
        ArrayList<AbstractNode> list = new ArrayList<AbstractNode>();
        if (packageIds != null) {
            List<CrawledPackage> packages = getPackagesByID(packageIds);

            list.addAll(packages);
        }
        if (linkIds != null) {
            List<CrawledLink> links = getLinksById(linkIds);

            list.addAll(links);
        }

        return new SelectionInfo<CrawledPackage, CrawledLink>(null, list, false);

    }

    @Override
    public void moveToDownloadlist(final long[] linkIds, final long[] packageIds) throws BadParameterException {

        LinkCollector.getInstance().moveLinksToDownloadList(getSelectionInfo(linkIds, packageIds));

    }

    @Override
    public void removeLinks(final long[] linkIds, final long[] packageIds) throws BadParameterException {
        LinkCollector lc = LinkCollector.getInstance();
        lc.writeLock();
        try {
            lc.removeChildren(getSelectionInfo(linkIds, packageIds).getChildren());
        } finally {
            lc.writeUnlock();
        }

    }

    @Override
    public void renameLink(long linkId, String newName) throws BadParameterException {
        CrawledLink lc = getLinkById(linkId);

        lc.setName(newName);

    }

    @Override
    public void renamePackage(long packageId, String newName) throws BadParameterException {
        CrawledPackage lc = getPackageByID(packageId);

        lc.setName(newName);

    }

    @Override
    public void setEnabled(boolean enabled, final long[] linkIds, final long[] packageIds) throws BadParameterException {
        try {
            LinkCollector.getInstance().writeLock();
            List<CrawledLink> sdl = getSelectionInfo(linkIds, packageIds).getChildren();
            for (CrawledLink dl : sdl) {
                dl.setEnabled(enabled);
            }
        } finally {
            LinkCollector.getInstance().writeUnlock();
        }

    }

    @Override
    public void movePackages(long[] packageIds, long afterDestPackageId) throws BadParameterException {

        List<CrawledPackage> selectedPackages = getPackagesByID(packageIds);
        CrawledPackage afterDestPackage = getPackageByID(afterDestPackageId);
        LinkCollector.getInstance().move(selectedPackages, afterDestPackage);

    }

    private static CrawledPackage getPackageByID(long afterDestPackageId) throws BadParameterException {
        CrawledPackage ret = LinkCollector.getInstance().getPackageByID(afterDestPackageId);
        if (ret == null) throw new BadParameterException("PackageID Unknown");
        return ret;
    }

    private static List<CrawledPackage> getPackagesByID(long[] packageIds) throws BadParameterException {
        List<CrawledPackage> ret = LinkCollector.getInstance().getPackagesByID(packageIds);
        if (ret.size() != packageIds.length) { throw new BadParameterException("One or more PackageIDs Unknown"); }
        return ret;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void moveLinks(long[] linkIds, long afterLinkID, long destPackageID) throws BadParameterException {
        LinkCollector dlc = LinkCollector.getInstance();
        List<CrawledLink> selectedLinks = getLinksById(linkIds);
        CrawledLink afterLink = afterLinkID <= 0 ? null : getLinkById(afterLinkID);
        CrawledPackage destpackage = getPackageByID(destPackageID);
        dlc.move(selectedLinks, destpackage, afterLink);

    }

    private static CrawledLink getLinkById(long linkIds) throws BadParameterException {
        CrawledLink ret = LinkCollector.getInstance().getLinkByID(linkIds);
        if (ret == null) { throw new BadParameterException("LinkID Unknown"); }
        return ret;
    }

    private static List<CrawledLink> getLinksById(long[] linkIds) throws BadParameterException {
        List<CrawledLink> ret = LinkCollector.getInstance().getLinksByID(linkIds);
        if (ret.size() != linkIds.length) { throw new BadParameterException("One or more LinkIDs Unknown"); }
        return ret;
    }

    @Override
    public List<String> getDownloadFolderHistorySelectionBase() {

        return DownloadPathHistoryManager.getInstance().listPathes(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder());

    }

    @Override
    public List<LinkVariantStorableV2> getVariants(long linkid) throws BadParameterException {
        ArrayList<LinkVariantStorableV2> ret = new ArrayList<LinkVariantStorableV2>();
        CrawledLink cl = getLinkById(linkid);
        for (LinkVariant lv : cl.getDownloadLink().getDefaultPlugin().getVariantsByLink(cl.getDownloadLink())) {
            ret.add(new LinkVariantStorableV2(lv.getUniqueId(), CFG_GUI.EXTENDED_VARIANT_NAMES_ENABLED.isEnabled() ? lv.getExtendedName() : lv.getName()));
        }
        return ret;
    }

    @Override
    public void setVariant(long linkid, String variantID) throws BadParameterException {
        CrawledLink cl = getLinkById(linkid);

        for (LinkVariant lv : cl.getDownloadLink().getDefaultPlugin().getVariantsByLink(cl.getDownloadLink())) {
            if (lv.getUniqueId().equals(variantID)) {
                LinkCollector.getInstance().setActiveVariantForLink(cl, lv);

                return;
            }
        }
        throw new BadParameterException("Unknown variantID");

    }

    @Override
    public void addVariantCopy(long linkid, final long destinationAfterLinkID, final long destinationPackageID, final String variantID) throws BadParameterException {
        // get link
        final CrawledLink link = getLinkById(linkid);

        // move and add
        LinkCollector.getInstance().getQueue().add(new QueueAction<Void, BadParameterException>() {

            @Override
            protected Void run() throws BadParameterException {
                // search variant by id
                LinkVariant v = null;
                for (LinkVariant lv : link.getDownloadLink().getDefaultPlugin().getVariantsByLink(link.getDownloadLink())) {
                    if (lv.getUniqueId().equals(variantID)) {
                        v = lv;
                        break;
                    }
                }
                if (v == null) { throw new BadParameterException("Unknown variantID"); }

                // create new downloadlink
                final DownloadLink dllink = new DownloadLink(link.getDownloadLink().getDefaultPlugin(), link.getDownloadLink().getName(), link.getDownloadLink().getHost(), link.getDownloadLink().getDownloadURL(), true);
                dllink.setProperties(link.getDownloadLink().getProperties());

                // create crawledlink
                final CrawledLink cl = new CrawledLink(dllink);

                final ArrayList<CrawledLink> list = new ArrayList<CrawledLink>();
                list.add(cl);

                cl.getDownloadLink().getDefaultPlugin().setActiveVariantByLink(cl.getDownloadLink(), v);

                // check if package already contains this variant

                boolean readL = link.getParentNode().getModifyLock().readLock();

                try {

                    for (CrawledLink cLink : link.getParentNode().getChildren()) {
                        if (dllink.getLinkID().equals(cLink.getLinkID())) { throw new BadParameterException("Variant is already in this package"); }
                    }
                } finally {
                    link.getParentNode().getModifyLock().readUnlock(readL);
                }

                if (destinationPackageID < 0) {
                    LinkCollector.getInstance().moveOrAddAt(link.getParentNode(), list, link.getParentNode().indexOf(link) + 1);
                } else {

                    LinkCollector dlc = LinkCollector.getInstance();

                    CrawledLink afterLink = destinationAfterLinkID <= 0 ? null : getLinkById(destinationAfterLinkID);
                    CrawledPackage destpackage = getPackageByID(destinationPackageID);
                    dlc.move(list, destpackage, afterLink);
                }

                java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);
                checkableLinks.add(cl);
                LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                linkChecker.check(checkableLinks);
                return null;
            }
        });

    }

    @Override
    public void addContainer(String type, String content) {
        String fileName = null;
        if ("DLC".equalsIgnoreCase(type)) {
            fileName = "linkcollectorDLCAPI" + System.nanoTime() + ".dlc";
        } else if ("RSDF".equalsIgnoreCase(type)) {
            fileName = "linkcollectorDLCAPI" + System.nanoTime() + ".rsdf";
        } else if ("CCF".equalsIgnoreCase(type)) {
            fileName = "linkcollectorDLCAPI" + System.nanoTime() + ".ccf";
        }
        if (fileName != null) {
            try {
                File tmp = Application.getTempResource(fileName);
                byte[] write = IO.readStream(-1, new Base64InputStream(new ByteArrayInputStream(content.substring(13).getBytes("UTF-8"))));
                IO.writeToFile(tmp, write);
                LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(new LinkOriginDetails(LinkOrigin.MYJD), tmp.getAbsolutePath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}