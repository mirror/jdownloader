package jd.plugins.optional.jdpremclient;

import jd.plugins.PluginForHost;

public interface JDPremInterface {

    /* set the plugin we replaced */
    public void setReplacedPlugin(PluginForHost plugin);

    /* once the override is enabled, you cannot disable it until next restart */
    public void enablePlugin();

}
