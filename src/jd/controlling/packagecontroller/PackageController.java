package jd.controlling.packagecontroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.appwork.utils.ModifyLock;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerSelectionInfo;
import org.jdownloader.gui.views.components.packagetable.dragdrop.MergePosition;
import org.jdownloader.logging.LogController;

public abstract class PackageController<PackageType extends AbstractPackageNode<ChildType, PackageType>, ChildType extends AbstractPackageChildrenNode<PackageType>> implements AbstractNodeNotifier {
    protected final AtomicLong structureChanged = new AtomicLong(System.currentTimeMillis());
    protected final AtomicLong childrenChanged  = new AtomicLong(System.currentTimeMillis());
    protected final AtomicLong backendChanged   = new AtomicLong(System.currentTimeMillis());

    public long getBackendChanged() {
        return backendChanged.get();
    }

    protected final AtomicLong                                contentChanged             = new AtomicLong(System.currentTimeMillis());

    protected final LogSource                                 logger                     = LogController.CL();

    protected final WeakHashMap<UniqueAlltimeID, PackageType> uniqueAlltimeIDPackageMap  = new WeakHashMap<UniqueAlltimeID, PackageType>();
    protected final WeakHashMap<UniqueAlltimeID, ChildType>   uniqueAlltimeIDChildrenMap = new WeakHashMap<UniqueAlltimeID, ChildType>();

    public long getPackageControllerChanges() {
        return structureChanged.get();
    }

    public List<ChildType> getAllChildren() {
        final List<ChildType> ret = new ArrayList<ChildType>();
        final boolean readL = readLock();
        try {
            for (final PackageType fp : getPackages()) {
                final boolean readL2 = fp.getModifyLock().readLock();
                try {
                    ret.addAll(fp.getChildren());
                } finally {
                    fp.getModifyLock().readUnlock(readL2);
                }
            }
        } finally {
            readUnlock(readL);
        }
        return ret;
    }

    public List<AbstractNode> getCopy() {
        final List<AbstractNode> ret = new ArrayList<AbstractNode>();
        final boolean readL = readLock();
        try {
            for (final PackageType fp : getPackages()) {
                ret.add(fp);
                final boolean readL2 = fp.getModifyLock().readLock();
                try {
                    ret.addAll(fp.getChildren());
                } finally {
                    fp.getModifyLock().readUnlock(readL2);
                }
            }
        } finally {
            readUnlock(readL);
        }
        return ret;
    }

    public long getChildrenChanges() {
        return childrenChanged.get();
    }

    public long getContentChanges() {
        return contentChanged.get();
    }

    protected final ArrayList<PackageType> packages = new ArrayList<PackageType>();
    protected final ModifyLock             lock     = new ModifyLock();
    protected final ModifyLock             mapLock  = new ModifyLock();

    protected ModifyLock getMapLock() {
        return mapLock;
    }

    protected final Queue QUEUE = new Queue(getClass().getName()) {

                                    @Override
                                    public void killQueue() {
                                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(new Throwable("YOU CANNOT KILL ME!"));
                                        /*
                                         * this queue can't be killed
                                         */
                                    }

                                };

    /**
     * add a Package at given position position in this PackageController. in case the Package is already controlled by this
     * PackageController this function does move it to the given position
     *
     * @param pkg
     * @param index
     */
    protected void addmovePackageAt(final PackageType pkg, final int index) {
        addmovePackageAt(pkg, index, false);
    }

    public Queue getQueue() {
        return QUEUE;
    }

    protected void updateUniqueAlltimeIDMaps(final List<PackageType> packages) {
        if (packages != null && packages.size() > 0) {
            getMapLock().writeLock();
            try {
                for (final PackageType pkg : packages) {
                    uniqueAlltimeIDPackageMap.put(pkg.getUniqueID(), pkg);
                    final boolean readL = pkg.getModifyLock().readLock();
                    try {
                        for (ChildType child : pkg.getChildren()) {
                            uniqueAlltimeIDChildrenMap.put(child.getUniqueID(), child);
                        }
                    } finally {
                        pkg.getModifyLock().readUnlock(readL);
                    }
                }
            } finally {
                getMapLock().writeUnlock();
            }
        }
    }

    /**
     * Returns how many children the controller holds.
     *
     * @return
     * @TODO: Needs optimization!
     */
    public int getChildrenCount() {
        int ret = 0;
        final boolean readL = readLock();
        try {
            for (final PackageType fp : getPackages()) {
                final boolean readL2 = fp.getModifyLock().readLock();
                try {
                    ret = ret + fp.getChildren().size();
                } finally {
                    fp.getModifyLock().readUnlock(readL2);
                }
            }
        } finally {
            readUnlock(readL);
        }
        return ret;
    }

