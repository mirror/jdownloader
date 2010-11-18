package jd.plugins.optional.remoteserv.test;

import jd.plugins.optional.remoteserv.remotecall.server.RemoteCallServer;

public class RemoteCallServerImpl extends RemoteCallServer {
    private static final RemoteCallServerImpl INSTANCE = new RemoteCallServerImpl();

    public static RemoteCallServerImpl getInstance() {
        return RemoteCallServerImpl.INSTANCE;
    }

    private RemoteCallServerImpl() {
        super(8080);
    }

}
