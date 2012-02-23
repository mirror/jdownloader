package jd.controlling.packagecontroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import jd.controlling.IOEQ;

import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;

public abstract class PackageController<PackageType extends AbstractPackageNode<ChildType, PackageType>, ChildType extends AbstractPackageChildrenNode<PackageType>> implements AbstractNodeNotifier<PackageType> {
    private final AtomicLong structureChanged = new AtomicLong(0);
    private final AtomicLong childrenChanged  = new AtomicLong(0);
    private final AtomicLong contentChanged   = new AtomicLong(0);

    public long getPackageControllerChanges() {
        return structureChanged.get();
    }

    public long getChildrenChanges() {
        return childrenChanged.get();
    }

    public long getContentChanges() {
        return contentChanged.get();
    }

    protected LinkedList<PackageType>    packages  = new LinkedList<PackageType>();

    private final ReentrantReadWriteLock lock      = new ReentrantReadWriteLock();
    private final ReadLock               readLock  = this.lock.readLock();
    private final WriteLock              writeLock = this.lock.writeLock();

    /**
     * add a Package at given position position in this PackageController. in
     * case the Package is already controlled by this PackageController this
     * function does move it to the given position
     * 
     * @param pkg
     * @param index
     */
    public void addmovePackageAt(final PackageType pkg, final int index) {
        addmovePackageAt(pkg, index, false);
    }

