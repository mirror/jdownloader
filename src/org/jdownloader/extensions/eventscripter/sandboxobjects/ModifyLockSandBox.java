package org.jdownloader.extensions.eventscripter.sandboxobjects;

import org.appwork.utils.ModifyLock;

public class ModifyLockSandBox {
    private final ModifyLock lock;

    protected ModifyLockSandBox(ModifyLock lock) {
        this.lock = lock;
    }

    public boolean readLock() {
        return lock.readLock();
    }

    public final void readUnlock(final boolean state) throws IllegalMonitorStateException {
        lock.readUnlock(state);
    }

    public final void writeLock() {
        lock.writeLock();
    }

    public final void writeUnlock() throws IllegalMonitorStateException {
        lock.writeUnlock();
    }
}
