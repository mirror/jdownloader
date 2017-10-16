package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.appwork.remoteapi.events.EventObject;
import org.appwork.remoteapi.events.EventPublisher;
import org.appwork.remoteapi.events.RemoteAPIEventsSender;
import org.appwork.storage.JSonStorage;
import org.jdownloader.extensions.eventscripter.ScriptAPI;

@ScriptAPI(description = "The Event Object")
public class EventSandbox {
    private final EventObject event;

    public String getId() {
        if (event != null) {
            return event.getEventid();
        } else {
            return null;
        }
    }

    public String getData() {
        final Object data = event != null ? event.getEventdata() : null;
        if (data == null) {
            return null;
        } else {
            return JSonStorage.toString(data);
        }
    }

    public String getPublisher() {
        if (event != null) {
            return event.getPublisher().getPublisherName();
        } else {
            return null;
        }
    }

    public EventSandbox(EventObject event) {
        this.event = event;
    }

    @Override
    public int hashCode() {
        if (event != null) {
            return event.hashCode();
        } else {
            return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EventSandbox) {
            return ((EventSandbox) obj).event == event;
        } else {
            return super.equals(obj);
        }
    }

    public EventSandbox() {
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
        final EventPublisher publisher = new EventPublisher() {
            @Override
            public void unregister(RemoteAPIEventsSender eventsAPI) {
            }

            @Override
            public void register(RemoteAPIEventsSender eventsAPI) {
            }

            @Override
            public String getPublisherName() {
                return "test";
            }

            @Override
            public String[] getPublisherEventIDs() {
                return null;
            }
        };
        this.event = new EventObject() {
            @Override
            public EventPublisher getPublisher() {
                return publisher;
            }

            @Override
            public String getEventid() {
                return "test";
            }

            @Override
            public Object getEventdata() {
                return data;
            }

            @Override
            public String getCollapseKey() {
                return null;
            }
        };
    }
}
