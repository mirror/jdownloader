package org.jdownloader.api.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.views.SelectionInfo;

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
        HashSet<Long> linkLookup = new HashSet<Long>();
        for (long l : linkIds) {
            linkLookup.add(l);
        }
        return linkLookup;
    }

    public void setEnabled(boolean enabled, final long[] linkIds, final long[] packageIds) {
        List<ChildType> sdl = getSelectionInfo(linkIds, packageIds).getChildren();
        for (ChildType dl : sdl) {
            dl.setEnabled(enabled);
        }
    }

    public void movePackages(long[] packageIds, long afterDestPackageId) {
        List<PackageType> selectedPackages = getPackages(packageIds);
        PackageType afterDestPackage = null;
        if (afterDestPackageId > 0) {
            List<PackageType> packages = getPackages(afterDestPackageId);
            if (packages.size() > 0) {
                afterDestPackage = packages.get(0);
            }
        }
        packageController.move(selectedPackages, afterDestPackage);
    }

    public void moveChildren(long[] linkIds, long afterLinkID, long destPackageID) {
        List<ChildType> selectedLinks = getChildren(linkIds);
        ChildType afterLink = null;

        List<PackageType> packages = getPackages(destPackageID);
        if (packages.size() > 0) {
            PackageType destpackage = packages.get(0);
            if (afterLinkID > 0) {
                List<ChildType> children = getChildren(afterLinkID);
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
        List<ChildType> children = getChildren(linkIds);
        List<PackageType> packages = getPackages(packageIds);
        packageController.removeChildren(children);

        for (PackageType pkg : packages) {
            packageController.removePackage(pkg);
        }
    }

    public void startOnlineStatusCheck(long[] linkIds, long[] packageIds) {
        SelectionInfo<PackageType, ChildType> selection = getSelectionInfo(linkIds, packageIds);

        final List<?> children = selection.getChildren();
        final List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(children.size());
        for (Object l : children) {
            if (l instanceof DownloadLink || l instanceof CrawledLink) {
                checkableLinks.add(((CheckableLink) l));
            }
        }
        final LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
        linkChecker.check(checkableLinks);
    }

    public HashMap<Long, String> getDownloadUrls(final long[] linkIds, final long[] packageIds) {
        SelectionInfo<PackageType, ChildType> selection = getSelectionInfo(linkIds, packageIds);

        List<?> children = selection.getChildren();
        HashMap<Long, String> result = new HashMap<>();
        for (Object l : children) {
            if (l instanceof DownloadLink) {
                DownloadLink link = (DownloadLink) l;
                result.put(link.getUniqueID().getID(), link.getPluginPatternMatcher());
            }
        }
        return result;
    }

}
