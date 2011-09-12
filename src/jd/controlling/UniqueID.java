package jd.controlling;

import java.util.concurrent.atomic.AtomicLong;

public class UniqueID {
    private static final AtomicLong ID = new AtomicLong(0);
    private final long              id = ID.incrementAndGet();

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof UniqueID) {
            if (((UniqueID) o).id == id) return true;
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
}
