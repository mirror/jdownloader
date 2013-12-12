package org.jdownloader.api.test;

import org.appwork.storage.Storage;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;

public class KeepAliveTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClient api) throws Exception {
        api.keepalive();
    }

}
