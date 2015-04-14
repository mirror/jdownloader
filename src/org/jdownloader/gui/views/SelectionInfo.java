package org.jdownloader.gui.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;

import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelData;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;

public class SelectionInfo<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> {

    public static class PluginView<ChildrenType extends AbstractPackageChildrenNode> extends ArrayList<ChildrenType> {

        private final PluginForHost plugin;

        public PluginView(PluginForHost pkg) {
            this.plugin = pkg;
        }

        public List<ChildrenType> getChildren() {
            return this;
        }

        public PluginForHost getPlugin() {
            return plugin;
        }

    };

    public static class PackageView<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends ArrayList<ChildrenType> {
        private final PackageType pkg;
        private final boolean     packageIncluded;
        private final int         pkgSize;
        private final boolean     isExpanded;

        public PackageView(PackageType pkg, boolean packageIncluded) {
            this.pkg = pkg;
            boolean readL = pkg.getModifyLock().readLock();
            try {
                this.pkgSize = pkg.getChildren().size();
                this.isExpanded = pkg.isExpanded();
            } finally {
                pkg.getModifyLock().readUnlock(readL);
            }
            this.packageIncluded = packageIncluded;
        }

        public List<ChildrenType> getChildren() {
            return this;
        }

        public PackageType getPackage() {
            return pkg;
        }

        public boolean isPackageSelected() {
            return packageIncluded;
        }

        public boolean isExpanded() {
            return isExpanded;
        }

        public int getPackageSize() {
            return pkgSize;
        }

        public boolean isFull() {
            return size() == pkgSize;
        }
    };

    private final List<? extends AbstractNode>                                   rawSelection;

    protected List<PackageControllerTableModelFilter<PackageType, ChildrenType>> childrenFilters = null;

    private final AbstractNode                                                   contextObject;

    @SuppressWarnings("unchecked")
    public SelectionInfo(final AbstractNode contextObject, final List<? extends AbstractNode> selection, final boolean applyTableFilter) {
        this.contextObject = contextObject;
        if (selection == null || selection.size() == 0) {
            if (contextObject == null) {
                rawSelection = new ArrayList<AbstractNode>(0);
            } else {
                final List<AbstractNode> rawSelection = new ArrayList<AbstractNode>(1);
                rawSelection.add(contextObject);
                this.rawSelection = rawSelection;
            }
        } else {
            rawSelection = selection;
        }

        final PackageControllerTable<PackageType, ChildrenType> table;
        if (contextObject != null) {
            if (contextObject instanceof DownloadLink || contextObject instanceof FilePackage) {
                table = (PackageControllerTable<PackageType, ChildrenType>) DownloadsTable.getInstance();
            } else {
                table = (PackageControllerTable<PackageType, ChildrenType>) LinkGrabberTable.getInstance();
            }
        } else if (rawSelection != null && rawSelection.size() > 0 && rawSelection.get(0) != null) {
            if (rawSelection.get(0) instanceof DownloadLink || rawSelection.get(0) instanceof FilePackage) {
                table = (PackageControllerTable<PackageType, ChildrenType>) DownloadsTable.getInstance();
            } else {
                table = (PackageControllerTable<PackageType, ChildrenType>) LinkGrabberTable.getInstance();
            }
        } else {
            table = null;
        }
        if (table != null) {
            if (applyTableFilter) {
                final PackageControllerTableModelData<PackageType, ChildrenType> tableData = table.getModel().getTableData();
                childrenFilters = tableData.getChildrenFilters();
            }
            table.getController().getQueue().addWait(new QueueAction<Void, RuntimeException>(QueuePriority.HIGH) {

                @Override
                protected Void run() throws RuntimeException {
                    aggregate();
                    return null;
                }
            });
        } else {
            aggregate();
        }
    }

    public boolean contains(AbstractPackageNode<?, ?> pkg) {
        return getPackageViewsMap().containsKey(pkg);
    }

    protected final ArrayList<ChildrenType> children = new ArrayList<ChildrenType>();

    public boolean contains(AbstractPackageChildrenNode<?> child) {
        return getChildren().contains(child);
    }

    @SuppressWarnings("unchecked")
    protected void aggregate() {
        // use cases:
        // we use this class not only for real selections, but also for faked selections. that means, that the selection of a child without
        // it's package is possible even if the package is collapsed
        // some children of a expanded package with or without the package itself
        // some children of a collapsed package with or without the package itself
        // all children of a expanded package with or without the package itself
        // all children of a collapsed package with or without the package itself
        // no children, but the package colapsed or expanded

        // if we selected a package, and ALL it's links, we want all
        // links
        // if we selected a package, and nly afew links, we probably
        // want only these few links.
        // if we selected a package, and it is NOT expanded, we want
        // all
        // links
        final LinkedHashSet<ChildrenType> lastPackageChildren = new LinkedHashSet<ChildrenType>();
        PackageView<PackageType, ChildrenType> lastPackageView = null;
        for (AbstractNode node : getRawSelection()) {
            if (node == null) {
                continue;
            } else if (node instanceof AbstractPackageNode) {
                /* rawSelection contains package */
                final PackageType currentPackage = (PackageType) node;
                if (lastPackageView == null || lastPackageView.getPackage() != currentPackage) {
                    aggregatePackagePackageView(lastPackageView, lastPackageChildren);
                    lastPackageChildren.clear();
                    lastPackageView = internalPackageView(currentPackage, true);
                }
            } else if (node instanceof AbstractPackageChildrenNode) {
                /* rawSelection contains child */
                final ChildrenType currentChild = (ChildrenType) node;
                final PackageType currentPackage = currentChild.getParentNode();
                if (lastPackageView == null || lastPackageView.getPackage() != currentPackage) {
                    aggregatePackagePackageView(lastPackageView, lastPackageChildren);
                    lastPackageChildren.clear();
                    lastPackageView = internalPackageView(currentPackage, false);
                }
                lastPackageChildren.add(currentChild);
            }
        }
        aggregatePackagePackageView(lastPackageView, lastPackageChildren);
    }

