package org.jdownloader.gui.views.linkgrabber;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

public class LinkTreeUtils {

    public static ArrayList<CrawledLink> getChildren(ArrayList<AbstractNode> selection) {
        ArrayList<CrawledLink> ret = new ArrayList<CrawledLink>();
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

    public static ArrayList<CrawledPackage> getPackages(AbstractNode contextObject, ArrayList<AbstractNode> selection) {

        HashSet<CrawledPackage> ret = new HashSet<CrawledPackage>();
        if (contextObject != null) {
            if (contextObject instanceof CrawledPackage) {
                ret.add((CrawledPackage) contextObject);
            } else {
                ret.add((CrawledPackage) ((CrawledLink) contextObject).getParentNode());
            }

        }
        if (selection != null) {
            for (AbstractNode a : selection) {

                if (a instanceof CrawledPackage) {
                    ret.add((CrawledPackage) a);
                } else {
                    ret.add((CrawledPackage) ((CrawledLink) a).getParentNode());
                }
            }
        }
        return new ArrayList<CrawledPackage>(ret);
    }

    public static ArrayList<CrawledLink> getSelectedChildren(ArrayList<AbstractNode> selection2) {
        HashSet<AbstractNode> has = new HashSet<AbstractNode>(selection2);
        HashSet<CrawledLink> ret = new HashSet<CrawledLink>();
        for (AbstractNode node : selection2) {
            if (node instanceof CrawledLink) {
                ret.add((CrawledLink) node);
            } else {

                // if we selected a package, and ALL it's links, we want all
                // links
                // if we selected a package, and nly afew links, we probably
                // want only these few links.
                // if we selected a package, and it is NOT expanded, we want all
                // links

                if (!((CrawledPackage) node).isExpanded()) {
                    // add allTODO
                    List<CrawledLink> childs = ((CrawledPackage) node).getChildren();
                    ret.addAll(childs);
                    // LinkGrabberTableModel.getInstance().getAllChildrenNodes()
                } else {
                    List<CrawledLink> childs = ((CrawledPackage) node).getChildren();
                    boolean containsNone = true;
                    boolean containsAll = true;
                    for (CrawledLink l : childs) {
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

        return new ArrayList<CrawledLink>(ret);
    }

}
