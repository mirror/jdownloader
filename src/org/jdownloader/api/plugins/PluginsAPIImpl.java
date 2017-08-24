package org.jdownloader.api.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appwork.exceptions.WTFException;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.utils.StringUtils;
import org.jdownloader.api.config.AdvancedConfigQueryStorable;
import org.jdownloader.api.config.InvalidValueException;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public class PluginsAPIImpl implements PluginsAPI {
    @Override
    public List<String> getPluginRegex(String URL) {
        List<String> ret = new ArrayList<String>();
        if (StringUtils.isNotEmpty(URL)) {
            URL = URL.replaceAll("^https?://(www.)?", "");
            for (LazyHostPlugin lhp : HostPluginController.getInstance().list()) {
                if (URL.equals(lhp.getDisplayName())) {
                    ret.add(lhp.getPattern().pattern());
                }
            }
            for (LazyCrawlerPlugin lhp : CrawlerPluginController.getInstance().list()) {
                if (URL.equals(lhp.getDisplayName())) {
                    ret.add(lhp.getPattern().pattern());
                }
            }
        }
        return ret;
    }

    @Override
    public HashMap<String, ArrayList<String>> getAllPluginRegex() {
        final HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
        for (final LazyHostPlugin lhp : HostPluginController.getInstance().list()) {
            ArrayList<String> list = map.get(lhp.getDisplayName());
            if (list == null) {
                list = new ArrayList<String>();
                map.put(lhp.getDisplayName(), list);
            }
            list.add(lhp.getPattern().pattern());
        }
        for (final LazyCrawlerPlugin lhp : CrawlerPluginController.getInstance().list()) {
            ArrayList<String> list = map.get(lhp.getDisplayName());
            if (list == null) {
                list = new ArrayList<String>();
                map.put(lhp.getDisplayName(), list);
            }
            list.add(lhp.getPattern().pattern());
        }
        return map;
    }

    @Override
    public List<PluginAPIStorable> list(final PluginsQueryStorable query) {
        final ArrayList<PluginAPIStorable> result = new ArrayList<PluginAPIStorable>();
        for (final LazyHostPlugin hPlg : HostPluginController.getInstance().list()) {
            try {
                if (hPlg.isHasConfig()) {
                    result.add(createPluginListStorable(hPlg, query));
                }
            } catch (Exception e) {
                throw new WTFException(e);
            }
        }
        for (final LazyCrawlerPlugin cPlg : CrawlerPluginController.getInstance().list()) {
            try {
                if (cPlg.isHasConfig()) {
                    result.add(createPluginListStorable(cPlg, query));
                }
            } catch (Exception e) {
                throw new WTFException(e);
            }
        }
        return result;
    }

    @Override
    public List<PluginConfigEntryAPIStorable> query(final AdvancedConfigQueryStorable query) throws BadParameterException {
        final ArrayList<PluginConfigEntryAPIStorable> result = new ArrayList<PluginConfigEntryAPIStorable>();
        final List<LazyPlugin<?>> plugins = getAllPlugins();
        for (LazyPlugin<?> lazyPlugin : plugins) {
            try {
                final PluginConfigAdapter adapter = new PluginConfigAdapter(lazyPlugin);
                result.addAll(adapter.listConfigEntries(query));
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public boolean set(final String interfaceName, final String displayName, String key, final Object newValue) throws BadParameterException, InvalidValueException {
        if (StringUtils.isEmpty(interfaceName)) {
            throw new BadParameterException("interfaceName is empty");
        }
        if (StringUtils.isEmpty(displayName)) {
            throw new BadParameterException("displayName is empty");
        }
        try {
            final PluginConfigAdapter adapter = new PluginConfigAdapter(interfaceName, displayName);
            return adapter.setValue(key, newValue);
        } catch (ClassNotFoundException e) {
            throw new BadParameterException("interface:" + interfaceName + "|displayName:" + displayName);
        }
    }

    private PluginAPIStorable createPluginListStorable(LazyPlugin<?> lazyPlugin, final PluginsQueryStorable query) {
        final PluginAPIStorable storable = new PluginAPIStorable();
        storable.setClassName(lazyPlugin.getClassName());
        storable.setDisplayName(lazyPlugin.getDisplayName());
        if (query.isPattern()) {
            storable.setPattern(lazyPlugin.getPattern().pattern());
        }
        if (query.isVersion()) {
            storable.setVersion(String.valueOf(lazyPlugin.getVersion()));
        }
        return storable;
    }

    @Override
    public boolean reset(String interfaceName, String displayName, String key) throws BadParameterException, InvalidValueException {
        if (StringUtils.isEmpty(interfaceName)) {
            throw new BadParameterException("interfaceName is empty");
        } else if (StringUtils.isEmpty(displayName)) {
            throw new BadParameterException("displayName is empty");
        } else if (StringUtils.isEmpty(key)) {
            throw new BadParameterException("key is empty");
        }
        try {
            final PluginConfigAdapter adapter = new PluginConfigAdapter(interfaceName, displayName);
            return adapter.resetValue(key);
        } catch (ClassNotFoundException e1) {
            throw new BadParameterException("interface:" + interfaceName + "|displayName:" + displayName);
        }
    }

    @Override
    public Object get(String interfaceName, String displayName, String key) throws BadParameterException {
        try {
            final PluginConfigAdapter wrapper = new PluginConfigAdapter(interfaceName, displayName);
            return wrapper.getValue(key);
        } catch (ClassNotFoundException e) {
            throw new BadParameterException("no matching config entry");
        }
    }

    private List<LazyPlugin<?>> getAllPlugins() {
        final ArrayList<LazyPlugin<?>> results = new ArrayList<LazyPlugin<?>>();
        results.addAll(HostPluginController.getInstance().list());
        results.addAll(CrawlerPluginController.getInstance().list());
        return results;
    }
}