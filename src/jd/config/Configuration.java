package jd.config;

import java.io.Serializable;
import java.util.Vector;

import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.InteractionTrigger;
import jd.router.RouterData;

/**
 * In dieser Klasse werden die benutzerspezifischen Einstellungen festgehalten
 * 
 * @author astaldo
 */
public class Configuration extends Property implements Serializable {
    /**
     * Gibt an ob die SerializeFunktionen im XMl MOdus Arbeiten oder nocht
     */
    public transient static boolean saveAsXML                          = false;

    /**
     * serialVersionUID
     */
    private static final long       serialVersionUID                   = -2709887320616014389L;

    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden
     * soll
     */
    public static final String      PARAM_DOWNLOAD_READ_TIMEOUT        = "DOWNLOAD_READ_TIMEOUT";

    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden
     * soll
     */
    public static final String      PARAM_DOWNLOAD_CONNECT_TIMEOUT     = "DOWNLOAD_CONNECT_TIMEOUT";

    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden
     * soll
     */
    public static final String      PARAM_DOWNLOAD_MAX_SIMULTAN        = "DOWNLOAD_MAX_SIMULTAN";

    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden
     * soll
     */
    public static final String      PARAM_LOGGER_LEVEL                 = "LOGGER_LEVEL";

    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden
     * soll
     */
    public static final String      PARAM_HOME_DIRECTORY               = "HOME_DIRECTORY";

    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden
     * soll
     */
    public static final String      PARAM_DOWNLOAD_DIRECTORY           = "DOWNLOAD_DIRECTORY";

    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden
     * soll
     */
    public static final String      PARAM_FINISHED_DOWNLOADS_ACTION    = "FINISHED_DOWNLOADS_ACTION";



    /**
     * String ID um einen fertiggestellten download beim programmstart aus der
     * queue zu entfernen
     */
    public static final String      FINISHED_DOWNLOADS_REMOVE_AT_START = "beim Programstart entfernen";

    /**
     * String ID um einen fertiggestellten download nimcht zu entfernen
     */
    public static final String      FINISHED_DOWNLOADS_NO_REMOVE       = "nicht entfernen";

    /**
     * String ID um einen fertiggestellten download sofort zu entfernen
     */
    public static final String      FINISHED_DOWNLOADS_REMOVE          = "sofort entfernen";

    /**
     * Parameter für den key unter dem der zugehöroge Wert abgespeichert werden
     * soll
     */
    public static final String      PARAM_DISABLE_RECONNECT            = "DISABLE_RECONNECT";

    public static final String      PARAM_HTTPSEND_USER                = "HTTPSEND_USER";

    public static final String      PARAM_HTTPSEND_PASS                = "HTTPSEND_PASS";

    public static final String      PARAM_HTTPSEND_REQUESTS            = "HTTPSEND_REQUESTS";

    public static final String      PARAM_HTTPSEND_IPCHECKWAITTIME     = "HTTPSEND_IPCHECKWAITTIME";

    public static final String      PARAM_HTTPSEND_RETRIES             = "HTTPSEND_RETRIES";

    public static final String      PARAM_HTTPSEND_WAITFORIPCHANGE     = "HTTPSEND_WAITFORIPCHANGE";

 

    public static final String      PARAM_NO_TRAY                      = "NO_TRAY";

    public static final String      PARAM_MIN_FREE_SPACE               = "MIN_FREE_SPACE";

    public static final String      PARAM_START_DOWNLOADS_AFTER_START  = "START_DOWNLOADS_AFTER_START";

    public static final String      PARAM_USE_PACKETNAME_AS_SUBFOLDER  = "USE_PACKETNAME_AS_SUBFOLDER";

    public static final String      PARAM_GLOBAL_IP_CHECK_SITE         = "GLOBAL_IP_CHECK_SITE";

    public static final String      PARAM_GLOBAL_IP_PATTERN            = "GLOBAL_IP_PATTERN";



