package org.jdownloader.api.test;

import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.StorageMyJDClient;

public class StorageDropTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        StorageMyJDClient storageAPI = new StorageMyJDClient(api);
        final String storageID = Dialog.getInstance().showInputDialog(0, "Enter storageID to drop", "Enter", null, null, null, null);
        storageAPI.drop(storageID);

    }

}
