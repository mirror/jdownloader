package org.jdownloader.gui.views.components.packagetable;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.gui.views.ArraySet;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.settings.GeneralSettings;

public class LinkTreeUtils {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T extends AbstractNode> java.util.List<T> getPackages(AbstractNode contextObject, java.util.List<AbstractNode> selection, java.util.List<T> container) {
        HashSet<T> ret = new HashSet<T>();
        if (contextObject != null) {
            if (contextObject instanceof AbstractPackageNode) {
                ret.add((T) contextObject);
            } else {
                ret.add((T) ((AbstractPackageChildrenNode) contextObject).getParentNode());
            }
        }
        if (selection != null) {
            for (AbstractNode a : selection) {
                if (a instanceof AbstractPackageNode) {
                    ret.add((T) a);
                } else {
                    ret.add((T) ((AbstractPackageChildrenNode) a).getParentNode());
                }
            }
        }
        container.addAll(ret);
        return container;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T extends AbstractNode> java.util.List<T> getSelectedChildren(List<AbstractNode> selection2, java.util.List<T> container) {
        HashSet<AbstractNode> has = new HashSet<AbstractNode>(selection2);
        HashSet<T> ret = new HashSet<T>();
        for (AbstractNode node : selection2) {
            if (node instanceof AbstractPackageChildrenNode) {
                ret.add((T) node);
            } else {

                // if we selected a package, and ALL it's links, we want all links
                // if we selected a package, and only a few links, we probably want only these few links.
                // if we selected a package, and it is NOT expanded, we want all links
                boolean readL = ((AbstractPackageNode) node).getModifyLock().readLock();
                try {
                    if (!((AbstractPackageNode) node).isExpanded()) {
                        // add allTODO
                        List<T> childs = ((AbstractPackageNode) node).getChildren();
                        ret.addAll(childs);
                        // LinkGrabberTableModel.getInstance().getAllChildrenNodes()
                    } else {
                        List<T> childs = ((AbstractPackageNode) node).getChildren();
                        boolean containsNone = true;
                        boolean containsAll = true;
                        for (AbstractNode l : childs) {
                            if (has.contains(l)) {
                                containsNone = false;
                            } else {
                                containsAll = false;
                            }

                        }
                        if (containsAll || containsNone) {
                            ret.addAll(childs);
                        }
                    }
                } finally {
                    ((AbstractPackageNode) node).getModifyLock().readUnlock(readL);
                }
            }
        }
        container.addAll(ret);
        return container;
    }

    public static File getDownloadDirectory(AbstractNode node) {
        String directory = null;
        if (node instanceof DownloadLink) {
            FilePackage parent = ((DownloadLink) node).getFilePackage();

            if (parent != null) {
                directory = parent.getView().getDownloadDirectory();
            }

            return getDownloadDirectory(directory, parent == null ? null : parent.getName());
        } else if (node instanceof FilePackage) {
            directory = ((FilePackage) node).getView().getDownloadDirectory();

            return getDownloadDirectory(directory, ((FilePackage) node).getName());
        } else if (node instanceof CrawledLink) {
            CrawledPackage parent = ((CrawledLink) node).getParentNode();
            if (parent != null) {
                directory = parent.getDownloadFolder();
            }

            return getDownloadDirectory(directory, parent == null ? null : parent.getName());
        } else if (node instanceof CrawledPackage) {
            directory = ((CrawledPackage) node).getDownloadFolder();

            return getDownloadDirectory(directory, ((CrawledPackage) node).getName());
        } else {
            throw new WTFException("Unknown Type: " + node.getClass());
        }

    }

    public static File getRawDownloadDirectory(AbstractNode node) {
        String directory = null;
        if (node instanceof DownloadLink) {
            FilePackage parent = ((DownloadLink) node).getFilePackage();

            if (parent != null) {
                directory = parent.getView().getDownloadDirectory();
            }

        } else if (node instanceof FilePackage) {
            directory = ((FilePackage) node).getView().getDownloadDirectory();
        } else if (node instanceof CrawledLink) {
            CrawledPackage parent = ((CrawledLink) node).getParentNode();
            if (parent != null) {
                directory = parent.getRawDownloadFolder();
            }
        } else if (node instanceof CrawledPackage) {
            directory = ((CrawledPackage) node).getRawDownloadFolder();
        } else {
            throw new WTFException("Unknown Type: " + node.getClass());
        }
        return getRawDownloadDirectory(directory);
    }

    private static File getRawDownloadDirectory(String path) {
        if (path == null) {
            return null;
        }

        if (CrossSystem.isAbsolutePath(path)) {
            return new File(path);
        } else {

            return new File(org.jdownloader.settings.staticreferences.CFG_GENERAL.DEFAULT_DOWNLOAD_FOLDER.getValue(), path);
        }
    }

    public static File getDownloadDirectory(String path, String packagename) {
        if (path == null) {
            return null;
        }
        path = PackagizerController.replaceDynamicTags(path, packagename);
        if (CrossSystem.isAbsolutePath(path)) {
            return new File(path);
        } else {

            return new File(PackagizerController.replaceDynamicTags(org.jdownloader.settings.staticreferences.CFG_GENERAL.DEFAULT_DOWNLOAD_FOLDER.getValue(), packagename), path);
        }
    }

    public static HashSet<String> getURLs(List<? extends AbstractNode> links, final boolean openInBrowser) {
        HashSet<String> urls = new HashSet<String>();
        if (links == null || links.size() == 0) {
            return urls;
        }

        String rawURL = null;
        ArraySet actualChildren = new SelectionInfo(null, links, true).getChildren();
        for (Object node : actualChildren) {
            DownloadLink link = null;
            if (node instanceof DownloadLink) {
                link = (DownloadLink) node;
            } else if (node instanceof CrawledLink) {
                link = ((CrawledLink) node).getDownloadLink();
            }
            if (link != null) {
                rawURL = link.getContentUrlOrPatternMatcher();
                urls.add(link.getView().getDisplayUrl());

            }
        }
        if (openInBrowser) {
            // should always open browserURL, otherwise you get users going to final links returned from decrypters into directhttp or
            // dedicated hoster plugins.
        } else if (actualChildren.size() == 1 && (rawURL != null && (!rawURL.matches("((?-i)ftp|https?)://.+"))) && JsonConfig.create(GeneralSettings.class).isCopySingleRealURL()) {
            // for 'copy urls' and 'open in browser', when youtube type of prefixes are pointless within this context! Only open rawURL when
            // URL are actually traditional browser URL structure.
        } else if (actualChildren.size() == 1 && rawURL != null && JsonConfig.create(GeneralSettings.class).isCopySingleRealURL()) {
            urls.clear();
            urls.add(rawURL);
        }
        return urls;
    }
}
