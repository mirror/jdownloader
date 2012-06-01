package org.jdownloader.jdserv;

import org.appwork.remotecall.RemoteCallInterface;

public interface GeoDBInterface extends RemoteCallInterface {

    public String getIP();

    public String getCountryCodebyIP(String ip);

    public String getCountryNameByIP(String ip);

    public String getASNByIP(String ip);

    public String getCountryCode();

    public String getCountryName();

    public String getASN();

}
