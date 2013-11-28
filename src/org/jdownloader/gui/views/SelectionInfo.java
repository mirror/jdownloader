package org.jdownloader.gui.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;

import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;

public class SelectionInfo<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> {

    private static List<AbstractNode> pack(AbstractNode clicked) {
        java.util.List<AbstractNode> ret = new ArraySet<AbstractNode>();
        ret.add(clicked);
        return ret;
    }

    private ArrayList<PackageView<PackageType, ChildrenType>>                  packageViews;
    private ArrayList<PluginView<ChildrenType>>                                pluginViews;
    private AbstractNode                                                       contextObject;

    private List<? extends AbstractNode>                                       rawSelection;

    private ArraySet<ChildrenType>                                             children;

    private List<PackageControllerTableModelFilter<PackageType, ChildrenType>> filters = null;

    public List<PackageControllerTableModelFilter<PackageType, ChildrenType>> getTableFilters() {
        return filters;
    }

    private ArraySet<AbstractNode>                                       raw;
    private HashMap<PluginForHost, PluginView<ChildrenType>>             pluginView;

    private HashMap<PackageType, PackageView<PackageType, ChildrenType>> view;
    private boolean                                                      applyTableFilter;

    public static class PluginView<ChildrenType extends AbstractPackageChildrenNode> {
        private ArraySet<ChildrenType> children;
        private PluginForHost          plugin;

        public PluginView(PluginForHost pkg) {
            children = new ArraySet<ChildrenType>();

            this.plugin = pkg;

        }

        public ArraySet<ChildrenType> getChildren() {
            return children;
        }

        public PluginForHost getPlugin() {
            return plugin;
        }

        public void addChildren(List<ChildrenType> children) {
            this.children.addAll(children);
        }

        public void addChild(ChildrenType child) {
            children.add(child);
        }

    };

    public static class PackageView<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> {
        private ArraySet<ChildrenType> children;
        private PackageType            pkg;
        private boolean                packageIncluded;

        public PackageView(PackageType pkg, boolean packageIncluded) {
            children = new ArraySet<ChildrenType>();

            this.pkg = pkg;
            this.packageIncluded = packageIncluded;
        }

        public ArraySet<ChildrenType> getChildren() {
            return children;
        }

        public PackageType getPackage() {
            return pkg;
        }

        public boolean isPackageSelected() {
            return packageIncluded;
        }

        public void addChildren(List<ChildrenType> children) {
            this.children.addAll(children);
        }

        public void addChild(ChildrenType child) {
            children.add(child);
        }

        public boolean isFull() {
            return children.size() == pkg.getChildren().size();
        }
    };

    @SuppressWarnings("unchecked")
    public SelectionInfo(AbstractNode contextObject, List<? extends AbstractNode> selection, boolean applyTableFilter) {
        this.contextObject = contextObject;
        this.applyTableFilter = applyTableFilter;
        if (selection == null || selection.size() == 0) {
            if (contextObject == null) {
                rawSelection = new ArraySet<AbstractNode>();
            } else {
                rawSelection = pack(contextObject);
            }
        } else {
            rawSelection = selection;
        }

        children = new ArraySet<ChildrenType>();
        packageViews = new ArrayList<PackageView<PackageType, ChildrenType>>();

        view = new HashMap<PackageType, PackageView<PackageType, ChildrenType>>();
        pluginView = new HashMap<PluginForHost, SelectionInfo.PluginView<ChildrenType>>();
        pluginViews = new ArrayList<SelectionInfo.PluginView<ChildrenType>>();

        // System.out.println(kEvent);

        PackageControllerTable<PackageType, ChildrenType> table = null;
        if (contextObject != null) {
            if (contextObject instanceof DownloadLink || contextObject instanceof FilePackage) {
                table = (PackageControllerTable<PackageType, ChildrenType>) DownloadsTable.getInstance();
            } else {
                table = (PackageControllerTable<PackageType, ChildrenType>) LinkGrabberTable.getInstance();
            }
        } else if (rawSelection != null && rawSelection.size() > 0) {
            if (rawSelection.get(0) instanceof DownloadLink || rawSelection.get(0) instanceof FilePackage) {
                table = (PackageControllerTable<PackageType, ChildrenType>) DownloadsTable.getInstance();
            } else {
                table = (PackageControllerTable<PackageType, ChildrenType>) LinkGrabberTable.getInstance();
            }
        }
        if (table != null && applyTableFilter) {
            PackageControllerTableModel<PackageType, ChildrenType> tableModel = table.getModel();
            List<PackageControllerTableModelFilter<PackageType, ChildrenType>> lfilters = tableModel.getEnabledTableFilters();
            if (lfilters != null && lfilters.size() > 0) {
                filters = lfilters;
            }

        }
        agregate();

    }

    public boolean contains(AbstractPackageNode<?, ?> child) {

        return view.containsKey(child);
    }

    public boolean contains(AbstractPackageChildrenNode<?> child) {

        return children.contains(child);

    }

