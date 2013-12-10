package org.jdownloader.api.polling;

import java.util.HashMap;

import org.appwork.storage.Storable;

public class PollingResultAPIStorable implements Storable {

    private String                  eventName;
    private org.jdownloader.myjdownloader.client.json.JsonMap eventData;

    public PollingResultAPIStorable(/* Storable */) {

    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public org.jdownloader.myjdownloader.client.json.JsonMap getEventData() {
        return eventData;
    }

    public void setEventData(org.jdownloader.myjdownloader.client.json.JsonMap eventData) {
        this.eventData = eventData;
    }
}
