package org.jdownloader.plugins.components.config;

import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "eurovision.de", type = Type.CRAWLER)
public interface EurovisionConfig extends ArdConfigInterface {
}