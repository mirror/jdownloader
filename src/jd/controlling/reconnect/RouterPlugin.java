package jd.controlling.reconnect;

import java.util.logging.Logger;

import javax.swing.JComponent;

import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.ipcheck.IPCheckProvider;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;

public abstract class RouterPlugin {

    protected static final Logger LOG             = JDLogger.getLogger();

    private final Storage         storage;

    private long                  lastDuration;

    private IPCheckProvider       ipCheckProvider = null;
    /**
     * Constant that has to be returned by {@link #getExternalIP()} if there is
     * no internet connection
     */
    public static final String    OFFLINE         = "offline";

    /**
     * constant that has to be returned by {@link #getExternalIP()} if a ip
     * chekc is temp. not available
     */
    public static final String    NOT_AVAILABLE   = "na";

    public RouterPlugin() {
        this.storage = JSonStorage.getPlainStorage(this.getID());
    }

    /**
     * Finds all settings for a complete reconnect without user interaction
     * 
     * @return
     */
    public int autoDetection() {
        return -1;
    }

    final public void doReconnect() throws ReconnectException, InterruptedException {
        final long start = System.currentTimeMillis();
        this.performReconnect();
        this.lastDuration = System.currentTimeMillis() - start;
    }

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
     * Returns the storage INstance for this plugin
     * 
     * @return
     */
    public Storage getStorage() {
        return this.storage;
    }

    /**
     * override this method to set a special waittime. For example, if the check
     * is local through the users router, we do not have to wait long.<br>
     * By default, this method returns the settings form advanced reconnect
     * panel
     */
    public int getWaittimeBeforeFirstIPCheck() {
        return JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_IPCHECKWAITTIME, 5);
    }

    /**
     * Returns if this plugin might be able to find a valid setup without any
     * userinteraction
     * 
     * @return
     */
    public boolean hasAutoDetection() {
        return false;
    }

    /**
     * If the plugin has an interactive Settingsdetection wizard, this method
     * has to return true;
     * 
     * @return
     */
    public boolean hasDetectionWizard() {
        return false;
    }

    /**
     * returns true, if this router implementation is able to perform a
     * reconnect
     * 
     * @return
     */
    public abstract boolean isReconnectionEnabled();

    /**
     * performs all reconnect actions
     * 
     * @param progress
     * @return
     */
    protected abstract void performReconnect() throws ReconnectException, InterruptedException;

    /**
     * runs a detection wizard and
     * 
     * @return returns the time the reconnect took. returns -1 if the action has
     *         not been successfull
     * @throws InterruptedException
     */
    public int runDetectionWizard() throws InterruptedException {
        return -1;
    }

    public IPCheckProvider setIPCheckProvider(final IPCheckProvider p) {
        return this.ipCheckProvider = p;

    }

    @Override
    public String toString() {
        return this.getName();
    }

}
