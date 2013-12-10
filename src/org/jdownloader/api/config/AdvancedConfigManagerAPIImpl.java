package org.jdownloader.api.config;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.StringUtils;
import org.jdownloader.settings.advanced.AdvancedConfigAPIEntry;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class AdvancedConfigManagerAPIImpl implements AdvancedConfigManagerAPI {
    public AdvancedConfigManagerAPIImpl() {

    }

    public ArrayList<AdvancedConfigAPIEntry> list(String pattern, boolean returnDescription, boolean returnValues, boolean returnDefaultValues) {
        ArrayList<AdvancedConfigAPIEntry> ret = new ArrayList<AdvancedConfigAPIEntry>();
        java.util.List<AdvancedConfigEntry> entries = AdvancedConfigManager.getInstance().list();
        Pattern cPat = StringUtils.isEmpty(pattern) ? null : Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        for (AdvancedConfigEntry entry : entries) {
            String key = entry.getKeyHandler().getStorageHandler().getConfigInterface().getName() + "." + entry.getKeyHandler().getKey();
            if (cPat == null || cPat.matcher(key).matches()) {
                ret.add(new AdvancedConfigAPIEntry(entry, returnDescription, returnValues, returnDefaultValues));
            }
        }
        return ret;
    }

    public Object get(String interfaceName, String storage, String key) {
        KeyHandler<Object> kh = getKeyHandler(interfaceName, storage, key);
        return kh.getValue();
    }

    public boolean set(String interfaceName, String storage, String key, Object value) throws InvalidValueException {
        KeyHandler<Object> kh = getKeyHandler(interfaceName, storage, key);
        Type rc = kh.getRawType();
        String json = JSonStorage.serializeToJson(value);
        TypeRef<Object> type = new TypeRef<Object>(rc) {
        };

        try {
            Object v = JSonStorage.stringToObject(json, type, null);
            kh.setValue(v);
        } catch (Exception e) {
            return false;
            // throw new InvalidValueException(e);
        }
        return true;

    }

    public boolean reset(String interfaceName, String storage, String key) {
        KeyHandler<Object> kh = getKeyHandler(interfaceName, storage, key);
        try {
            kh.setValue(kh.getDefaultValue());
        } catch (ValidationException e) {
            return false;
        }
        return true;
    }

    public Object getDefault(String interfaceName, String storage, String key) {
        KeyHandler<Object> kh = getKeyHandler(interfaceName, storage, key);
        return kh.getDefaultValue();
    }

    private KeyHandler<Object> getKeyHandler(String interfaceName, String storage, String key) {
        StorageHandler<?> storageHandler = StorageHandler.getStorageHandler(interfaceName, storage);
        if (storageHandler == null) return null;
        KeyHandler<Object> kh = storageHandler.getKeyHandler(key);
        return kh;
    }

    // @Override
    // public List<ConfigInterfaceAPIStorable> queryConfigInterfaces(APIQuery
    // query) {
    // AdvancedConfigManager acm = AdvancedConfigManager.getInstance();
    // List<ConfigInterfaceAPIStorable> result = new
    // ArrayList<ConfigInterfaceAPIStorable>();
    //
    // for (AdvancedConfigEntry ace : acm.list()) {
    // if (!result.contains(new ConfigInterfaceAPIStorable(ace))) {
    // result.add(new ConfigInterfaceAPIStorable(ace));
    // } else {
    // ConfigInterfaceAPIStorable cis = result.get(result.indexOf(new
    // ConfigInterfaceAPIStorable(ace)));
    // cis.getInfoMap().put("settingsCount", (Integer)
    // cis.getInfoMap().get("settingsCount") + 1);
    // }
    // }
    // return result;
    // }
    //
    // @SuppressWarnings("rawtypes")
    // @Override
    // public List<ConfigEntryAPIStorable> queryConfigSettings(APIQuery query) {
    // AdvancedConfigManager acm = AdvancedConfigManager.getInstance();
    // List<ConfigEntryAPIStorable> result = new
    // ArrayList<ConfigEntryAPIStorable>();
    //
    // // retrieve packageUUIDs from queryParams
    // List<String> interfaceKeys = new ArrayList<String>();
    // if (!query._getQueryParam("interfaceKeys", List.class, new
    // ArrayList()).isEmpty()) {
    // List uuidsFromQuery = query._getQueryParam("interfaceKeys", List.class,
    // new ArrayList());
    // for (Object o : uuidsFromQuery) {
    // try {
    // interfaceKeys.add((String) o);
    // } catch (ClassCastException e) {
    // continue;
    // }
    // }
    // }
    //
    // for (AdvancedConfigEntry ace : acm.list()) {
    // ConfigEntryAPIStorable ces = new ConfigEntryAPIStorable(ace);
    // // if only certain interfaces selected, skip if interface not included
    // if (!interfaceKeys.isEmpty()) {
    // if
    // (!interfaceKeys.contains(ces.getInterfaceKey().substring(ces.getInterfaceKey().lastIndexOf(".")
    // + 1,
    // ces.getInterfaceKey().length()))) {
    // continue;
    // }
    // }
    // KeyHandler<?> kh = ((AdvancedConfigInterfaceEntry) ace).getKeyHandler();
    //
    // org.jdownloader.myjdownloader.client.json.JsonMap infoMap = new org.jdownloader.myjdownloader.client.json.JsonMap();
    // if (query.fieldRequested("value")) {
    // infoMap.put("value", kh.getValue());
    // }
    // if (query.fieldRequested("defaultValue")) {
    // infoMap.put("defaultValue", kh.getDefaultValue());
    // }
    // if (query.fieldRequested("dataType")) {
    // infoMap.put("dataType", kh.getRawClass().getName());
    // }
    // if (query.fieldRequested("description")) {
    // infoMap.put("description", ace.getDescription());
    // }
    //
    // ces.setInfoMap(infoMap);
    // result.add(ces);
    // }
    // return result;
    // }

    @Override
    public ArrayList<AdvancedConfigAPIEntry> list() {
        return list(null, false, false, false);
    }

}
