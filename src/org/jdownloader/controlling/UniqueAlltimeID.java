package org.jdownloader.controlling;

import java.util.concurrent.atomic.AtomicLong;

public class UniqueAlltimeID {
    private static final AtomicLong ID = new AtomicLong(System.currentTimeMillis());
    private long                    id;

    public UniqueAlltimeID() {
        id = ID.incrementAndGet();
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof UniqueAlltimeID) {
            if (((UniqueAlltimeID) o).id == id) return true;
        }
        return false;
    }

    public long getID() {
        return id;
    }

    @Override
    public String toString() {
        return id + "";
    }

    /**
     * WARNING: by manually changing the ID you can break unique state of this Instance!
     * 
     * @param ID
     */
    public void setID(long ID) {
        this.id = ID;
    }

    public static String create() {
        return ID.incrementAndGet() + "";
    }
}
