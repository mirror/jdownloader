package org.jdownloader.api.polling;

import org.appwork.remoteapi.QueryResponseMap;
import org.appwork.storage.Storable;

public class PollingResultAPIStorable implements Storable {

    private String           eventName;
    private QueryResponseMap eventData;

    public PollingResultAPIStorable(/* Storable */) {

    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public QueryResponseMap getEventData() {
        return eventData;
    }

    public void setEventData(QueryResponseMap eventData) {
        this.eventData = eventData;
    }
}
