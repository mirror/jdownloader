package jd.router;

import java.io.Serializable;
import java.util.logging.Logger;

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
    public transient static final String HTTP_POST = "<POST>";
    public transient static final String HTTP_GET  = "<GET>";
    
    public transient static final int LOGIN_TYPE_AUTH = 0;
    public transient static final int LOGIN_TYPE_WEB  = 1;
    /**
     * Name des Routers
     */
    private String routerName;
    private int    loginType;
    private String loginString;
    private int    httpPort;
    private String connectionConnect;
    private String connectionDisconnect;
    private String connectionLogoff;
    private String ipAddressSite;
    private String ipAddressOffline;
    private String ipAddressPre;
    private String ipAddressPost;
    
    public String getConnectionConnect()                             { return connectionConnect;    }
    public String getConnectionDisconnect()                          { return connectionDisconnect; }
    public String getConnectionLogoff()                              { return connectionLogoff;     }
    public String getLoginString()                                   { return loginString;          }
    public String getRouterName()                                    { return routerName;           }
    public String getIpAddressPost()                                 { return ipAddressPost;        }
    public String getIpAddressPre()                                  { return ipAddressPre;         }
    public String getIpAddressOffline()                              { return ipAddressOffline;     }
    public String getIpAddressSite()                                 { return ipAddressSite;        }
    public int getLoginType()                                        { return loginType;            }
    public int getHttpPort()                                         { return httpPort;             }
    public void setConnectionConnect(String connectionConnect)       { this.connectionConnect = connectionConnect;       }
    public void setConnectionDisconnect(String connectionDisconnect) { this.connectionDisconnect = connectionDisconnect; }
    public void setConnectionLogoff(String connectionLogoff)         { this.connectionLogoff = connectionLogoff;         }
    public void setLoginString(String loginString)                   { this.loginString = loginString;                   }
    public void setHttpPort(int httpPort)                            { this.httpPort = httpPort;                         }
    public void setLoginType(int loginType)                          { this.loginType = loginType;                       }
    public void setRouterName(String routerName)                     { this.routerName = routerName;                     }
    public void setIpAddressPost(String ipAddressPost)               { this.ipAddressPost = ipAddressPost;               }
    public void setIpAddressPre(String ipAddressPre)                 { this.ipAddressPre = ipAddressPre;                 }
    public void setIpAddressOffline(String ipAddressOffline)         { this.ipAddressOffline = ipAddressOffline;         }
    public void setIpAddressSite(String ipAddressSite)               { this.ipAddressSite = ipAddressSite;               }
    
    public String toString(){
        return "Router:"+routerName;
    }
}
