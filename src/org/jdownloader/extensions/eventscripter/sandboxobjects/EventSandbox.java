package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.appwork.remoteapi.events.EventObject;
import org.appwork.storage.JSonStorage;
import org.jdownloader.extensions.eventscripter.ScriptAPI;

@ScriptAPI(description = "The Event Object")
public class EventSandbox {

    private String id;
    private Object data;

    public String getId() {
        return id;
    }

    public String getData() {
        if (data == null) {
            return null;
        } else {
            return JSonStorage.toString(data);
        }
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
        // we need this so getTestProperties has some dummy data and ScriptThread can preinit the needed classes
        this.id = "test";
        final Map<String, Object> data = new HashMap<String, Object>();
        data.put("int", new Integer(1));
        data.put("long", new Long(1));
        data.put("string", "test");
        data.put("boolean", Boolean.TRUE);
        data.put("double", new Double(1));
        data.put("float", new Float(1));
        data.put("list", new ArrayList<Object>(0));
        data.put("set", new HashSet<Object>(0));
        data.put("array", new Object[0]);
        data.put("map", new HashMap<String, Object>());
        this.data = data;
        this.publisher = "test";
    }

}
