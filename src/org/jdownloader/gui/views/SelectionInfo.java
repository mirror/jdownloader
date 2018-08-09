package org.jdownloader.gui.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.UniqueAlltimeID;

public class SelectionInfo<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> {
    public static class PluginView<ChildrenType> extends ArrayList<ChildrenType> {
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

    public static interface PackageView<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> {
        public List<ChildrenType> getChildren();

        public PackageType getPackage();

        public boolean isPackageSelected();

        public List<ChildrenType> getSelectedChildren();

        public boolean isExpanded();
    };

    protected final List<AbstractNode>                           rawSelection;
    private final AbstractNode                                   contextObject;
    protected final PackageController<PackageType, ChildrenType> controller;

    public PackageController<PackageType, ChildrenType> getController() {
        return controller;
    }

    public SelectionInfo(final AbstractNode contextObject) {
        this(contextObject, new ArrayList<AbstractNode>(0));
    }

    protected SelectionInfo(PackageController<PackageType, ChildrenType> controller) {
        this.contextObject = null;
        this.rawSelection = new ArrayList<AbstractNode>();
        this.controller = controller;
    }

    @SuppressWarnings("unchecked")
    public SelectionInfo(final AbstractNode contextObject, final List<? extends AbstractNode> selection) {
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
            rawSelection = (List<AbstractNode>) selection;
        }
        final PackageController<?, ?> controller;
        if (contextObject != null) {
            if (contextObject instanceof DownloadLink || contextObject instanceof FilePackage) {
                controller = DownloadController.getInstance();
            } else {
                controller = LinkCollector.getInstance();
            }
        } else if (rawSelection != null && rawSelection.size() > 0 && rawSelection.get(0) != null) {
            if (rawSelection.get(0) instanceof DownloadLink || rawSelection.get(0) instanceof FilePackage) {
                controller = DownloadController.getInstance();
            } else {
                controller = LinkCollector.getInstance();
            }
        } else {
            controller = null;
        }
        if (controller != null) {
            aggregate(controller.getQueue());
        } else {
            aggregate(null);
        }
        this.controller = (PackageController<PackageType, ChildrenType>) controller;
    }

