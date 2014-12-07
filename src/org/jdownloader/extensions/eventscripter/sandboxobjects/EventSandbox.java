package org.jdownloader.extensions.eventscripter.sandboxobjects;

import org.appwork.remoteapi.events.EventObject;
import org.jdownloader.extensions.eventscripter.ScriptAPI;

@ScriptAPI(description = "The Event Object")
public class EventSandbox {

    private String id;
    private Object data;

    public String getId() {
        return id;
    }

    public Object getData() {
        return data;
    }

    public String getPublisher() {
        return publisher;
    }

    private String publisher;

    public EventSandbox(EventObject event) {

        this.id = event.getEventid();
        this.data = event.getEventdata();
        this.publisher = event.getPublisher().getPublisherName();
    }

    public EventSandbox() {
    }

}