    public void sortPackageChildren(final PackageType pkg, final Comparator<ChildType> comparator) {
        if (pkg != null && comparator != null) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    writeLock();
                    try {
                        synchronized (pkg) {
                            List<ChildType> children = pkg.getChildren();
                            Collections.sort(children, comparator);
                        }
                    } finally {
                        writeUnlock();
                    }
                    structureChanged.incrementAndGet();
                    _controllerStructureChanged(this.getQueuePrio());
                    return null;
                }

            });
        }
    }

    protected void addmovePackageAt(final PackageType pkg, final int index, final boolean allowEmpty) {
        if (pkg != null) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    if (allowEmpty == false && pkg.getChildren().size() == 0) {
                        /* we dont want empty packages here */
                        return null;
                    }
                    boolean isNew = true;

                    /**
                     * iterate through all packages, remove the existing one and
                     * add at given position
                     */
                    PackageController<PackageType, ChildType> controller = pkg.getControlledBy();
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
                        ListIterator<PackageType> li = packages.listIterator();
                        int currentIndex = 0;
                        boolean done = false;
                        boolean addLast = false;
                        if (index < 0 || index > packages.size() - 1) {
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
                                 * current element is pkg and index is correct,
                                 * nothing to do
                                 */
                                return null;
                            } else if (currentIndex == index) {
                                /*
                                 * current position is wished index, lets add
                                 * pkg here
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
                            packages.addLast(pkg);
                        }
                        pkg.setControlledBy(PackageController.this);
                    } finally {
                        writeUnlock();
                    }
                    structureChanged.incrementAndGet();
                    if (isNew) {
                        if (pkg.getChildren().size() > 0) childrenChanged.incrementAndGet();
                        _controllerPackageNodeAdded(pkg, this.getQueuePrio());
                    } else {
                        _controllerStructureChanged(this.getQueuePrio());
                    }
                    return null;
                }
            });
        }
    }

    public int size() {
        return packages.size();
    }

    /* remove the Package from this PackageController */
    public void removePackage(final PackageType pkg) {
        if (pkg != null) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    boolean removed = false;
                    ArrayList<ChildType> remove = null;
                    PackageController<PackageType, ChildType> controller = pkg.getControlledBy();
                    if (controller == null) {
                        Log.exception(new Throwable("NO CONTROLLER!!!"));
                        return null;
                    }
                    if (pkg.getControlledBy() != null) {
                        if (PackageController.this != pkg.getControlledBy()) {
                            /* this should never happen */
                            Log.exception(new Throwable("removing a package which is not controlled by this controller?!?!?"));
                        }
                        pkg.setControlledBy(null);
                        writeLock();
                        try {
                            removed = controller.packages.remove(pkg);
                        } finally {
                            writeUnlock();
                        }
                    }
                    synchronized (pkg) {
                        remove = new ArrayList<ChildType>(pkg.getChildren());
                    }
                    if (removed && remove != null) {
                        if (remove.size() > 0) {
                            childrenChanged.incrementAndGet();
                            controller._controllerParentlessLinks(remove, this.getQueuePrio());
                        }
                    }
                    if (removed) {
                        controller.structureChanged.incrementAndGet();
                        controller._controllerPackageNodeRemoved(pkg, this.getQueuePrio());
                    }
                    return null;
                }
            });
        }
    }

    public void removeChildren(final List<ChildType> removechildren) {
        if (removechildren != null && removechildren.size() > 0) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    LinkedList<ChildType> children = new LinkedList<ChildType>(removechildren);
                    /* build map for removal of children links */
                    boolean childrenRemoved = false;
                    HashMap<PackageType, LinkedList<ChildType>> removeaddMap = new HashMap<PackageType, LinkedList<ChildType>>();
                    for (ChildType child : children) {
                        PackageType parent = child.getParentNode();
                        if (parent == null) {
                            continue;
                        }
                        LinkedList<ChildType> pmap = removeaddMap.get(parent);
                        if (pmap == null) {
                            childrenRemoved = true;
                            pmap = new LinkedList<ChildType>();
                            removeaddMap.put(parent, pmap);
                        }
                        pmap.add(child);
                    }
                    Set<Entry<PackageType, LinkedList<ChildType>>> eset = removeaddMap.entrySet();
                    Iterator<Entry<PackageType, LinkedList<ChildType>>> it = eset.iterator();
                    while (it.hasNext()) {
                        /* remove children from other packages */
                        Entry<PackageType, LinkedList<ChildType>> next = it.next();
                        PackageType cpkg = next.getKey();
                        PackageController<PackageType, ChildType> controller = cpkg.getControlledBy();
                        if (controller == null) {
                            Log.exception(new Throwable("NO CONTROLLER!!!"));
                        } else {
                            controller.removeChildren(cpkg, next.getValue(), true);
                        }
                    }
                    structureChanged.incrementAndGet();
                    if (childrenRemoved) childrenChanged.incrementAndGet();
                    _controllerStructureChanged(this.getQueuePrio());
                    return null;
                }
            });
        }
    }

    public List<ChildType> getChildrenByFilter(AbstractPackageChildrenNodeFilter<ChildType> filter) {
        ArrayList<ChildType> ret = new ArrayList<ChildType>();
        boolean readL = readLock();
        try {
            for (PackageType pkg : packages) {
                synchronized (pkg) {
                    for (ChildType child : pkg.getChildren()) {
                        if (filter.returnMaxResults() > 0 && ret.size() == filter.returnMaxResults()) {
                            /* max results found, lets return */
                            return ret;
                        }
                        if (filter.isChildrenNodeFiltered(child)) {
                            ret.add(child);
                        }
                    }
                }
            }
        } finally {
            readUnlock(readL);
        }
        return ret;
    }

    public void merge(final PackageType dest, final ArrayList<ChildType> srcLinks, final ArrayList<PackageType> srcPkgs, final boolean mergeTop) {
        if (dest == null) return;
        if (srcLinks == null && srcPkgs == null) return;
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                int positionMerge = mergeTop ? 0 : -1;
                if (srcLinks != null) {
                    /* move srcLinks to dest */
                    addmoveChildren(dest, srcLinks, positionMerge);
                    if (positionMerge != -1) {
                        /* update positionMerge in case we want merge@top */
                        positionMerge += srcLinks.size();
                    }
                }
                if (srcPkgs != null) {
                    for (PackageType pkg : srcPkgs) {
                        /* move links from srcPkgs to dest */
                        int size = pkg.getChildren().size();
                        addmoveChildren(dest, pkg.getChildren(), positionMerge);
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

    public void addmoveChildren(final PackageType pkg, final List<ChildType> movechildren, final int index) {
        if (pkg != null && movechildren != null && movechildren.size() > 0) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    LinkedList<ChildType> children = new LinkedList<ChildType>(movechildren);
                    if (PackageController.this != pkg.getControlledBy()) {
                        /*
                         * package not yet under control of this
                         * PackageController so lets add it
                         */
                        PackageController.this.addmovePackageAt(pkg, -1, true);
                    }
                    /* build map for removal of children links */
                    boolean newChildren = false;
                    HashMap<PackageType, LinkedList<ChildType>> removeaddMap = new HashMap<PackageType, LinkedList<ChildType>>();
                    for (ChildType child : children) {
                        PackageType parent = child.getParentNode();
                        if (parent == null || pkg == parent) {
                            /* parent is our destination, so no need here */
                            if (parent == null) {
                                newChildren = true;
                            }
                            continue;
                        }
                        LinkedList<ChildType> pmap = removeaddMap.get(parent);
                        if (pmap == null) {
                            pmap = new LinkedList<ChildType>();
                            removeaddMap.put(parent, pmap);
                        }
                        pmap.add(child);
                    }
                    Set<Entry<PackageType, LinkedList<ChildType>>> eset = removeaddMap.entrySet();
                    Iterator<Entry<PackageType, LinkedList<ChildType>>> it = eset.iterator();
                    while (it.hasNext()) {
                        /* remove children from other packages */
                        Entry<PackageType, LinkedList<ChildType>> next = it.next();
                        PackageType cpkg = next.getKey();
                        PackageController<PackageType, ChildType> controller = cpkg.getControlledBy();
                        if (controller == null) {
                            Log.exception(new Throwable("NO CONTROLLER!!!"));
                        } else {
                            controller.removeChildren(cpkg, next.getValue(), false);
                        }
                    }
                    writeLock();
                    try {
                        synchronized (pkg) {
                            int destIndex = index;
                            List<ChildType> pkgchildren = pkg.getChildren();
                            /* remove all */
                            /*
                             * TODO: speed optimization, we have to correct the
                             * index to match changes in children structure
                             */
                            for (ChildType child : children) {
                                int childI = pkgchildren.indexOf(child);
                                if (childI >= 0) {
                                    if (childI < destIndex) destIndex -= 1;
                                    pkgchildren.remove(childI);
                                }
                            }
                            /* add at wanted position */
                            if (destIndex < 0 || destIndex > pkgchildren.size() - 1) {
                                /* add at the end */
                                pkgchildren.addAll(children);
                            } else {
                                pkgchildren.addAll(destIndex, children);
                            }
                            for (ChildType child : children) {
                                child.setParentNode(pkg);
                            }
                        }
                        pkg.notifyStructureChanges();
                    } finally {
                        writeUnlock();
                    }
                    structureChanged.incrementAndGet();
                    if (newChildren) childrenChanged.incrementAndGet();
                    _controllerStructureChanged(this.getQueuePrio());

                    return null;
                }
            });
        }
    }

    /**
     * remove the given children from the package. also removes the package from
     * this PackageController in case it is empty after removal of the children
     * 
     * @param pkg
     * @param children
     */
    public void removeChildren(final PackageType pkg, final List<ChildType> children, final boolean doNotifyParentlessLinks) {
        if (pkg != null && children != null && children.size() > 0) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    LinkedList<ChildType> links = new LinkedList<ChildType>(children);
                    PackageController<PackageType, ChildType> controller = pkg.getControlledBy();
                    if (controller == null) {
                        Log.exception(new Throwable("NO CONTROLLER!!!"));
                        return null;
                    }
                    writeLock();
                    try {
                        synchronized (pkg) {
                            List<ChildType> pkgchildren = pkg.getChildren();
                            Iterator<ChildType> it = links.iterator();
                            while (it.hasNext()) {
                                ChildType dl = it.next();
                                if (pkgchildren.remove(dl)) {
                                    /*
                                     * set FilePackage to null if the link was
                                     * controlled by this FilePackage
                                     */
                                    if (dl.getParentNode() == pkg) {
                                        dl.setParentNode(null);
                                    } else {
                                        Log.exception(new Throwable("removing children from wrong parent?!?!?"));
                                    }
                                } else {
                                    /* child not part of given package */
                                    it.remove();
                                }
                            }
                        }
                        pkg.notifyStructureChanges();
                    } finally {
                        writeUnlock();
                    }
                    if (links.size() > 0) {
                        controller.structureChanged.incrementAndGet();
                        if (doNotifyParentlessLinks) {
                            childrenChanged.incrementAndGet();
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
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                ArrayList<PackageType> clearList = null;
                boolean readL = readLock();
                try {
                    clearList = new ArrayList<PackageType>(packages);
                } finally {
                    readUnlock(readL);
                }
                for (PackageType pkg : clearList) {
                    removePackage(pkg);
                }
                return null;
            }
        });
    }

    public void move(final ArrayList<PackageType> srcPkgs, final PackageType afterDest) {
        if (srcPkgs == null || srcPkgs.size() == 0) return;
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                for (PackageType srcPkg : srcPkgs) {
                    int destination = 0;
                    if (afterDest != null) {
                        int destI = 0;
                        boolean readL = readLock();
                        try {
                            destI = packages.indexOf(afterDest);
                        } finally {
                            readUnlock(readL);
                        }
                        destination = Math.max(destI, 0) + 1;
                    }
                    addmovePackageAt(srcPkg, destination);
                }
                return null;
            }
        });
    }

    public void move(final ArrayList<ChildType> srcLinks, final PackageType dstPkg, final ChildType afterLink) {
        if (dstPkg == null || srcLinks == null || srcLinks.size() == 0) return;
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                int destination = 0;
                if (afterLink != null) {
                    int destI = 0;
                    synchronized (dstPkg) {
                        destI = dstPkg.getChildren().indexOf(afterLink);
                    }
                    destination = Math.max(destI, 0) + 1;
                }
                addmoveChildren(dstPkg, srcLinks, destination);
                return null;
            }
        });
    }

    abstract protected void _controllerParentlessLinks(final List<ChildType> links, QueuePriority priority);

    abstract protected void _controllerPackageNodeRemoved(PackageType pkg, QueuePriority priority);

    abstract protected void _controllerStructureChanged(QueuePriority priority);

    abstract protected void _controllerPackageNodeAdded(PackageType pkg, QueuePriority priority);

    public boolean readLock() {
        if (!this.writeLock.isHeldByCurrentThread()) {
            this.readLock.lock();
            return true;
        }
        return false;
    }

    public void readUnlock(boolean state) {
        if (state == false) return;
        readUnlock();
    }

    public void readUnlock() {
        try {
            this.readLock.unlock();
        } catch (final IllegalMonitorStateException e) {
            Log.exception(e);
        }
    }

    public void writeLock() {
        this.writeLock.lock();
    }

    public void writeUnlock() {
        this.writeLock.unlock();
    }

    public LinkedList<PackageType> getPackages() {
        return packages;
    }

    public void nodeUpdated(PackageType source) {
        contentChanged.incrementAndGet();
    }

    public int indexOf(PackageType pkg) {
        boolean readL = readLock();
        try {
            return packages.indexOf(pkg);
        } finally {
            readUnlock(readL);
        }
    }

}
