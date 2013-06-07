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
}
