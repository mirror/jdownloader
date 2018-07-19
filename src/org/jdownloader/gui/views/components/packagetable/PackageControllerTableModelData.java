package org.jdownloader.gui.views.components.packagetable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.ChildrenView;
import jd.controlling.packagecontroller.PackageController;

import org.jdownloader.gui.views.linkgrabber.quickfilter.FilterTable;

public class PackageControllerTableModelData<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends ArrayList<AbstractNode> {
    public static interface PackageControllerTableModelDataPackage {
        public AbstractPackageNode getPackage();

        public boolean isExpanded();

        public List<? extends AbstractNode> getVisibleChildren();

        public List<? extends AbstractNode> getInvisibleChildren();

        public int getPackageIndex();
    }

    private final static AtomicLong                                            VERSION                          = new AtomicLong(-1);
    private List<PackageControllerTableModelFilter<PackageType, ChildrenType>> packageFilters                   = null;
    private List<PackageControllerTableModelFilter<PackageType, ChildrenType>> childrenFilters                  = null;
    private List<PackageControllerTableModelCustomizer>                        tableModelCustomizer             = null;
    private final long                                                         version                          = VERSION.incrementAndGet();
    private boolean                                                            filtered                         = false;
    private final List<PackageControllerTableModelDataPackage>                 modelDataPackages                = new ArrayList<PackageControllerTableModelDataPackage>();
    private final List<AbstractNode>                                           filteredChildren                 = new ArrayList<AbstractNode>();
    private final List<AbstractNode>                                           hiddenChildren                   = new ArrayList<AbstractNode>();
    private final BitSet                                                       hiddenPackagesSingleChildIndices = new BitSet();

    protected List<AbstractNode> getHiddenChildren() {
        return hiddenChildren;
    }

    protected List<AbstractNode> getFilteredChildren() {
        return filteredChildren;
    }

    protected int getRowforObject(final AbstractNode node, final PackageController controller) {
        if (node instanceof AbstractPackageNode) {
            final AbstractPackageNode pkg = (AbstractPackageNode) node;
            if (pkg.getControlledBy() == controller) {
                for (final PackageControllerTableModelDataPackage dataPackage : getModelDataPackages()) {
                    if (dataPackage.getPackage() == pkg) {
                        return dataPackage.getPackageIndex();
                    }
                }
            }
            return -1;
        }
        if (node instanceof AbstractPackageChildrenNode) {
            final Object parent = ((AbstractPackageChildrenNode) node).getParentNode();
            if (parent != null) {
                final AbstractPackageNode pkg = (AbstractPackageNode) parent;
                if (pkg.getControlledBy() == controller) {
                    for (final PackageControllerTableModelDataPackage dataPackage : getModelDataPackages()) {
                        if (dataPackage.getPackage() == pkg) {
                            final int packageIndex = dataPackage.getPackageIndex();
                            final List<? extends AbstractNode> children = dataPackage.getVisibleChildren();
                            final int childIndex = children.indexOf(node);
                            if (childIndex >= 0) {
                                if (isHiddenPackageSingleChildIndex(packageIndex)) {
                                    return packageIndex;
                                } else {
                                    return packageIndex + childIndex + 1;
                                }
                            }
                            return -1;
                        }
                    }
                }
            }
            return -1;
        }
        return -1;
    }

    protected int addHiddenPackageSingleChild(ChildrenType childNode) {
        final int index = this.size();
        hiddenPackagesSingleChildIndices.set(index);
        add(childNode);
        return index;
    }

    protected int addPackageNode(PackageType pkgNode) {
        final int index = this.size();
        super.add(pkgNode);
        return index;
    }

    public boolean isHiddenPackageSingleChildIndex(int index) {
        return hiddenPackagesSingleChildIndices.get(index);
    }

    protected void add(PackageControllerTableModelDataPackage tableModelDataPackage) {
        final ChildrenView<?> view = tableModelDataPackage.getPackage().getView();
        if (view != null) {
            view.setTableModelDataPackage(tableModelDataPackage);
        }
        modelDataPackages.add(tableModelDataPackage);
    }

    public List<PackageControllerTableModelDataPackage> getModelDataPackages() {
        return modelDataPackages;
    }

