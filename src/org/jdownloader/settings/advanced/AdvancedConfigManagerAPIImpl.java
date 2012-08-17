package org.jdownloader.settings.advanced;

import java.util.ArrayList;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.api.config.AdvancedConfigManagerAPI;

public class AdvancedConfigManagerAPIImpl implements AdvancedConfigManagerAPI {

    public ArrayList<AdvancedConfigAPIEntry> list() {
        ArrayList<AdvancedConfigAPIEntry> ret = new ArrayList<AdvancedConfigAPIEntry>();
        java.util.List<AdvancedConfigEntry> entries = AdvancedConfigManager.getInstance().list();
        for (AdvancedConfigEntry entry : entries) {
            ret.add(new AdvancedConfigAPIEntry(entry));
        }
        return ret;
    }

    public Object get(String interfacename, String key) {
        KeyHandler<Object> kh = getKeyHandler(interfacename, key);
        return kh.getValue();
    }

    public boolean set(String interfacename, String key, String value) {
        KeyHandler<Object> kh = getKeyHandler(interfacename, key);
        Class<Object> rc = kh.getRawClass();
        Object v = JSonStorage.restoreFromString(value, new TypeRef<Object>(rc) {
        }, null);
        try {
            kh.setValue(v);
        } catch (ValidationException e) {
            return false;
        }
        return true;
    }

    public boolean reset(String interfacename, String key) {
        KeyHandler<Object> kh = getKeyHandler(interfacename, key);
        try {
            kh.setValue(kh.getDefaultValue());
        } catch (ValidationException e) {
            return false;
        }
        return true;
    }

    public Object getDefault(String interfacename, String key) {
        KeyHandler<Object> kh = getKeyHandler(interfacename, key);
        return kh.getDefaultValue();
    }

    @SuppressWarnings("unchecked")
    private KeyHandler<Object> getKeyHandler(String interfacename, String key) {
        ConfigInterface inf;
        try {
            inf = JsonConfig.create((Class<ConfigInterface>) Class.forName(interfacename));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        KeyHandler<Object> kh = inf.getStorageHandler().getKeyHandler(key);
        return kh;
    }
}
