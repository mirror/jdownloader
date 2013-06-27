package jd.controlling.packagecontroller;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ModifyLock {

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public boolean readLock() {
        if (!this.lock.writeLock().isHeldByCurrentThread()) {
            this.lock.readLock().lock();
            return true;
        }
        return false;
    }

    public void readUnlock(boolean state) throws IllegalMonitorStateException {
        if (state == false) return;
        this.lock.readLock().unlock();
    }

    public void writeLock() {
        this.lock.writeLock().lock();
    }

    public void writeUnlock() throws IllegalMonitorStateException {
        this.lock.writeLock().unlock();
    }

    public void runReadLock(Runnable run) {
        Boolean readL = null;
        try {
            readL = readLock();
            run.run();
        } finally {
            if (readL != null) readUnlock(readL);
        }
    }

    public void runWriteLock(Runnable run) {
        try {
            writeLock();
            run.run();
        } finally {
            writeUnlock();
        }
    }
}
