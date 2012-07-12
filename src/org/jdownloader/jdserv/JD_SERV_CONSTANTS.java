package org.jdownloader.jdserv;

import org.jdownloader.remotecall.RemoteClient;

public class JD_SERV_CONSTANTS {
    public static final String             HOST      = "update3.jdownloader.org/jdserv";
    // public static final String HOST = "192.168.2.250/thomas/fcgi";
    public static final RemoteClient       CLIENT    = new RemoteClient(HOST);
    public static final GeoDBInterface     GEO       = CLIENT.create(GeoDBInterface.class);
    public static final InstallerInterface INSTALLER = CLIENT.create(InstallerInterface.class);
    public static final CounterInterface   COUNT     = CLIENT.create(CounterInterface.class);

    public static void main(String[] args) {

        LogCollection log = new RemoteClient("update3.jdownloader.org/jdserv").create(UploadInterface.class).get("1342021886903", null);
        System.out.println(log.getId());

    }
}
