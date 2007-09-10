package jd;

import java.io.Serializable;
import java.util.Vector;

import jd.controlling.interaction.Interaction;
import jd.router.RouterData;

/**
 * In dieser Klasse werden die benutzerspezifischen Einstellungen festgehalten
 * 
 * @author astaldo
 */
public class Configuration extends Property implements Serializable {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID      = -2709887320616014389L;

    private boolean           useJAC                = true;

    /**
     * Hier wird das Downloadverzeichnis gespeichert
     */
    private String            downloadDirectory     = ".";

    /**
     * Die unterschiedlichen Interaktionen. (ZB Reconnect nach einem Download)
     */

    private Vector<Interaction>     interactions          = new Vector<Interaction>();

    /**
     * Hier sind die Angaben für den Router gespeichert
     */

    private RouterData        routerData            = new RouterData();

    /**
     * Benutzername für den Router
     */

    private String            routerUsername        = null;

    /**
     * Gibt an wie oft Versucht werden soll eine neue IP zu bekommen. (1&1 lässt
     * grüßen)
     */
    private int               reconnectRetries      = 0;

    /**
     * Password für den Router
     */
    private String            routerPassword        = null;

    /**
     * Fertige Downloads entfernen
     */
    private boolean           removeDownloadedFiles = true;

    /**
     * Level für das Logging
     */
    private String            loggerLevel           = "ALL";

    /**
     * Wartezeit zwischen reconnect und erstem IP Check
     */
    private int               waitForIPCheck        = 0;

    public Configuration() {
    // WebUpdate updater=new WebUpdate();
    // updater.setTrigger(Interaction.INTERACTION_APPSTART);
    // interactions.add(updater);
    }

    public String getDownloadDirectory() {
        return downloadDirectory;
    }

    // public HashMap<Integer, Vector<Interaction>> getInteractionMap() { return
    // interactionMap; }
    public String getRouterPassword() {
        return routerPassword;
    }

    public String getRouterUsername() {
        return routerUsername;
    }

    public RouterData getRouterData() {
        return routerData;
    }

    public boolean useJAC() {
        return useJAC;
    }

    public void setDownloadDirectory(String downloadDirectory) {
        this.downloadDirectory = downloadDirectory;
    }

    public void setRouterPassword(String routerPassword) {
        this.routerPassword = routerPassword;
    }

    public void setRouterUsername(String routerUsername) {
        this.routerUsername = routerUsername;
    }

    public void setRouterData(RouterData routerData) {
        this.routerData = routerData;
    }

    public void setUseJAC(boolean useJAC) {
        this.useJAC = useJAC;
    }


    public String getLoggerLevel() {
        return loggerLevel;
    }

    public void setLoggerLevel(String loggerLevel) {
        this.loggerLevel = loggerLevel;
    }

    /**
     * @return the removeDownloadedFiles
     */
    public boolean isRemoveDownloadedFiles() {
        return removeDownloadedFiles;
    }

    /**
     * @param removeDownloadedFiles the removeDownloadedFiles to set
     */
    public void setRemoveDownloadedFiles(boolean removeDownloadedFiles) {
        this.removeDownloadedFiles = removeDownloadedFiles;
    }

    /**
     * @return the reconnectRetries
     */
    public int getReconnectRetries() {
        return reconnectRetries;
    }

    /**
     * @param reconnectRetries the reconnectRetries to set
     */
    public void setReconnectRetries(int reconnectRetries) {
        this.reconnectRetries = reconnectRetries;
    }

    /**
     * Wartezeit zwischen reconnect und erstem IP Check
     * 
     * @return
     */
    public int getWaitForIPCheck() {
        return waitForIPCheck;
    }

    public void setWaitForIPCheck(int waitForIPCheck) {
        this.waitForIPCheck = waitForIPCheck;
    }

    /**
     * Gibt die Interactionen zurück
     * 
     * @return
     */

    public Vector<Interaction> getInteractions() {
        return interactions;
    }

    /**
     * Setzt die INteractionen
     * 
     * @param interactions
     */
    public void setInteractions(Vector<Interaction> interactions) {
        this.interactions = interactions;
    }

}
