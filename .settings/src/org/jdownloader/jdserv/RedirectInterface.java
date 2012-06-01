package org.jdownloader.jdserv;

import org.appwork.remotecall.RemoteCallInterface;

public interface RedirectInterface extends RemoteCallInterface {
    //

    // public static CounterInterface INST =
    // JD_SERV_CONSTANTS.create(CounterInterface.class);

    void redirect(String url);

}
