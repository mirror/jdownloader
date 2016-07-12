package org.jdownloader.plugins.config;

import org.appwork.utils.event.Eventsender;

public class PluginConfigPanelEventSenderEventSender extends Eventsender<PluginConfigPanelEventSenderListener, PluginConfigPanelEventSenderEvent> {
    @Override
    protected void fireEvent(PluginConfigPanelEventSenderListener listener, PluginConfigPanelEventSenderEvent event) {
        event.callListener(listener);
    }
}