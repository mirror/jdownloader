package org.jdownloader.api.test;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;

public class RegainTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClient api) throws Exception {
        api.reconnect();

        Dialog.getInstance().showMessageDialog("Done. New SessionToken: " + JSonStorage.serializeToJson(api.getSessionInfo()));
    }

}
