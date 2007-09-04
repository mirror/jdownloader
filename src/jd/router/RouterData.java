package jd.router;

import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
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
    public transient static final int TYPE_WEB_GET  = 2;
    public transient static final int TYPE_WEB_POST = 3;
    
    private static Logger logger = Plugin.getLogger();
    /**
     * Name des Routers
     */
    private String routerName;
    /**
     * Art des Disconnects
     */
    private int disconnectType = TYPE_WEB_GET;
    
    private int routerPort=80;
    private String routerIP="192.168.0.1";
    /**
     * Art des connects
     */
    private int connectType = TYPE_WEB_GET;
    /**
     * Art des Logins
     */
    private int loginType = TYPE_WEB_GET;
    /**
     * LoginAdresse
     */
    private String login = null;
    /**
     * String zum Aufbauen der Verbindung (meist http://www.google.de)
     * Dieser String ist eine vollständige URL
     */
    private String connect = null;
    /**
     * String zum Trennen der Verbindung
     * Dieser String zeigt NUR die page des Routers an (zB Forms/DiagADSL_1)
     * Hier muß noch http://<routerIP>/ hinzugefügt werden
     */
    private String disconnect = null;
    /**
     * Beim Post können hiermit verschiedene Parameter zum Einloggen übergeben werden
     */
    private HashMap<String, String> loginProperties = null;
    /**
     * Beim Anmelden sollen diese Parameter als Post verschickt werden
     */
    private String loginPostParams = null;
    /**
     * Beim Post können hiermit verschiedene Parameter zur Trennung übergeben werden
     */
    private HashMap<String, String> disconnectRequestProperties = null;
    /**
     * Beim Trennen sollen diese Parameter als Post verschickt werden
     * (zB DiagDSLDisconnect=PPPoE%20Trennung))
     */
    private String disconnectPostParams = null;
    /**
     * Beim Post können hiermit verschiedene Parameter zum Verbinden übergeben werden
     */
    private HashMap<String, String> connectRequestProperties = null;  
    
    /**
     * Beim Verbinden sollen diese Parameter als Post verschickt werden
     * (zB DiagDSLDisconnect=PPPoE%20Trennung))
     */
    private String                    connectPostParams = null;
    /**
     * String zum Ausloggen
     */
    private String logoff = null;
    /**
     * Die Routerseite, auf der die IP Adresse ausgelesen werden kann
     */
    private String ipAddressSite = null;
    /**
     * Dieser Text zeigt an, daß der Router offline ist (Keine IP)
     */
    private String ipAddressOffline = null;
    /**
     * RegEx zum finden der IPAdresse
     */
    private String comment=null;
    private String ipAddressRegEx;
    public HashMap<String, String> getConnectRequestProperties() { return connectRequestProperties; }
    public void setConnectRequestProperties(HashMap<String, String> connectRequestProperties) { this.connectRequestProperties = connectRequestProperties;       }

    
    
    public HashMap<String, String> getDisconnectRequestProperties() { return disconnectRequestProperties; }
    public void setDisconnectRequestProperties(HashMap<String, String> disconnectRequestProperties) { this.disconnectRequestProperties = disconnectRequestProperties;       }

    public String getConnect()                                 { return connect;              }
    public String getDisconnect()                              { return disconnect;           }
    public String getLogoff()                                  { return logoff;               }
    public String getLogin()                                   { return login;                }
    public String getRouterName()                              { return routerName;           }
    public String getIpAddressOffline()                        { return ipAddressOffline;     }
    public String getIpAddressSite()                           { return ipAddressSite;        }
    public String getDisconnectPostParams()                    { return disconnectPostParams; }
    public String getConnectPostParams()                       { return connectPostParams;    }
    public HashMap<String, String> getLoginRequestProperties() { return loginProperties;      }
    public String getLoginPostParams()                         { return loginPostParams;      }
    public int getDisconnectType()                             { return disconnectType;       }
    public int getConnectType()                                { return connectType;          }
    public int getLoginType()                                  { return loginType;            }
    public String getIpAddressRegEx()                          { return ipAddressRegEx;       }
    public void setConnect(String connect)                                         { this.connect = connect;                           }
    public void setDisconnect(String disconnect)                                   { this.disconnect = disconnect;                     }
    public void setLogoff(String logoff)                                           { this.logoff = logoff;                             }
    public void setLogin(String login)                                             { this.login = login;                               }
    public void setRouterName(String routerName)                                   { this.routerName = routerName;                     }
    public void setIpAddressOffline(String ipAddressOffline)                       { this.ipAddressOffline = ipAddressOffline;         }
    public void setIpAddressSite(String ipAddressSite)                             { this.ipAddressSite = ipAddressSite;               }
    public void setIpAddressRegEx(String ipAddressRegEx)                           { this.ipAddressRegEx = ipAddressRegEx;             }
    public void setDisconnectPostParams(String disconnectPostParams)               { this.disconnectPostParams = disconnectPostParams; }
    public void setConnectPostParams(String connectPostParams)                     { this.connectPostParams = connectPostParams;       }
    
    public void setDisconnectType(int loginType)                                   { this.disconnectType = loginType;                  }
    public void setConnectType(int loginType)                                      { this.connectType = loginType;                     }
    public void setLoginType(int loginType)                                        { this.loginType = loginType;                       }
    public void setLoginRequestProperties(HashMap<String, String> loginProperties) { this.loginProperties = loginProperties;           }
    public void setLoginPostParams(String loginPostParams)                         { this.loginPostParams = loginPostParams;           }

    /**
     * Liefert die IP Adresse aus einem Text zurück
     * 
     * @param data Der Text, der die IP-Adresse enthält
     * @return Die IP-Adresse oder null
     */
    public String getIPAdress(String data){
        logger.info(data);
        String ipAddress = null;
        if(data == null)
            return null;
        if(ipAddressOffline!=null && ipAddressOffline.length()>0&&data.contains(ipAddressOffline)){
            logger.fine("offline");
            return null;
        }
        Pattern pattern = Pattern.compile(ipAddressRegEx);
        Matcher matcher = pattern.matcher(data);
        if(matcher.find())
            ipAddress = matcher.group(1);
        return ipAddress;
    }
    public String toString(){
        return routerName;
    }
    /**
     * @return the routerIP
     */
    public String getRouterIP() {
        return routerIP;
    }
    /**
     * @param routerIP the routerIP to set
     */
    public void setRouterIP(String routerIP) {
        this.routerIP = routerIP;
    }
    /**
     * @return the routerPort
     */
    public int getRouterPort() {
        return routerPort;
    }
    /**
     * @param routerPort the routerPort to set
     */
    public void setRouterPort(int routerPort) {
        this.routerPort = routerPort;
    }
}
