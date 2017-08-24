package org.jdownloader.api.plugins;

import java.lang.reflect.Type;
import java.util.ArrayList;
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
        this.lazyPlugin = lazyPlugin;
        if (this.lazyPlugin == null) {
            throw new ClassNotFoundException();
        }
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
            initialized = true;
            try {
                final String configInterfaceName = getConfigInterface();
                if (configInterfaceName == null) {
                    final Plugin plugin = lazyPlugin.getPrototype(null);
                    this.oldConfig = plugin.getConfig();
                } else {
                    final PluginClassLoaderChild classLoader = PluginClassLoader.getInstance().getChild();
                    final Class<? extends PluginConfigInterface> configInterface = (Class<? extends PluginConfigInterface>) classLoader.loadClass(configInterfaceName);
                    this.config = PluginJsonConfig.get(configInterface);
                }
            } catch (Throwable e) {
                throw new WTFException(e);
            }
        }
    }

    private String getConfigInterface() {
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
        if (isHostPlugin()) {
            return ((LazyHostPlugin) lazyPlugin).isHasConfig() || ((LazyHostPlugin) lazyPlugin).getConfigInterface() != null;
        } else if (isCrawlerPlugin()) {
            return ((LazyCrawlerPlugin) lazyPlugin).isHasConfig() || ((LazyCrawlerPlugin) lazyPlugin).getConfigInterface() != null;
        } else {
            return false;
        }
    }

    public boolean isHostPlugin() {
        return this.lazyPlugin instanceof LazyHostPlugin;
    }

    public boolean isCrawlerPlugin() {
        return this.lazyPlugin instanceof LazyCrawlerPlugin;
    }

    public Object getValue(String key) throws BadParameterException {
        if (!StringUtils.isEmpty(key)) {
            init();
            if (this.isOldConfig()) {
                if (this.oldConfig != null && this.oldConfig.getEntries().size() > 0) {
                    for (ConfigEntry entry : this.oldConfig.getEntries()) {
                        if (entry.getPropertyName() != null && entry.getPropertyName().equals(key)) {
                            return entry.getPropertyInstance().getProperty(key);
                        }
                    }
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
                if (this.oldConfig != null && this.oldConfig.getEntries().size() > 0) {
                    for (ConfigEntry entry : this.oldConfig.getEntries()) {
                        if (entry.getPropertyName() != null && entry.getPropertyName().equals(key)) {
                            return entry.getDefaultValue();
                        }
                    }
                }
            } else if (config != null) {
                final KeyHandler<Object> kh = this.config._getStorageHandler().getKeyHandler(key);
                if (kh != null) {
                    return kh.getDefaultValue();
                }
            }
        }
        throw new BadParameterException("no matching config entry:" + key);
    }

    public boolean setValue(String key, Object value) {
        if (!StringUtils.isEmpty(key)) {
            init();
            if (isOldConfig()) {
                if (this.oldConfig != null && this.oldConfig.getEntries().size() > 0) {
                    for (ConfigEntry entry : this.oldConfig.getEntries()) {
                        if (entry.getPropertyName() != null && entry.getPropertyName().equals(key)) {
                            entry.getPropertyInstance().setProperty(key, value);
                            return true;
                        }
                    }
                }
            } else if (config != null) {
                final KeyHandler<Object> kh = this.config._getStorageHandler().getKeyHandler(key);
                if (kh != null) {
                    final Type rc = kh.getRawType();
                    final String json = JSonStorage.serializeToJson(value);
                    final TypeRef<Object> type = new TypeRef<Object>(rc) {
                    };
                    final Object v = JSonStorage.stringToObject(json, type, null);
                    kh.setValue(v);
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
                    final String check = lazyPlugin.getClassName() + "." + this.lazyPlugin.getDisplayName() + "." + entry.getPropertyName();
                    if (cPat != null && !cPat.matcher(check).matches()) {
                        continue;
                    } else {
                        PluginConfigEntryAPIStorable storable = createAPIStorable(entry, query);
                        if (storable != null) {
                            result.add(storable);
                        }
                    }
                }
            } else {
                for (final KeyHandler<?> keyHandler : config._getStorageHandler().getKeyHandler()) {
                    final String check = lazyPlugin.getClassName() + "." + this.lazyPlugin.getDisplayName() + "." + keyHandler.getKey();
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
        storable.setStorage(this.lazyPlugin.getDisplayName());
        return storable;
    }

    public PluginConfigEntryAPIStorable createAPIStorable(ConfigEntry entry, AdvancedConfigQueryStorable query) {
        final AbstractType type = getAbstractTypeFromConfigType(entry.getType());
        if (type != null) {
            final PluginConfigEntryAPIStorable storable = new PluginConfigEntryAPIStorable();
            storable.setInterfaceName(OLD_CONFIG_PREFIX + "." + this.lazyPlugin.getClassName());
            storable.setKey(entry.getPropertyName());
            storable.setAbstractType(type);
            storable.setStorage(this.lazyPlugin.getDisplayName());
            if (query.isDescription()) {
                storable.setDocs(entry.getLabel());
            }
            if (query.isValues()) {
                switch (type) {
                case BOOLEAN:
                    storable.setValue(entry.getPropertyInstance().getBooleanProperty(entry.getPropertyName()));
                    break;
                case INT:
                    storable.setValue(entry.getPropertyInstance().getIntegerProperty(entry.getPropertyName()));
                    break;
                case LONG:
                    storable.setValue(entry.getPropertyInstance().getLongProperty(entry.getPropertyName(), -1l));
                    break;
                case STRING:
                    storable.setValue(entry.getPropertyInstance().getStringProperty(entry.getPropertyName()));
                    break;
                default:
                    storable.setValue(entry.getPropertyInstance().getProperty(entry.getPropertyName()));
                }
            }
            if (query.isDefaultValues()) {
                storable.setDefaultValue(entry.getDefaultValue());
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
        if (configContainerType == ConfigContainer.TYPE_CHECKBOX) {
            return AbstractType.BOOLEAN;
        } else if (configContainerType == ConfigContainer.TYPE_COMBOBOX || configContainerType == ConfigContainer.TYPE_SPINNER || configContainerType == ConfigContainer.TYPE_COMBOBOX_INDEX || configContainerType == ConfigContainer.TYPE_RADIOFIELD) {
            return AbstractType.OBJECT_LIST;
        } else if (configContainerType == ConfigContainer.TYPE_PASSWORDFIELD || configContainerType == ConfigContainer.TYPE_TEXTFIELD || configContainerType == ConfigContainer.TYPE_TEXTAREA) {
            return AbstractType.STRING;
        } else {
            return null;
        }
    }
}