    public static final String      PARAM_HTTPSEND_IP                  = "HTTPSEND_IP";

    public static final String      PARAM_CURRENT_BROWSE_PATH          = "CURRENT_BROWSE_PATH";

    public static final String      PARAM_CLIPBOARD_ALWAYS_ACTIVE      = "CLIPBOARD_ALWAYS_ACTIVE";

   

    public static final String      PARAM_WEBUPDATE_LOAD_ALL_TOOLS     = "WEBUPDATE_LOAD_ALL_TOOLS";

    public static final String      PARAM_WEBUPDATE_AUTO_RESTART       = "WEBUPDATE_AUTO_RESTART";

    public static final String      PARAM_UPDATE_HASH                  = "UPDATE_HASH";

    public static final String      PARAM_WRITE_LOG                    = "WRITE_LOG";

    public static final String      PARAM_WRITE_LOG_PATH               = "WRITE_LOG_PATH";

  

  



    public static final String      PARAM_RECONNECT_TYPE               = "RECONNECT_TYPE";

    public static final String PARAM_UNRAR_INSTANCE = "UNRAR_INSTANCE";

    public static final String PARAM_JAC_METHODS = "JAC_METHODS";

    public static final String PARAM_FILEWRITER_INSTANCE = "FILEWRITER_INSTANCE";



    

    public static final String PARAM_GLOBAL_IP_DISABLE = "GLOBAL_IP_DISABLE";

   



    public static final String PARAM_HOST_PRIORITY = "HOST_PRIORITY";
    public static final String PARAM_CAPTCHA_JAC_DISABLE = "CAPTCHA_JAC_DISBALE";
    public static final String PARAM_CAPTCHA_INPUT_SHOWTIME = "CAPTCHA_INPUT_SHOWTIME";
    /**
     * Die unterschiedlichen Interaktionen. (ZB Reconnect nach einem Download)
     */
    private Vector<Interaction>     interactions                       = new Vector<Interaction>();

    /**
     * Hier sind die Angaben für den Router gespeichert
     */
    private RouterData              routerData                         = new RouterData();

    /**
     * Benutzername für den Router
     */
    private String                  routerUsername                     = null;

    /**
     * Gibt an wie oft Versucht werden soll eine neue IP zu bekommen. (1&1 lässt
     * grüßen)
     */
    private int                     reconnectRetries                   = 0;

    /**
     * Password für den Router
     */
    private String                  routerPassword                     = null;

    /**
     * Wartezeit zwischen reconnect und erstem IP Check
     */
    private int                     waitForIPCheck                     = 0;

    private String                  version                            = "";

    /**
     * Konstruktor für ein Configuration Object
     */
    public Configuration() {
    // WebUpdate updater=new WebUpdate();
    // updater.setTrigger(Interaction.INTERACTION_APPSTART);
    // interactions.add(updater);
    }

    // public HashMap<Integer, Vector<Interaction>> getInteractionMap() { return
    // interactionMap; }
    /**
     * @return Gibt das Routeradmin Passwort zurück
     */
    public String getRouterPassword() {
        return routerPassword;
    }

    /**
     * @return gibt den router-admin-Username zurück
     */
    public String getRouterUsername() {
        return routerUsername;
    }

    /**
     * GIbt das routerdata objekt zurück. darin sind alle informationen
     * gespeichert die aus der routerdata.xml importiert worden sind. (für einen
     * router)
     * 
     * @return Gibt das routerdata objekt zurück
     */
    public RouterData getRouterData() {
        return routerData;
    }

    /**
     * @param routerPassword
     */
    public void setRouterPassword(String routerPassword) {
        this.routerPassword = routerPassword;
    }

    /**
     * @param routerUsername
     */
    public void setRouterUsername(String routerUsername) {
        this.routerUsername = routerUsername;
    }

