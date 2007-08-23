package jd.router;

import java.io.Serializable;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.captcha.UTILITIES;
import jd.plugins.HTTPPost;
import jd.plugins.Plugin;
import jd.plugins.RequestInfo;

/**
 * Hier werden die Daten für einen Router gespeichert
 * 
 * @author astaldo
 */
public class Reconnector {
    /**
     * Reconnected Jede Fritzbox. keine Logins dürfen gesetzt sein
     * 
     * @return Neue IP
     */
    public static String fritzBox() {
        //Hol die aktuelle IP
        String ip = getCurrentIp(5000);
        if (ip != null) {
            UTILITIES.getLogger().fine("Old IP: " + ip);
            //Post
            HTTPPost post = new HTTPPost("192.168.178.1", 49000, "/upnp/control/WANIPConn1", "", true);
            //requestProperties
            post.getConnection().setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:WANIPConnection:1#ForceTermination\"");
            post.getConnection().setRequestProperty("CONTENT-TYPE", "text/xml ; charset=\"utf-8\"");
            //Verbindung aufbauen
            post.connect();
            //PostParameter
            post.post("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">\r\n<s:Body>\r\n<u:ForceTermination xmlns:u=\"urn:schemas-upnp-org:service:WANIPConnection:1\" />\r\n</s:Body>\r\n</s:Envelope>");
            //lesen
            post.getRequestInfo();
            //schließen
            post.close();
        } else {
            UTILITIES.getLogger().fine("No Connection");
        }
        //Auf neue Ip Warten
        int i = 0;
        String newIp;
        while (true) {
            newIp = getCurrentIp(1000);
            if (newIp == null) {
                i++;
                continue;
            }
            if (!newIp.equals(ip))
                break;
            UTILITIES.wait(1000);
            i++;
        }
        UTILITIES.getLogger().fine("NEUE Ip nach " + i + " sek: " + newIp);

        return newIp;
    }

    /**
     * Wartet time millisekunden auf eine neue ip
     * 
     * @param time
     * @return gibt die aktuelle ip zurück oder null falls keine verbindung
     *         besteht
     */
    public static String getCurrentIp(int time) {
        int oldRequestTimeout = UTILITIES.getREQUEST_TIMEOUT();
        int oldReadTimeout = UTILITIES.getREAD_TIMEOUT();
        UTILITIES.setREQUEST_TIMEOUT(time);
        UTILITIES.setREAD_TIMEOUT(time);
        String[] matches = UTILITIES.getMatches(UTILITIES.getPagewithScanner("http://www.meineip.de/"), "Ihre IP-Adresse lautet:</th>°<td><b>°</b></td>");
        UTILITIES.setREQUEST_TIMEOUT(oldRequestTimeout);
        UTILITIES.setREAD_TIMEOUT(oldReadTimeout);
        return matches == null ? null : matches[1];
    }
}