    private void aggregatePackagePackageView(PackageView<PackageType, ChildrenType> lastPackageView, LinkedHashSet<ChildrenType> lastPackageChildren) {
        if (lastPackageView != null) {
            final PackageType lastPackage = lastPackageView.getPackage();
            PluginView<ChildrenType> lastPluginView = null;
            if (lastPackageView.isPackageSelected() && (lastPackageView.isExpanded == false || lastPackageChildren.size() == 0)) {
                final boolean readL = lastPackage.getModifyLock().readLock();
                try {
                    final List<ChildrenType> packageChildren = lastPackage.getChildren();
                    if (childrenFilters == null || childrenFilters.size() == 0) {
                        children.addAll(packageChildren);
                        lastPackageView.addAll(packageChildren);
                        for (final ChildrenType child : packageChildren) {
                            (lastPluginView = internalPluginView(child, lastPluginView)).add(child);
                        }
                    } else {
                        childrenLoop: for (final ChildrenType child : packageChildren) {
                            for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : childrenFilters) {
                                if (filter.isFiltered(child)) {
                                    continue childrenLoop;
                                }
                            }
                            children.add(child);
                            lastPackageView.add(child);
                            (lastPluginView = internalPluginView(child, lastPluginView)).add(child);
                        }
                    }
                } finally {
                    lastPackage.getModifyLock().readUnlock(readL);
                }
            } else {
                children.addAll(lastPackageChildren);
                lastPackageView.addAll(lastPackageChildren);
                for (final ChildrenType child : lastPackageChildren) {
                    (lastPluginView = internalPluginView(child, lastPluginView)).add(child);
                }
            }
        }
    }

    private final HashMap<PluginForHost, PluginView<ChildrenType>> pluginViews = new HashMap<PluginForHost, SelectionInfo.PluginView<ChildrenType>>();

    protected PluginView<ChildrenType> internalPluginView(ChildrenType node, PluginView<ChildrenType> lastPluginView) {
        final PluginForHost plugin = node instanceof CrawledLink ? ((CrawledLink) node).gethPlugin() : ((DownloadLink) node).getDefaultPlugin();
        if (lastPluginView != null && lastPluginView.getPlugin() == plugin) {
            /* faster than map lookup */
            return lastPluginView;
        }
        PluginView<ChildrenType> pv = pluginViews.get(plugin);
        if (pv == null) {
            pv = new PluginView<ChildrenType>(plugin);
            pluginViews.put(plugin, pv);
        }
        return pv;
    }

    public Collection<PluginView<ChildrenType>> getPluginViews() {
        return pluginViews.values();
    }

    private final List<PackageView<PackageType, ChildrenType>>                 packageViewList = new ArrayList<PackageView<PackageType, ChildrenType>>();
    private final HashMap<PackageType, PackageView<PackageType, ChildrenType>> packageViews    = new HashMap<PackageType, PackageView<PackageType, ChildrenType>>();

    /**
     * @param pkg
     * @return
     */
    protected PackageView<PackageType, ChildrenType> internalPackageView(PackageType pkg, boolean packageIncluded) {
        PackageView<PackageType, ChildrenType> pv = getPackageView(pkg);
        if (pv == null) {
            pv = new PackageView<PackageType, ChildrenType>(pkg, packageIncluded);
            getPackageViews().add(pv);
            getPackageViewsMap().put(pkg, pv);
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
        if (isLinkContext()) {
            return (ChildrenType) getRawContext();
        }
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
        final AbstractNode context = getRawContext();
        if (context == null) {
            throw new BadContextException("Context is null");
        }
        if (isPackageContext()) {
            return (PackageType) context;
        } else {
            return ((ChildrenType) context).getParentNode();
        }
    }

    /**
     * Returns either the context pacakge, or the context link's package, or the first links package
     *
     * @see #getContextPackage()
     * @return
     */
    public PackageType getFirstPackage() {
        final AbstractNode context = getRawContext();
        if (context == null) {
            final List<ChildrenType> lchildren = getChildren();
            if (lchildren.size() == 0) {
                throw new BadContextException("Invalid Context");
            }
            return lchildren.get(0).getParentNode();
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
    public List<ChildrenType> getChildren() {
        return children;
    }

    /**
     * true if the direct context is a link
     *
     * @return
     */
    public boolean isLinkContext() {
        final AbstractNode context = getRawContext();
        return context != null && context instanceof AbstractPackageChildrenNode;
    }

    /**
     * false if there are selected links
     *
     * @return
     */
    public boolean isEmpty() {
        final List<ChildrenType> lchildren = getChildren();
        return lchildren.size() == 0;
    }

    /**
     * true if the direct context is a package
     *
     * @return
     */
    public boolean isPackageContext() {
        final AbstractNode context = getRawContext();
        return context != null && context instanceof AbstractPackageNode;
    }

    public boolean isFullPackageSelection(PackageType pkg) {
        final PackageView<PackageType, ChildrenType> ret = getPackageView(pkg);
        if (ret == null) {
            return false;
        }
        return ret.isFull();
    }

    public List<PackageView<PackageType, ChildrenType>> getPackageViews() {
        return packageViewList;
    }

    protected Map<PackageType, PackageView<PackageType, ChildrenType>> getPackageViewsMap() {
        return packageViews;
    }

    public PackageView<PackageType, ChildrenType> getPackageView(PackageType entry) {
        return getPackageViewsMap().get(entry);
    }

}
