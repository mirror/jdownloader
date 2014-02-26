package jd.controlling.reconnect.ipcheck;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.RouterUtils;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.logging.LogController;

public class IP {
    public static final String IP_PATTERN = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";
    
    /**
     * validates the adress, and returns an IP instance or throws an exception in case of validation errors
     * 
     * @param ip
     * @return
     * @throws IPCheckException
     */
    public static IP getInstance(final String ip) throws IPCheckException {
        if (ip.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            final String parts[] = ip.split("\\.");
            if (parts.length == 4) {
                /* filter private networks */
                final int n1 = Integer.parseInt(parts[0]);
                /* 10.0.0.0-10.255.255.255 */
                if (n1 == 10) { throw new InvalidIPRangeException(ip); }
                final int n2 = Integer.parseInt(parts[1]);
                /* 192.168.0.0 - 192.168.255.255 */
                if (n1 == 192 && n2 == 168) { throw new InvalidIPRangeException(ip); }
                /* 172.16.0.0 - 172.31.255.255 */
                if (n1 == 172 && n2 >= 16 && n2 <= 31) { throw new InvalidIPRangeException(ip); }
                final int n3 = Integer.parseInt(parts[2]);
                final int n4 = Integer.parseInt(parts[3]);
                /* fritzbox sends 0.0.0.0 while its offline */
                if (n1 == 0 && n2 == 0 && n3 == 0 && n4 == 0) { throw new OfflineException(ip); }
                if (n1 >= 0 && n1 <= 255 && n2 >= 0 && n2 <= 255 && n3 >= 0 && n3 <= 255 && n4 >= 0 && n4 <= 255) {
                    
                    if (!IP.validateIP(ip)) { throw new ForbiddenIPException(ip); }
                    return new IP(ip);
                } else {
                    throw new InvalidIPException(ip);
                }
            }
        }
        throw new InvalidIPException(ip);
        
    }
    
    /**
     * Überprüft ob eine IP gültig ist. das verwendete Pattern kann in der config editiert werden.
     * 
     * @param ip
     * @return
     */
    public static boolean validateIP(final String ip) {
        if (ip == null) { return false; }
        try {
            return Pattern.compile(JsonConfig.create(ReconnectConfig.class).getGlobalIPCheckPattern()).matcher(ip.trim()).matches();
        } catch (final Exception e) {
            LogController.CL().severe("Could not validate IP!");
            LogController.CL().log(e);
        }
        return true;
    }
    
    protected final String ip;
    
    private IP(final String ip) {
        this.ip = ip;
    }
    
    public String getIP() {
        return ip;
    }
    
    public boolean equals(final Object c) {
        if (c != null && c instanceof IP) {
            final IP ip = (IP) c;
            return ip.ip.equals(this.ip);
        }
        return false;
    }
    
    public int hashCode() {
        return this.ip.hashCode();
    }
    
    public String toString() {
        return this.ip != null ? this.ip : "unknown";
    }
    
    public static void main(String[] args) {
        System.out.println(isValidRouterIP("192.168.0.1"));
    }
    
    public static boolean isValidRouterIP(String gatewayIP) {
        boolean localip = isLocalIP(gatewayIP);
        if (!localip) {
            try {
                localip = isLocalIP(InetAddress.getByName(gatewayIP).getHostAddress());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        if (!localip) return false;
        return RouterUtils.checkPort(gatewayIP);
    }
    
    public static boolean isLocalIP(String ip) {
        
        if (ip == null) return false;
        if (ip.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            final String parts[] = ip.split("\\.");
            if (parts.length == 4) {
                /* filter private networks */
                final int n1 = Integer.parseInt(parts[0]);
                final int n2 = Integer.parseInt(parts[1]);
                // final int n3 = Integer.parseInt(parts[2]);
                // final int n4 = Integer.parseInt(parts[3]);
                /* 10.0.0.0-10.255.255.255 */
                if (n1 == 10 || n1 == 127) { return true; }
                /* 192.168.0.0 - 192.168.255.255 */
                if (n1 == 192 && n2 == 168) { return true; }
                /* 172.16.0.0 - 172.31.255.255 */
                if (n1 == 172 && n2 >= 16 && n2 <= 31) { return true; }
                
            }
        }
        return false;
    }
    
}
