package org.jdownloader.plugins.config;

import org.appwork.utils.event.SimpleEvent;

public abstract class PluginConfigPanelEventSenderEvent extends SimpleEvent<Object, Object, PluginConfigPanelEventSenderEvent.Type> {
    public static enum Type {
        RESET;
    }

    public PluginConfigPanelEventSenderEvent() {
        super(null, null, null);
    }

    abstract public void callListener(PluginConfigPanelEventSenderListener listener);
}