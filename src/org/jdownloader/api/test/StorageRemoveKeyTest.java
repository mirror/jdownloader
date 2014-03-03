package org.jdownloader.api.test;

import java.lang.reflect.Type;

import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.StorageMyJDClient;

public class StorageRemoveKeyTest extends Test {
    
    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        StorageMyJDClient<Type> storageAPI = new StorageMyJDClient<Type>(api);
        final String storageID = Dialog.getInstance().showInputDialog(0, "Enter storageID", "Enter", null, null, null, null);
        final String key = Dialog.getInstance().showInputDialog(0, "Enter key", "Enter", null, null, null, null);
        storageAPI.removeKey(storageID, key);
    }
    
}
