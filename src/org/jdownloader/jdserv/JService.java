package org.jdownloader.jdserv;

import org.appwork.remotecall.RemoteCallInterface;
import org.appwork.remotecall.server.RemoteCallException;

public interface JService extends RemoteCallInterface {

    public String check() throws RemoteCallException;

    public String st(String id, String version, String os) throws RemoteCallException;

    public long get5();

    public long get10();

    public long get30();

    public long get60();

    public long getNew(int minutes, String country, String os);

}
