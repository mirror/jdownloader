package org.jdownloader.api.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.StringUtils;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.myjdownloader.client.bindings.interfaces.AdvancedConfigInterface;
import org.jdownloader.settings.advanced.AdvancedConfigAPIEntry;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class AdvancedConfigManagerAPIImpl implements AdvancedConfigManagerAPI {
    public AdvancedConfigManagerAPIImpl() {
        RemoteAPIController.validateInterfaces(AdvancedConfigManagerAPI.class, AdvancedConfigInterface.class);
    }

    @Deprecated
    public ArrayList<AdvancedConfigAPIEntry> list(String pattern, boolean returnDescription, boolean returnValues, boolean returnDefaultValues) {
        return list(pattern, returnDescription, returnValues, returnDefaultValues, false);
    }

    public ArrayList<AdvancedConfigAPIEntry> list(String pattern, boolean returnDescription, boolean returnValues, boolean returnDefaultValues, boolean returnEnumInfo) {
        try {
            ArrayList<AdvancedConfigAPIEntry> ret = new ArrayList<AdvancedConfigAPIEntry>();
            java.util.List<AdvancedConfigEntry> entries = AdvancedConfigManager.getInstance().list();
            Pattern cPat = StringUtils.isEmpty(pattern) ? null : Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            for (AdvancedConfigEntry entry : entries) {
                String key = entry.getKeyHandler().getStorageHandler().getConfigInterface().getName() + "." + entry.getKeyHandler().getKey();
                if (cPat == null || cPat.matcher(key).matches()) {
                    AdvancedConfigAPIEntry acae;
                    ret.add(acae = new AdvancedConfigAPIEntry(entry, returnDescription, returnValues, returnDefaultValues));
                    if (returnEnumInfo && Clazz.isEnum(entry.getType())) {

                        ArrayList<EnumOption> enumOptions;

                        enumOptions = listEnumOptions(entry.getType());

                        String[] constants = new String[enumOptions.size()];
                        String[] labels = new String[enumOptions.size()];
                        boolean hasLabels = false;
                        String label = null;
                        Enum<?> value = ((Enum<?>) entry.getValue());
                        for (int i = 0; i < enumOptions.size(); i++) {
                            EnumOption option = enumOptions.get(i);
                            constants[i] = option.getName();
                            labels[i] = option.getLabel();
                            hasLabels |= StringUtils.isNotEmpty(labels[i]);
                            if (value != null && value.name().equals(option.getName())) {
                                label = labels[i];
                            }
                        }
                        acae.setEnumLabel(label);
                        acae.setEnumLabels(hasLabels ? labels : null);
                        acae.setEnumOptions(constants);
                    }

                }

            }
            return ret;
        } catch (Exception e) {
            throw new WTFException(e);
        }

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
        return list(null, false, false, false, false);
    }

    @Override
    public ArrayList<EnumOption> listEnum(String type) throws BadParameterException {

        try {
            Class<?> cls = Class.forName(type);

            return listEnumOptions(cls);
        } catch (Exception e) {
            throw new BadParameterException(e, "Bad Type: " + type);
        }

    }

    /**
     * @param cls
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws NoSuchFieldException
     */
    public ArrayList<EnumOption> listEnumOptions(Class<?> cls) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        ArrayList<EnumOption> ret = new ArrayList<EnumOption>();

        Object[] values = (Object[]) cls.getMethod("values", new Class[] {}).invoke(null, new Object[] {});
        for (final Object o : values) {
            String label = null;
            EnumLabel lbl = cls.getDeclaredField(o.toString()).getAnnotation(EnumLabel.class);
            if (lbl != null) {
                label = lbl.value();
            } else {

                if (o instanceof LabelInterface) {

                    label = (((LabelInterface) o).getLabel());
                }
            }
            ret.add(new EnumOption(o.toString(), label));
        }

        return ret;
    }

}
