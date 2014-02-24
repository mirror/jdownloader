package org.jdownloader.statistics;

import org.appwork.storage.Storable;

public class RIDWrapper<T> implements Storable {
    public RIDWrapper(/* storable */) {

    }

    private T    data;
    private long rid = -1;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getRid() {
        return rid;
    }

    public void setRid(long rid) {
        this.rid = rid;
    }
}
