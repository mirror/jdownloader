package org.jdownloader.jdserv;

import org.appwork.remotecall.RemoteCallInterface;

public interface CounterInterface extends RemoteCallInterface {
    //

    // public static CounterInterface INST =
    // JD_SERV_CONSTANTS.create(CounterInterface.class);

    void inc(String key);

    long getValue(String key);

}
