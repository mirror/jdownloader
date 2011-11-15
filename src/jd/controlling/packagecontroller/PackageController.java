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

public abstract class PackageController<E extends AbstractPackageNode<V, E>, V extends AbstractPackageChildrenNode<E>> {
    private final AtomicLong structureChanged = new AtomicLong(0);
    private final AtomicLong childrenChanged  = new AtomicLong(0);

    public long getPackageControllerChanges() {
        return structureChanged.get();
    }

    public long getChildrenChanges() {
        return childrenChanged.get();
    }

    protected LinkedList<E>              packages  = new LinkedList<E>();

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
    public void addmovePackageAt(final E pkg, final int index) {
        addmovePackageAt(pkg, index, false);
    }

    public void sortPackageChildren(final E pkg, final Comparator<V> comparator) {
        if (pkg != null && comparator != null) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    writeLock();
                    try {
                        synchronized (pkg) {
                            List<V> children = pkg.getChildren();
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

    protected void addmovePackageAt(final E pkg, final int index, final boolean allowEmpty) {
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
                    PackageController<E, V> controller = pkg.getControlledBy();
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
                        ListIterator<E> li = packages.listIterator();
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
                            E c = li.next();
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
                                E replaced = c;
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
    public void removePackage(final E pkg) {
        if (pkg != null) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    boolean removed = false;
                    ArrayList<V> remove = null;
                    PackageController<E, V> controller = pkg.getControlledBy();
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
                        remove = new ArrayList<V>(pkg.getChildren());
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

    public void removeChildren(final List<V> removechildren) {
        if (removechildren != null && removechildren.size() > 0) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    LinkedList<V> children = new LinkedList<V>(removechildren);
                    /* build map for removal of children links */
                    boolean childrenRemoved = false;
                    HashMap<E, LinkedList<V>> removeaddMap = new HashMap<E, LinkedList<V>>();
                    for (V child : children) {
                        E parent = child.getParentNode();
                        if (parent == null) {
                            continue;
                        }
                        LinkedList<V> pmap = removeaddMap.get(parent);
                        if (pmap == null) {
                            childrenRemoved = true;
                            pmap = new LinkedList<V>();
                            removeaddMap.put(parent, pmap);
                        }
                        pmap.add(child);
                    }
                    Set<Entry<E, LinkedList<V>>> eset = removeaddMap.entrySet();
                    Iterator<Entry<E, LinkedList<V>>> it = eset.iterator();
                    while (it.hasNext()) {
                        /* remove children from other packages */
                        Entry<E, LinkedList<V>> next = it.next();
                        E cpkg = next.getKey();
                        PackageController<E, V> controller = cpkg.getControlledBy();
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

    public List<V> getChildrenByFilter(AbstractPackageChildrenNodeFilter<V> filter) {
        ArrayList<V> ret = new ArrayList<V>();
        boolean readL = readLock();
        try {
            for (E pkg : packages) {
                synchronized (pkg) {
                    for (V child : pkg.getChildren()) {
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

    public void addmoveChildren(final E pkg, final List<V> movechildren, final int index) {
        if (pkg != null && movechildren != null && movechildren.size() > 0) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    LinkedList<V> children = new LinkedList<V>(movechildren);
                    if (PackageController.this != pkg.getControlledBy()) {
                        /*
                         * package not yet under control of this
                         * PackageController so lets add it
                         */
                        PackageController.this.addmovePackageAt(pkg, -1, true);
                    }
                    /* build map for removal of children links */
                    boolean newChildren = false;
                    HashMap<E, LinkedList<V>> removeaddMap = new HashMap<E, LinkedList<V>>();
                    for (V child : children) {
                        E parent = child.getParentNode();
                        if (parent == null || pkg == parent) {
                            /* parent is our destination, so no need here */
                            if (parent == null) {
                                newChildren = true;
                            }
                            continue;
                        }
                        LinkedList<V> pmap = removeaddMap.get(parent);
                        if (pmap == null) {
                            pmap = new LinkedList<V>();
                            removeaddMap.put(parent, pmap);
                        }
                        pmap.add(child);
                    }
                    Set<Entry<E, LinkedList<V>>> eset = removeaddMap.entrySet();
                    Iterator<Entry<E, LinkedList<V>>> it = eset.iterator();
                    while (it.hasNext()) {
                        /* remove children from other packages */
                        Entry<E, LinkedList<V>> next = it.next();
                        E cpkg = next.getKey();
                        PackageController<E, V> controller = cpkg.getControlledBy();
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
                            List<V> pkgchildren = pkg.getChildren();
                            /* remove all */
                            /*
                             * TODO: speed optimization, we have to correct the
                             * index to match changes in children structure
                             */
                            for (V child : children) {
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
                            for (V child : children) {
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
    public void removeChildren(final E pkg, final List<V> children, final boolean doNotifyParentlessLinks) {
        if (pkg != null && children != null && children.size() > 0) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    LinkedList<V> links = new LinkedList<V>(children);
                    PackageController<E, V> controller = pkg.getControlledBy();
                    if (controller == null) {
                        Log.exception(new Throwable("NO CONTROLLER!!!"));
                        return null;
                    }
                    writeLock();
                    try {
                        synchronized (pkg) {
                            List<V> pkgchildren = pkg.getChildren();
                            Iterator<V> it = links.iterator();
                            while (it.hasNext()) {
                                V dl = it.next();
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
                ArrayList<E> clearList = null;
                boolean readL = readLock();
                try {
                    clearList = new ArrayList<E>(packages);
                } finally {
                    readUnlock(readL);
                }
                for (E pkg : clearList) {
                    removePackage(pkg);
                }
                return null;
            }
        });
    }

    abstract protected void _controllerParentlessLinks(final List<V> links, QueuePriority priority);

    abstract protected void _controllerPackageNodeRemoved(E pkg, QueuePriority priority);

    abstract protected void _controllerStructureChanged(QueuePriority priority);

    abstract protected void _controllerPackageNodeAdded(E pkg, QueuePriority priority);

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

    public LinkedList<E> getPackages() {
        return packages;
    }
}
