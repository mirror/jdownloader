package org.jdownloader.api.settings;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;

@ApiNamespace("settings")
public interface SettingsAPI extends RemoteAPIInterface {

    public SettingsAPIStorable get();

    public boolean set(/* JSON hashmap */); // FIXME parameter type

}
