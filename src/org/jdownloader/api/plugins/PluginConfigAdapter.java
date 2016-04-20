package org.jdownloader.api.plugins;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ConfigInterface;
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
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class PluginConfigAdapter {
    private static final String OLD_CONFIG_PREFIX = "deprecated";

    private Plugin              plugin;
    private LazyPlugin          lazyPlugin;
    private ConfigInterface     config;
    private ConfigContainer     oldConfig;

    public PluginConfigAdapter(LazyPlugin lazyPlugin) throws ClassNotFoundException {
        this.lazyPlugin = lazyPlugin;
        this.plugin = lazyPlugin.getPrototype(null);
        initConfig();
    }

    private void initConfig() throws ClassNotFoundException {
        if (this.isOldConfig()) {
            try {
                this.oldConfig = this.plugin.getConfig();
            } catch (WTFException e) {
                e.printStackTrace();
                throw new ClassNotFoundException();
            }
        } else {
            PluginClassLoaderChild classLoader = PluginClassLoader.getInstance().getChild();
            Class<? extends PluginConfigInterface> configInterface = (Class<? extends PluginConfigInterface>) classLoader.loadClass(plugin.getConfigInterface().getName());
            this.config = PluginJsonConfig.get(configInterface);
        }
    }

    public PluginConfigAdapter(String interfaceName, String displayName) throws ClassNotFoundException {
        if (interfaceName.contains("jd.plugins.hoster")) {
            this.lazyPlugin = HostPluginController.getInstance().get(displayName);
        } else if (interfaceName.contains("jd.plugins.decrypter")) {
            this.lazyPlugin = CrawlerPluginController.getInstance().get(displayName);
        }
        if (this.lazyPlugin != null) {
            this.plugin = this.lazyPlugin.getPrototype(null);
        } else {
            throw new ClassNotFoundException();
        }
        initConfig();
    }

    public boolean isOldConfig() {
        return this.plugin.getConfigInterface() == null;
    }

    public boolean isHostPlugin() {
        return this.plugin instanceof PluginForHost;
    }

    public boolean isCrawlerPlugin() {
        return this.plugin instanceof PluginForDecrypt;
    }

    public Object getValue(String key) throws BadParameterException {
        if (this.isOldConfig()) {
            if (this.oldConfig != null && this.oldConfig.getEntries().size() > 0) {
                for (ConfigEntry entry : this.oldConfig.getEntries()) {
                    if (entry.getPropertyName() != null && entry.getPropertyName().equals(key)) {
                        return entry.getPropertyInstance().getProperty(key);
                    }
                }
            }
        } else {
            KeyHandler<Object> kh = this.config._getStorageHandler().getKeyHandler(key);
            return kh.getValue();
        }

        throw new BadParameterException("no matching config entry");
    }

    public Object getDefaultValue(String key) throws BadParameterException {
        if (this.isOldConfig()) {
            if (this.oldConfig != null && this.oldConfig.getEntries().size() > 0) {
                for (ConfigEntry entry : this.oldConfig.getEntries()) {
                    if (entry.getPropertyName() != null && entry.getPropertyName().equals(key)) {
                        return entry.getDefaultValue();
                    }
                }
            }
        } else {
            KeyHandler<Object> kh = this.config._getStorageHandler().getKeyHandler(key);
            return kh.getDefaultValue();
        }

        throw new BadParameterException("no matching config entry");
    }

    public boolean setValue(String key, Object value) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        if (isOldConfig() && this.oldConfig.getEntries() != null) {
            for (ConfigEntry entry : this.oldConfig.getEntries()) {
                if (entry.getPropertyName() != null && entry.getPropertyName().equals(key)) {
                    entry.getPropertyInstance().setProperty(key, value);
                    return true;
                }
            }
        } else {
            KeyHandler<Object> kh = this.config._getStorageHandler().getKeyHandler(key);
            Type rc = kh.getRawType();
            String json = JSonStorage.serializeToJson(value);
            TypeRef<Object> type = new TypeRef<Object>(rc) {
            };
            Object v = JSonStorage.stringToObject(json, type, null);
            kh.setValue(v);
            return true;
        }
        return false;
    }

    public List<PluginConfigEntryAPIStorable> listConfigEntries(AdvancedConfigQueryStorable query) throws UpdateRequiredClassNotFoundException {
        List<PluginConfigEntryAPIStorable> result = new ArrayList<PluginConfigEntryAPIStorable>();

        boolean configInterfaceMatch = query == null || query.getConfigInterface() == null || (query.getConfigInterface().equals(OLD_CONFIG_PREFIX + "." + lazyPlugin.getClassName()) || (plugin.getConfigInterface() != null && query.getConfigInterface().equals(plugin.getConfigInterface().getName())));
        if (query == null) {
            query = new AdvancedConfigQueryStorable();
        }
        if (!configInterfaceMatch) {
            return result;
        } else {
            Pattern cPat = null;
            if (!StringUtils.isEmpty(query.getPattern())) {
                cPat = Pattern.compile(query.getPattern(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            }
            if (isOldConfig()) {
                for (final ConfigEntry entry : oldConfig.getEntries()) {
                    if (cPat != null && !cPat.matcher(this.plugin.getClass().getName() + "." + this.lazyPlugin.getDisplayName() + "." + entry.getPropertyName()).matches()) {
                        continue;
                    } else {
                        PluginConfigEntryAPIStorable storable = createAPIStorable(entry, query);
                        if (storable != null) {
                            result.add(storable);
                        }
                    }
                }
            } else {
                final PluginConfigInterface cfg = PluginJsonConfig.get((Class<PluginConfigInterface>) this.plugin.getConfigInterface());
                final HashMap<Method, KeyHandler<?>> khMap = cfg._getStorageHandler().getMap();
                final HashSet<KeyHandler<?>> dupeMap = new HashSet<KeyHandler<?>>();
                for (KeyHandler keyHandler : khMap.values()) {
                    if (cPat != null && !cPat.matcher(keyHandler.getStorageHandler().getConfigInterface().getName() + "." + this.lazyPlugin.getDisplayName() + "." + keyHandler.getKey()).matches()) {
                        continue;
                    } else {
                        if (dupeMap.add(keyHandler)) {
                            final AdvancedConfigEntry entry = new AdvancedConfigEntry(cfg, keyHandler);
                            final PluginConfigEntryAPIStorable storable = createAPIStorable(entry, query);
                            if (storable != null) {
                                result.add(storable);
                            }
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

    private AbstractType getAbstractTypeFromConfigType(int configContainerType) {
        if (configContainerType == ConfigContainer.TYPE_CHECKBOX) {
            return AbstractType.BOOLEAN;
        } else if (configContainerType == ConfigContainer.TYPE_COMBOBOX || configContainerType == ConfigContainer.TYPE_SPINNER || configContainerType == ConfigContainer.TYPE_COMBOBOX_INDEX || configContainerType == ConfigContainer.TYPE_RADIOFIELD) {
            return AbstractType.OBJECT_LIST;
        } else if (configContainerType == ConfigContainer.TYPE_PASSWORDFIELD || configContainerType == ConfigContainer.TYPE_TEXTFIELD || configContainerType == ConfigContainer.TYPE_TEXTAREA) {
            return AbstractType.STRING;
        }
        return null;
    }
}
