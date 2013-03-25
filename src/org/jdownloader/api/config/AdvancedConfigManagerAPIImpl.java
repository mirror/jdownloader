package org.jdownloader.api.config;

import java.util.ArrayList;
import java.util.List;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.QueryResponseMap;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.jdownloader.settings.advanced.AdvancedConfigAPIEntry;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.settings.advanced.AdvancedConfigInterfaceEntry;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class AdvancedConfigManagerAPIImpl implements AdvancedConfigManagerAPI {

    public ArrayList<AdvancedConfigAPIEntry> list() {
        ArrayList<AdvancedConfigAPIEntry> ret = new ArrayList<AdvancedConfigAPIEntry>();
        java.util.List<AdvancedConfigEntry> entries = AdvancedConfigManager.getInstance().list();
        for (AdvancedConfigEntry entry : entries) {
            ret.add(new AdvancedConfigAPIEntry(entry));
        }
        return ret;
    }

    public Object get(int storageID, String key) {
        KeyHandler<Object> kh = getKeyHandler(storageID, key);
        return kh.getValue();
    }

    public boolean set(int storageID, String key, String value) {
        KeyHandler<Object> kh = getKeyHandler(storageID, key);
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

    public boolean reset(int storageID, String key) {
        KeyHandler<Object> kh = getKeyHandler(storageID, key);
        try {
            kh.setValue(kh.getDefaultValue());
        } catch (ValidationException e) {
            return false;
        }
        return true;
    }

    public Object getDefault(int storageID, String key) {
        KeyHandler<Object> kh = getKeyHandler(storageID, key);
        return kh.getDefaultValue();
    }

    private KeyHandler<Object> getKeyHandler(int storageID, String key) {
        StorageHandler<?> storageHandler = StorageHandler.getStorageHandler(storageID);
        if (storageHandler == null) return null;
        KeyHandler<Object> kh = storageHandler.getKeyHandler(key);
        return kh;
    }

    @Override
    public List<ConfigInterfaceAPIStorable> queryConfigInterfaces(APIQuery query) {
        AdvancedConfigManager acm = AdvancedConfigManager.getInstance();
        List<ConfigInterfaceAPIStorable> result = new ArrayList<ConfigInterfaceAPIStorable>();

        for (AdvancedConfigEntry ace : acm.list()) {
            if (!result.contains(new ConfigInterfaceAPIStorable(ace))) {
                result.add(new ConfigInterfaceAPIStorable(ace));
            } else {
                ConfigInterfaceAPIStorable cis = result.get(result.indexOf(new ConfigInterfaceAPIStorable(ace)));
                cis.getInfoMap().put("settingsCount", (Integer) cis.getInfoMap().get("settingsCount") + 1);
            }
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<ConfigEntryAPIStorable> queryConfigSettings(APIQuery query) {
        AdvancedConfigManager acm = AdvancedConfigManager.getInstance();
        List<ConfigEntryAPIStorable> result = new ArrayList<ConfigEntryAPIStorable>();

        // retrieve packageUUIDs from queryParams
        List<String> interfaceKeys = new ArrayList<String>();
        if (!query._getQueryParam("interfaceKeys", List.class, new ArrayList()).isEmpty()) {
            List uuidsFromQuery = query._getQueryParam("interfaceKeys", List.class, new ArrayList());
            for (Object o : uuidsFromQuery) {
                try {
                    interfaceKeys.add((String) o);
                } catch (ClassCastException e) {
                    continue;
                }
            }
        }

        for (AdvancedConfigEntry ace : acm.list()) {
            ConfigEntryAPIStorable ces = new ConfigEntryAPIStorable(ace);
            // if only certain interfaces selected, skip if interface not included
            if (!interfaceKeys.isEmpty()) {
                if (!interfaceKeys.contains(ces.getInterfaceKey().substring(ces.getInterfaceKey().lastIndexOf(".") + 1, ces.getInterfaceKey().length()))) {
                    continue;
                }
            }
            KeyHandler<?> kh = ((AdvancedConfigInterfaceEntry) ace).getKeyHandler();

            QueryResponseMap infoMap = new QueryResponseMap();
            if (query.fieldRequested("value")) {
                infoMap.put("value", kh.getValue());
            }
            if (query.fieldRequested("defaultValue")) {
                infoMap.put("defaultValue", kh.getDefaultValue());
            }
            if (query.fieldRequested("dataType")) {
                infoMap.put("dataType", kh.getRawClass().getName());
            }
            if (query.fieldRequested("description")) {
                infoMap.put("description", ace.getDescription());
            }

            ces.setInfoMap(infoMap);
            result.add(ces);
        }
        return result;
    }
}
