package jd.controlling.downloadcontroller;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class FileAccessManager {

    private HashMap<File, Object> locks;

    public FileAccessManager() {
        locks = new HashMap<File, Object>();
    }

    public synchronized Object unlock(File file) {
        return locks.remove(file);
    }

    public synchronized boolean unlock(File file, Object lockHolder) {
        Object currentHolder = getLockHolder(file);
        if (currentHolder != null && lockHolder == currentHolder) {
            locks.remove(file);
            return true;
        } else {
            return false;
        }
    }

    public synchronized void lock(File file, Object newLockHolder) throws FileIsLockedException {
        Object currentHolder = getLockHolder(file);
        if (currentHolder != null) {
            if (currentHolder == newLockHolder) return;
            throw new FileIsLockedException(currentHolder);
        }
        locks.put(file, newLockHolder);
    }

    public synchronized void unlockAllHeldby(Object lockHolder) {
        Iterator<Entry<File, Object>> it = locks.entrySet().iterator();
        while (it.hasNext()) {
            Entry<File, Object> next = it.next();
            if (next.getValue() == lockHolder) {
                it.remove();
            }
        }
    }

    public synchronized Object getLockHolder(File file) {
        return locks.get(file);
    }

    public synchronized boolean isLocked(File file) {
        return locks.get(file) != null;
    }

}
