package jd.nutils;

import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;

public class IPAddress {
    public static final String IP_PATTERN = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";

    /**
     * Überprüft ob eine IP gültig ist. das verwendete Pattern aknn in der
     * config editiert werden.
     * 
     * @param ip
     * @return
     */
    public static boolean validateIP(String ip) {
//        /* Zeitstempel nicht der Validierung unterziehen */
//        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) return true;
        try {
            return Pattern.compile(SubConfiguration.getConfig("DOWNLOAD").getStringProperty(Configuration.PARAM_GLOBAL_IP_MASK, IP_PATTERN)).matcher(ip).matches();
        } catch (Exception e) {
            JDLogger.getLogger().severe("Could not validate IP! " + e);
        }
        return true;
    }

}
