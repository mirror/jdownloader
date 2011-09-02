package jd.controlling.packagecontroller;

import java.util.ArrayList;
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

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;

public abstract class PackageController<E extends AbstractPackageNode<V, E>, V extends AbstractPackageChildrenNode<E>> {
    private final AtomicLong structureChanged = new AtomicLong(0);

    public long getPackageControllerChanges() {
        return structureChanged.get();
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
        if (pkg != null) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
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
                        /* remove package from another controller */
                        controller.removePackage(pkg);
                    }
                    writeLock();
                    try {
                        ListIterator<E> li = packages.listIterator();
                        int counter = 0;
                        boolean done = false;
                        boolean addLast = false;
                        if (index < 0) {
                            /* add end of list */
                            done = true;
                            addLast = true;
                        }
                        while (li.hasNext()) {
                            if (done && !need2Remove) break;
                            E c = li.next();
                            if (counter == index) {
                                li.add(pkg);
                                done = true;
                            }
                            if (c == pkg) {
                                li.remove();
                                isNew = false;
                                need2Remove = false;
                            }
                            counter++;
                        }
                        if (!done || addLast) {
                            /**
                             * index > packages.size , then add at end
                             */
                            packages.addLast(pkg);
                        }
                        pkg.setControlledBy(PackageController.this);
                    } finally {
                        writeUnlock();
                    }
                    structureChanged.incrementAndGet();
                    if (isNew) {
                        _controllerPackageNodeAdded(pkg);
                    } else {
                        _controllerStructureChanged();
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
                            removed = packages.remove(pkg);
                        } finally {
                            writeUnlock();
                        }
                    }
                    synchronized (pkg) {
                        remove = new ArrayList<V>(pkg.getChildren());
                    }
                    if (removed && remove != null) {
                        controller._controllerParentlessLinks(remove);
                    }
                    if (removed) {
                        controller.structureChanged.incrementAndGet();
                        controller._controllerPackageNodeRemoved(pkg);
                    }
                    return null;
                }
            });
        }
    }

    public void addmoveChildren(final E pkg, final List<V> children, final int index) {
        if (pkg != null && children != null && children.size() > 0) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    if (PackageController.this != pkg.getControlledBy()) {
                        /*
                         * package not yet under control of this
                         * PackageController so lets add it
                         */
                        PackageController.this.addmovePackageAt(pkg, -1);
                    }
                    /* build map for removal of children links */
                    HashMap<E, LinkedList<V>> removeaddMap = new HashMap<E, LinkedList<V>>();
                    for (V child : children) {
                        E parent = child.getParentNode();
                        if (parent == null || pkg == parent) {
                            /* parent is our destination, so no need here */
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
                        controller.removeChildren(cpkg, next.getValue(), false);
                    }
                    writeLock();
                    try {
                        synchronized (pkg) {
                            List<V> pkgchildren = pkg.getChildren();
                            /* remove all */
                            pkgchildren.removeAll(children);
                            /* add at wanted position */
                            if (index < 0 || index >= pkgchildren.size() - 1) {
                                /* add at the end */
                                pkgchildren.addAll(children);
                            } else {
                                pkgchildren.addAll(index, children);
                            }
                            for (V child : children) {
                                child.setParentNode(pkg);
                            }
                            pkg.notifyChanges();
                        }
                    } finally {
                        writeUnlock();
                    }
                    structureChanged.incrementAndGet();
                    _controllerStructureChanged();
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
                            pkg.notifyChanges();
                        }
                    } finally {
                        writeUnlock();
                    }
                    if (links.size() > 0) {
                        controller.structureChanged.incrementAndGet();
                        if (doNotifyParentlessLinks) controller._controllerParentlessLinks(links);
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
                writeLock();
                try {
                    clearList = new ArrayList<E>(packages);
                } finally {
                    writeUnlock();
                }
                for (E pkg : clearList) {
                    removePackage(pkg);
                }
                return null;
            }
        });
    }

    abstract protected void _controllerParentlessLinks(final List<V> links);

    abstract protected void _controllerPackageNodeRemoved(E pkg);

    abstract protected void _controllerStructureChanged();

    abstract protected void _controllerPackageNodeAdded(E pkg);

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
