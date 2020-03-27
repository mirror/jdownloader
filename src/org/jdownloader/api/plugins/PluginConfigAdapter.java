package org.jdownloader.api.plugins;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.StringUtils;
import org.jdownloader.api.config.AdvancedConfigQueryStorable;
import org.jdownloader.myjdownloader.client.bindings.AdvancedConfigEntryDataStorable.AbstractType;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class PluginConfigAdapter {
    private static final String   OLD_CONFIG_PREFIX = "deprecated";
    private final LazyPlugin<?>   lazyPlugin;
    private PluginConfigInterface config;
    private ConfigContainer       oldConfig;

    public PluginConfigAdapter(LazyPlugin<?> lazyPlugin) throws ClassNotFoundException {
        if (lazyPlugin == null) {
            throw new ClassNotFoundException();
        } else {
            this.lazyPlugin = lazyPlugin;
        }
    }

    public LazyPlugin<?> getLazyPlugin() {
        return lazyPlugin;
    }

    public PluginConfigAdapter(String interfaceName, String displayName) throws ClassNotFoundException {
        if (interfaceName.contains("jd.plugins.hoster")) {
            this.lazyPlugin = HostPluginController.getInstance().get(displayName);
        } else if (interfaceName.contains("jd.plugins.decrypter")) {
            this.lazyPlugin = CrawlerPluginController.getInstance().get(displayName);
        } else if (interfaceName.startsWith("org.jdownloader.plugins.components")) {
            LazyPlugin<?> lazyPlugin = HostPluginController.getInstance().get(displayName);
            if (lazyPlugin != null && StringUtils.equals(((LazyHostPlugin) lazyPlugin).getConfigInterface(), interfaceName)) {
                this.lazyPlugin = lazyPlugin;
            } else {
                lazyPlugin = CrawlerPluginController.getInstance().get(displayName);
                if (lazyPlugin != null && StringUtils.equals(((LazyCrawlerPlugin) lazyPlugin).getConfigInterface(), interfaceName)) {
                    this.lazyPlugin = lazyPlugin;
                } else {
                    this.lazyPlugin = null;
                }
            }
        } else {
            this.lazyPlugin = null;
        }
        if (lazyPlugin == null) {
            throw new ClassNotFoundException("interfaceName:" + interfaceName + "|displayName:" + displayName);
        }
    }

    private boolean initialized = false;

    private synchronized void init() {
        if (!initialized) {
            try {
                final String configInterfaceName = getConfigInterface();
                if (configInterfaceName == null) {
                    final Plugin plugin = getLazyPlugin().getPrototype(null);
                    this.oldConfig = plugin.getConfig();
                } else {
                    final PluginClassLoaderChild classLoader = PluginClassLoader.getInstance().getChild();
                    final Class<? extends PluginConfigInterface> configInterface = (Class<? extends PluginConfigInterface>) classLoader.loadClass(configInterfaceName);
                    this.config = PluginJsonConfig.get(configInterface);
                }
            } catch (Throwable e) {
                throw new WTFException(e);
            } finally {
                initialized = true;
            }
        }
    }

    private String getConfigInterface() {
        final LazyPlugin<?> lazyPlugin = getLazyPlugin();
        if (isHostPlugin()) {
            return ((LazyHostPlugin) lazyPlugin).getConfigInterface();
        } else if (isCrawlerPlugin()) {
            return ((LazyCrawlerPlugin) lazyPlugin).getConfigInterface();
        } else {
            return null;
        }
    }

    public boolean isOldConfig() {
        return getConfigInterface() == null;
    }

    public boolean hasConfig() {
        final LazyPlugin<?> lazyPlugin = getLazyPlugin();
        if (isHostPlugin()) {
            return ((LazyHostPlugin) lazyPlugin).isHasConfig() || ((LazyHostPlugin) lazyPlugin).getConfigInterface() != null;
        } else if (isCrawlerPlugin()) {
            return ((LazyCrawlerPlugin) lazyPlugin).isHasConfig() || ((LazyCrawlerPlugin) lazyPlugin).getConfigInterface() != null;
        } else {
            return false;
        }
    }

    public boolean isHostPlugin() {
        return getLazyPlugin() instanceof LazyHostPlugin;
    }

    public boolean isCrawlerPlugin() {
        return getLazyPlugin() instanceof LazyCrawlerPlugin;
    }

    private ConfigEntry getConfigEntry(String key) {
        if (this.oldConfig != null && this.oldConfig.getEntries().size() > 0) {
            for (ConfigEntry entry : this.oldConfig.getEntries()) {
                if (entry.getPropertyName() != null && entry.getPropertyName().equals(key)) {
                    return entry;
                }
            }
        }
        return null;
    }

    public Object getValue(String key) throws BadParameterException {
        if (!StringUtils.isEmpty(key)) {
            init();
            if (this.isOldConfig()) {
                final ConfigEntry entry = getConfigEntry(key);
                if (entry != null) {
                    return getConfigEntryValue(entry);
                }
            } else if (config != null) {
                final KeyHandler<Object> kh = this.config._getStorageHandler().getKeyHandler(key);
                if (kh != null) {
                    return kh.getValue();
                }
            }
        }
        throw new BadParameterException("no matching config entry");
    }

    public Object getDefaultValue(String key) throws BadParameterException {
        if (!StringUtils.isEmpty(key)) {
            init();
            if (this.isOldConfig()) {
                final ConfigEntry entry = getConfigEntry(key);
                if (entry != null) {
                    return getConfigEntryDefaultValue(entry);
                }
            } else if (config != null) {
                final KeyHandler<Object> keyHandler = this.config._getStorageHandler().getKeyHandler(key);
                if (keyHandler != null) {
                    return keyHandler.getDefaultValue();
                }
            }
        }
        throw new BadParameterException("no matching config entry:" + key);
    }

    public boolean setValue(final String key, Object value) {
        if (!StringUtils.isEmpty(key)) {
            if (value instanceof String) {
                value = StringUtils.nullify((String) value);
            }
            init();
            if (isOldConfig()) {
                final ConfigEntry entry = getConfigEntry(key);
                if (entry != null) {
                    final AbstractType type = getAbstractTypeFromConfigType(entry.getType());
                    if (type != null) {
                        switch (type) {
                        case ENUM:
                            // Special Handling -> enum Value(String) to index
                            final Object[] values = entry.getList();
                            if (values != null) {
                                final int index = Arrays.asList(values).indexOf(value);
                                if (index != -1) {
                                    entry.getPropertyInstance().setProperty(key, index);
                                    return true;
                                } else if (value == null) {
                                    entry.getPropertyInstance().removeProperty(key);
                                    return true;
                                }
                            }
                            return false;
                        case LONG:
                            if (value instanceof String) {
                                value = Long.parseLong((String) value);
                            }
                            if (value instanceof Number) {
                                long num = ((Number) value).longValue();
                                if (num < entry.getStart()) {
                                    num = entry.getStart();
                                } else if (num > entry.getEnd()) {
                                    num = entry.getEnd();
                                }
                                entry.getPropertyInstance().setProperty(key, num);
                            } else {
                                entry.getPropertyInstance().removeProperty(key);
                            }
                            return true;
                        case BOOLEAN:
                            if (value instanceof Boolean) {
                                entry.getPropertyInstance().setProperty(key, value);
                            } else if (value instanceof String) {
                                entry.getPropertyInstance().setProperty(key, StringUtils.equalsIgnoreCase("true", (String) value));
                            } else {
                                entry.getPropertyInstance().removeProperty(key);
                            }
                            return true;
                        default:
                            entry.getPropertyInstance().setProperty(key, value);
                            return true;
                        }
                    }
                }
                return false;
            } else if (config != null) {
                final KeyHandler<Object> keyHandler = this.config._getStorageHandler().getKeyHandler(key);
                if (keyHandler != null) {
                    final Type rc = keyHandler.getRawType();
                    final String json = JSonStorage.serializeToJson(value);
                    final TypeRef<Object> type = new TypeRef<Object>(rc) {
                    };
                    final Object v = JSonStorage.stringToObject(json, type, null);
                    keyHandler.setValue(v);
                    return true;
                }
            }
        }
        return false;
    }

    public List<PluginConfigEntryAPIStorable> listConfigEntries(AdvancedConfigQueryStorable query) throws UpdateRequiredClassNotFoundException {
        final List<PluginConfigEntryAPIStorable> result = new ArrayList<PluginConfigEntryAPIStorable>();
        if (!hasConfig()) {
            return result;
        }
        final LazyPlugin<?> lazyPlugin = getLazyPlugin();
        boolean configInterfaceMatch = query == null || query.getConfigInterface() == null || (query.getConfigInterface().equals(OLD_CONFIG_PREFIX + "." + lazyPlugin.getClassName()) || (getConfigInterface() != null && query.getConfigInterface().equals(getConfigInterface())));
        if (query == null) {
            query = new AdvancedConfigQueryStorable();
        }
        if (!configInterfaceMatch) {
            return result;
        } else {
            init();
            Pattern cPat = null;
            if (!StringUtils.isEmpty(query.getPattern())) {
                cPat = Pattern.compile(query.getPattern(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            }
            if (isOldConfig()) {
                for (final ConfigEntry entry : oldConfig.getEntries()) {
                    final String check = lazyPlugin.getClassName() + "." + lazyPlugin.getDisplayName() + "." + entry.getPropertyName();
                    if (cPat != null && !cPat.matcher(check).matches()) {
                        continue;
                    } else {
                        final PluginConfigEntryAPIStorable storable = createAPIStorable(entry, query);
                        if (storable != null) {
                            result.add(storable);
                        }
                    }
                }
            } else {
                for (final KeyHandler<?> keyHandler : config._getStorageHandler().getKeyHandler()) {
                    final String check = lazyPlugin.getClassName() + "." + lazyPlugin.getDisplayName() + "." + keyHandler.getKey();
                    if (cPat != null && !cPat.matcher(check).matches()) {
                        continue;
                    } else {
                        final AdvancedConfigEntry entry = new AdvancedConfigEntry(config, keyHandler);
                        final PluginConfigEntryAPIStorable storable = createAPIStorable(entry, query);
                        if (storable != null) {
                            result.add(storable);
                        }
                    }
                }
            }
        }
        return result;
    }

    public PluginConfigEntryAPIStorable createAPIStorable(final AdvancedConfigEntry entry, final AdvancedConfigQueryStorable query) {
        final PluginConfigEntryAPIStorable storable = new PluginConfigEntryAPIStorable(entry, query);
        storable.setStorage(getLazyPlugin().getDisplayName());
        return storable;
    }

    private String getEnumDefault(ConfigEntry entry) {
        final Object[] values = entry.getList();
        if (values != null && values.length > 0) {
            final Object defaultValue = entry.getDefaultValue();
            if (defaultValue instanceof String && Arrays.asList(values).contains(defaultValue)) {
                return (String) defaultValue;
            } else if (defaultValue instanceof Number && values.length > ((Number) defaultValue).intValue()) {
                return String.valueOf(values[((Number) defaultValue).intValue()]);
            } else {
                return String.valueOf(values[0]);
            }
        }
        return null;
    }

    private String getEnumValue(ConfigEntry entry) {
        final Object[] values = entry.getList();
        if (values != null && values.length > 0) {
            final Object value = entry.getPropertyInstance().getProperty(entry.getPropertyName());
            if (value instanceof String && Arrays.asList(values).contains(value)) {
                return (String) value;
            } else if (value instanceof Number && values.length > ((Number) value).intValue()) {
                return String.valueOf(values[((Number) value).intValue()]);
            } else {
                return getEnumDefault(entry);
            }
        }
        return null;
    }

    private Object getConfigEntryValue(ConfigEntry entry) {
        final AbstractType type = getAbstractTypeFromConfigType(entry.getType());
        if (type != null) {
            final String key = entry.getPropertyName();
            if (entry.getPropertyInstance().hasProperty(key)) {
                switch (type) {
                case BOOLEAN:
                    return entry.getPropertyInstance().getBooleanProperty(key);
                case LONG:
                    return entry.getPropertyInstance().getLongProperty(key, -1l);
                case STRING:
                    return entry.getPropertyInstance().getStringProperty(key);
                case ENUM:
                    return getEnumValue(entry);
                default:
                    return entry.getPropertyInstance().getProperty(key);
                }
            } else {
                return getConfigEntryDefaultValue(entry);
            }
        }
        return null;
    }

    private Object getConfigEntryDefaultValue(ConfigEntry entry) {
        final AbstractType type = getAbstractTypeFromConfigType(entry.getType());
        if (type != null) {
            switch (type) {
            case ENUM:
                return getEnumDefault(entry);
            case BOOLEAN:
            case LONG:
            case STRING:
            default:
                return entry.getDefaultValue();
            }
        }
        return null;
    }

    public PluginConfigEntryAPIStorable createAPIStorable(ConfigEntry entry, AdvancedConfigQueryStorable query) {
        final LazyPlugin<?> lazyPlugin = getLazyPlugin();
        final AbstractType type = getAbstractTypeFromConfigType(entry.getType());
        if (type != null) {
            final PluginConfigEntryAPIStorable storable = new PluginConfigEntryAPIStorable();
            storable.setInterfaceName(OLD_CONFIG_PREFIX + "." + lazyPlugin.getClassName());
            storable.setKey(entry.getPropertyName());
            storable.setAbstractType(type);
            storable.setStorage(lazyPlugin.getDisplayName());
            if (query.isDescription()) {
                storable.setDocs(entry.getLabel());
            }
            if (query.isValues()) {
                storable.setValue(getConfigEntryValue(entry));
            }
            if (query.isDefaultValues()) {
                storable.setDefaultValue(getConfigEntryDefaultValue(entry));
            }
            if (query.isEnumInfo()) {
                final Object[] list = entry.getList();
                if (list != null && list.length > 0) {
                    String[][] options = new String[list.length][1];
                    for (int i = 0; i < list.length; i++) {
                        options[i][0] = String.valueOf(list[i]);
                    }
                    storable.setEnumOptions(options);
                }
            }
            return storable;
        }
        return null;
    }

    public boolean resetValue(String key) throws BadParameterException {
        return setValue(key, this.getDefaultValue(key));
    }

    private AbstractType getAbstractTypeFromConfigType(final int configContainerType) {
        switch (configContainerType) {
        case ConfigContainer.TYPE_CHECKBOX:
            return AbstractType.BOOLEAN;
        case ConfigContainer.TYPE_SPINNER:
            return AbstractType.LONG;
        case ConfigContainer.TYPE_COMBOBOX_INDEX:
            return AbstractType.ENUM;
        case ConfigContainer.TYPE_TEXTFIELD:
        case ConfigContainer.TYPE_TEXTAREA:
        case ConfigContainer.TYPE_PASSWORDFIELD:
            return AbstractType.STRING;
        default:
            return null;
        }
    }
}
