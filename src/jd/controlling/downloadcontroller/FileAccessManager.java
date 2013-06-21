package jd.controlling.downloadcontroller;

import java.io.File;
import java.util.HashMap;

public class FileAccessManager {

    private HashMap<File, Object> locks;

    public FileAccessManager() {
        locks = new HashMap<File, Object>();
    }

    public Object unlock(File file) {
        return locks.remove(file);
    }

    public synchronized void setLock(File file, Object newLockHolder) throws FileIsLockedException {

        Object currentHolder = getLockHolder(file);
        if (currentHolder != null) throw new FileIsLockedException(currentHolder);
        locks.put(file, newLockHolder);
    }

    public Object getLockHolder(File file) {
        return locks.get(file);
    }

}
