package jd.controlling.reconnect;

import java.util.logging.Logger;

import javax.swing.JComponent;

import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;

import com.sun.istack.internal.Nullable;

public abstract class RouterPlugin {

    protected static final Logger LOG           = JDLogger.getLogger();

    private final Storage         storage;
    /**
     * Constant that has to be returned by {@link #getExternalIP()} if there is
     * no internet connection
     */
    public static final String    OFFLINE       = "offline";

    /**
     * constant that has to be returned by {@link #getExternalIP()} if a ip
     * chekc is temp. not available
     */
    public static final String    NOT_AVAILABLE = "na";

    public RouterPlugin() {
        this.storage = JSonStorage.getPlainStorage(this.getID());
    }

    /**
     * performs all reconnect actions
     * 
     * @param progress
     * @return
     */
    public abstract void doReconnect() throws ReconnectException;

    /**
     * returns the external IP
     * 
     * @return {@link #OFFLINE} or {@value #NOT_AVAILABLE} a valid IP the ip
     *         adress
     * @throws GetIpException
     *             if this plugin cannot get a valid IP response
     */
    @Nullable
    public abstract String getExternalIP() throws GetIpException;

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
     * Returns the IPCheck interval. Override this method for routerplugins that
     * check ip locally, and may allow a shorter interval for a faster
     * reconnection
     * 
     * @return
     */
    public int getIpCheckInterval() {
        // TODO Auto-generated method stub
        return 5;
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
     * returns true, if this router implementation is able to check the external
     * IP
     * 
     * @return
     */
    public abstract boolean isIPCheckEnabled();

    /**
     * returns true, if this router implementation is able to perform a
     * reconnect
     * 
     * @return
     */
    public abstract boolean isReconnectionEnabled();

    public abstract void setCanCheckIP(boolean b);

    public String toString() {
        return this.getName();
    }
}
