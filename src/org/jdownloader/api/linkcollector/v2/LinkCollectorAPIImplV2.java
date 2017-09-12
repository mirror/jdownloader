package org.jdownloader.api.linkcollector.v2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcollector.LinkCollector.MoveLinksMode;
import jd.controlling.linkcollector.LinkCollector.MoveLinksSettings;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkModifier;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackageView;
import jd.controlling.linkcrawler.modifier.CommentModifier;
import jd.controlling.linkcrawler.modifier.DownloadFolderModifier;
import jd.controlling.linkcrawler.modifier.PackageNameModifier;
import jd.plugins.DownloadLink;

import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.net.Base64InputStream;
import org.appwork.utils.net.CharSequenceInputStream;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.content.v2.ContentAPIImplV2;
import org.jdownloader.api.utils.PackageControllerUtils;
import org.jdownloader.api.utils.SelectionInfoUtils;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.bindings.CleanupActionOptions;
import org.jdownloader.myjdownloader.client.bindings.PriorityStorable;
import org.jdownloader.myjdownloader.client.bindings.UrlDisplayTypeStorable;
import org.jdownloader.myjdownloader.client.bindings.interfaces.LinkgrabberInterface;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.UrlDisplayType;

public class LinkCollectorAPIImplV2 implements LinkCollectorAPIV2 {
    private LogSource                                                 logger;
    private final PackageControllerUtils<CrawledPackage, CrawledLink> packageControllerUtils;

    public LinkCollectorAPIImplV2() {
        RemoteAPIController.validateInterfaces(LinkCollectorAPIV2.class, LinkgrabberInterface.class);
        packageControllerUtils = new PackageControllerUtils<CrawledPackage, CrawledLink>(LinkCollector.getInstance());
        logger = LogController.getInstance().getLogger(LinkCollectorAPIImplV2.class.getName());
    }

