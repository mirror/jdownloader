package org.jdownloader.jdserv;

import org.appwork.remotecall.RemoteCallInterface;

public interface EventLogInterface extends RemoteCallInterface {
    public static EventLogInterface INST = JD_SERV_CONSTANTS.create(EventLogInterface.class);

}