    public void sortPackageChildren(final PackageType pkg, final PackageControllerComparator<ChildType> comparator) {
        if (pkg != null && comparator != null) {
            QUEUE.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    final ArrayList<ChildType> children = getChildrenCopy(pkg);
                    try {
                        try {
                            Collections.sort(children, comparator);
                        } catch (final Throwable e) {
                            LogController.CL(true).log(e);
                        }
                        try {
                            pkg.getModifyLock().writeLock();
                            pkg.setCurrentSorter(comparator);
                            for (ChildType child : children) {
                                /* this resets getPreviousParentNodeID */
                                child.setParentNode(pkg);
                            }
                            pkg.getChildren().clear();
                            pkg.getChildren().addAll(children);
                        } finally {
                            pkg.getModifyLock().writeUnlock();
                            pkg.nodeUpdated(pkg, NOTIFY.STRUCTURE_CHANCE, null);
                        }
                    } catch (final Throwable e) {
                        LogController.CL(true).log(e);
                    }
                    structureChanged.set(backendChanged.incrementAndGet());
                    _controllerPackageNodeStructureChanged(pkg, this.getQueuePrio());
                    return null;
                }

            });
        }
    }

    protected void addmovePackageAt(final PackageType pkg, final int index, final boolean allowEmpty) {
        if (pkg != null) {
            QUEUE.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    if (allowEmpty == false && pkg.getChildren().size() == 0) {
                        /* we dont want empty packages here */
                        return null;
                    }
                    boolean isNew = true;

                    /**
                     * iterate through all packages, remove the existing one and add at given position
                     */
                    final PackageController<PackageType, ChildType> controller = pkg.getControlledBy();
                    boolean need2Remove = false;
                    if (PackageController.this == controller) {
                        /* we have to reposition the package */
                        need2Remove = true;
                    } else if (controller != null) {
                        /* first remove package from another controller */
                        controller.removePackage(pkg);
                    }
                    writeLock();
                    try {
                        final ListIterator<PackageType> li = getPackages().listIterator();
                        int currentIndex = 0;
                        boolean done = false;
                        boolean addLast = false;
                        if (index < 0 || index > getPackages().size() - 1) {
                            /* lets add pkg at end of list */
                            done = true;
                            addLast = true;
                        }
                        while (li.hasNext()) {
                            if (done && need2Remove == false) {
                                /* we no longer need to iterate through list */
                                break;
                            }
                            PackageType c = li.next();
                            if (c == pkg && currentIndex == index) {
                                /*
                                 * current element is pkg and index is correct, nothing to do
                                 */

                                return null;
                            } else if (currentIndex == index) {
                                /*
                                 * current position is wished index, lets add pkg here
                                 */
                                PackageType replaced = c;
                                li.set(pkg);
                                li.add(replaced);
                                done = true;
                            } else if (c == pkg) {
                                /* current element is pkg, lets remove it */
                                li.remove();
                                isNew = false;
                                need2Remove = false;
                            }
                            currentIndex++;
                        }
                        if (!done || addLast) {
                            getPackages().add(pkg);
                        }
                        pkg.setControlledBy(PackageController.this);
                    } finally {
                        writeUnlock();
                    }
                    getMapLock().writeLock();
                    try {
                        /*
                         * update uniqueAlltimeIDmaps
                         */
                        uniqueAlltimeIDPackageMap.put(pkg.getUniqueID(), pkg);
                        final boolean readL = pkg.getModifyLock().readLock();
                        try {
                            for (ChildType child : pkg.getChildren()) {
                                uniqueAlltimeIDChildrenMap.put(child.getUniqueID(), child);
                            }
                        } finally {
                            pkg.getModifyLock().readUnlock(readL);
                        }
                    } finally {
                        getMapLock().writeUnlock();
                    }
                    final long version = backendChanged.incrementAndGet();
                    structureChanged.set(version);
                    if (isNew) {
                        if (pkg.getChildren().size() > 0) {
                            childrenChanged.set(version);
                        }
                        _controllerPackageNodeAdded(pkg, this.getQueuePrio());
                    } else {
                        _controllerPackageNodeStructureChanged(pkg, this.getQueuePrio());
                    }
                    _controllerStructureChanged(this.getQueuePrio());
                    return null;
                }
            });
        }
    }

    public int size() {
        boolean readL = readLock();
        try {
            return packages.size();
        } finally {
            readUnlock(readL);
        }
    }

    private List<PackageControllerModifyVetoListener<PackageType, ChildType>> vetoListener = new CopyOnWriteArrayList<PackageControllerModifyVetoListener<PackageType, ChildType>>();
    private volatile PackageControllerComparator<AbstractNode>                sorter;

    public void removeVetoListener(PackageControllerModifyVetoListener<PackageType, ChildType> list) {
        vetoListener.remove(list);
    }

    public void addVetoListener(PackageControllerModifyVetoListener<PackageType, ChildType> list) {
        vetoListener.add(list);
    }

    public List<ChildType> askForRemoveVetos(Object asker, PackageType pkg) {
        final ArrayList<ChildType> noVetos = getChildrenCopy(pkg);
        for (PackageControllerModifyVetoListener<PackageType, ChildType> listener : vetoListener) {
            final List<ChildType> response = listener.onAskToRemovePackage(asker, pkg, noVetos);
            if (response != null) {
                noVetos.retainAll(response);
            } else if (response == null) {
                noVetos.clear();
            }
            if (noVetos.size() == 0) {
                return noVetos;
            }
        }
        return noVetos;
    }

    public List<ChildType> askForRemoveVetos(Object asker, List<ChildType> children) {
        final ArrayList<ChildType> noVetos = new ArrayList<ChildType>(children);
        for (PackageControllerModifyVetoListener<PackageType, ChildType> listener : vetoListener) {
            final List<ChildType> response = listener.onAskToRemoveChildren(asker, noVetos);
            if (response != null) {
                noVetos.retainAll(response);
            } else if (response == null) {
                noVetos.clear();
            }
            if (noVetos.size() == 0) {
                return noVetos;
            }
        }
        return noVetos;
    }

    /* remove the Package from this PackageController */
    public void removePackage(final PackageType pkg) {

        if (pkg != null) {
            QUEUE.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    boolean removed = false;
                    final PackageController<PackageType, ChildType> controller = pkg.getControlledBy();
                    if (controller == null) {
                        logger.log(new Throwable("NO CONTROLLER!!!"));
                        return null;
                    }
                    if (pkg.getControlledBy() != null) {
                        if (PackageController.this != pkg.getControlledBy()) {
                            /* this should never happen */
                            logger.log(new Throwable("removing a package which is not controlled by this controller?!?!?"));
                        }
                        pkg.setControlledBy(null);
                        writeLock();
                        try {
                            removed = controller.getPackages().remove(pkg);
                        } finally {
                            writeUnlock();
                        }
                    }
                    if (removed) {
                        final List<ChildType> remove = getChildrenCopy(pkg);
                        getMapLock().writeLock();
                        try {
                            uniqueAlltimeIDPackageMap.remove(pkg.getUniqueID());
                            for (final ChildType child : remove) {
                                uniqueAlltimeIDChildrenMap.remove(child.getUniqueID());
                            }
                        } finally {
                            getMapLock().writeUnlock();
                        }
                        final long version = backendChanged.incrementAndGet();
                        if (remove.size() > 0) {
                            childrenChanged.set(version);
                            controller._controllerParentlessLinks(remove, this.getQueuePrio());
                        }
                        controller.structureChanged.set(version);
                        controller._controllerPackageNodeRemoved(pkg, this.getQueuePrio());
                        _controllerStructureChanged(this.getQueuePrio());
                    }
                    return null;
                }
            });
        }
    }

    public void removeChildren(final List<ChildType> removechildren) {

        if (removechildren != null && removechildren.size() > 0) {
            QUEUE.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    internalRemoveChildren(removechildren);
                    _controllerStructureChanged(this.getQueuePrio());

                    return null;
                }
            });
        }
    }

    protected void internalRemoveChildren(List<ChildType> removechildren) {
        final List<ChildType> children = new ArrayList<ChildType>(removechildren);
        /* build map for removal of children links */
        boolean childrenRemoved = false;
        final HashMap<PackageType, List<ChildType>> removeaddMap = new HashMap<PackageType, List<ChildType>>();
        for (final ChildType child : children) {
            final PackageType parent = child.getParentNode();
            if (parent == null) {
                continue;
            }
            List<ChildType> pmap = removeaddMap.get(parent);
            if (pmap == null) {
                childrenRemoved = true;
                pmap = new ArrayList<ChildType>();
                removeaddMap.put(parent, pmap);
            }
            pmap.add(child);
        }
        final Set<Entry<PackageType, List<ChildType>>> eset = removeaddMap.entrySet();
        final Iterator<Entry<PackageType, List<ChildType>>> it = eset.iterator();
        while (it.hasNext()) {
            /* remove children from other packages */
            final Entry<PackageType, List<ChildType>> next = it.next();
            final PackageType cpkg = next.getKey();
            final PackageController<PackageType, ChildType> controller = cpkg.getControlledBy();
            if (controller == null) {
                logger.log(new Throwable("NO CONTROLLER!!!"));
            } else {
                controller.removeChildren(cpkg, next.getValue(), true);
            }
        }
        final long version = backendChanged.incrementAndGet();
        structureChanged.set(version);
        if (childrenRemoved) {
            childrenChanged.set(version);
        }

    }

    /**
     * Returns all children. if filter!=null not all children are returned, but only the accepted ones
     *
     * @param filter
     * @return
     */
    public List<ChildType> getChildrenByFilter(AbstractPackageChildrenNodeFilter<ChildType> filter) {
        final List<ChildType> ret = new ArrayList<ChildType>();
        for (PackageType pkg : getPackagesCopy()) {
            for (ChildType child : getChildrenCopy(pkg)) {
                if (filter != null && filter.returnMaxResults() > 0 && ret.size() == filter.returnMaxResults()) {
                    /* max results found, lets return */
                    return ret;
                }
                if (filter == null || filter.acceptNode(child)) {
                    ret.add(child);
                }
            }
        }
        return ret;
    }

    public void visitNodes(final AbstractNodeVisitor<ChildType, PackageType> visitor, final boolean liveWalk) {
        if (visitor != null) {
            if (liveWalk) {
                final boolean pkgLock = readLock();
                try {
                    for (PackageType pkg : getPackages()) {
                        Boolean visitNode = visitor.visitPackageNode(pkg);
                        if (visitNode == null) {
                            return;
                        }
                        if (Boolean.TRUE.equals(visitNode)) {
                            final boolean childLock = pkg.getModifyLock().readLock();
                            try {
                                for (ChildType child : pkg.getChildren()) {
                                    visitNode = visitor.visitChildrenNode(child);
                                    if (visitNode == null) {
                                        return;
                                    } else if (Boolean.FALSE.equals(visitNode)) {
                                        break;
                                    }
                                }
                            } finally {
                                pkg.getModifyLock().readUnlock(childLock);
                            }

                        }
                    }
                } finally {
                    readUnlock(pkgLock);
                }
            } else {
                for (PackageType pkg : getPackagesCopy()) {
                    Boolean visitNode = visitor.visitPackageNode(pkg);
                    if (visitNode == null) {
                        return;
                    }
                    if (Boolean.TRUE.equals(visitNode)) {
                        for (ChildType child : getChildrenCopy(pkg)) {
                            visitNode = visitor.visitChildrenNode(child);
                            if (visitNode == null) {
                                return;
                            } else if (Boolean.FALSE.equals(visitNode)) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public void merge(final PackageType dest, final java.util.List<ChildType> srcLinks, final java.util.List<PackageType> srcPkgs, final MergePosition mergeposition) {
        if (dest == null) {
            return;
        }
        if (srcLinks == null && srcPkgs == null) {
            return;
        }
        QUEUE.add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                int positionMerge = 0;

                switch (mergeposition) {
                case BOTTOM:
                    positionMerge = dest.getChildren().size();
                    break;
                case TOP:
                    positionMerge = 0;
                    break;
                default:
                    positionMerge = -1;
                }
                if (srcLinks != null) {
                    /* move srcLinks to dest */
                    moveOrAddAt(dest, srcLinks, positionMerge);
                    if (positionMerge != -1) {
                        /* update positionMerge in case we want merge@top */
                        positionMerge += srcLinks.size();
                    }
                }
                if (srcPkgs != null) {
                    for (PackageType pkg : srcPkgs) {
                        /* move links from srcPkgs to dest */
                        int size = pkg.getChildren().size();
                        moveOrAddAt(dest, pkg.getChildren(), positionMerge);
                        if (positionMerge != -1) {
                            /* update positionMerge in case we want merge@top */
                            positionMerge += size;
                        }
                    }
                }
                return null;
            }
        });
    }

    public void moveOrAddAt(final PackageType pkg, final List<ChildType> movechildren, final int moveChildrenindex) {
        moveOrAddAt(pkg, movechildren, moveChildrenindex, -1);
    }

    public void moveOrAddAt(final PackageType pkg, final List<ChildType> moveChildren, final int moveChildrenindex, final int pkgIndex) {
        if (pkg != null && moveChildren != null && moveChildren.size() > 0) {
            QUEUE.add(new QueueAction<Void, RuntimeException>() {
                /**
                 * Kind of binarysearch to add new links in a sorted list
                 *
                 * @param pkgchildren
                 * @param elementsToMove
                 * @param sorter
                 * @return
                 */
                protected final int search(List<ChildType> pkgchildren, ChildType elementToMove, PackageControllerComparator<ChildType> sorter) {
                    int min = 0;
                    int max = pkgchildren.size() - 1;
                    int mid = 0;
                    int comp;
                    while (min <= max) {
                        mid = (max + min) / 2;
                        ChildType midValue = pkgchildren.get(mid);
                        comp = sorter.compare(elementToMove, midValue);
                        if (min == max) {
                            //
                            return comp > 0 ? min + 1 : min;
                        }
                        if (comp < 0) {
                            max = mid;
                        } else if (comp > 0) {
                            min = mid + 1;
                        } else {
                            return mid;
                        }
                    }
                    return mid;
                }

                private final boolean containsOnlyNewChildren(final List<ChildType> children) {
                    for (final ChildType child : children) {
                        final PackageType parent = child.getParentNode();
                        if (parent != null) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                protected Void run() throws RuntimeException {
                    if (PackageController.this != pkg.getControlledBy()) {
                        /*
                         * package not yet under control of this PackageController so lets add it
                         */
                        PackageController.this.addmovePackageAt(pkg, pkgIndex, true);
                    }
                    boolean newChildren = false;
                    if (pkgIndex == -1 && moveChildrenindex == -1 && containsOnlyNewChildren(moveChildren)) {
                        try {
                            newChildren = true;
                            pkg.getModifyLock().writeLock();
                            final PackageControllerComparator<ChildType> sorter = pkg.getCurrentSorter();
                            final List<ChildType> pkgChildren = pkg.getChildren();
                            final int maxIndex = moveChildren.size();
                            if (sorter != null) {
                                for (int index = 0; index < maxIndex; index++) {
                                    final ChildType moveChild = moveChildren.get(index);
                                    pkgChildren.add(search(pkgChildren, moveChild, sorter), moveChild);
                                    moveChild.setParentNode(pkg);
                                }
                            } else {
                                pkgChildren.addAll(moveChildren);
                                for (int index = 0; index < maxIndex; index++) {
                                    final ChildType moveChild = moveChildren.get(index);
                                    /* this resets getPreviousParentNodeID */
                                    moveChild.setParentNode(pkg);
                                }
                            }
                        } finally {
                            pkg.getModifyLock().writeUnlock();
                            pkg.nodeUpdated(pkg, NOTIFY.STRUCTURE_CHANCE, null);
                        }
                        final boolean readL = pkg.getModifyLock().readLock();
                        try {
                            autoFileNameCorrection(pkg.getChildren(), pkg);
                        } catch (final Throwable e) {
                            logger.log(e);
                        } finally {
                            pkg.getModifyLock().readUnlock(readL);
                        }
                        getMapLock().writeLock();
                        try {
                            for (final ChildType moveChild : moveChildren) {
                                uniqueAlltimeIDChildrenMap.put(moveChild.getUniqueID(), moveChild);
                            }
                        } finally {
                            getMapLock().writeUnlock();
                        }
                    } else {
                        final List<ChildType> elementsToMove = new ArrayList<ChildType>(moveChildren);
                        /* build map for removal of children links */
                        final HashMap<PackageType, List<ChildType>> removeaddMap = new HashMap<PackageType, List<ChildType>>();
                        for (final ChildType child : elementsToMove) {
                            final PackageType parent = child.getParentNode();
                            if (parent == null || pkg == parent) {
                                /* parent is our destination, so no need here */
                                if (parent == null) {
                                    newChildren = true;
                                }
                                continue;
                            }
                            List<ChildType> pmap = removeaddMap.get(parent);
                            if (pmap == null) {
                                pmap = new ArrayList<ChildType>();
                                removeaddMap.put(parent, pmap);
                            }
                            pmap.add(child);
                            newChildren = true;
                        }
                        final Set<Entry<PackageType, List<ChildType>>> eset = removeaddMap.entrySet();
                        final Iterator<Entry<PackageType, List<ChildType>>> it = eset.iterator();
                        while (it.hasNext()) {
                            /* remove children from other packages */
                            final Entry<PackageType, List<ChildType>> next = it.next();
                            PackageType cpkg = next.getKey();
                            final PackageController<PackageType, ChildType> controller = cpkg.getControlledBy();
                            if (controller == null) {
                                logger.log(new Throwable("NO CONTROLLER!!!"));
                            } else {
                                controller.removeChildren(cpkg, next.getValue(), false);
                            }
                        }
                        final ArrayList<ChildType> children = getChildrenCopy(pkg);
                        int destIndex = moveChildrenindex;
                        /* remove all */
                        /*
                         * TODO: speed optimization, we have to correct the index to match changes in children structure
                         * 
                         * TODO: optimize this loop. only process existing links in this package
                         */
                        for (final ChildType child : elementsToMove) {
                            int childI = children.indexOf(child);
                            if (childI >= 0) {
                                if (childI < destIndex) {
                                    destIndex -= 1;
                                }
                                children.remove(childI);
                            }
                        }
                        /* add at wanted position */
                        if (destIndex < 0 || destIndex > children.size()) {
                            /* add at the end */
                            final PackageControllerComparator<ChildType> sorter = pkg.getCurrentSorter();
                            if (sorter != null) {
                                for (final ChildType c : elementsToMove) {
                                    children.add(search(children, c, sorter), c);
                                }
                            } else {
                                children.addAll(elementsToMove);
                            }
                        } else {
                            pkg.setCurrentSorter(null);
                            children.addAll(destIndex, elementsToMove);
                        }
                        if (newChildren) {
                            try {
                                autoFileNameCorrection(children, pkg);
                            } catch (final Throwable e) {
                                logger.log(e);
                            }
                        }
                        try {
                            pkg.getModifyLock().writeLock();
                            for (final ChildType child : elementsToMove) {
                                child.setParentNode(pkg);
                            }
                            for (final ChildType child : children) {
                                /* this resets getPreviousParentNodeID */
                                child.setParentNode(pkg);
                            }
                            pkg.getChildren().clear();
                            pkg.getChildren().addAll(children);
                        } finally {
                            pkg.getModifyLock().writeUnlock();
                            pkg.nodeUpdated(pkg, NOTIFY.STRUCTURE_CHANCE, null);
                        }
                        getMapLock().writeLock();
                        try {
                            for (ChildType child : elementsToMove) {
                                uniqueAlltimeIDChildrenMap.put(child.getUniqueID(), child);
                            }
                        } finally {
                            getMapLock().writeUnlock();
                        }
                    }
                    final long version = backendChanged.incrementAndGet();
                    structureChanged.set(version);
                    if (newChildren) {
                        childrenChanged.set(version);
                    }
                    _controllerPackageNodeStructureChanged(pkg, this.getQueuePrio());
                    _controllerStructureChanged(this.getQueuePrio());
                    return null;
                }
            });
        }
    }

    protected void autoFileNameCorrection(List<ChildType> pkgchildren, PackageType pkg) {
    }

    /**
     * remove the given children from the package. also removes the package from this PackageController in case it is empty after removal of
     * the children
     *
     * @param pkg
     * @param children
     */
    public void removeChildren(final PackageType pkg, final List<ChildType> children, final boolean doNotifyParentlessLinks) {

        if (pkg != null && children != null && children.size() > 0) {
            QUEUE.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    final ArrayList<ChildType> links = new ArrayList<ChildType>(children);
                    final PackageController<PackageType, ChildType> controller = pkg.getControlledBy();
                    if (controller == null) {
                        logger.log(new Throwable("NO CONTROLLER!!!"));
                        return null;
                    }
                    boolean notifyStructureChanges = false;
                    try {
                        pkg.getModifyLock().writeLock();
                        final List<ChildType> pkgchildren = pkg.getChildren();
                        for (final ChildType child : pkgchildren) {
                            /* this resets getPreviousParentNodeID */
                            child.setParentNode(pkg);
                        }
                        final ListIterator<ChildType> it = links.listIterator(links.size());
                        while (it.hasPrevious()) {
                            final ChildType dl = it.previous();
                            if (pkgchildren.remove(dl)) {
                                notifyStructureChanges = true;
                                /*
                                 * set FilePackage to null if the link was controlled by this FilePackage
                                 */
                                if (dl.getParentNode() == pkg) {
                                    dl.setParentNode(null);
                                } else {
                                    logger.log(new Throwable("removing children from wrong parent?!?!?"));
                                }
                            } else {
                                /* child not part of given package */
                                it.remove();
                            }
                        }
                    } finally {
                        pkg.getModifyLock().writeUnlock();
                        if (notifyStructureChanges) {
                            pkg.nodeUpdated(pkg, NOTIFY.STRUCTURE_CHANCE, null);
                        }
                    }
                    if (links.size() > 0) {
                        final long version = backendChanged.incrementAndGet();
                        controller.structureChanged.set(version);
                        if (doNotifyParentlessLinks) {
                            getMapLock().writeLock();
                            childrenChanged.set(version);
                            try {
                                for (final ChildType child : links) {
                                    uniqueAlltimeIDChildrenMap.remove(child.getUniqueID());
                                }
                            } finally {
                                getMapLock().writeUnlock();
                            }
                            controller._controllerParentlessLinks(links, this.getQueuePrio());
                        }
                        if (pkg.getChildren().size() == 0) {
                            controller.removePackage(pkg);
                        }
                    }
                    return null;
                }
            });
        }
    }

    public void clear() {
        QUEUE.add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                for (PackageType pkg : getPackagesCopy()) {
                    removePackage(pkg);
                }
                return null;
            }
        });
    }

    public void move(final java.util.List<PackageType> srcPkgs, final PackageType afterDest) {
        if (srcPkgs == null || srcPkgs.size() == 0) {
            return;
        }
        QUEUE.add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                PackageType internalafterDest = afterDest;
                for (PackageType srcPkg : srcPkgs) {
                    int destination = 0;
                    if (internalafterDest != null) {
                        int destI = indexOf(internalafterDest);
                        destination = Math.max(destI, 0) + 1;
                    }
                    addmovePackageAt(srcPkg, destination);
                    /* move next package after last moved one */
                    internalafterDest = srcPkg;
                }
                return null;
            }
        });
    }

    public void move(final java.util.List<ChildType> srcLinks, final PackageType dstPkg, final ChildType afterLink) {
        if (dstPkg == null || srcLinks == null || srcLinks.size() == 0) {
            return;
        }
        QUEUE.add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                int destination = 0;
                if (afterLink != null) {
                    int destI = dstPkg.indexOf(afterLink);
                    destination = Math.max(destI, 0) + 1;
                }
                moveOrAddAt(dstPkg, srcLinks, destination);
                return null;
            }
        });
    }

    abstract protected void _controllerParentlessLinks(final List<ChildType> links, QueuePriority priority);

    abstract protected void _controllerStructureChanged(QueuePriority priority);

    abstract protected void _controllerPackageNodeAdded(PackageType pkg, QueuePriority priority);

    abstract protected void _controllerPackageNodeRemoved(PackageType pkg, QueuePriority priority);

    abstract protected void _controllerPackageNodeStructureChanged(PackageType pkg, QueuePriority priority);

    public boolean readLock() {
        return lock.readLock();
    }

    public void readUnlock(boolean state) {
        lock.readUnlock(state);
    }

    public void writeLock() {
        lock.writeLock();
    }

    public void writeUnlock() {
        lock.writeUnlock();
    }

    public ArrayList<PackageType> getPackages() {
        return packages;
    }

    public ArrayList<PackageType> getPackagesCopy() {
        final boolean readL = readLock();
        try {
            return new ArrayList<PackageType>(getPackages());
        } finally {
            readUnlock(readL);
        }
    }

    public ArrayList<ChildType> getChildrenCopy(final PackageType pkg) {
        final boolean readL2 = pkg.getModifyLock().readLock();
        try {
            return new ArrayList<ChildType>(pkg.getChildren());
        } finally {
            pkg.getModifyLock().readUnlock(readL2);
        }
    }

    public void nodeUpdated(AbstractNode source, NOTIFY notify, Object param) {
        contentChanged.incrementAndGet();
    }

    public int indexOf(PackageType pkg) {
        boolean readL = readLock();
        try {
            return getPackages().indexOf(pkg);
        } finally {
            readUnlock(readL);
        }
    }

    protected volatile PackageControllerSelectionInfo<PackageType, ChildType> selectionInfo = null;

    public SelectionInfo<PackageType, ChildType> getSelectionInfo() {
        final long version = getBackendChanged();
        final PackageControllerSelectionInfo<PackageType, ChildType> lSelectionInfo = selectionInfo;
        if (lSelectionInfo != null && lSelectionInfo.getBackendVersion() == version) {
            return lSelectionInfo;
        }
        return getQueue().addWait(new QueueAction<SelectionInfo<PackageType, ChildType>, RuntimeException>(Queue.QueuePriority.HIGH) {

            @Override
            protected SelectionInfo<PackageType, ChildType> run() throws RuntimeException {
                final long version = getBackendChanged();
                PackageControllerSelectionInfo<PackageType, ChildType> lSelectionInfo = selectionInfo;
                if (lSelectionInfo != null && lSelectionInfo.getBackendVersion() == version) {
                    return lSelectionInfo;
                }
                lSelectionInfo = new PackageControllerSelectionInfo<PackageType, ChildType>(PackageController.this);
                selectionInfo = lSelectionInfo;
                return lSelectionInfo;
            }
        });

    }

    public int lastIndexOf(PackageType pkg) {
        boolean readL = readLock();
        try {
            return getPackages().lastIndexOf(pkg);
        } finally {
            readUnlock(readL);
        }
    }

    public void sort(final PackageControllerComparator<AbstractNode> comparator, final boolean sortPackages) {
        this.sorter = comparator;
        if (comparator != null) {
            QUEUE.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    final ArrayList<PackageType> lpackages = getPackagesCopy();
                    try {
                        Collections.sort(lpackages, comparator);
                    } catch (final Throwable e) {
                        LogController.CL(true).log(e);
                    }
                    if (sortPackages) {
                        final PackageControllerComparator<ChildType> sorter = new PackageControllerComparator<ChildType>() {

                            @Override
                            public int compare(ChildType o1, ChildType o2) {
                                return comparator.compare(o1, o2);
                            }

                            @Override
                            public String getID() {
                                return comparator.getID();
                            }

                            @Override
                            public boolean isAsc() {
                                return comparator.isAsc();
                            }

                        };
                        try {
                            for (final PackageType pkg : lpackages) {
                                final ArrayList<ChildType> children = getChildrenCopy(pkg);
                                try {
                                    Collections.sort(children, comparator);
                                } catch (final Throwable e) {
                                    LogController.CL(true).log(e);
                                }
                                pkg.getModifyLock().writeLock();
                                try {
                                    pkg.setCurrentSorter(sorter);
                                    for (ChildType child : children) {
                                        /* this resets getPreviousParentNodeID */
                                        child.setParentNode(pkg);
                                    }
                                    pkg.getChildren().clear();
                                    pkg.getChildren().addAll(children);
                                } finally {
                                    pkg.getModifyLock().writeUnlock();
                                    pkg.nodeUpdated(pkg, NOTIFY.STRUCTURE_CHANCE, null);
                                }
                            }
                        } finally {
                            structureChanged.set(backendChanged.incrementAndGet());
                        }
                    }
                    writeLock();
                    try {
                        getPackages().clear();
                        getPackages().addAll(lpackages);
                    } finally {
                        writeUnlock();
                    }
                    structureChanged.set(backendChanged.incrementAndGet());
                    if (sortPackages) {
                        for (final PackageType pkg : lpackages) {
                            _controllerPackageNodeStructureChanged(pkg, this.getQueuePrio());
                        }
                    }
                    _controllerStructureChanged(this.getQueuePrio());
                    return null;
                }
            });
        }
    }

    public PackageType getPackageByID(long longID) {
        final UniqueAlltimeID packageId = new UniqueAlltimeID(longID);
        final boolean readL = getMapLock().readLock();
        try {
            final PackageType pkg = uniqueAlltimeIDPackageMap.get(packageId);
            if (pkg != null) {
                return pkg;
            }
            return null;
        } finally {
            getMapLock().readUnlock(readL);
        }
    }

    public List<PackageType> getPackagesByID(long[] packageUUIDs) {
        final boolean readL = getMapLock().readLock();
        try {
            final ArrayList<PackageType> ret = new ArrayList<PackageType>(packageUUIDs.length);
            for (final long longID : packageUUIDs) {
                final UniqueAlltimeID packageId = new UniqueAlltimeID(longID);
                final PackageType pkg = uniqueAlltimeIDPackageMap.get(packageId);
                if (pkg != null) {
                    ret.add(pkg);
                }
            }
            return ret;
        } finally {
            getMapLock().readUnlock(readL);
        }
    }

    public ChildType getLinkByID(long longID) {
        final UniqueAlltimeID childID = new UniqueAlltimeID(longID);
        final boolean readL = getMapLock().readLock();
        try {
            final ChildType child = uniqueAlltimeIDChildrenMap.get(childID);
            if (child != null) {
                return child;
            }
            return null;
        } finally {
            getMapLock().readUnlock(readL);
        }
    }

    public List<ChildType> getLinksByID(long[] childUUIDs) {
        final boolean readL = getMapLock().readLock();
        try {
            final ArrayList<ChildType> ret = new ArrayList<ChildType>(childUUIDs.length);
            for (final long longID : childUUIDs) {
                final UniqueAlltimeID childID = new UniqueAlltimeID(longID);
                final ChildType child = uniqueAlltimeIDChildrenMap.get(childID);
                if (child != null) {
                    ret.add(child);
                }
            }
            return ret;
        } finally {
            getMapLock().readUnlock(readL);
        }
    }

    public PackageControllerComparator<AbstractNode> getSorter() {
        return sorter;
    }
}
