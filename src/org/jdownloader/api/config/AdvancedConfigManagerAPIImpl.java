package org.jdownloader.api.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.swing.Icon;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;

import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.reflection.Clazz;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.myjdownloader.MyJDownloaderHttpConnection;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.extensions.OptionalExtension;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.myjdownloader.client.bindings.AdvancedConfigEntryDataStorable.AbstractType;
import org.jdownloader.myjdownloader.client.bindings.interfaces.AdvancedConfigInterface;
import org.jdownloader.myjdownloader.client.json.SessionInfoResponse;
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
    public boolean set(final RemoteAPIRequest request, String interfaceName, String storage, String key, Object value) throws InvalidValueException {
        if (EXTENSION.equals(interfaceName)) {
            final Boolean setValue;
            try {
                final String jsonString = JSonStorage.serializeToJson(value);
                setValue = JSonStorage.restoreFromString(jsonString, TypeRef.BOOLEAN);
                if (setValue == null) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
            for (final LazyExtension ext : ExtensionController.getInstance().getExtensions()) {
                if (createExtensionToggleDummyKey("Enable", ext).equals(key)) {
                    try {
                        ext._setEnabled(Boolean.TRUE.equals(setValue));
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
            if (key != null && key.startsWith("InstallExtension")) {
                final String toInstall = StringUtils.toLowerCaseOrNull(key.substring("InstallExtension".length()));
                installExtension(toInstall, Boolean.TRUE.equals(setValue));
                return true;
            } else {
                return false;
            }
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
                final GenericConfigEventListener<Object> listener = createRestartRequiredConfigEventListener(request, keyHandler);
                keyHandler.getEventSender().addListener(listener);
                try {
                    keyHandler.setValue(setValue);
                } finally {
                    keyHandler.getEventSender().removeListener(listener);
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    protected boolean isMyJDownloaderWebinterface(final RemoteAPIRequest request) {
        final MyJDownloaderHttpConnection con = MyJDownloaderHttpConnection.getMyJDownloaderHttpConnection(request);
        final SessionInfoResponse sessionInfo = con != null ? con.getSessionInfo() : null;
        return sessionInfo != null && StringUtils.startsWithCaseInsensitive(sessionInfo.getAppKey(), "myjd_webinterface");
    }

    protected GenericConfigEventListener<Object> createRestartRequiredConfigEventListener(final RemoteAPIRequest request, final KeyHandler<Object> keyHandler) {
        if (isMyJDownloaderWebinterface(request) && keyHandler.getAnnotation(RequiresRestart.class) != null) {
            return new GenericConfigEventListener<Object>() {
                @Override
                public void onConfigValueModified(final KeyHandler<Object> keyHandler, final Object newValue) {
                    if (JDGui.bugme(WarnLevel.NORMAL)) {
                        new Thread("RestartRequired:" + keyHandler.getKey()) {
                            {
                                setDaemon(true);
                            }

                            @Override
                            public void run() {
                                final ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.BUTTONS_HIDE_CANCEL, _GUI.T.AdvancedConfigEntry_setValue_restart_warning_title(keyHandler.getKey()), _GUI.T.AdvancedConfigEntry_setValue_restart_warning(keyHandler.getKey()), NewTheme.I().getIcon(IconKey.ICON_WARNING, 32), null, null) {
                                    @Override
                                    public String getDontShowAgainKey() {
                                        return "RestartRequiredAdvancedConfig_" + getKey();
                                    }
                                };
                                d.show();
                            }
                        }.start();
                    }
                }

                private String getHandlerKey() {
                    return keyHandler.getKey();
                }

                private String getKey() {
                    return getConfigInterfaceName().concat(".").concat(getHandlerKey());
                }

                private String getConfigInterfaceName() {
                    String ret = keyHandler.getStorageHandler().getConfigInterface().getSimpleName();
                    if (ret.contains("Config")) {
                        ret = ret.replace("Config", "");
                    }
                    return ret;
                }

                @Override
                public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
                }
            };
        } else {
            return null;
        }
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
    public boolean reset(RemoteAPIRequest request, String interfaceName, String storage, String key) {
        final KeyHandler<Object> keyHandler = getKeyHandler(interfaceName, storage, key);
        if (keyHandler == null) {
            return false;
        } else {
            try {
                final GenericConfigEventListener<Object> listener = createRestartRequiredConfigEventListener(request, keyHandler);
                keyHandler.getEventSender().addListener(listener);
                try {
                    keyHandler.setValue(keyHandler.getDefaultValue());
                } finally {
                    keyHandler.getEventSender().removeListener(listener);
                }
            } catch (ValidationException e) {
                return false;
            }
            return true;
        }
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
        for (final OptionalExtension ext : ExtensionController.getInstance().getOptionalExtensions()) {
            final AdvancedConfigAPIEntry entry = new AdvancedConfigAPIEntry();
            entry.setInterfaceName(EXTENSION);
            entry.setAbstractType(AbstractType.BOOLEAN);
            if (query.isDefaultValues()) {
                entry.setDefaultValue(false);
            }
            if (query.isDescription()) {
                entry.setDocs("Install Extension: " + ext.getName());
            }
            final String dummyKey = "InstallExtension" + StringUtils.toUpperCaseOrNull(ext.getExtensionID());
            entry.setKey(dummyKey);
            if (query.isValues()) {
                entry.setValue(ext.isInstalled());
            }
            ret.add(entry);
        }
        for (final LazyExtension ext : ExtensionController.getInstance().getExtensions()) {
            final AdvancedConfigAPIEntry entry = new AdvancedConfigAPIEntry();
            entry.setInterfaceName(EXTENSION);
            entry.setAbstractType(AbstractType.BOOLEAN);
            if (query.isDefaultValues()) {
                entry.setDefaultValue(false);
            }
            if (query.isDescription()) {
                entry.setDocs("Enable/Disable Extension: " + ext.getName());
            }
            final String dummyKey = createExtensionToggleDummyKey("Enable", ext);
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
        final int lastP = ext.getClassname().lastIndexOf(".");
        final String dummyKey = namespace + ext.getClassname().substring(lastP + 1);
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

    private void installExtension(final String toInstall, final boolean installFlag) {
        final UpdateController controller = UpdateController.getInstance();
        if (StringUtils.isNotEmpty(toInstall) && controller.isHandlerSet()) {
            new Thread("Install Extension:" + toInstall + "|" + installFlag) {
                public void run() {
                    if (installFlag != controller.isExtensionInstalled(toInstall)) {
                        if (UIOManager.I().showConfirmDialog(0, _GUI.T.lit_are_you_sure(), installFlag ? _GUI.T.installExtension_remote_install_rly(toInstall) : _GUI.T.installExtension_remote_remove_rly(toInstall))) {
                            UpdaterListener listener = null;
                            try {
                                final AtomicLong last = new AtomicLong(Time.systemIndependentCurrentJVMTimeMillis());
                                controller.getEventSender().addListener(listener = new UpdaterListener() {
                                    @Override
                                    public void onUpdatesAvailable(boolean selfupdate, InstallLog installlog) {
                                    }

                                    @Override
                                    public void onUpdaterStatusUpdate(final String label, Icon icon, final double p) {
                                        if (System.currentTimeMillis() - last.get() > 5000) {
                                            if (installFlag) {
                                                UIOManager.I().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI.T.installExtension_remote_install_title(toInstall), _GUI.T.installExtension_remote_install_wait(), new AbstractIcon(IconKey.ICON_WAIT, 32), _GUI.T.lit_continue(), null);
                                            } else {
                                                UIOManager.I().showConfirmDialog(UIOManager.BUTTONS_HIDE_CANCEL, _GUI.T.installExtension_remote_remove_title(toInstall), _GUI.T.installExtension_remote_remove_wait(), new AbstractIcon(IconKey.ICON_WAIT, 32), _GUI.T.lit_continue(), null);
                                            }
                                            last.set(Time.systemIndependentCurrentJVMTimeMillis());
                                        }
                                    }
                                });
                                if (installFlag) {
                                    controller.runExtensionInstallation(toInstall);
                                } else {
                                    controller.runExtensionUnInstallation(toInstall);
                                }
                                while (true) {
                                    Thread.sleep(1000);
                                    if (!controller.isRunning()) {
                                        break;
                                    } else {
                                        controller.waitForUpdate();
                                    }
                                }
                                final boolean pending = controller.hasPendingUpdates();
                                boolean restart;
                                if (installFlag) {
                                    restart = UIOManager.I().showConfirmDialog(0, "Install Extension " + toInstall, _GUI.T.UninstalledExtension_waiting_for_restart(), new AbstractIcon(IconKey.ICON_RESTART, 32), _GUI.T.lit_restart_now(), _GUI.T.lit_later());
                                } else {
                                    restart = UIOManager.I().showConfirmDialog(0, "Remove Extension " + toInstall, _GUI.T.InstalledExtension_waiting_for_restart(), new AbstractIcon(IconKey.ICON_RESTART, 32), _GUI.T.lit_restart_now(), _GUI.T.lit_later());
                                }
                                if (restart) {
                                    RestartController.getInstance().asyncRestart(new SmartRlyRestartRequest(true));
                                }
                            } catch (Exception e) {
                                UIOManager.I().showException(_GUI.T.lit_error_occured(), e);
                            } finally {
                                controller.getEventSender().removeListener(listener);
                            }
                        }
                    }
                };
            }.start();
        }
    }
}
