package jd.controlling.downloadcontroller;

public class FileIsLockedException extends Exception {

    private Object lockHolder;

    public Object getLockHolder() {
        return lockHolder;
    }

    public void setLockHolder(Object lockHolder) {
        this.lockHolder = lockHolder;
    }

    public FileIsLockedException(Object currentHolder) {
        super("File is locked by " + currentHolder);
        lockHolder = currentHolder;
    }

}
