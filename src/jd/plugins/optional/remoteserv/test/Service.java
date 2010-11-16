package jd.plugins.optional.remoteserv.test;

import java.net.ConnectException;

public interface Service {
    public void setData(final DataImpl dataImpl);

    public int substract(final int i, final int j) throws ConnectException;
}
