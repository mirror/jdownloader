package org.jdownloader.statistics;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;

public class LogEntryWrapper implements Storable {
    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private String type;

    public LogEntryWrapper(/* storable */) {

    }

    public LogEntryWrapper(AbstractLogEntry e) {
        this.data = JSonStorage.serializeToJson(e);
        this.type = e.getClass().getSimpleName();
    }

}
