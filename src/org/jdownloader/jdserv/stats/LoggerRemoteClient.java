package org.jdownloader.jdserv.stats;

import org.jdownloader.remotecall.RemoteClient;

public class LoggerRemoteClient extends RemoteClient {

    public LoggerRemoteClient(RemoteClient client) {
        super(client.getHost());

    }
}
