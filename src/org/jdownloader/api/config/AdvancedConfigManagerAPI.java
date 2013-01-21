package org.jdownloader.api.config;

import java.util.ArrayList;
import java.util.List;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.ApiDoc;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.storage.config.annotations.AllowStorage;
import org.jdownloader.settings.advanced.AdvancedConfigAPIEntry;

@ApiNamespace("config")
public interface AdvancedConfigManagerAPI extends RemoteAPIInterface {

    @ApiDoc("list all available config entries")
    public ArrayList<AdvancedConfigAPIEntry> list();

    @AllowStorage(value = { Object.class })
    @ApiDoc("get value from interface by key")
    public Object get(String interfacename, String key);

    @AllowStorage(value = { Object.class })
    @ApiDoc("set value to interface by key")
    public boolean set(String interfacename, String key, String value);

    @ApiDoc("reset interface by key to its default value")
    public boolean reset(String interfacename, String key);

    @AllowStorage(value = { Object.class })
    @ApiDoc("get default value from interface by key")
    public Object getDefault(String interfacename, String key);

    public List<ConfigInterfaceAPIStorable> queryConfigInterfaces(APIQuery query);

    List<ConfigEntryAPIStorable> queryConfigSettings(APIQuery query);
}
