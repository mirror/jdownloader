package jd.plugins.event;

/**
 * An diesen Listener werden die PluginEvents verteilt
 *
 * @author astaldo
 */
public interface PluginListener {
    public abstract void pluginEvent(PluginEvent event);
}
