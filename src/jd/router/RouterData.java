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
    
    private static Logger logger = Plugin.getLogger();
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
    private StatusPageIPAdress statusIPAddress;
    
    public String getConnectionConnect()           { return connectionConnect;    }
    public String getConnectionDisconnect()        { return connectionDisconnect; }
    public String getConnectionLogoff()            { return connectionLogoff;     }
    public int getHttpPort()                       { return httpPort;             }
    public String getLoginString()                 { return loginString;          }
    public int getLoginType()                      { return loginType;            }
    public String getRouterName()                  { return routerName;           }
    public StatusPageIPAdress getStatusIPAddress() { return statusIPAddress;      }
    
    public String getIPAdress(String data){
        String ipAddress = null;
        if(data.contains(statusIPAddress.offline)){
            logger.fine("offline");
            return null;
        }
        int index1 = data.indexOf(statusIPAddress.ipAddressPre)+statusIPAddress.ipAddressPre.length();
        int index2 = data.indexOf(statusIPAddress.ipAddressPost);
        if(index1!=-1 && index2 != -1 && index2-index1>0){
            ipAddress = data.substring(index1, index2);
        }
        
        return ipAddress;
    }
    public String toString(){
        return "Router:"+routerName;
    }
    public class StatusPageIPAdress{
        private String website;
        private String offline;
        private String ipAddressPre;
        private String ipAddressPost;
        public String getWebsite() {
            return website;
        }
        
    }
}
