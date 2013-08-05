package org.jdownloader.api.extraction;

import java.lang.reflect.Type;
import java.util.List;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StorageHandler;

public class ExtractionAPIImpl implements ExtractionAPI {
    @Override
    public void addArchivePassword(String password) {
        KeyHandler<Object> kh = getKeyHandler("org.jdownloader.extensions.extraction.ExtractionConfig", "cfg/org.jdownloader.extensions.extraction.ExtractionExtension", "passwordList");
        @SuppressWarnings("unchecked")
        List<String> passworList = (List<String>) kh.getValue();
        passworList.add(password);

        Type rc = kh.getRawType();
        String json = JSonStorage.toString(passworList);
        TypeRef<Object> type = new TypeRef<Object>(rc) {
        };

        try {
            Object v;
            synchronized (JSonStorage.LOCK) {
                v = JSonStorage.getMapper().stringToObject(json, type);
            }

            kh.setValue(v);
        } catch (Exception e) {
            // throw new InvalidValueException(e);
        }
    }

    private KeyHandler<Object> getKeyHandler(String interfaceName, String storage, String key) {
        StorageHandler<?> storageHandler = StorageHandler.getStorageHandler(interfaceName, storage);
        if (storageHandler == null) return null;
        KeyHandler<Object> kh = storageHandler.getKeyHandler(key);
        return kh;
    }
}
