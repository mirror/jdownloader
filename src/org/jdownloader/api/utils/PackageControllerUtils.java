package org.jdownloader.api.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;

import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.FileCreationManager.DeleteOption;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.gui.packagehistorycontroller.DownloadPathHistoryManager;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.packagetable.PackageControllerSelectionInfo;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.LinkgrabberSettings;
import org.jdownloader.myjdownloader.client.bindings.CleanupActionOptions;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.translate._JDT;
import org.jdownloader.utils.JDFileUtils;

public class PackageControllerUtils<PackageType extends AbstractPackageNode<ChildType, PackageType>, ChildType extends AbstractPackageChildrenNode<PackageType>> {
    private final PackageController<PackageType, ChildType> packageController;

    public PackageControllerUtils(PackageController<PackageType, ChildType> packageController) {
        this.packageController = packageController;
    }

    public List<AbstractNode> getAbstractNodes(long[] linkIds, long[] packageIds) {
        final ArrayList<AbstractNode> ret = new ArrayList<AbstractNode>();
        if ((packageIds != null && packageIds.length > 0) || (linkIds != null && linkIds.length > 0)) {
            convertIdsToObjects(ret, linkIds, packageIds);
        }
        return ret;
    }

    public List<PackageType> getPackages(long... packageIds) {
        final List<PackageType> ret = new ArrayList<PackageType>();
        if (packageIds != null && packageIds.length > 0) {
            convertIdsToObjects(ret, null, packageIds);
        }
        return ret;
    }

    public List<ChildType> getChildren(long... linkIds) {
        final List<ChildType> ret = new ArrayList<ChildType>();
        if (linkIds != null && linkIds.length > 0) {
            convertIdsToObjects(ret, linkIds, null);
        }
        return ret;
    }

    public SelectionInfo<PackageType, ChildType> getSelectionInfo(long[] linkIds, long[] packageIds) {
        return new SelectionInfo<PackageType, ChildType>(null, getAbstractNodes(linkIds, packageIds));
    }

    public PackageType getPackageInstanceByChildrenType(ChildType ct) {
        if (ct instanceof DownloadLink) {
            return (PackageType) FilePackage.getInstance();
        } else if (ct instanceof CrawledLink) {
            return (PackageType) new CrawledPackage();
        }
        return null;
    }

