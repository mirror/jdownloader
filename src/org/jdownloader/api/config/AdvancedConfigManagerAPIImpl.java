package org.jdownloader.api.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.swing.Icon;

import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.extensions.UninstalledExtension;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.myjdownloader.client.bindings.AdvancedConfigEntryDataStorable.AbstractType;
import org.jdownloader.myjdownloader.client.bindings.interfaces.AdvancedConfigInterface;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.advanced.AdvancedConfigAPIEntry;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.settings.advanced.AdvancedConfigManager;
import org.jdownloader.updatev2.InstallLog;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyRestartRequest;
import org.jdownloader.updatev2.UpdateController;
import org.jdownloader.updatev2.UpdaterListener;

public class AdvancedConfigManagerAPIImpl implements AdvancedConfigManagerAPI {
    private static final String EXTENSION = "Extension";

    public AdvancedConfigManagerAPIImpl() {
        RemoteAPIController.validateInterfaces(AdvancedConfigManagerAPI.class, AdvancedConfigInterface.class);
    }

    @Override
    public ArrayList<AdvancedConfigAPIEntry> query(final AdvancedConfigQueryStorable query) {
        final ArrayList<AdvancedConfigAPIEntry> ret = new ArrayList<AdvancedConfigAPIEntry>();
        final java.util.List<AdvancedConfigEntry> entries = AdvancedConfigManager.getInstance().list();
        final boolean isInterfaceQuery = !StringUtils.isEmpty(query.getConfigInterface());
        Pattern cPat = null;
        if (!StringUtils.isEmpty(query.getPattern())) {
            cPat = Pattern.compile(query.getPattern(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        }
        for (final AdvancedConfigEntry entry : entries) {
            if (isInterfaceQuery && !query.getConfigInterface().equals(entry.getKeyHandler().getStorageHandler().getConfigInterface().getName())) {
                continue;
            } else {
                if (cPat != null && !cPat.matcher(entry.getKeyHandler().getStorageHandler().getConfigInterface().getName() + "." + entry.getKeyHandler().getKey()).matches()) {
                    continue;
                } else {
                    try {
                        ret.add(createAPIEntry(query.isDescription(), query.isValues(), query.isDefaultValues(), query.isEnumInfo(), entry));
                    } catch (Exception e) {
                        throw new WTFException(e);
                    }
                }
            }
        }
        if (query.isIncludeExtensions()) {
            ret.addAll(createExtensionConfigList(query));
        }
        return ret;
    }

    @Override
    public Object get(String interfaceName, String storage, String key) {
        final KeyHandler<Object> kh = getKeyHandler(interfaceName, storage, key);
        if (kh == null) {
            return null;
        } else {
            return kh.getValue();
        }
    }

    @Override
    public boolean set(String interfaceName, String storage, String key, Object value) throws InvalidValueException {
        if (EXTENSION.equals(interfaceName)) {
            final String json = JSonStorage.serializeToJson(value);
            for (final LazyExtension ext : ExtensionController.getInstance().getExtensions()) {
                if (createExtensionToggleDummyKey("Enable", ext).equals(key)) {
                    try {
                        final Object v = JSonStorage.stringToObject(json, TypeRef.BOOLEAN, null);
                        ext._setEnabled((Boolean) v);
                        return true;
                    } catch (Exception e) {
                        return false;
                        // throw new InvalidValueException(e);
                    }
                }
            }
            if (key != null && key.startsWith("InstallExtension")) {
                final String toInstall = key.substring("InstallExtension".length()).toLowerCase(Locale.ENGLISH);
                installExtension(toInstall);
                return true;
            }
            return false;
        }
        final KeyHandler<Object> keyHandler = getKeyHandler(interfaceName, storage, key);
        if (keyHandler != null) {
            final Type type = keyHandler.getRawType();
            final TypeRef<Object> typeRef = new TypeRef<Object>(type) {
            };
            try {
                Object setValue = null;
                if (value instanceof String) {
                    try {
                        // for example Map<String,Integer> or Number
                        setValue = JSonStorage.restoreFromString((String) value, typeRef);
                    } catch (Exception e) {
                        // for example ENUM
                        final String jsonString = JSonStorage.serializeToJson(value);
                        setValue = JSonStorage.restoreFromString(jsonString, typeRef);
                    }
                } else {
                    // for example Array(List) or Set
                    final String jsonString = JSonStorage.serializeToJson(value);
                    setValue = JSonStorage.restoreFromString(jsonString, typeRef);
                }
                keyHandler.setValue(setValue);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    @Deprecated
    public ArrayList<AdvancedConfigAPIEntry> list(String pattern, boolean returnDescription, boolean returnValues, boolean returnDefaultValues) {
        return list(pattern, returnDescription, returnValues, returnDefaultValues, false);
    }

    @Override
    @Deprecated
    public ArrayList<AdvancedConfigAPIEntry> list(String pattern, boolean returnDescription, boolean returnValues, boolean returnDefaultValues, boolean returnEnumInfo) {
        try {
            AdvancedConfigQueryStorable query = new AdvancedConfigQueryStorable();
            query.setPattern(pattern);
            query.setDescription(true);
            query.setValues(returnValues);
            query.setDefaultValues(returnDefaultValues);
            query.setEnumInfo(returnEnumInfo);
            query.setIncludeExtensions(true);
            return query(query);
        } catch (Exception e) {
            throw new WTFException(e);
        }
    }

    @Override
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
    private ArrayList<EnumOption> listEnumOptions(Class<?> cls) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
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

    private List<AdvancedConfigAPIEntry> createExtensionConfigList(final AdvancedConfigQueryStorable query) {
        final ArrayList<AdvancedConfigAPIEntry> ret = new ArrayList<AdvancedConfigAPIEntry>();
        for (final UninstalledExtension ext : ExtensionController.getInstance().getUninstalledExtensions()) {
            AdvancedConfigAPIEntry entry = new AdvancedConfigAPIEntry();
            entry.setAbstractType(AbstractType.BOOLEAN);
            if (query.isDefaultValues()) {
                entry.setDefaultValue(false);
            }
            if (query.isDescription()) {
                entry.setDocs("Install Extension: " + ext.getName());
            }
            entry.setInterfaceName(EXTENSION);
            String dummyKey = "InstallExtension" + ext.getId().toUpperCase(Locale.ENGLISH);
            entry.setKey(dummyKey);
            if (query.isValues()) {
                entry.setValue(false);
            }
            ret.add(entry);
        }
        for (final LazyExtension ext : ExtensionController.getInstance().getExtensions()) {
            // enable
            AdvancedConfigAPIEntry entry = new AdvancedConfigAPIEntry();
            entry.setAbstractType(AbstractType.BOOLEAN);
            if (query.isDefaultValues()) {
                entry.setDefaultValue(false);
            }
            if (query.isDescription()) {
                entry.setDocs("Enable/Disable Extension: " + ext.getName());
            }
            entry.setInterfaceName(EXTENSION);
            String dummyKey = createExtensionToggleDummyKey("Enable", ext);
            entry.setKey(dummyKey);
            if (query.isValues()) {
                entry.setValue(ext._isEnabled());
            }
            ret.add(entry);
        }
        return ret;
    }

    private AdvancedConfigAPIEntry createAPIEntry(boolean returnDescription, boolean returnValues, boolean returnDefaultValues, boolean returnEnumInfo, final AdvancedConfigEntry entry) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, NoSuchFieldException {
        AdvancedConfigAPIEntry acae = new AdvancedConfigAPIEntry(entry, returnDescription, returnValues, returnDefaultValues);
        if (returnEnumInfo && Clazz.isEnum(entry.getClazz())) {
            ArrayList<EnumOption> enumOptions;
            enumOptions = listEnumOptions(entry.getClazz());
            String[][] constants = new String[enumOptions.size()][2];
            String label = null;
            Enum<?> value = ((Enum<?>) entry.getValue());
            for (int i = 0; i < enumOptions.size(); i++) {
                EnumOption option = enumOptions.get(i);
                constants[i] = new String[] { option.getName(), option.getLabel() };
                if (value != null && value.name().equals(option.getName())) {
                    label = constants[i][1];
                }
            }
            acae.setEnumLabel(label);
            acae.setEnumOptions(constants);
        }
        return acae;
    }

    protected String createExtensionToggleDummyKey(String namespace, final LazyExtension ext) {
        int lastP = ext.getClassname().lastIndexOf(".");
        String dummyKey = namespace + ext.getClassname().substring(lastP + 1);
        return dummyKey;
    }

    private KeyHandler<Object> getKeyHandler(String interfaceName, String storage, String key) {
        StorageHandler<?> storageHandler = StorageHandler.getStorageHandler(interfaceName, storage);
        if (storageHandler == null) {
            // check plugins
            if (interfaceName.startsWith("jd.plugins.hoster.") || interfaceName.startsWith("jd.plugins.decrypter")) {
                ArrayList<AdvancedConfigEntry> ret = new ArrayList<AdvancedConfigEntry>();
                final PluginClassLoaderChild pluginClassLoader = PluginClassLoader.getInstance().getChild();
                for (LazyHostPlugin hplg : HostPluginController.getInstance().list()) {
                    String ifName = hplg.getConfigInterface();
                    if (StringUtils.equals(ifName, interfaceName)) {
                        ConfigInterface cf;
                        try {
                            cf = PluginJsonConfig.get((Class<PluginConfigInterface>) pluginClassLoader.loadClass(ifName));
                            storageHandler = cf._getStorageHandler();
                            break;
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        System.out.println(ifName);
                    }
                }
                for (LazyCrawlerPlugin cplg : CrawlerPluginController.getInstance().list()) {
                    String ifName = cplg.getConfigInterface();
                    if (StringUtils.isNotEmpty(ifName)) {
                        System.out.println(ifName);
                    }
                }
            }
            if (storageHandler == null) {
                return null;
            }
        }
        KeyHandler<Object> kh = storageHandler.getKeyHandler(key);
        return kh;
    }

    private void installExtension(final String toInstall) {
        new Thread("Install Extension") {
            public void run() {
                if (UIOManager.I().showConfirmDialog(0, _GUI.T.lit_are_you_sure(), _GUI.T.installExtension_remote_rly(toInstall))) {
                    UpdaterListener listener = null;
                    try {
                        final AtomicLong last = new AtomicLong(System.currentTimeMillis());
                        UpdateController.getInstance().getEventSender().addListener(listener = new UpdaterListener() {
                            @Override
                            public void onUpdatesAvailable(boolean selfupdate, InstallLog installlog) {
                                System.out.println();
                            }

                            @Override
                            public void onUpdaterStatusUpdate(final String label, Icon icon, final double p) {
                                if (System.currentTimeMillis() - last.get() > 5000) {
                                    UIOManager.I().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI.T.installExtension_remote_title(toInstall), _GUI.T.installExtension_remote_wait(), new AbstractIcon(IconKey.ICON_WAIT, 32), _GUI.T.lit_continue(), null);
                                    last.set(System.currentTimeMillis());
                                }
                            }
                        });
                        UpdateController.getInstance().runExtensionInstallation(toInstall);
                        while (true) {
                            Thread.sleep(1000);
                            if (!UpdateController.getInstance().isRunning()) {
                                break;
                            }
                            UpdateController.getInstance().waitForUpdate();
                        }
                        // boolean installed = UpdateController.getInstance().isExtensionInstalled(id);
                        final boolean pending = UpdateController.getInstance().hasPendingUpdates();
                        //
                        if (UIOManager.I().showConfirmDialog(0, "Install Extension " + toInstall, _GUI.T.UninstalledExtension_waiting_for_restart(), new AbstractIcon(IconKey.ICON_RESTART, 32), _GUI.T.lit_restart_now(), _GUI.T.lit_later())) {
                            RestartController.getInstance().asyncRestart(new SmartRlyRestartRequest(true));
                        }
                    } catch (Exception e) {
                        UIOManager.I().showException(_GUI.T.lit_error_occured(), e);
                    } finally {
                        UpdateController.getInstance().getEventSender().removeListener(listener);
                    }
                }
            };
        }.start();
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
}
