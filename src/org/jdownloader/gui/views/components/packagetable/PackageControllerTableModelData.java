package org.jdownloader.gui.views.components.packagetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.jdownloader.gui.views.linkgrabber.quickfilter.FilterTable;

public class PackageControllerTableModelData<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends ArrayList<AbstractNode> {

    private final static AtomicLong                                            VERSION              = new AtomicLong(0);
    private List<PackageControllerTableModelFilter<PackageType, ChildrenType>> packageFilters       = null;
    private List<PackageControllerTableModelFilter<PackageType, ChildrenType>> childrenFilters      = null;
    private List<ChildrenType>                                                 allChildren          = new ArrayList<ChildrenType>();
    private List<PackageControllerTableModelCustomizer>                        tableModelCustomizer = null;
    private final long                                                         version              = VERSION.incrementAndGet();
    private boolean                                                            filtered             = false;

    public long getVersion() {
        return version;
    }

    public List<PackageControllerTableModelCustomizer> getTableModelCustomizer() {
        return tableModelCustomizer;
    }

    public void setTableModelCustomizer(List<PackageControllerTableModelCustomizer> tableModelCustomizer) {
        if (tableModelCustomizer != null && tableModelCustomizer.size() == 0) tableModelCustomizer = null;
        this.tableModelCustomizer = tableModelCustomizer;
    }

    public List<ChildrenType> getAllChildrenNodes() {
        return allChildren;
    }

    protected void setAllChildrenNodes(List<ChildrenType> allChildren) {
        this.allChildren = Collections.unmodifiableList(allChildren);
    }

    public List<PackageControllerTableModelFilter<PackageType, ChildrenType>> getPackageFilters() {
        return packageFilters;
    }

    protected void setPackageFilters(List<PackageControllerTableModelFilter<PackageType, ChildrenType>> packageFilters) {
        if (packageFilters != null && packageFilters.size() == 0) packageFilters = null;
        this.packageFilters = packageFilters;
        updateFilteredState();
    }

    public List<PackageControllerTableModelFilter<PackageType, ChildrenType>> getChildrenFilters() {
        return childrenFilters;
    }

    protected void setChildrenFilters(List<PackageControllerTableModelFilter<PackageType, ChildrenType>> childrenFilters) {
        if (childrenFilters != null && childrenFilters.size() == 0) childrenFilters = null;
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

}
