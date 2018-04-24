package org.jdownloader.extensions.eventscripter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.appwork.storage.Storable;
import org.jdownloader.controlling.UniqueAlltimeID;

public class ScriptEntry implements Storable {
    public ScriptEntry(/* Storable */) {
    }

    private EventTrigger          eventTrigger;
    private boolean               enabled;
    private String                name;
    private final UniqueAlltimeID uniqueAlltimeID = new UniqueAlltimeID();

    public long getID() {
        return uniqueAlltimeID.getID();
    }

    public void setID(long uniqueAlltimeID) {
        this.uniqueAlltimeID.setID(uniqueAlltimeID);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    private String              script;
    private Map<String, Object> eventTriggerSettings;

    public synchronized void setEventTriggerSettings(Map<String, Object> eventTriggerSettings) {
        if (eventTriggerSettings != null && !(eventTriggerSettings instanceof ConcurrentHashMap)) {
            this.eventTriggerSettings = new ConcurrentHashMap<String, Object>(eventTriggerSettings);
        } else {
            this.eventTriggerSettings = eventTriggerSettings;
        }
    }

    public EventTrigger getEventTrigger() {
        final EventTrigger eventTrigger = this.eventTrigger;
        if (eventTrigger == null) {
            return EventTrigger.NONE;
        } else {
            return eventTrigger;
        }
    }

    public void setEventTrigger(EventTrigger eventTrigger) {
        this.eventTrigger = eventTrigger;
    }

    public synchronized Map<String, Object> getEventTriggerSettings() {
        if (eventTriggerSettings == null) {
            eventTriggerSettings = new ConcurrentHashMap<String, Object>();
        }
        return eventTriggerSettings;
    }
}
