package org.jdownloader.api.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("plugins")
public interface PluginsAPI extends RemoteAPIInterface {
    List<String> getPluginRegex(String URL);

    // Return Value: HashMap<String, List<String>>();
    HashMap<String, ArrayList<String>> getAllPluginRegex();
}
