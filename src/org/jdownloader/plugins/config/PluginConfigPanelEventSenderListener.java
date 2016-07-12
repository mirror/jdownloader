package org.jdownloader.plugins.config;

import java.util.EventListener;
import java.util.HashSet;

import org.appwork.storage.config.ConfigInterface;

import jd.plugins.Plugin;
import jd.plugins.PluginConfigPanelNG;

public interface PluginConfigPanelEventSenderListener extends EventListener {
    void onConfigPanelReset(Plugin plugin, PluginConfigPanelNG pluginConfigPanelNG, HashSet<ConfigInterface> interfaces);
}