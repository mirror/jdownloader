package jd.router;

import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.plugins.Plugin;
/**
 * Hier werden die Daten für einen Router gespeichert
 * 
 * @author astaldo
 */
public class RouterData implements Serializable{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 5321872876497404319L;

    public transient static final int LOGIN_TYPE_AUTH     = 1;
    public transient static final int LOGIN_TYPE_WEB_GET  = 2;
    public transient static final int LOGIN_TYPE_WEB_POST = 3;
    
    private static Logger logger = Plugin.getLogger();
    /**
     * Name des Routers
     */
    private String routerName;
    /**
     * Art des Logins (bisher immer WEB)
     */
    private int loginType = LOGIN_TYPE_WEB_GET;
    /**
     * LoginAdresse
     */
    private String loginString;
    /**
     * String zum Aufbauen der Verbindung (meist http://www.google.de)
     * Dieser String ist eine vollständige URL
     */
    private String connectionConnect;
    /**
     * String zum Trennen der Verbindung
     * Dieser String zeigt NUR die page des Routers an (zB Forms/DiagADSL_1)
     * Hier muß noch http://<routerIP>/ hinzugefügt werden
     */
    private String connectionDisconnect;
    /**
     * Beim Post können hiermit verschiedene Parameter übergeben werden
     */
    private HashMap<String, String> disconnectrequestProperties;
    /**
     * Beim Trennen sollen diese Parameter als Post verschickt werden
     * (zB DiagDSLDisconnect=PPPoE%20Trennung))
     */
    private String disconnectPostParams;
    /**
     * String zum Ausloggen
     */
    private String connectionLogoff;
    /**
     * Die Routerseite, auf der die IP Adresse ausgelesen werden kann
     */
    private String ipAddressSite;
    /**
     * Dieser Text zeigt an, daß der Router offline ist (Keine IP)
     */
    private String ipAddressOffline;
    /**
     * Dieser Text befindet sich unmittelbar vor der IP-Adresse
     */
    private String ipAddressPre;
    /**
     * Dieser Text befindet sich unmittelbar nach der IP-Adresse
     */
    private String ipAddressPost;
    /**
     * TODO RegEx wird nicht genutzt
     */
    private Pattern ipAddressRegEx;
    public HashMap<String, String> getDisconnectRequestProperties() { return disconnectrequestProperties; }
    public void setDisconnectRequestProperties(HashMap<String, String> disconnectRequestProperties) { this.disconnectrequestProperties = disconnectRequestProperties;       }

    public String getConnectionConnect()                  { return connectionConnect;    }
    public String getConnectionDisconnect()               { return connectionDisconnect; }
    public String getConnectionLogoff()                   { return connectionLogoff;     }
    public String getLoginString()                        { return loginString;          }
    public String getRouterName()                         { return routerName;           }
    public String getIpAddressPost()                      { return ipAddressPost;        }
    public String getIpAddressPre()                       { return ipAddressPre;         }
    public String getIpAddressOffline()                   { return ipAddressOffline;     }
    public String getIpAddressSite()                      { return ipAddressSite;        }
    public String getDisconnectPostParams()               { return disconnectPostParams; }
    public int getLoginType()                             { return loginType;            }
    public Pattern getIpAddressRegEx()                    { return ipAddressRegEx;       }
    public void setConnectionConnect(String connectionConnect)       { this.connectionConnect = connectionConnect;       }
    public void setConnectionDisconnect(String connectionDisconnect) { this.connectionDisconnect = connectionDisconnect; }
    public void setConnectionLogoff(String connectionLogoff)         { this.connectionLogoff = connectionLogoff;         }
    public void setLoginString(String loginString)                   { this.loginString = loginString;                   }
    public void setLoginType(int loginType)                          { this.loginType = loginType;                       }
    public void setRouterName(String routerName)                     { this.routerName = routerName;                     }
    public void setIpAddressPost(String ipAddressPost)               { this.ipAddressPost = ipAddressPost;               }
    public void setIpAddressPre(String ipAddressPre)                 { this.ipAddressPre = ipAddressPre;                 }
    public void setIpAddressOffline(String ipAddressOffline)         { this.ipAddressOffline = ipAddressOffline;         }
    public void setIpAddressSite(String ipAddressSite)               { this.ipAddressSite = ipAddressSite;               }
    public void setIpAddressRegEx(Pattern ipAddressRegEx)            { this.ipAddressRegEx = ipAddressRegEx;             }
    public void setDisconnectPostParams(String disconnectPostParams) { this.disconnectPostParams = disconnectPostParams; }

    /**
     * Liefert die IP Adresse aus einem Text zurück
     * 
     * @param data Der Text, der die IP-Adresse enthält
     * @return Die IP-Adresse oder null
     */
    public String getIPAdress(String data){
        String ipAddress = null;
        if(data == null)
            return null;
        if(data.contains(ipAddressOffline)){
            logger.fine("offline");
            return null;
        }
        if(ipAddressRegEx != null){
            //RegExCheck
        }
        else{
            int index1 = data.indexOf(ipAddressPre)+ipAddressPre.length();
            int index2 = data.indexOf(ipAddressPost);
            if(index1!=-1 && index2 != -1 && index2-index1>0){
                ipAddress = data.substring(index1, index2);
            }
        }
        return ipAddress;
    }
    public String toString(){
        return "Router:"+routerName;
    }
}