    @Override
    public ArrayList<CrawledPackageAPIStorableV2> queryPackages(CrawledPackageQueryStorable queryParams) throws BadParameterException {
        ArrayList<CrawledPackageAPIStorableV2> result = new ArrayList<CrawledPackageAPIStorableV2>();
        LinkCollector lc = LinkCollector.getInstance();
        // filter out packages, if specific packageUUIDs given, else return all packages
        List<CrawledPackage> packages;
        if (queryParams.getPackageUUIDs() != null && queryParams.getPackageUUIDs().length > 0) {
            packages = packageControllerUtils.getPackages(queryParams.getPackageUUIDs());
        } else {
            packages = lc.getPackagesCopy();
        }
        if (packages.size() == 0) {
            return result;
        }
        int startWith = queryParams.getStartAt();
        int maxResults = queryParams.getMaxResults();
        if (startWith > packages.size() - 1) {
            return result;
        }
        if (startWith < 0) {
            startWith = 0;
        }
        if (maxResults < 0) {
            maxResults = packages.size();
        }
        for (int i = startWith; i < startWith + maxResults; i++) {
            final CrawledPackage pkg = packages.get(i);
            boolean readL = pkg.getModifyLock().readLock();
            try {
                CrawledPackageAPIStorableV2 cps = new CrawledPackageAPIStorableV2(pkg);
                final CrawledPackageView view = new CrawledPackageView(pkg);
                view.aggregate();
                if (queryParams.isSaveTo()) {
                    cps.setSaveTo(LinkTreeUtils.getDownloadDirectory(pkg).toString());
                }
                if (queryParams.isBytesTotal()) {
                    cps.setBytesTotal(view.getFileSize());
                }
                if (queryParams.isChildCount()) {
                    cps.setChildCount(view.size());
                }
                if (queryParams.isPriority()) {
                    cps.setPriority(PriorityStorable.get(pkg.getPriorityEnum().name()));
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
                if (i == packages.size() - 1) {
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
        final List<CrawledPackage> matched;
        if (queryParams.getPackageUUIDs() != null && queryParams.getPackageUUIDs().length > 0) {
            matched = packageControllerUtils.getPackages(queryParams.getPackageUUIDs());
        } else {
            matched = lc.getPackagesCopy();
        }
        final List<CrawledLink> links = new ArrayList<CrawledLink>();
        if (queryParams.getJobUUIDs() != null && queryParams.getJobUUIDs().length > 0) {
            final Set<Long> jobUUIDs = new HashSet<Long>();
            for (final long id : queryParams.getJobUUIDs()) {
                jobUUIDs.add(id);
            }
            for (CrawledPackage pkg : matched) {
                final boolean readL = pkg.getModifyLock().readLock();
                try {
                    for (CrawledLink link : pkg.getChildren()) {
                        if (jobUUIDs.contains(link.getJobID())) {
                            links.add(link);
                        }
                    }
                } finally {
                    pkg.getModifyLock().readUnlock(readL);
                }
            }
        } else {
            // collect children of the selected packages and convert to storables for response
            for (CrawledPackage pkg : matched) {
                final boolean readL = pkg.getModifyLock().readLock();
                try {
                    links.addAll(pkg.getChildren());
                } finally {
                    pkg.getModifyLock().readUnlock(readL);
                }
            }
        }
        if (links.isEmpty()) {
            return result;
        }
        int startWith = queryParams.getStartAt();
        int maxResults = queryParams.getMaxResults();
        if (startWith > links.size() - 1) {
            return result;
        }
        if (startWith < 0) {
            startWith = 0;
        }
        if (maxResults < 0) {
            maxResults = links.size();
        }
        for (int i = startWith; i < Math.min(startWith + maxResults, links.size()); i++) {
            CrawledLink cl = links.get(i);
            CrawledLinkAPIStorableV2 cls = toStorable(queryParams, cl);
            result.add(cls);
        }
        return result;
    }

    public static CrawledLinkAPIStorableV2 toStorable(CrawledLinkQueryStorable queryParams, CrawledLink cl) {
        CrawledLinkAPIStorableV2 cls = new CrawledLinkAPIStorableV2(cl);
        ContentAPIImplV2 contentAPI = RemoteAPIController.getInstance().getContentAPI();
        if (queryParams.isPassword() && cl.getDownloadLink() != null) {
            cls.setDownloadPassword(cl.getDownloadLink().getDownloadPassword());
        }
        if (queryParams.isPriority()) {
            cls.setPriority(org.jdownloader.myjdownloader.client.bindings.PriorityStorable.get(cl.getPriority().name()));
        }
        if (queryParams.isVariantID() || queryParams.isVariantName() || queryParams.isVariantIcon() || queryParams.isVariants()) {
            try {
                if (cl.hasVariantSupport()) {
                    if (queryParams.isVariants()) {
                        cls.setVariants(true);
                    }
                    if (queryParams.isVariantID() || queryParams.isVariantName() || queryParams.isVariantIcon()) {
                        LinkVariant v = cl.getDownloadLink().getDefaultPlugin().getActiveVariantByLink(cl.getDownloadLink());
                        LinkVariantStorableV2 s = new LinkVariantStorableV2();
                        if (v != null) {
                            if (queryParams.isVariantID()) {
                                s.setId(v._getUniqueId());
                            }
                            if (queryParams.isVariantName()) {
                                s.setName(v._getName(cl));
                            }
                            if (queryParams.isVariantIcon()) {
                                Icon icon = v._getIcon(cl);
                                if (icon != null) {
                                    s.setIconKey(contentAPI.getIconKey(icon));
                                }
                            }
                        }
                        cls.setVariant(s);
                    }
                }
            } catch (Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
            }
        }
        if (queryParams.isComment()) {
            cls.setComment(cl.getComment());
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
        return cls;
    }

    @Override
    public int getPackageCount() {
        return LinkCollector.getInstance().size();
    }

    @Override
    public LinkCollectingJobAPIStorable addLinks(final AddLinksQueryStorable query) {
        return add(query);
    }

    private static List<File> processDataURLs(final AddLinksQueryStorable query) {
        final List<File> ret = new ArrayList<File>();
        final String[] dataURLs = query.getDataURLs();
        if (dataURLs != null) {
            for (final String dataURL : dataURLs) {
                final String extension = new Regex(dataURL, "data:application/([a-z0-9A-Z]{1,4})").getMatch(0);
                if (extension != null) {
                    final File tmp = Application.getTempResource("linkcollectorAPI" + System.nanoTime() + "." + extension);
                    try {
                        if (tmp.exists() && !tmp.delete()) {
                            throw new IOException("Failed to delete tmp file:" + tmp);
                        }
                        final InputStream is = getInputStreamFromBase64DataURL(dataURL);
                        final FileOutputStream fos = new FileOutputStream(tmp);
                        try {
                            final byte[] buf = new byte[8192];
                            int read = 0;
                            while ((read = is.read(buf)) != -1) {
                                fos.write(buf, 0, read);
                            }
                        } finally {
                            fos.close();
                        }
                        ret.add(tmp);
                    } catch (final IOException e) {
                        tmp.delete();
                        LogController.getInstance().getLogger(LinkCollectorAPIImplV2.class.getName()).log(e);
                    }
                }
            }
            // clear reference
            query.setDataURLs(null);
        }
        return ret;
    }

    public static LinkCollectingJobAPIStorable add(final AddLinksQueryStorable query) {
        Priority p = Priority.DEFAULT;
        try {
            p = Priority.valueOf(query.getPriority().name());
        } catch (Throwable ignore) {
        }
        final Priority fp = p;
        final StringBuilder sb = new StringBuilder();
        if (query.getLinks() != null) {
            sb.append(query.getLinks());
            query.setLinks(null);
        }
        final List<File> files = processDataURLs(query);
        if (files != null) {
            for (final File file : files) {
                if (sb.length() > 0) {
                    sb.append("\r\n");
                }
                sb.append(file.toURI().toString());
            }
        }
        final LinkCollectingJob job = new LinkCollectingJob(LinkOrigin.MYJD.getLinkOriginDetails(), sb.toString());
        job.setCustomSourceUrl(query.getSourceUrl());
        job.setAssignJobID(Boolean.TRUE.equals(query.isAssignJobID()));
        final boolean overwritePackagizerRules = Boolean.TRUE.equals(query.isOverwritePackagizerRules());
        final HashSet<String> finalExtPws;
        if (StringUtils.isNotEmpty(query.getExtractPassword())) {
            finalExtPws = new HashSet<String>();
            finalExtPws.add(query.getExtractPassword());
        } else {
            finalExtPws = null;
        }
        final List<CrawledLinkModifier> modifiers = new ArrayList<CrawledLinkModifier>();
        if (StringUtils.isNotEmpty(query.getComment())) {
            modifiers.add(new CommentModifier(query.getComment()));
        }
        if (StringUtils.isNotEmpty(query.getPackageName())) {
            modifiers.add(new PackageNameModifier(query.getPackageName(), overwritePackagizerRules));
        }
        if (StringUtils.isNotEmpty(query.getDestinationFolder())) {
            modifiers.add(new DownloadFolderModifier(query.getDestinationFolder(), overwritePackagizerRules));
        }
        if (StringUtils.isNotEmpty(query.getDownloadPassword())) {
            modifiers.add(new CrawledLinkModifier() {
                final String downloadPassword = query.getDownloadPassword();

                @Override
                public void modifyCrawledLink(CrawledLink link) {
                    final DownloadLink dlLink = link.getDownloadLink();
                    if (dlLink != null) {
                        dlLink.setDownloadPassword(downloadPassword);
                    }
                }
            });
        }
        if (query.isAutostart() != null) {
            final boolean autostart = Boolean.TRUE.equals(query.isAutostart());
            modifiers.add(new CrawledLinkModifier() {
                @Override
                public void modifyCrawledLink(CrawledLink link) {
                    link.setAutoConfirmEnabled(autostart);
                    link.setAutoStartEnabled(autostart);
                }
            });
        }
        if (finalExtPws != null && finalExtPws.size() > 0) {
            modifiers.add(new CrawledLinkModifier() {
                @Override
                public void modifyCrawledLink(CrawledLink link) {
                    link.getArchiveInfo().getExtractionPasswords().addAll(finalExtPws);
                }
            });
        }
        if (query.isAutoExtract() != null) {
            modifiers.add(new CrawledLinkModifier() {
                final BooleanStatus autoExtract = BooleanStatus.convert(query.isAutoExtract());

                @Override
                public void modifyCrawledLink(CrawledLink link) {
                    link.getArchiveInfo().setAutoExtract(autoExtract);
                }
            });
        }
        if (!Priority.DEFAULT.equals(fp)) {
            modifiers.add(new CrawledLinkModifier() {
                @Override
                public void modifyCrawledLink(CrawledLink link) {
                    link.setPriority(fp);
                }
            });
        }
        switch (BooleanStatus.convert(query.isDeepDecrypt())) {
        case TRUE:
            job.setDeepAnalyse(true);
            break;
        case FALSE:
            job.setDeepAnalyse(false);
            break;
        default:
            break;
        }
        if (modifiers.size() > 0) {
            final CrawledLinkModifier modifier = new CrawledLinkModifier() {
                @Override
                public void modifyCrawledLink(CrawledLink link) {
                    for (CrawledLinkModifier mod : modifiers) {
                        mod.modifyCrawledLink(link);
                    }
                }
            };
            if (overwritePackagizerRules) {
                job.addPostPackagizerModifier(modifier);
            } else {
                job.addPrePackagizerModifier(modifier);
            }
        }
        LinkCollector.getInstance().getAddLinksThread(job, null).start();
        return new LinkCollectingJobAPIStorable(job);
    }

    @Override
    public long getChildrenChanged(long structureWatermark) {
        return packageControllerUtils.getChildrenChanged(structureWatermark);
    }

    @Override
    public void moveToDownloadlist(final long[] linkIds, final long[] packageIds) throws BadParameterException {
        SelectionInfo<CrawledPackage, CrawledLink> selectionInfo = packageControllerUtils.getSelectionInfo(linkIds, packageIds);
        LinkCollector.getInstance().moveLinksToDownloadList(new MoveLinksSettings(MoveLinksMode.MANUAL, org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.isEnabled(), null, null), selectionInfo);
    }

    @Override
    public void removeLinks(final long[] linkIds, final long[] packageIds) throws BadParameterException {
        packageControllerUtils.remove(linkIds, packageIds);
    }

    @Override
    public void renameLink(long linkId, String newName) throws BadParameterException {
        final List<CrawledLink> children = packageControllerUtils.getChildren(linkId);
        if (children.size() > 0) {
            children.get(0).setName(newName);
        }
    }

    @Override
    public void renamePackage(long packageId, String newName) throws BadParameterException {
        final List<CrawledPackage> selectionInfo = packageControllerUtils.getPackages(packageId);
        if (selectionInfo.size() > 0) {
            final CrawledPackage lc = selectionInfo.get(0);
            if (lc != null) {
                lc.setName(newName);
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled, final long[] linkIds, final long[] packageIds) throws BadParameterException {
        packageControllerUtils.setEnabled(enabled, linkIds, packageIds);
    }

    @Override
    public void movePackages(long[] packageIds, long afterDestPackageId) throws BadParameterException {
        packageControllerUtils.movePackages(packageIds, afterDestPackageId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void moveLinks(long[] linkIds, long afterLinkID, long destPackageID) throws BadParameterException {
        packageControllerUtils.moveChildren(linkIds, afterLinkID, destPackageID);
    }

    @Override
    public List<String> getDownloadFolderHistorySelectionBase() {
        return DownloadPathHistoryManager.getInstance().listPaths(org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder());
    }

    @Override
    public List<LinkVariantStorableV2> getVariants(long linkid) throws BadParameterException {
        final ArrayList<LinkVariantStorableV2> ret = new ArrayList<LinkVariantStorableV2>();
        final List<CrawledLink> children = packageControllerUtils.getChildren(linkid);
        if (children.size() > 0) {
            final CrawledLink cl = children.get(0);
            for (LinkVariant lv : cl.getDownloadLink().getDefaultPlugin().getVariantsByLink(cl.getDownloadLink())) {
                ret.add(new LinkVariantStorableV2(lv._getUniqueId(), lv._getName(cl)));
            }
        }
        return ret;
    }

    @Override
    public void setVariant(long linkid, String variantID) throws BadParameterException {
        final List<CrawledLink> children = packageControllerUtils.getChildren(linkid);
        if (children.size() > 0) {
            final CrawledLink cl = children.get(0);
            if (cl != null) {
                for (LinkVariant lv : cl.getDownloadLink().getDefaultPlugin().getVariantsByLink(cl.getDownloadLink())) {
                    if (lv._getUniqueId().equals(variantID)) {
                        LinkCollector.getInstance().setActiveVariantForLink(cl, lv);
                        return;
                    }
                }
                throw new BadParameterException("Unknown variantID");
            }
        }
    }

    @Override
    public void addVariantCopy(long linkid, final long destinationAfterLinkID, final long destinationPackageID, final String variantID) throws BadParameterException {
        List<CrawledLink> children = packageControllerUtils.getChildren(linkid);
        if (children.size() > 0) {
            final CrawledLink link = children.get(0);
            if (link != null) {
                // move and add
                LinkCollector.getInstance().getQueue().add(new QueueAction<Void, BadParameterException>() {
                    @Override
                    protected Void run() throws BadParameterException {
                        // search variant by id
                        LinkVariant v = null;
                        for (LinkVariant lv : link.getDownloadLink().getDefaultPlugin().getVariantsByLink(link.getDownloadLink())) {
                            if (lv._getUniqueId().equals(variantID)) {
                                v = lv;
                                break;
                            }
                        }
                        if (v == null) {
                            throw new BadParameterException("Unknown variantID");
                        }
                        // create new downloadlink
                        final DownloadLink dllink = new DownloadLink(link.getDownloadLink().getDefaultPlugin(), link.getDownloadLink().getView().getDisplayName(), link.getDownloadLink().getHost(), link.getDownloadLink().getPluginPatternMatcher(), true);
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
                                if (dllink.getLinkID().equals(cLink.getLinkID())) {
                                    throw new BadParameterException("Variant is already in this package");
                                }
                            }
                        } finally {
                            link.getParentNode().getModifyLock().readUnlock(readL);
                        }
                        if (destinationPackageID < 0) {
                            LinkCollector.getInstance().moveOrAddAt(link.getParentNode(), list, link.getParentNode().indexOf(link) + 1);
                        } else {
                            LinkCollector dlc = LinkCollector.getInstance();
                            CrawledLink afterLink = null;
                            CrawledPackage destpackage = null;
                            if (destinationAfterLinkID > 0) {
                                List<CrawledLink> children = packageControllerUtils.getChildren(destinationAfterLinkID);
                                if (children.size() > 0) {
                                    afterLink = children.get(0);
                                }
                            }
                            if (destinationPackageID > 0) {
                                List<CrawledPackage> packages = packageControllerUtils.getPackages(destinationPackageID);
                                if (packages.size() > 0) {
                                    destpackage = packages.get(0);
                                }
                            }
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
        }
    }

    public static InputStream getInputStreamFromBase64DataURL(final String dataURL) throws IOException {
        final int base64Index = dataURL.indexOf(";base64,");
        if (base64Index == -1 || base64Index + 8 >= dataURL.length()) {
            throw new IOException("Invalid DataURL: " + dataURL);
        }
        return new Base64InputStream(new CharSequenceInputStream(dataURL, base64Index + 8));
    }

    @Override
    public LinkCollectingJobAPIStorable addContainer(String type, String content) {
        return loadContainer(type, content);
    }

    public static LinkCollectingJobAPIStorable loadContainer(String type, String content) {
        final String fileName;
        if ("DLC".equalsIgnoreCase(type)) {
            fileName = "linkcollectorDLCAPI" + System.nanoTime() + ".dlc";
        } else if ("RSDF".equalsIgnoreCase(type)) {
            fileName = "linkcollectorDLCAPI" + System.nanoTime() + ".rsdf";
        } else if ("CCF".equalsIgnoreCase(type)) {
            fileName = "linkcollectorDLCAPI" + System.nanoTime() + ".ccf";
        } else if ("CRAWLJOB".equalsIgnoreCase(type)) {
            fileName = "linkcollectorDLCAPI" + System.nanoTime() + ".crawljob";
        } else {
            fileName = null;
        }
        if (fileName != null) {
            final File tmp = Application.getTempResource(fileName);
            try {
                if (tmp.exists() && !tmp.delete()) {
                    throw new IOException("Failed to delete tmp file:" + tmp);
                }
                final InputStream is = getInputStreamFromBase64DataURL(content);
                final FileOutputStream fos = new FileOutputStream(tmp);
                try {
                    final byte[] buf = new byte[8192];
                    int read = 0;
                    while ((read = is.read(buf)) != -1) {
                        fos.write(buf, 0, read);
                    }
                } finally {
                    fos.close();
                }
                LinkCollectingJob job = new LinkCollectingJob(LinkOrigin.MYJD.getLinkOriginDetails(), tmp.toURI().toString());
                LinkCollector.getInstance().getAddLinksThread(job, null).start();
                return new LinkCollectingJobAPIStorable(job);
            } catch (IOException e) {
                tmp.delete();
                LogController.getInstance().getLogger(LinkCollectorAPIImplV2.class.getName()).log(e);
            }
        }
        return null;
    }

    @Override
    public void setPriority(PriorityStorable priority, long[] linkIds, long[] packageIds) throws BadParameterException {
        final org.jdownloader.controlling.Priority jdPriority = org.jdownloader.controlling.Priority.valueOf(priority.name());
        List<CrawledLink> children = packageControllerUtils.getChildren(linkIds);
        List<CrawledPackage> pkgs = packageControllerUtils.getPackages(packageIds);
        for (CrawledLink dl : children) {
            dl.setPriority(jdPriority);
        }
        for (CrawledPackage pkg : pkgs) {
            pkg.setPriorityEnum(jdPriority);
        }
    }

    @Override
    public void startOnlineStatusCheck(long[] linkIds, long[] packageIds) throws BadParameterException {
        packageControllerUtils.startOnlineStatusCheck(linkIds, packageIds);
    }

    @Override
    public Map<String, List<Long>> getDownloadUrls(final long[] linkIds, final long[] packageIds, UrlDisplayTypeStorable[] urlDisplayTypes) throws BadParameterException {
        final List<UrlDisplayType> types = new ArrayList<UrlDisplayType>();
        for (final UrlDisplayTypeStorable urlDisplayType : urlDisplayTypes) {
            try {
                types.add(UrlDisplayType.valueOf(urlDisplayType.name()));
            } catch (Exception e) {
                throw new BadParameterException(e.getMessage());
            }
        }
        return SelectionInfoUtils.getURLs(packageControllerUtils.getSelectionInfo(linkIds, packageIds), types);
    }

    @Override
    public void movetoNewPackage(long[] linkIds, long[] pkgIds, String newPkgName, String downloadPath) throws BadParameterException {
        packageControllerUtils.movetoNewPackage(linkIds, pkgIds, newPkgName, downloadPath);
    }

    @Override
    public void setDownloadDirectory(String directory, long[] packageIds) throws BadParameterException {
        if (StringUtils.isEmpty(directory)) {
            throw new BadParameterException("invalid dir");
        }
        packageControllerUtils.setDownloadDirectory(directory, packageIds);
    }

    @Override
    public void splitPackageByHoster(long[] linkIds, long[] pkgIds) {
        packageControllerUtils.splitPackageByHoster(linkIds, pkgIds);
    }

    @Override
    public void cleanup(final long[] linkIds, final long[] packageIds, final CleanupActionOptions.Action action, final CleanupActionOptions.Mode mode, final CleanupActionOptions.SelectionType selectionType) throws BadParameterException {
        packageControllerUtils.cleanup(linkIds, packageIds, action, mode, selectionType);
    }

    @Override
    public boolean clearList() {
        LinkCollector.getInstance().clear();
        return true;
    }

    @Override
    public boolean setDownloadPassword(final long[] linkIds, final long[] packageIds, final String pass) throws BadParameterException {
        return packageControllerUtils.setDownloadPassword(linkIds, packageIds, pass);
    }

    @Override
    public boolean abort() {
        LinkCollector.getInstance().abort();
        return true;
    }

    @Override
    public boolean abort(long jobId) {
        final List<JobLinkCrawler> jobs = LinkCollector.getInstance().getJobLinkCrawlerByJobId(jobId);
        boolean ret = false;
        for (final JobLinkCrawler job : jobs) {
            if (job.abort()) {
                ret = true;
            }
        }
        return ret;
    }

    @Override
    public boolean isCollecting() {
        return LinkCollector.getInstance().isCollecting();
    }

    @Override
    public List<JobLinkCrawlerAPIStorable> queryLinkCrawlerJobs(final LinkCrawlerJobsQueryStorable query) {
        final List<JobLinkCrawlerAPIStorable> result = new ArrayList<JobLinkCrawlerAPIStorable>();
        if (query.getJobIds() != null) {
            final List<JobLinkCrawler> jobs = LinkCollector.getInstance().getJobLinkCrawlerByJobId(query.getJobIds());
            for (final JobLinkCrawler job : jobs) {
                result.add(new JobLinkCrawlerAPIStorable(query, job));
            }
        }
        return result;
    }
}