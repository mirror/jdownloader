package org.jdownloader.jdserv;

import org.jdownloader.remotecall.RemoteClient;

public class JD_SERV_CONSTANTS {
    // public static final String HOST = "update3.jdownloader.org/jdserv";
    public static final String             HOST      = "192.168.2.250/thomas/fcgi";
    public static final RemoteClient       CLIENT    = new RemoteClient(HOST);
    public static final GeoDBInterface     GEO       = CLIENT.create(GeoDBInterface.class);
    public static final InstallerInterface INSTALLER = CLIENT.create(InstallerInterface.class);

    public static void main(String[] args) {
        System.out.println("\"DE\"".substring(1, 3));
        // CounterInterface.INST.inc("test");
        // System.out.println(CounterInterface.INST.getValue("test"));#
        String ip = null;
        System.out.println("IP: " + GEO.getIP());
        System.out.println("ASN: " + GEO.getASNByIP(ip));
        GEO.getCountryCode();

        System.out.println("Country Code: " + GEO.getCountryCodebyIP(ip));
        System.out.println("Country Name: " + GEO.getCountryNameByIP(ip));
        // CLIENT.create(RedirectInterface.class).redirect("google.de?q=affenschauckel");

    }

    public static String redirect(String buyPremiumLink) {
        return buyPremiumLink.replace("http://", "http://" + HOST + "/redirect?");
    }
}