    /**
     * @param routerData
     */
    public void setRouterData(RouterData routerData) {
        this.routerData = routerData;
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
     * @return Wartezeit zwischen reconnect und ip-check
     */
    public int getWaitForIPCheck() {
        return waitForIPCheck;
    }

    /**
     * Setztd ie Wartezeit zwischen dem Reconnect und dem ersten IP-Check
     * 
     * @param waitForIPCheck
     */
    public void setWaitForIPCheck(int waitForIPCheck) {
        this.waitForIPCheck = waitForIPCheck;
    }

    /**
     * Gibt die Interactionen zurück. Alle eingestellten Interactionen werden
     * hier in einem vector zurückgegeben
     * 
     * @return Vector<Interaction>
     */
    public Vector<Interaction> getInteractions() {
        return interactions;
    }

    /**
     * Setzt die Interactionen
     * 
     * @param interactions
     */
    public void setInteractions(Vector<Interaction> interactions) {
        this.interactions = interactions;
    }

    /**
     * Gibt alle Interactionen zurück bei denen die TRigger übereinstimmen. z.B.
     * alle reconnect Aktionen
     * 
     * @param it
     * @return Alle interactionen mit dem TRigger it
     */
    public Vector<Interaction> getInteractions(InteractionTrigger it) {
        Vector<Interaction> ret = new Vector<Interaction>();
        for (int i = 0; i < interactions.size(); i++) {
            if (interactions.elementAt(i).getTrigger().getID() == it.getID()) ret.add(interactions.elementAt(i));
        }
        return ret;
    }

    /**
     * Gibt alle Interactionen zurück bei der die AKtion inter gleicht
     * 
     * @param inter
     * @return Alle interactionen mit dem Selben interaction-Event wie inter
     */
    public Vector<Interaction> getInteractions(Interaction inter) {
        Vector<Interaction> ret = new Vector<Interaction>();
        for (int i = 0; i < interactions.size(); i++) {
            if (inter.getInteractionName().equals(interactions.elementAt(i).getInteractionName())) ret.add(interactions.elementAt(i));
        }
        return ret;
    }

    /**
     * Setzt die Version der Configfile
     * 
     * @param version
     */
    public void setConfigurationVersion(String version) {
        this.version = version;
    }

    /**
     * Gibt die version der Configfile zurück. Ändert sich die Konfigversion,
     * werden die defaulteinstellungen erneut geschrieben. So wird
     * sichergestellt, dass bei einem Update eine Aktuelle Configfie erstellt
     * wird
     * 
     * @return Versionsstring der Konfiguration
     */
    public String getConfigurationVersion() {
        if (version == null) return "0.0.0";
        return version;
    }

    /**
     * Legt die defaulteinstellungen in das configobjekt
     */
    public void setDefaultValues() {

   
  

        if (getProperty("maxSimultanDownloads") == null || ((Integer) getProperty("maxSimultanDownloads")) == 0) {
            setProperty("maxSimultanDownloads", 3);
        }
        if (getProperty("maxSimultanDownloads") == null || ((Integer) getProperty("maxSimultanDownloads")) == 0) {
            setProperty("maxSimultanDownloads", 3);
        }

        String id = "$Id$";
        if (id.length() > 22) {
            setConfigurationVersion(id.substring(22, id.length() - 2));
        }
    }

    /**
     * GIbt alle Properties der Config aus
     * 
     * @return toString
     */
    public String toString() {
        return "Configuration " + this.getProperties() + " Interaction " + this.interactions;
    }

    /**
     * Gibt den Wert zu key zurück. falls dieser Wert == null ist wird der
     * defaultValue zurückgegeben
     * 
     * @param key
     * @param defaultValue
     * @return Wert zu key oder defaultValue
     */
    public Object getProperty(String key, Object defaultValue) {
        if (getProperty(key) == null) return defaultValue;
        return getProperty(key);
    }
}
