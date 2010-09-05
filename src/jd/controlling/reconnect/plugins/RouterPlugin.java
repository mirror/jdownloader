package jd.controlling.reconnect.plugins;

import javax.swing.JComponent;

import jd.controlling.ProgressController;

import com.sun.istack.internal.Nullable;

public abstract class RouterPlugin {

    /**
     * returns true, if this router implementation is able to check the external
     * IP
     * 
     * @return
     */
    public abstract boolean canCheckIP();

    /**
     * returns true, if this router implementation is able to perform a
     * reconnect
     * 
     * @return
     */
    public abstract boolean canRefreshIP();

    /**
     * performs all reconnect actions
     * 
     * @param progress
     * @return
     */
    public abstract boolean doReconnect(ProgressController progress);

    /**
     * returns the external IP
     * 
     * @return
     * @throws RouterPluginException
     */
    @Nullable
    public abstract String getExternalIP() throws RouterPluginException;

    /**
     * 
     * @return Config GUI for this plugin
     */
    public abstract JComponent getGUI();

    /**
     * Returns a UNIQUE ID for this plugin.This ID should NEVER change
     * 
     * @return
     */
    public abstract String getID();

    /**
     * Returns a translated readable name of the plugin
     * 
     * @return
     */
    public abstract String getName();

    public String toString() {
        return this.getName();
    }
}
