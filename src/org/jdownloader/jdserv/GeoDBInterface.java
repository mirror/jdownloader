package org.jdownloader.jdserv;

import org.appwork.remotecall.RemoteCallInterface;
import org.appwork.remotecall.server.Requestor;

public interface GeoDBInterface extends RemoteCallInterface {

    public String getIP(Requestor req);

    public String getCountryCode(Requestor req, String ip);

    public String getCountryName(Requestor req, String ip);

    public String getASN(Requestor req, String ip);

}
