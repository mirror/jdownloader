package org.jdownloader.plugins.config;

import java.util.EventListener;
import java.util.Set;

import jd.plugins.Plugin;
import jd.plugins.PluginConfigPanelNG;

import org.appwork.storage.config.ConfigInterface;

public interface PluginConfigPanelEventSenderListener extends EventListener {
    void onConfigPanelReset(Plugin plugin, PluginConfigPanelNG pluginConfigPanelNG, Set<ConfigInterface> interfaces);
}