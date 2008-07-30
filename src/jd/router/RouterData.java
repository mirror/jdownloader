//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.router;

import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.utils.JDUtilities;
/**
 * Hier werden die Daten für einen Router gespeichert
 * 
 * @author astaldo
 */
public class RouterData implements Serializable{
    private static Logger logger = JDUtilities.getLogger();

    public transient static final int LOGIN_TYPE_AUTH     = 1;
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 5321872876497404319L;
    public transient static final int TYPE_WEB_GET  = 2;
    
    public transient static final int TYPE_WEB_POST = 3;
    /**
     * String zum Aufbauen der Verbindung (meist http://www.google.de)
     * Dieser String ist eine vollständige URL
     */
    private String connect = null;
    /**
     * Beim Verbinden sollen diese Parameter als Post verschickt werden
     * (zB DiagDSLDisconnect=PPPoE%20Trennung))
     */
    private String                    connectPostParams = null;
    
    /**
     * Beim Post können hiermit verschiedene Parameter zum Verbinden übergeben werden
     */
    private HashMap<String, String> connectRequestProperties = null;
    /**
     * Art des connects
     */
    private int connectType = TYPE_WEB_GET;
    /**
     * String zum Trennen der Verbindung
     * Dieser String zeigt NUR die page des Routers an (zB Forms/DiagADSL_1)
     * Hier muß noch http://<routerIP>/ hinzugefügt werden
     */
    private String disconnect = null;
    /**
     * Beim Trennen sollen diese Parameter als Post verschickt werden
     * (zB DiagDSLDisconnect=PPPoE%20Trennung))
     */
    private String disconnectPostParams = null;
    /**
     * Beim Post können hiermit verschiedene Parameter zur Trennung übergeben werden
     */
    private HashMap<String, String> disconnectRequestProperties = null;
    /**
     * Art des Disconnects
     */
    private int disconnectType = TYPE_WEB_GET;
    /**
     * Dieser Text zeigt an, daß der Router offline ist (Keine IP)
     */
    private String ipAddressOffline = null;
    /**
     * RegEx zum finden der IPAdresse
     */
    
    private String ipAddressRegEx;
    /**
     * Die Routerseite, auf der die IP Adresse ausgelesen werden kann
     */
    private String ipAddressSite = null;
    /**
     * LoginAdresse
     */
    private String login = null;
    /**
     * Beim Anmelden sollen diese Parameter als Post verschickt werden
     */
    private String loginPostParams = null;
    /**
     * Beim Post können hiermit verschiedene Parameter zum Einloggen übergeben werden
     */
    private HashMap<String, String> loginProperties = null;  
    
    /**
     * Art des Logins
     */
    private int loginType = TYPE_WEB_GET;
    /**
     * String zum Ausloggen
     */
    private String logoff = null;
    private String routerIP="192.168.0.1";
    /**
     * Name des Routers
     */
    private String routerName;
    private int routerPort=80;
    public String getConnect()                                 { return connect;              }
    public String getConnectPostParams()                       { return connectPostParams;    }

    
    
    public HashMap<String, String> getConnectRequestProperties() { return connectRequestProperties; }
    public int getConnectType()                                { return connectType;          }

    public String getDisconnect()                              { return disconnect;           }
    public String getDisconnectPostParams()                    { return disconnectPostParams; }
    public HashMap<String, String> getDisconnectRequestProperties() { return disconnectRequestProperties; }
    public int getDisconnectType()                             { return disconnectType;       }
    public String getIpAddressOffline()                        { return ipAddressOffline;     }
    public String getIpAddressRegEx()                          { return ipAddressRegEx;       }
    public String getIpAddressSite()                           { return ipAddressSite;        }
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
    public String getLogin()                                   { return login;                }
    public String getLoginPostParams()                         { return loginPostParams;      }
    public HashMap<String, String> getLoginRequestProperties() { return loginProperties;      }
    public int getLoginType()                                  { return loginType;            }
    public String getLogoff()                                  { return logoff;               }
    /**
     * @return the routerIP
     */
    public String getRouterIP() {
        return routerIP;
    }
    public String getRouterName()                              { return routerName;           }
    /**
     * @return the routerPort
     */
    public int getRouterPort() {
        return routerPort;
    }
    public void setConnect(String connect)                                         { this.connect = connect;                           }
    public void setConnectPostParams(String connectPostParams)                     { this.connectPostParams = connectPostParams;       }
    public void setConnectRequestProperties(HashMap<String, String> connectRequestProperties) { this.connectRequestProperties = connectRequestProperties;       }
    public void setConnectType(int loginType)                                      { this.connectType = loginType;                     }
    public void setDisconnect(String disconnect)                                   { this.disconnect = disconnect;                     }
    public void setDisconnectPostParams(String disconnectPostParams)               { this.disconnectPostParams = disconnectPostParams; }
    public void setDisconnectRequestProperties(HashMap<String, String> disconnectRequestProperties) { this.disconnectRequestProperties = disconnectRequestProperties;       }
    public void setDisconnectType(int loginType)                                   { this.disconnectType = loginType;                  }
    public void setIpAddressOffline(String ipAddressOffline)                       { this.ipAddressOffline = ipAddressOffline;         }
    
    public void setIpAddressRegEx(String ipAddressRegEx)                           { this.ipAddressRegEx = ipAddressRegEx;             }
    public void setIpAddressSite(String ipAddressSite)                             { this.ipAddressSite = ipAddressSite;               }
    public void setLogin(String login)                                             { this.login = login;                               }
    public void setLoginPostParams(String loginPostParams)                         { this.loginPostParams = loginPostParams;           }
    public void setLoginRequestProperties(HashMap<String, String> loginProperties) { this.loginProperties = loginProperties;           }

    public void setLoginType(int loginType)                                        { this.loginType = loginType;                       }
    public void setLogoff(String logoff)                                           { this.logoff = logoff;                             }
    /**
     * @param routerIP the routerIP to set
     */
    public void setRouterIP(String routerIP) {
        this.routerIP = routerIP;
    }
    public void setRouterName(String routerName)                                   { this.routerName = routerName;                     }
    /**
     * @param routerPort the routerPort to set
     */
    public void setRouterPort(int routerPort) {
        this.routerPort = routerPort;
    }
    @Override
    public String toString(){
        return routerName;
    }
}
