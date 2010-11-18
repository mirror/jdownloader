package jd.plugins.optional.remoteserv.remotecall.server;

import jd.plugins.optional.remoteserv.remotecall.RemoteCallService;

public class RemoteCallServiceWrapper {

    public static RemoteCallServiceWrapper create(final RemoteCallService serviceImpl) {
        final RemoteCallServiceWrapper ret = new RemoteCallServiceWrapper(serviceImpl);
        return ret;
    }

    private final RemoteCallService _service;

    private RemoteCallServiceWrapper(final RemoteCallService serviceImpl) {
        this._service = serviceImpl;
    }

}
