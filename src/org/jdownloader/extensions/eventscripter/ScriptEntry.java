package org.jdownloader.extensions.eventscripter;

import java.util.HashMap;

import org.appwork.storage.Storable;

public class ScriptEntry implements Storable {

    public ScriptEntry(/* Storable */) {

    }

    private EventTrigger eventTrigger;
    private boolean      enabled;
    private String       name;

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

    private String                  script;
    private HashMap<String, Object> eventTriggerSettings;

    public void setEventTriggerSettings(HashMap<String, Object> eventTriggerSettings) {
        this.eventTriggerSettings = eventTriggerSettings;
    }

    public EventTrigger getEventTrigger() {
        if (eventTrigger == null) {
            return EventTrigger.NONE;
        }
        return eventTrigger;
    }

    public void setEventTrigger(EventTrigger eventTrigger) {
        this.eventTrigger = eventTrigger;
    }

    public HashMap<String, Object> getEventTriggerSettings() {
        if (eventTriggerSettings == null) {
            eventTriggerSettings = new HashMap<String, Object>();
        }
        return eventTriggerSettings;
    }
}
