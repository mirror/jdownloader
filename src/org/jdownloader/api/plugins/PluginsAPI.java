package org.jdownloader.api.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.storage.config.annotations.AllowStorage;
import org.jdownloader.api.config.AdvancedConfigQueryStorable;
import org.jdownloader.api.config.InvalidValueException;

@ApiNamespace(org.jdownloader.myjdownloader.client.bindings.interfaces.PluginsInterface.NAMESPACE)
public interface PluginsAPI extends RemoteAPIInterface {
    List<String> getPluginRegex(String URL);

    HashMap<String, ArrayList<String>> getAllPluginRegex();

    List<PluginAPIStorable> list(PluginsQueryStorable query);

    @AllowStorage(value = { Object.class })
    @AllowNonStorableObjects
    boolean set(final String interfaceName, final String displayName, String key, final Object newValue) throws BadParameterException, InvalidValueException;

    boolean reset(final String interfaceName, final String displayName, String key) throws BadParameterException, InvalidValueException;

    @AllowStorage(value = { Object.class })
    public Object get(String interfaceName, String displayName, String key) throws BadParameterException;

    List<PluginConfigEntryAPIStorable> query(AdvancedConfigQueryStorable query) throws BadParameterException;
}