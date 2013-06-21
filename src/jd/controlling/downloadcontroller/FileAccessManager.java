package jd.controlling.downloadcontroller;

import java.io.File;
import java.util.HashMap;

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

    public synchronized void setLock(File file, Object newLockHolder) throws FileIsLockedException {
        Object currentHolder = getLockHolder(file);
        if (currentHolder != null) throw new FileIsLockedException(currentHolder);
        locks.put(file, newLockHolder);
    }

    public synchronized Object getLockHolder(File file) {
        return locks.get(file);
    }

}
