package org.jdownloader.api.test;

import java.lang.reflect.Type;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.StorageMyJDClient;

public class StorageListTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        StorageMyJDClient<Type> storageAPI = new StorageMyJDClient<Type>(api);
        Dialog.getInstance().showMessageDialog(JSonStorage.serializeToJson(storageAPI.list()));
    }

}
