package jd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Vector;

import jd.controlling.interaction.Interaction;
import jd.router.RouterData;
/**
 * In dieser Klasse werden die benutzerspezifischen Einstellungen festgehalten
 * 
 * @author astaldo
 */
public class Configuration implements Serializable{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -2709887320616014389L;
    /**
     * Hier wird das Downloadverzeichnis gespeichert
     */
    private static String downloadDirectory;
    /**
     * Die unterschiedlichen Interaktionen.
     * (ZB Reconnect nach einem Download)
     */
    private static HashMap<Integer, Vector<Interaction>> interactions = new HashMap<Integer, Vector<Interaction>>();
    /**
     * Hier sind die Angaben für den Router gespeichert
     */
    private static RouterData routerData;
    /**
     * Benutzername für den Router
     */
    private static String routerUsername;
    /**
     * Password für den Router
     */
    private static String routerPassword;
    
    public static String getDownloadDirectory() {
        return downloadDirectory;
    }
    public static void setDownloadDirectory(String downloadDirectory) {
        Configuration.downloadDirectory = downloadDirectory;
    }
    public static HashMap<Integer, Vector<Interaction>> getInteractions() {
        return interactions;
    }
    public static RouterData getRouterData() { return routerData;     }
    public static String getRouterPassword() { return routerPassword; }
    public static String getRouterUsername() { return routerUsername; }
}
