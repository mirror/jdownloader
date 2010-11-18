package jd.plugins.optional.remoteserv.test;

import jd.plugins.optional.remoteserv.remotecall.RemoteCallService;

public class ServiceImpl extends RemoteCallService implements Service {

    public void setData(final DataImpl dataImpl) {
        System.out.println("Received " + dataImpl);
    }

    public int substract(final int i, final int j) {
        return i - j;
    }

}
