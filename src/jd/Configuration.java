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
    /**
     * IP des Routers
     */
    private String routerIP;
    /**
     * Router Port
     */
    private int routerPort;
    
    public String getDownloadDirectory()                           { return downloadDirectory;  }
    public HashMap<Integer, Vector<Interaction>> getInteractions() { return interactions;       }
    public String getRouterPassword()                              { return routerPassword;     }
    public String getRouterUsername()                              { return routerUsername;     }
    public RouterData getRouterData()                              { return routerData;         }
    public String getRouterIP()                                    { return routerIP;           }
    public int getRouterPort()                                     { return routerPort;             }
    public void setDownloadDirectory(String downloadDirectory) { this.downloadDirectory = downloadDirectory; }
    public void setRouterPassword(String routerPassword)       { this.routerPassword = routerPassword;       }
    public void setRouterUsername(String routerUsername)       { this.routerUsername = routerUsername;       }
    public void setRouterData(RouterData routerData)           { this.routerData = routerData;               }
    public void setRouterIP(String routerIP)                   { this.routerIP = routerIP;                   }
    public void setRouterPort(int httpPort)                    { this.routerPort = httpPort;                 }
}
