package org.jdownloader.settings.advanced;

import java.util.ArrayList;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.KeyHandler;

public class AdvancedConfigManagerAPIImpl implements AdvancedConfigManagerAPI {

    public ArrayList<AdvancedConfigAPIEntry> list() {
        ArrayList<AdvancedConfigAPIEntry> ret = new ArrayList<AdvancedConfigAPIEntry>();
        ArrayList<AdvancedConfigEntry> entries = AdvancedConfigManager.getInstance().list();
        for (AdvancedConfigEntry entry : entries) {
            ret.add(new AdvancedConfigAPIEntry(entry));
        }
        return ret;
    }

    public Object get(String interfacename, String key) {
        ConfigInterface inf;
        try {
            inf = JsonConfig.create((Class<ConfigInterface>) Class.forName(interfacename));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return inf.getStorageHandler().getKeyHandler(key).getValue();
    }

    public boolean set(String interfacename, String key, String value) {
        ConfigInterface inf;
        try {
            inf = JsonConfig.create((Class<ConfigInterface>) Class.forName(interfacename));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        KeyHandler<Object> kh = inf.getStorageHandler().getKeyHandler(key);
        Class<Object> rc = kh.getRawClass();

        Object v = JSonStorage.restoreFromString(value, new TypeRef<Object>(rc) {
        }, null);

        inf.getStorageHandler().getKeyHandler(key).setValue(v);
        return true;
    }
}
