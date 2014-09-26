package org.jdownloader.jdserv;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("GeoDBInterface")
public interface GeoDBInterface extends RemoteAPIInterface {

    public String getIP(RemoteAPIRequest request);

    public String getCountryCodebyIP(String ip, RemoteAPIRequest request);

    public String getCountryNameByIP(String ip, RemoteAPIRequest request);

    public String getASNByIP(String ip, RemoteAPIRequest request);

    public String getCountryCode(RemoteAPIRequest request);

    public String getCountryName(RemoteAPIRequest request);

    public String getASN(RemoteAPIRequest request);

}