    //
    // public SelectionInfo(List<? extends AbstractNode> selection) {
    // this(null, selection, null);
    //
    // }

    private SelectionInfo() {
    }

    @SuppressWarnings("unchecked")
    protected void agregate() {
        raw = new ArraySet<AbstractNode>(rawSelection);
        // use cases:
        // we use this class not only for real selections, but also for faked selections. that means, that the selection of a child without
        // it's package is possible even if the package is collapsed
        // some children of a expanded package with or without the package itself
        // some children of a collapsed package with or without the package itself
        // all children of a expanded package with or without the package itself
        // all children of a collapsed package with or without the package itself
        // no children, but the package colapsed or expanded

        // LinkedHashSet<AbstractNode> notSelectedParents = new LinkedHashSet<AbstractNode>();
        // if we selected a link, and not its parent, this parent will not be agregated. That's why we add them here.
        // for (AbstractNode node : rawSelection) {
        // if (node == null) continue;
        //
        // if (node instanceof AbstractPackageChildrenNode) {
        // // if (!has.contains(((AbstractPackageChildrenNode) node).getParentNode())) {
        // PackageType pkg = (PackageType) ((AbstractPackageChildrenNode) node).getParentNode();
        // if (pkg != null && pkg.isExpanded()) {
        // raw.add(pkg);
        //
        // }
        //
        // // }
        // }
        // }

        for (AbstractNode node : raw) {
            if (node == null) continue;
            if (node instanceof AbstractPackageChildrenNode) {

                PackageType pkg = (PackageType) ((ChildrenType) node).getParentNode();
                PackageView<PackageType, ChildrenType> pv = internalPackageView(pkg);
                pv.addChild((ChildrenType) node);
                addPluginView(node);
                children.add((ChildrenType) node);

            } else {

                // if we selected a package, and ALL it's links, we want all
                // links
                // if we selected a package, and nly afew links, we probably
                // want only these few links.
                // if we selected a package, and it is NOT expanded, we want
                // all
                // links

                PackageView<PackageType, ChildrenType> pv = internalPackageView((PackageType) node);

                if (!((PackageType) node).isExpanded()) {
                    // add allTODO
                    boolean readL = ((PackageType) node).getModifyLock().readLock();
                    try {
                        List<ChildrenType> childs = ((PackageType) node).getChildren();
                        ArraySet<ChildrenType> unFiltered = new ArraySet<ChildrenType>();
                        if (filters == null) {
                            children.addAll(childs);

                            pv.addChildren(childs);
                            for (ChildrenType l : childs) {
                                addPluginView(l);
                            }
                        } else {
                            for (ChildrenType l : childs) {
                                boolean filtered = false;
                                for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : filters) {
                                    if (filter.isFiltered(l)) {
                                        filtered = true;
                                        break;
                                    }
                                }
                                if (!filtered) {
                                    unFiltered.add(l);
                                    pv.addChild(l);
                                    addPluginView(l);
                                }
                            }
                            children.addAll(unFiltered);

                        }
                    } finally {
                        ((PackageType) node).getModifyLock().readUnlock(readL);
                    }

                } else {
                    boolean readL = ((PackageType) node).getModifyLock().readLock();
                    try {
                        List<ChildrenType> childs = ((PackageType) node).getChildren();
                        boolean containsNone = true;
                        boolean containsAll = true;
                        ArraySet<ChildrenType> selected = new ArraySet<ChildrenType>();
                        ArraySet<ChildrenType> unFiltered = new ArraySet<ChildrenType>();
                        for (ChildrenType l : childs) {
                            if (raw.contains(l)) {
                                selected.add(l);
                                containsNone = false;
                            } else {
                                containsAll = false;
                                if (filters != null) {
                                    boolean filtered = false;
                                    for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : filters) {
                                        if (filter.isFiltered(l)) {
                                            filtered = true;
                                            break;
                                        }
                                    }
                                    if (!filtered) unFiltered.add(l);
                                }
                            }

                        }
                        // table.getModel()
                        if (containsNone) {
                            // this is a special case. if the user selected only the package, we cannot simply add all children. We need to
                            // check if he works on a filtered view.
                            if (filters == null) {
                                pv.addChildren(childs);
                                children.addAll(childs);
                                for (ChildrenType l : childs) {
                                    addPluginView(l);
                                }
                            } else {
                                pv.addChildren(unFiltered);
                                children.addAll(unFiltered);
                                for (ChildrenType l : unFiltered) {
                                    addPluginView(l);
                                }

                            }
                        } else if (containsAll) {
                            pv.addChildren(childs);
                            children.addAll(childs);
                            for (ChildrenType l : childs) {
                                addPluginView(l);
                            }

                        } else {
                            pv.addChildren(selected);
                            for (ChildrenType l : selected) {
                                addPluginView(l);
                            }

                        }
                    } finally {
                        ((PackageType) node).getModifyLock().readUnlock(readL);
                    }
                }
            }
        }

    }

    protected void addPluginView(AbstractNode node) {
        PluginForHost plg = node instanceof CrawledLink ? ((CrawledLink) node).gethPlugin() : ((DownloadLink) node).getDefaultPlugin();
        internalPluginView(plg).addChild((ChildrenType) node);
    }

    private PluginView<ChildrenType> internalPluginView(PluginForHost pkg) {
        PluginView<ChildrenType> pv = pluginView.get(pkg);

        if (pv == null) {

            pv = new PluginView<ChildrenType>(pkg);
            pluginViews.add(pv);
            pluginView.put(pkg, pv);
        }
        return pv;
    }

    public List<PluginView<ChildrenType>> getPluginViews() {
        return pluginViews;
    }

    /**
     * @param pkg
     * @return
     */
    private PackageView<PackageType, ChildrenType> internalPackageView(PackageType pkg) {
        PackageView<PackageType, ChildrenType> pv = view.get(pkg);

        if (pv == null) {

            pv = new PackageView<PackageType, ChildrenType>(pkg, raw.contains(pkg));
            packageViews.add(pv);
            view.put(pkg, pv);
        }
        return pv;
    }

    /**
     * 
     * @see #getContextLink()
     * @return
     */
    public ChildrenType getLink() {
        return getContextLink();
    }

    /**
     * if this object is a childcontext, this returns the child, else throws exception
     * 
     * @return
     */
    public ChildrenType getContextLink() {
        if (isLinkContext()) return (ChildrenType) contextObject;

        throw new BadContextException("Not available in Packagecontext");
    }

    /**
     * If there is a context Object, this method returns it. try to muse {@link #getContextLink()} or {@link #getContextPackage()} instead
     * 
     * @return
     */
    public AbstractNode getRawContext() {
        return contextObject;
    }

    /**
     * if we have packagecontext, this returns the package, else the child's PACKAGE
     * 
     * @return
     */
    public PackageType getContextPackage() {
        if (contextObject == null) throw new BadContextException("Context is null");
        if (isPackageContext()) {
            return (PackageType) contextObject;
        } else {
            return ((ChildrenType) contextObject).getParentNode();
        }

    }

    /**
     * Returns either the context pacakge, or the context link's package, or the first links package
     * 
     * @see #getContextPackage()
     * @return
     */
    public PackageType getFirstPackage() {

        if (contextObject == null) {
            if (children.size() == 0) throw new BadContextException("Invalid Context");
            return children.get(0).getParentNode();
        } else {

            return getContextPackage();
        }

    }

    /**
     * @see #getContextPackage()
     * @return
     */
    public PackageType getPackage() {
        return getContextPackage();
    }

    /**
     * Returns a List of the rawselection. Contains packages and links as they were selected in the table. USe {@link #getChildren()}
     * instead
     * 
     * @return
     */
    public List<? extends AbstractNode> getRawSelection() {
        return rawSelection;
    }

    /**
     * A list of all selected children. This list also contains the children of collapsed selected packages
     * 
     * @return
     */
    public ArraySet<ChildrenType> getChildren() {
        return children;
    }

    /**
     * true if the direct context is a link
     * 
     * @return
     */
    public boolean isLinkContext() {
        return contextObject != null && contextObject instanceof AbstractPackageChildrenNode;
    }

    /**
     * false if there are selected links
     * 
     * @return
     */
    public boolean isEmpty() {
        return children == null || children.size() == 0;
    }

    /**
     * true if the direct context is a package
     * 
     * @return
     */
    public boolean isPackageContext() {
        return contextObject != null && contextObject instanceof AbstractPackageNode;
    }

    // public SelectionInfo<PackageType, ChildrenType> derive(AbstractNode contextObject2, MouseEvent event, KeyEvent kEvent, ActionEvent
    // actionEvent, ExtColumn<AbstractNode> column) {
    // SelectionInfo<PackageType, ChildrenType> ret = new SelectionInfo<PackageType, ChildrenType>();
    //
    // ret.allPackages = allPackages;
    // ret.children = children;
    // ret.contextColumn = this.contextColumn;
    // ret.contextObject = this.contextObject;
    // ret.filters = filters;
    // ret.packageViews = packageViews;
    //
    // ret.raw = raw;
    // ret.rawSelection = rawSelection;
    //
    // ret.table = table;
    // ret.view = view;
    //
    // if (contextObject2 != null) {
    //
    // ret.contextObject = contextObject2;
    // }
    //
    // if (column != null) {
    //
    // ret.contextColumn = column;
    // }
    // return ret;
    // }

    public boolean isFullPackageSelection(PackageType pkg) {
        PackageView<PackageType, ChildrenType> ret = view.get(pkg);
        if (ret == null) return false;
        return ret.isFull();
    }

    public List<PackageView<PackageType, ChildrenType>> getPackageViews() {
        return packageViews;
    }

    public PackageView<PackageType, ChildrenType> getPackageView(PackageType entry) {
        return view.get(entry);
    }

}
