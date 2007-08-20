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
    private String downloadDirectory=".";
    /**
     * Die unterschiedlichen Interaktionen.
     * (ZB Reconnect nach einem Download)
     */
    private HashMap<Integer, Vector<Interaction>> interactions = new HashMap<Integer, Vector<Interaction>>();
    /**
     * Hier sind die Angaben für den Router gespeichert
     */
    private RouterData routerData;
    /**
     * Benutzername für den Router
     */
    private String routerUsername;
    /**
     * Password für den Router
     */
    private String routerPassword;
    
    public String getDownloadDirectory() {
        return downloadDirectory;
    }
    public void setDownloadDirectory(String downloadDirectory) {
        this.downloadDirectory = downloadDirectory;
    }
    public HashMap<Integer, Vector<Interaction>> getInteractions() {
        return interactions;
    }
    public RouterData getRouterData() { return routerData;     }
    public String getRouterPassword() { return routerPassword; }
    public String getRouterUsername() { return routerUsername; }
}
