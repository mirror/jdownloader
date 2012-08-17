package org.jdownloader.gui.views.components.packagetable;

import java.io.File;
import java.util.ArrayList;
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
import org.appwork.utils.os.CrossSystem;

public class LinkTreeUtils {

    public static java.util.List<CrawledLink> getChildren(java.util.List<AbstractNode> selection) {
        java.util.List<CrawledLink> ret = new ArrayList<CrawledLink>();
        for (AbstractNode a : selection) {
            if (a instanceof CrawledLink) {
                ret.add((CrawledLink) a);
            } else if (a instanceof CrawledPackage) {
                if (!((CrawledPackage) a).isExpanded()) {
                    ret.addAll(((CrawledPackage) a).getChildren());
                }
            }
        }
        return ret;
    }

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

                // if we selected a package, and ALL it's links, we want all
                // links
                // if we selected a package, and nly afew links, we probably
                // want only these few links.
                // if we selected a package, and it is NOT expanded, we want all
                // links

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
            }
        }
        container.addAll(ret);
        return container;
    }

    public static File getDownloadDirectory(AbstractNode node) {
        String directory = null;
        if (node instanceof DownloadLink) {
            FilePackage parent = ((DownloadLink) node).getFilePackage();
            if (parent != null) directory = parent.getDownloadDirectory();
        } else if (node instanceof FilePackage) {
            directory = ((FilePackage) node).getDownloadDirectory();
        } else if (node instanceof CrawledLink) {
            CrawledPackage parent = ((CrawledLink) node).getParentNode();
            if (parent != null) directory = parent.getDownloadFolder();
        } else if (node instanceof CrawledPackage) {
            directory = ((CrawledPackage) node).getDownloadFolder();
        } else
            throw new WTFException("Unknown Type: " + node.getClass());
        return getDownloadDirectory(directory);
    }

    public static File getRawDownloadDirectory(AbstractNode node) {
        String directory = null;
        if (node instanceof DownloadLink) {
            FilePackage parent = ((DownloadLink) node).getFilePackage();
            if (parent != null) directory = parent.getDownloadDirectory();
        } else if (node instanceof FilePackage) {
            directory = ((FilePackage) node).getDownloadDirectory();
        } else if (node instanceof CrawledLink) {
            CrawledPackage parent = ((CrawledLink) node).getParentNode();
            if (parent != null) directory = parent.getRawDownloadFolder();
        } else if (node instanceof CrawledPackage) {
            directory = ((CrawledPackage) node).getRawDownloadFolder();
        } else
            throw new WTFException("Unknown Type: " + node.getClass());
        return getDownloadDirectory(directory);
    }

    public static File getDownloadDirectory(String path) {
        if (path == null) return null;
        if (CrossSystem.isAbsolutePath(path)) {
            return new File(path);
        } else {
            return new File(org.jdownloader.settings.staticreferences.CFG_GENERAL.DEFAULT_DOWNLOAD_FOLDER.getValue(), path);
        }
    }

    public static HashSet<String> getURLs(List<AbstractNode> links) {
        HashSet<String> urls = new HashSet<String>();
        if (links == null) return urls;
        String rawURL = null;
        for (AbstractNode node : links) {
            DownloadLink link = null;
            if (node instanceof DownloadLink) {
                link = (DownloadLink) node;
            } else if (node instanceof CrawledLink) {
                link = ((CrawledLink) node).getDownloadLink();
            } else if (node instanceof AbstractPackageNode) {
                List<AbstractNode> children = null;
                synchronized (node) {
                    children = ((AbstractPackageNode) node).getChildren();
                }
                urls.addAll(getURLs(children));
            }
            if (link != null) {
                rawURL = link.getDownloadURL();
                if (DownloadLink.LINKTYPE_CONTAINER != link.getLinkType()) {
                    if (link.gotBrowserUrl()) {
                        urls.add(link.getBrowserUrl());
                    } else {
                        urls.add(link.getDownloadURL());
                    }
                }
            }
        }
        if (links.size() == 1 && rawURL != null) {
            urls.clear();
            urls.add(rawURL);
        }
        return urls;
    }

}
