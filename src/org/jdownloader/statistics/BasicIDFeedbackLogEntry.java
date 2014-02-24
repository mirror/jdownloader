package org.jdownloader.statistics;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;

public class BasicIDFeedbackLogEntry extends AbstractFeedbackLogEntry implements Storable {
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    protected BasicIDFeedbackLogEntry(/* storable */) {
        super();
    }

    public BasicIDFeedbackLogEntry(boolean positive, String id) {
        super(positive);
        this.id = id;
    }

    @Override
    public String toString() {
        return JSonStorage.toString(this);
    }

    private int counter;

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public int getCounter() {
        return counter;
    }
}