    public PackageType getPackageInstance(PackageType ct) {
        if (ct instanceof FilePackage) {
            return (PackageType) FilePackage.getInstance();
        } else if (ct instanceof CrawledPackage) {
            return (PackageType) new CrawledPackage();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractNode> List<T> convertIdsToObjects(List<T> ret, long[] linkIds, long[] packageIds) {
        if (ret == null) {
            ret = new ArrayList<T>();
        }
        final HashSet<Long> linklookUp = createLookupSet(linkIds);
        final HashSet<Long> packageLookup = createLookupSet(packageIds);
        if (linklookUp != null || packageLookup != null) {
            boolean readL = packageController.readLock();
            try {
                main: for (PackageType pkg : packageController.getPackages()) {
                    System.out.println(pkg.getName() + " - " + pkg.getUniqueID().getID());
                    if (packageLookup != null && packageLookup.remove(pkg.getUniqueID().getID())) {
                        ret.add((T) pkg);
                        if ((packageLookup == null || packageLookup.size() == 0) && (linklookUp == null || linklookUp.size() == 0)) {
                            break main;
                        }
                    }
                    if (linklookUp != null) {
                        boolean readL2 = pkg.getModifyLock().readLock();
                        try {
                            for (ChildType child : pkg.getChildren()) {
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
                packageController.readUnlock(readL);
            }
        }
        return ret;
    }

    public static HashSet<Long> createLookupSet(long[] linkIds) {
        if (linkIds == null || linkIds.length == 0) {
            return null;
        }
        final HashSet<Long> linkLookup = new HashSet<Long>();
        for (long l : linkIds) {
            linkLookup.add(l);
        }
        return linkLookup;
    }

    public void setEnabled(boolean enabled, final long[] linkIds, final long[] packageIds) {
        final List<ChildType> sdl = getSelectionInfo(linkIds, packageIds).getChildren();
        for (ChildType dl : sdl) {
            dl.setEnabled(enabled);
        }
    }

    public void movePackages(long[] packageIds, long afterDestPackageId) {
        final List<PackageType> selectedPackages = getPackages(packageIds);
        PackageType afterDestPackage = null;
        if (afterDestPackageId > 0) {
            final List<PackageType> packages = getPackages(afterDestPackageId);
            if (packages.size() > 0) {
                afterDestPackage = packages.get(0);
            }
        }
        packageController.move(selectedPackages, afterDestPackage);
    }

    public void moveChildren(long[] linkIds, long afterLinkID, long destPackageID) {
        final List<PackageType> packages = getPackages(destPackageID);
        if (packages.size() > 0) {
            final List<ChildType> selectedLinks = getChildren(linkIds);
            ChildType afterLink = null;
            final PackageType destpackage = packages.get(0);
            if (afterLinkID > 0) {
                final List<ChildType> children = getChildren(afterLinkID);
                if (children.size() > 0) {
                    afterLink = children.get(0);
                }
            }
            packageController.move(selectedLinks, destpackage, afterLink);
        }
    }

    public long getChildrenChanged(long structureWatermark) {
        if (packageController.getBackendChanged() != structureWatermark) {
            return packageController.getBackendChanged();
        } else {
            return -1l;
        }
    }

    public void remove(final long[] linkIds, final long[] packageIds) {
        final List<ChildType> children = getChildren(linkIds);
        final List<PackageType> packages = getPackages(packageIds);
        packageController.removeChildren(children);
        for (final PackageType pkg : packages) {
            packageController.removePackage(pkg);
        }
    }

    public void startOnlineStatusCheck(long[] linkIds, long[] packageIds) {
        final SelectionInfo<PackageType, ChildType> selection = getSelectionInfo(linkIds, packageIds);
        final List<ChildType> children = selection.getChildren();
        final List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(children.size());
        for (final ChildType l : children) {
            if (l instanceof CheckableLink) {
                checkableLinks.add(((CheckableLink) l));
            }
        }
        if (checkableLinks.size() > 0) {
            final LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
            linkChecker.check(checkableLinks);
        }
    }

    public HashMap<Long, String> getDownloadUrls(final long[] linkIds, final long[] packageIds) {
        final SelectionInfo<PackageType, ChildType> selection = getSelectionInfo(linkIds, packageIds);
        final List<ChildType> children = selection.getChildren();
        final HashMap<Long, String> result = new HashMap<Long, String>();
        for (final ChildType l : children) {
            if (l instanceof DownloadLink) {
                final DownloadLink link = (DownloadLink) l;
                result.put(link.getUniqueID().getID(), link.getPluginPatternMatcher());
            } else if (l instanceof CrawledLink) {
                final CrawledLink link = (CrawledLink) l;
                result.put(link.getUniqueID().getID(), link.getURL());
            }
        }
        return result;
    }

    public void movetoNewPackage(long[] linkIds, long[] pkgIds, String newPkgName, String downloadPath) throws BadParameterException {
        if (StringUtils.isEmpty(newPkgName)) {
            throw new BadParameterException("empty package name");
        }
        final SelectionInfo<PackageType, ChildType> selection = getSelectionInfo(linkIds, pkgIds);
        if (selection.getChildren().size() > 0) {
            final PackageType pt = getPackageInstanceByChildrenType(selection.getChildren().get(0));
            setPackageName(pt, newPkgName);
            if (!StringUtils.isEmpty(downloadPath)) {
                setDirectory(pt, downloadPath);
            }
            if (pt != null) {
                packageController.moveOrAddAt(pt, selection.getChildren(), 0, -1);
            }
        }
    }

    public PackageType setPackageName(PackageType pt, String pkgName) {
        if (pt instanceof FilePackage) {
            ((FilePackage) pt).setName(pkgName);
        } else if (pt instanceof CrawledPackage) {
            ((CrawledPackage) pt).setName(pkgName);
        }
        return pt;
    }

    public PackageType copyPropertiesTo(PackageType sourcePkg, PackageType destPkg) {
        if (sourcePkg instanceof FilePackage && destPkg instanceof FilePackage) {
            ((FilePackage) sourcePkg).copyPropertiesTo((FilePackage) destPkg);
        } else if (sourcePkg instanceof CrawledPackage && destPkg instanceof CrawledPackage) {
            ((CrawledPackage) sourcePkg).copyPropertiesTo((CrawledPackage) destPkg);
        } else {
            throw new IllegalArgumentException("source and destination package not the same types");
        }
        return destPkg;
    }

    public PackageType setDirectory(PackageType pt, String directory) {
        if (pt instanceof FilePackage) {
            ((FilePackage) pt).setDownloadDirectory(directory);
            DownloadPathHistoryManager.getInstance().listPaths(directory);
        } else if (pt instanceof CrawledPackage) {
            ((CrawledPackage) pt).setDownloadFolder(directory);
            DownloadPathHistoryManager.getInstance().listPaths(directory);
        }
        return pt;
    }

    public void splitPackageByHoster(long[] linkIds, long[] pkgIds) {
        final SelectionInfo<PackageType, ChildType> selection = getSelectionInfo(linkIds, pkgIds);
        final HashMap<PackageType, HashMap<String, ArrayList<ChildType>>> splitMap = new HashMap<PackageType, HashMap<String, ArrayList<ChildType>>>();
        int insertAt = -1;
        for (AbstractNode child : selection.getChildren()) {
            final ChildType cL = (ChildType) child;
            final PackageType parent = cL.getParentNode();
            HashMap<String, ArrayList<ChildType>> parentMap = splitMap.get(parent);
            if (parentMap == null) {
                parentMap = new HashMap<String, ArrayList<ChildType>>();
                splitMap.put(parent, parentMap);
            }
            final String host = cL.getDomainInfo().getTld();
            ArrayList<ChildType> hostList = parentMap.get(host);
            if (hostList == null) {
                hostList = new ArrayList<ChildType>();
                parentMap.put(host, hostList);
            }
            hostList.add(cL);
        }
        final String nameFactory = JsonConfig.create(LinkgrabberSettings.class).getSplitPackageNameFactoryPattern();
        final Iterator<Entry<PackageType, HashMap<String, ArrayList<ChildType>>>> it = splitMap.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<PackageType, HashMap<String, ArrayList<ChildType>>> next = it.next();
            final PackageType sourcePackage = next.getKey();
            final HashMap<String, ArrayList<ChildType>> items = next.getValue();
            final Iterator<Entry<String, ArrayList<ChildType>>> it2 = items.entrySet().iterator();
            while (it2.hasNext()) {
                final Entry<String, ArrayList<ChildType>> next2 = it2.next();
                final String host = next2.getKey();
                final String newPackageName = getNewPackageName(nameFactory, sourcePackage.getName(), host);
                PackageType newPkg = getPackageInstanceByChildrenType(next2.getValue().get(0));
                copyPropertiesTo(sourcePackage, newPkg);
                setPackageName(newPkg, newPackageName);
                packageController.moveOrAddAt(newPkg, next2.getValue(), 0, insertAt);
                insertAt++;
            }
        }
    }

    public String getNewPackageName(String nameFactory, String oldPackageName, String host) {
        if (StringUtils.isEmpty(nameFactory)) {
            if (!StringUtils.isEmpty(oldPackageName)) {
                return oldPackageName;
            }
            return host;
        }
        if (!StringUtils.isEmpty(oldPackageName)) {
            nameFactory = nameFactory.replaceAll("\\{PACKAGENAME\\}", oldPackageName);
        } else {
            nameFactory = nameFactory.replaceAll("\\{PACKAGENAME\\}", _JDT.T.LinkCollector_addCrawledLink_variouspackage());
        }
        nameFactory = nameFactory.replaceAll("\\{HOSTNAME\\}", host);
        return nameFactory;
    }

    public void setDownloadDirectory(final String directory, long[] packageIds) {
        final SelectionInfo<PackageType, ChildType> selection = getSelectionInfo(new long[] {}, packageIds);
        for (PackageView<PackageType, ChildType> pkg : selection.getPackageViews()) {
            if (pkg.isPackageSelected()) {
                final PackageType pt = pkg.getPackage();
                final String finalDirectory = directory.replaceAll(PackagizerController.PACKAGETAG, pt.getName());
                if (pt instanceof FilePackage) {
                    DownloadWatchDog.getInstance().setDownloadDirectory((FilePackage) pt, finalDirectory);
                    DownloadPathHistoryManager.getInstance().add(directory);
                } else if (pt instanceof CrawledPackage) {
                    pt.getControlledBy().getQueue().add(new QueueAction<Void, RuntimeException>() {
                        @Override
                        protected Void run() throws RuntimeException {
                            ((CrawledPackage) pt).setDownloadFolder(finalDirectory);
                            DownloadPathHistoryManager.getInstance().add(directory);
                            return null;
                        }
                    });
                }
            }
        }
    }

    public boolean setDownloadPassword(final long[] linkIds, final long[] pkgIds, final String pass) throws BadParameterException {
        final SelectionInfo<PackageType, ChildType> selection = getSelectionInfo(linkIds, pkgIds);
        final List<ChildType> children = selection.getChildren();
        if (children.isEmpty()) {
            throw new BadParameterException("empty selection");
        } else {
            for (final ChildType child : children) {
                if (child instanceof DownloadLink) {
                    ((DownloadLink) child).setDownloadPassword(pass);
                } else if (child instanceof CrawledLink) {
                    ((CrawledLink) child).getDownloadLink().setDownloadPassword(pass);
                }
            }
            return true;
        }
    }

    /**
     * Executes cleanup actions on a selection
     *
     * @param linkIds
     * @param pkgIds
     * @param action
     * @param mode
     * @param selectionType
     * @return true if nodes were removed
     * @throws BadParameterException
     */
    public boolean cleanup(final long[] linkIds, final long[] pkgIds, final CleanupActionOptions.Action action, final CleanupActionOptions.Mode mode, final CleanupActionOptions.SelectionType selectionType) throws BadParameterException {
        final SelectionInfo<PackageType, ChildType> selection;
        if (CleanupActionOptions.SelectionType.ALL.equals(selectionType)) {
            selection = new PackageControllerSelectionInfo<PackageType, ChildType>(packageController);
        } else {
            selection = getSelectionInfo(linkIds, pkgIds);
        }
        if (selection.isEmpty()) {
            throw new BadParameterException("empty selection");
        } else {
            final List<ChildType> nodesToDelete = new ArrayList<ChildType>();
            switch (selectionType) {
            case ALL:
            case SELECTED:
                for (ChildType ct : selection.getChildren()) {
                    if (shouldDeleteLink(action, ct)) {
                        nodesToDelete.add(ct);
                    }
                }
                break;
            case UNSELECTED:
                // TODO: implement after remote views were implemented
                throw new BadParameterException("SelectionType UNSELECTED not yet supported");
            case NONE:
                return false;
            }
            if (nodesToDelete.size() > 0) {
                packageController.removeChildren(nodesToDelete);
                if (nodesToDelete.get(0) instanceof DownloadLink) {
                    final List<DownloadLink> links = (List<DownloadLink>) nodesToDelete;
                    switch (mode) {
                    case REMOVE_LINKS_ONLY:
                        break;
                    case REMOVE_LINKS_AND_DELETE_FILES:
                        DownloadWatchDog.getInstance().delete(links, DeleteOption.NULL);
                        break;
                    case REMOVE_LINKS_AND_RECYCLE_FILES:
                        DownloadWatchDog.getInstance().delete(links, JDFileUtils.isTrashSupported() ? DeleteOption.RECYCLE : DeleteOption.NULL);
                        break;
                    }
                }
                return true;
            }
        }
        return false;
    }

    protected boolean shouldDeleteLink(CleanupActionOptions.Action action, ChildType ct) {
        switch (action) {
        case DELETE_ALL:
            return true;
        case DELETE_DISABLED:
            return !ct.isEnabled();
        case DELETE_FAILED:
            if (ct instanceof DownloadLink) {
                return FinalLinkState.CheckFailed(((DownloadLink) ct).getFinalLinkState());
            } else {
                return false;
            }
        case DELETE_FINISHED:
            if (ct instanceof DownloadLink) {
                return FinalLinkState.CheckFinished(((DownloadLink) ct).getFinalLinkState());
            } else {
                return false;
            }
        case DELETE_OFFLINE:
            if (ct instanceof DownloadLink) {
                return ((DownloadLink) ct).getFinalLinkState() == FinalLinkState.OFFLINE;
            } else if (ct instanceof CrawledLink) {
                return ((CrawledLink) ct).getDownloadLink().getAvailableStatus() == AvailableStatus.FALSE;
            } else {
                return false;
            }
        case DELETE_DUPE:
            if (ct instanceof CrawledLink) {
                return DownloadController.getInstance().hasDownloadLinkByID(((CrawledLink) ct).getLinkID());
            } else {
                return false;
            }
        default:
            return false;
        }
    }
}