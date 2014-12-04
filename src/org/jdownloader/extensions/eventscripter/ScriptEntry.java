package org.jdownloader.extensions.eventscripter;

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

    private String script;

    public EventTrigger getEventTrigger() {
        if (eventTrigger == null) {
            return EventTrigger.NONE;
        }
        return eventTrigger;
    }

    public void setEventTrigger(EventTrigger eventTrigger) {
        this.eventTrigger = eventTrigger;
    }
}
