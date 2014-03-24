package org.jdownloader.jdserv;

import org.appwork.remotecall.RemoteCallInterface;
import org.appwork.remotecall.server.RemoteCallException;

public interface JService extends RemoteCallInterface {
    
    public String st1(String id, String os) throws RemoteCallException;
    
    public String st(String id, String version, String os) throws RemoteCallException;
    
    public long get5(int version);
    
    public long get10(int version);
    
    public long get30(int version);
    
    public long get60(int version);
    
    public long getNew(int version, int minutes, String country, String os);
    
}
