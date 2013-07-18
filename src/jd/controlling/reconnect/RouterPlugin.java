package jd.controlling.reconnect;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import jd.controlling.reconnect.ipcheck.IPCheckProvider;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.event.ProcessCallBack;

public abstract class RouterPlugin {

    private long            lastDuration;

    private IPCheckProvider ipCheckProvider = null;

    public RouterPlugin() {

    }

    //
    // final public void doReconnect() throws ReconnectException,
    // InterruptedException {
    // final long start = System.currentTimeMillis();
    // this.performReconnect();
    // this.lastDuration = System.currentTimeMillis() - start;
    // }

    abstract public ReconnectInvoker getReconnectInvoker();

    /**
     * 
     * @return Config GUI for this plugin
     */
    public abstract JComponent getGUI();

    public abstract ImageIcon getIcon16();

    /**
     * Returns a UNIQUE ID for this plugin.This ID should NEVER change
     * 
     * @return
     */
    public abstract String getID();

    public IPCheckProvider getIPCheckProvider() {
        return this.ipCheckProvider;
    }

    /**
     * returns the duration fot he latest reconnect call in ms
     * 
     * @return
     */
    public long getLastDuration() {
        return this.lastDuration;
    }

    /**
     * Returns a translated readable name of the plugin
     * 
     * @return
     */
    public abstract String getName();

    /**
     * override this method to set a special waittime. For example, if the check is local through the users router, we do not have to wait long.<br>
     * By default, this method returns the settings form advanced reconnect panel
     */
    public int getWaittimeBeforeFirstIPCheck() {
        return JsonConfig.create(ReconnectConfig.class).getSecondsBeforeFirstIPCheck();
    }

    /**
     * runs a detection wizard and
     * 
     * @param processCallBack
     * 
     * @return returns the time the reconnect took. returns -1 if the action has not been successful
     * @throws InterruptedException
     */
    public java.util.List<ReconnectResult> runDetectionWizard(ProcessCallBack processCallBack) throws InterruptedException {
        return null;
    }

    public IPCheckProvider setIPCheckProvider(final IPCheckProvider p) {
        return this.ipCheckProvider = p;

    }

    @Override
    public String toString() {
        return this.getName();
    }

    public void setSetup(ReconnectResult reconnectResult) {
    }

}
