package org.jdownloader.api.config;

import java.util.ArrayList;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.remoteapi.annotations.ApiDoc;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.BadParameterException;
import org.appwork.storage.config.annotations.AllowStorage;
import org.jdownloader.settings.advanced.AdvancedConfigAPIEntry;

@ApiNamespace(org.jdownloader.myjdownloader.client.bindings.interfaces.AdvancedConfigInterface.NAMESPACE)
public interface AdvancedConfigManagerAPI extends RemoteAPIInterface {
    @ApiDoc("list all possible enum values")
    @AllowNonStorableObjects
    public ArrayList<EnumOption> listEnum(String type) throws BadParameterException;

    @ApiDoc("list all available config entries")
    @AllowNonStorableObjects
    public ArrayList<AdvancedConfigAPIEntry> list();

    @ApiDoc("list entries based on the pattern regex")
    @AllowNonStorableObjects
    public ArrayList<AdvancedConfigAPIEntry> list(String pattern, boolean returnDescription, boolean returnValues, boolean returnDefaultValues, boolean returnEnumInfo);

    @Deprecated
    @ApiDoc("DEPRECATED! list entries based on the pattern regex")
    @AllowNonStorableObjects
    public ArrayList<AdvancedConfigAPIEntry> list(String pattern, boolean returnDescription, boolean returnValues, boolean returnDefaultValues);

    @AllowStorage(value = { Object.class })
    @ApiDoc("get value from interface by key")
    public Object get(String interfaceName, String storage, String key);

    @AllowStorage(value = { Object.class })
    @AllowNonStorableObjects
    @ApiDoc("set value to interface by key")
    public boolean set(String interfaceName, String storage, String key, Object value) throws InvalidValueException;

    @ApiDoc("reset interface by key to its default value")
    public boolean reset(String interfaceName, String storage, String key);

    @AllowStorage(value = { Object.class })
    @ApiDoc("get default value from interface by key")
    public Object getDefault(String interfaceName, String storage, String key);

}
