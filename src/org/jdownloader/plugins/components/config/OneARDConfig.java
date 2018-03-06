package org.jdownloader.plugins.components.config;

import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "one.ard.de", type = Type.CRAWLER)
public interface OneARDConfig extends ArdConfigInterface {
}