    public int indexOf(PackageType pkgNode) {
        int index = 0;
        for (PackageControllerTableModelDataPackage modelDataPackage : getModelDataPackages()) {
            if (modelDataPackage.getPackage() == pkgNode) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public int lastIndexOf(PackageType pkgNode) {
        for (int index = getModelDataPackages().size() - 1; index >= 0; index--) {
            final PackageControllerTableModelDataPackage modelDataPackage = getModelDataPackages().get(index);
            if (modelDataPackage.getPackage() == pkgNode) {
                return index;
            }
        }
        return -1;
    }

    public long getVersion() {
        return version;
    }

    public Iterator<ChildrenType> getVisibleChildrenIterator() {
        final Iterator<PackageControllerTableModelDataPackage> it = getModelDataPackages().iterator();
        return new Iterator<ChildrenType>() {
            ChildrenType                     ret = null;
            Iterator<? extends AbstractNode> it2 = null;

            @Override
            public boolean hasNext() {
                if (ret != null) {
                    return true;
                } else {
                    if (it2 != null) {
                        while (it2.hasNext()) {
                            ret = (ChildrenType) it2.next();
                            return true;
                        }
                        it2 = null;
                    }
                    while (it.hasNext()) {
                        final PackageControllerTableModelDataPackage next = it.next();
                        if (next.getVisibleChildren() != null && next.getVisibleChildren().size() > 0) {
                            it2 = next.getVisibleChildren().iterator();
                            ret = (ChildrenType) it2.next();
                            return true;
                        }
                    }
                    return false;
                }
            }

            @Override
            public ChildrenType next() {
                if (hasNext()) {
                    final ChildrenType ret = this.ret;
                    this.ret = null;
                    return ret;
                } else {
                    return null;
                }
            }

            @Override
            public void remove() {
            }
        };
    }

    public List<PackageControllerTableModelCustomizer> getTableModelCustomizer() {
        return tableModelCustomizer;
    }

    public void setTableModelCustomizer(List<PackageControllerTableModelCustomizer> tableModelCustomizer) {
        if (tableModelCustomizer == null || tableModelCustomizer.size() == 0) {
            tableModelCustomizer = null;
        }
        this.tableModelCustomizer = tableModelCustomizer;
    }

    public List<PackageControllerTableModelFilter<PackageType, ChildrenType>> getPackageFilters() {
        return packageFilters;
    }

    protected void setPackageFilters(List<PackageControllerTableModelFilter<PackageType, ChildrenType>> packageFilters) {
        if (packageFilters != null && packageFilters.size() == 0) {
            packageFilters = null;
        }
        this.packageFilters = packageFilters;
        updateFilteredState();
    }

    public List<PackageControllerTableModelFilter<PackageType, ChildrenType>> getChildrenFilters() {
        return childrenFilters;
    }

    protected void setChildrenFilters(List<PackageControllerTableModelFilter<PackageType, ChildrenType>> childrenFilters) {
        if (childrenFilters != null && childrenFilters.size() == 0) {
            childrenFilters = null;
        }
        this.childrenFilters = childrenFilters;
        updateFilteredState();
    }

    /*
     * updates the filtered flag
     *
     * we don't want quickfilters to count as filtered state, users will still be able to move/dragdrop stuff
     */
    private void updateFilteredState() {
        List<PackageControllerTableModelFilter<PackageType, ChildrenType>> lchildrenFilters = childrenFilters;
        if (lchildrenFilters != null) {
            for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : lchildrenFilters) {
                if (!(filter instanceof FilterTable)) {
                    filtered = true;
                    return;
                }
            }
        }
        List<PackageControllerTableModelFilter<PackageType, ChildrenType>> lpackageFilters = packageFilters;
        if (lpackageFilters != null) {
            for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : lpackageFilters) {
                if (!(filter instanceof FilterTable)) {
                    filtered = true;
                    return;
                }
            }
        }
        filtered = false;
    }

    public PackageControllerTableModelData(Collection<? extends AbstractNode> c) {
        super(c);
    }

    public PackageControllerTableModelData() {
        super();
    }

    public PackageControllerTableModelData(int initialCapacity) {
        super(initialCapacity);
    }

    public boolean isFiltered() {
        return filtered;
    }

    public boolean isHideSingleChildPackages() {
        return !hiddenPackagesSingleChildIndices.isEmpty();
    }
}