    protected void aggregate(Queue queue) {
        if (queue != null) {
            queue.addWait(new QueueAction<Void, RuntimeException>(QueuePriority.HIGH) {
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

    protected final ArrayList<ChildrenType> children = new ArrayList<ChildrenType>();

    public List<ChildrenType> getUnselectedChildren() {
        return null;
    }

    public boolean contains(final AbstractPackageChildrenNode<?> child) {
        final Object parentNode = child.getParentNode();
        final PackageView<PackageType, ChildrenType> packageView = getPackageViewsMap().get(parentNode);
        return (packageView != null && packageView.getChildren().contains(child)) || getChildren().contains(child);
    }

    private static class IndexedContainer<E> {
        private final E object;

        public E getObject() {
            return object;
        }

        public int getIndex() {
            return index;
        }

        private final int index;

        private IndexedContainer(E object, int index) {
            this.object = object;
            this.index = index;
        }
    }

    @SuppressWarnings("unchecked")
    protected void aggregate() {
        final ArrayList<IndexedContainer<PackageType>> packages = new ArrayList<IndexedContainer<PackageType>>();
        ArrayList<IndexedContainer<ChildrenType>> children = new ArrayList<IndexedContainer<ChildrenType>>();
        /**
         * sort nodes into packages/children
         */
        final int rawSize = getRawSelection().size();
        for (int rawIndex = 0; rawIndex < rawSize; rawIndex++) {
            final AbstractNode node = getRawSelection().get(rawIndex);
            if (node instanceof AbstractPackageNode) {
                packages.add(new IndexedContainer<PackageType>((PackageType) node, rawIndex));
            } else if (node instanceof AbstractPackageChildrenNode) {
                final ChildrenType child = (ChildrenType) node;
                if (child.getParentNode() != null) {
                    children.add(new IndexedContainer<ChildrenType>((ChildrenType) node, rawIndex));
                }
            }
        }
        final Comparator<IndexedContainer<? extends AbstractNode>> uniqueCmp = new Comparator<IndexedContainer<? extends AbstractNode>>() {
            public int compare(long x, long y) {
                return (x < y) ? -1 : ((x == y) ? 0 : 1);
            }

            @Override
            public int compare(IndexedContainer<? extends AbstractNode> o1, IndexedContainer<? extends AbstractNode> o2) {
                return compare(o1.getObject().getUniqueID().getID(), o2.getObject().getUniqueID().getID());
            }
        };
        final Comparator<IndexedContainer<?>> indexCmp = new Comparator<IndexedContainer<?>>() {
            public int compare(long x, long y) {
                return (x < y) ? -1 : ((x == y) ? 0 : 1);
            }

            @Override
            public int compare(IndexedContainer<?> o1, IndexedContainer<?> o2) {
                return compare(o1.getIndex(), o2.getIndex());
            }
        };
        /**
         * sort packages by uuid
         */
        Collections.sort(packages, uniqueCmp);
        PackageType lastPackage = null;
        final HashSet<UniqueAlltimeID> selectedPackages = new HashSet<UniqueAlltimeID>();
        /**
         * filter duplicated packages
         */
        for (final IndexedContainer<PackageType> pkg : packages) {
            if (lastPackage == null || lastPackage != pkg.getObject()) {
                lastPackage = pkg.getObject();
                selectedPackages.add(lastPackage.getUniqueID());
            }
        }
        /**
         * sort children by uuid
         */
        Collections.sort(children, uniqueCmp);
        ChildrenType lastChild = null;
        final ArrayList<IndexedContainer<ChildrenType>> selectedChildren = new ArrayList<IndexedContainer<ChildrenType>>();
        /**
         * filter duplicated children
         */
        for (final IndexedContainer<ChildrenType> child : children) {
            if (lastChild == null || lastChild != child.getObject()) {
                lastChild = child.getObject();
                selectedChildren.add(child);
            }
        }
        children.clear();
        children = null;
        /**
         * sort selectedChildren into correct order
         */
        Collections.sort(selectedChildren, indexCmp);
        /**
         * sort children by package uuid
         */
        Collections.sort(selectedChildren, new Comparator<IndexedContainer<ChildrenType>>() {
            public int compare(long x, long y) {
                return (x < y) ? -1 : ((x == y) ? 0 : 1);
            }

            @Override
            public int compare(IndexedContainer<ChildrenType> o1, IndexedContainer<ChildrenType> o2) {
                return compare(o1.getObject().getParentNode().getUniqueID().getID(), o2.getObject().getParentNode().getUniqueID().getID());
            }
        });
        lastPackage = null;
        int index = 0;
        final ArrayList<IndexedContainer<PackageView<PackageType, ChildrenType>>> fillPackages = new ArrayList<IndexedContainer<PackageView<PackageType, ChildrenType>>>();
        final ArrayList<ChildrenType> fillChildren = new ArrayList<ChildrenType>();
        int packageIndex = 0;
        for (final IndexedContainer<ChildrenType> child : selectedChildren) {
            if (lastPackage == null) {
                lastPackage = child.getObject().getParentNode();
                index = fillChildren.size();
                packageIndex = child.getIndex();
                fillChildren.add(child.getObject());
            } else if (lastPackage != child.getObject().getParentNode()) {
                final int finalIndex = index;
                final int finalSize = fillChildren.size() - finalIndex;
                final boolean isPackageSelected = selectedPackages.remove(lastPackage.getUniqueID());
                final boolean isExpanded = lastPackage.isExpanded();
                final PackageType pkg = lastPackage;
                PackageView<PackageType, ChildrenType> packageView = new PackageView<PackageType, ChildrenType>() {
                    @Override
                    public boolean isPackageSelected() {
                        return isPackageSelected;
                    }

                    @Override
                    public boolean isExpanded() {
                        return isExpanded;
                    }

                    @Override
                    public List<ChildrenType> getSelectedChildren() {
                        return fillChildren.subList(finalIndex, finalIndex + finalSize);
                    }

                    @Override
                    public PackageType getPackage() {
                        return pkg;
                    }

                    @Override
                    public List<ChildrenType> getChildren() {
                        return fillChildren.subList(finalIndex, finalIndex + finalSize);
                    }
                };
                fillPackages.add(new IndexedContainer<PackageView<PackageType, ChildrenType>>(packageView, packageIndex));
                index = fillChildren.size();
                fillChildren.add(child.getObject());
                lastPackage = child.getObject().getParentNode();
                packageIndex = child.getIndex();
            } else {
                fillChildren.add(child.getObject());
            }
        }
        if (lastPackage != null) {
            final int finalIndex = index;
            final int finalSize = fillChildren.size() - finalIndex;
            final boolean isPackageSelected = selectedPackages.remove(lastPackage.getUniqueID());
            final boolean isExpanded = lastPackage.isExpanded();
            final PackageType pkg = lastPackage;
            PackageView<PackageType, ChildrenType> packageView = new PackageView<PackageType, ChildrenType>() {
                @Override
                public boolean isPackageSelected() {
                    return isPackageSelected;
                }

                @Override
                public boolean isExpanded() {
                    return isExpanded;
                }

                @Override
                public List<ChildrenType> getSelectedChildren() {
                    return fillChildren.subList(finalIndex, finalIndex + finalSize);
                }

                @Override
                public PackageType getPackage() {
                    return pkg;
                }

                @Override
                public List<ChildrenType> getChildren() {
                    return fillChildren.subList(finalIndex, finalIndex + finalSize);
                }
            };
            fillPackages.add(new IndexedContainer<PackageView<PackageType, ChildrenType>>(packageView, packageIndex));
        }
        for (final IndexedContainer<PackageType> pkg : packages) {
            if (selectedPackages.remove(pkg.getObject().getUniqueID())) {
                final PackageType pkgO = pkg.getObject();
                final boolean isExpanded = pkgO.isExpanded();
                final int finalIndex = fillChildren.size();
                final int finalSize;
                final boolean readL = pkgO.getModifyLock().readLock();
                try {
                    finalSize = pkgO.getChildren().size();
                    fillChildren.addAll(pkgO.getChildren());
                } finally {
                    pkgO.getModifyLock().readUnlock(readL);
                }
                PackageView<PackageType, ChildrenType> packageView = new PackageView<PackageType, ChildrenType>() {
                    @Override
                    public boolean isPackageSelected() {
                        return true;
                    }

                    @Override
                    public boolean isExpanded() {
                        return isExpanded;
                    }

                    @Override
                    public List<ChildrenType> getSelectedChildren() {
                        return new ArrayList<ChildrenType>(0);
                    }

                    @Override
                    public PackageType getPackage() {
                        return pkgO;
                    }

                    @Override
                    public List<ChildrenType> getChildren() {
                        return fillChildren.subList(finalIndex, finalIndex + finalSize);
                    }
                };
                fillPackages.add(new IndexedContainer<PackageView<PackageType, ChildrenType>>(packageView, pkg.getIndex()));
            }
        }
        /**
         * sort PackageViews into correct order
         */
        Collections.sort(fillPackages, indexCmp);
        /**
         * fill PackageViews in correct order into this SelectionInfo
         */
        for (IndexedContainer<PackageView<PackageType, ChildrenType>> fillPackage : fillPackages) {
            final PackageView<PackageType, ChildrenType> fillPackageView = fillPackage.getObject();
            final boolean selected = fillPackageView.isPackageSelected();
            final boolean expanded = fillPackageView.isExpanded();
            final PackageType pkg = fillPackageView.getPackage();
            final int finalIndex = this.children.size();
            final int finalSize;
            final boolean readL = pkg.getModifyLock().readLock();
            try {
                final List<ChildrenType> viewChildren = fillPackageView.getChildren();
                finalSize = viewChildren.size();
                this.children.addAll(viewChildren);
            } finally {
                pkg.getModifyLock().readUnlock(readL);
            }
            PackageView<PackageType, ChildrenType> packageView;
            if (fillPackageView.getSelectedChildren().size() == 0) {
                packageView = new PackageView<PackageType, ChildrenType>() {
                    @Override
                    public boolean isPackageSelected() {
                        return selected;
                    }

                    @Override
                    public boolean isExpanded() {
                        return expanded;
                    }

                    @Override
                    public List<ChildrenType> getSelectedChildren() {
                        return new ArrayList<ChildrenType>(0);
                    }

                    @Override
                    public PackageType getPackage() {
                        return pkg;
                    }

                    @Override
                    public List<ChildrenType> getChildren() {
                        return SelectionInfo.this.children.subList(finalIndex, finalIndex + finalSize);
                    }
                };
            } else {
                packageView = new PackageView<PackageType, ChildrenType>() {
                    @Override
                    public boolean isPackageSelected() {
                        return selected;
                    }

                    @Override
                    public boolean isExpanded() {
                        return expanded;
                    }

                    @Override
                    public List<ChildrenType> getSelectedChildren() {
                        return SelectionInfo.this.children.subList(finalIndex, finalIndex + finalSize);
                    }

                    @Override
                    public PackageType getPackage() {
                        return pkg;
                    }

                    @Override
                    public List<ChildrenType> getChildren() {
                        return SelectionInfo.this.children.subList(finalIndex, finalIndex + finalSize);
                    }
                };
            }
            addPackageView(packageView, pkg);
        }
    }

    protected final HashMap<PluginForHost, PluginView<ChildrenType>> pluginViews          = new HashMap<PluginForHost, SelectionInfo.PluginView<ChildrenType>>();
    protected final AtomicBoolean                                    pluginViewsInitiated = new AtomicBoolean(false);

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

    public synchronized Collection<PluginView<ChildrenType>> getPluginViews() {
        if (pluginViewsInitiated.get() == false) {
            PluginView<ChildrenType> lastPluginView = null;
            for (final ChildrenType child : getChildren()) {
                (lastPluginView = internalPluginView(child, lastPluginView)).add(child);
            }
            pluginViewsInitiated.set(true);
        }
        return pluginViews.values();
    }

    protected final List<PackageView<PackageType, ChildrenType>>                 packageViewList = new ArrayList<PackageView<PackageType, ChildrenType>>();
    protected final HashMap<PackageType, PackageView<PackageType, ChildrenType>> packageViews    = new HashMap<PackageType, PackageView<PackageType, ChildrenType>>();

    /**
     * @param pkg
     * @return
     */
    protected void addPackageView(PackageView<PackageType, ChildrenType> packageView, PackageType pkg) {
        getPackageViews().add(packageView);
        getPackageViewsMap().put(pkg, packageView);
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
            final List<PackageView<PackageType, ChildrenType>> packageViews = getPackageViews();
            if (packageViews.size() == 0) {
                throw new BadContextException("Invalid Context");
            }
            return packageViews.get(0).getPackage();
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
    public List<AbstractNode> getRawSelection() {
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
     * optimized version of containsAll
     *
     * 1.) compare list size
     *
     * 2.) sum up uuid(checkSumPackage) and fill them into an array(pkgUUIDs)
     *
     * 3.) sum up uuid(checkSumView)
     *
     * 4.) compare both sums
     *
     * 5.) fill uuid into an array(viewUUIDS)
     *
     * 6.) sort pkgUUIDs and viewUUIDS
     *
     * 7.) compare pkgUUIDs and viewUUIDS
     *
     * @param pkg
     * @return
     */
    public boolean isPackageSelectionComplete(PackageType pkg) {
        final PackageView<PackageType, ChildrenType> packageView = getPackageViewsMap().get(pkg);
        if (packageView != null) {
            long checkSumPackage = 0;
            final long[] pkgUUIDs;
            final boolean readL = pkg.getModifyLock().readLock();
            final int size = packageView.getChildren().size();
            int index = 0;
            try {
                if (pkg.getChildren().size() == size) {
                    if (size == 0) {
                        return true;
                    }
                    pkgUUIDs = new long[size];
                    for (ChildrenType child : pkg.getChildren()) {
                        final long childUUID = child.getUniqueID().getID();
                        pkgUUIDs[index++] = childUUID;
                        checkSumPackage += childUUID;
                    }
                } else {
                    return false;
                }
            } finally {
                pkg.getModifyLock().readUnlock(readL);
            }
            long checkSumView = 0;
            for (ChildrenType child : packageView.getChildren()) {
                checkSumView += child.getUniqueID().getID();
            }
            if (checkSumPackage == checkSumView) {
                final long[] viewUUIDS = new long[size];
                index = 0;
                for (ChildrenType child : packageView.getChildren()) {
                    viewUUIDS[index++] = child.getUniqueID().getID();
                }
                Arrays.sort(viewUUIDS);
                Arrays.sort(pkgUUIDs);
                if (viewUUIDS[0] == pkgUUIDs[0] && viewUUIDS[size - 1] == pkgUUIDs[size - 1]) {
                    for (index = 0; index < size; index++) {
                        if (viewUUIDS[index] != pkgUUIDs[index]) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * false if there are selected links
     *
     * @return
     */
    public boolean isEmpty() {
        return getPackageViews().size() == 0 || getChildren().size() == 0;
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
