package jd.controlling.reconnect.ipcheck;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.regex.Pattern;

import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.RouterUtils;
import jd.controlling.reconnect.pluginsinc.liveheader.LiveHeaderReconnectSettings;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.jdownloader.logging.LogController;

public class IP {
    public static final String IP_PATTERN = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";

    /**
     * validates the address, and returns an IP instance or throws an exception in case of validation errors
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
                if (isLocalIP(ip)) {
                    throw new InvalidIPRangeException(ip);
                }
                final int n1 = Integer.parseInt(parts[0]);
                final int n2 = Integer.parseInt(parts[1]);
                final int n3 = Integer.parseInt(parts[2]);
                final int n4 = Integer.parseInt(parts[3]);
                /* fritzbox sends 0.0.0.0 while its offline */
                if (n1 == 0 && n2 == 0 && n3 == 0 && n4 == 0) {
                    throw new OfflineException(ip);
                }
                if (n1 >= 0 && n1 <= 255 && n2 >= 0 && n2 <= 255 && n3 >= 0 && n3 <= 255 && n4 >= 0 && n4 <= 255) {
                    if (!IP.validateIP(ip)) {
                        throw new ForbiddenIPException(ip);
                    }
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
    private static boolean validateIP(final String ip) {
        if (ip == null) {
            return false;
        } else {
            try {
                return ip != null && Pattern.compile(JsonConfig.create(ReconnectConfig.class).getGlobalIPCheckPattern()).matcher(ip.trim()).matches();
            } catch (final Exception e) {
                LogController.CL().severe("Could not validate IP!");
                LogController.CL().log(e);
            }
            return true;
        }
    }

    protected final String ip;

    private IP(final String ip) {
        this.ip = ip;
    }

    public String getIP() {
        return ip;
    }

    public boolean equals(final Object c) {
        if (c == null) {
            return false;
        } else if (c == this) {
            return true;
        } else if (c != null && c instanceof IP) {
            final IP ip = (IP) c;
            return StringUtils.equals(getIP(), ip.getIP());
        } else {
            return false;
        }
    }

    public int hashCode() {
        if (ip == null) {
            return super.hashCode();
        } else {
            return this.ip.hashCode();
        }
    }

    public String toString() {
        return this.ip != null ? this.ip : "unknown";
    }

    public static boolean isValidRouterIP(String gatewayIP) {
        if (StringUtils.isEmpty(gatewayIP)) {
            return false;
        } else {
            final String[] whiteListArray = JsonConfig.create(LiveHeaderReconnectSettings.class).getHostWhiteList();
            if (whiteListArray != null && Arrays.asList(whiteListArray).contains(gatewayIP)) {
                return RouterUtils.checkPort(gatewayIP);
            } else {
                return isLocalIP(gatewayIP) && RouterUtils.checkPort(gatewayIP);
            }
        }
    }

    public static boolean isLocalIP(String ip) {
        if (StringUtils.isEmpty(ip)) {
            return false;
        } else {
            try {
                final InetAddress[] inetAddresses = InetAddress.getAllByName(ip);
                for (final InetAddress inetAddress : inetAddresses) {
                    if (inetAddress.isSiteLocalAddress() || inetAddress.isLoopbackAddress()) {
                        return true;
                    }
                }
                return false;
            } catch (UnknownHostException e) {
                LogController.CL().log(e);
                return false;
            }
        }
    }
}
