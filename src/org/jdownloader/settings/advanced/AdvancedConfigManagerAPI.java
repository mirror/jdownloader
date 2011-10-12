package org.jdownloader.settings.advanced;

import java.util.ArrayList;

import org.appwork.remoteapi.ApiDoc;
import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.ApiSessionRequired;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.storage.config.annotations.AllowStorage;

@ApiNamespace("config")
@ApiSessionRequired
public interface AdvancedConfigManagerAPI extends RemoteAPIInterface {

    @ApiDoc("list all available config entries")
    public ArrayList<AdvancedConfigAPIEntry> list();

    @AllowStorage(value = { Object.class })
    @ApiDoc("get value from interface by key")
    public Object get(String interfacename, String key);

    @AllowStorage(value = { Object.class })
    @ApiDoc("set value to interface by key")
    public boolean set(String interfacename, String key, String value);
}
