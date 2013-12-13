package org.jdownloader.api.test;

import org.appwork.shutdown.ShutdownController;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;

public class WriteSessionTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {

        config.put("session", JSonStorage.serializeToJson(api.getSessionInfo()));
        ShutdownController.getInstance().requestShutdown();

    }

}
