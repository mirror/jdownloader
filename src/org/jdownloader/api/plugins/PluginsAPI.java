package org.jdownloader.api.plugins;

import java.util.List;

import org.appwork.remoteapi.QueryResponseMap;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("plugins")
public interface PluginsAPI extends RemoteAPIInterface {
    List<String> getPluginRegex(String URL);

    // Return Value: HashMap<String, List<String>>();
    QueryResponseMap getAllPluginRegex();
}
