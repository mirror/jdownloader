package org.jdownloader.api.extensions;

import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("extensions")
public interface ExtensionsAPI extends RemoteAPIInterface {

    public boolean isInstalled(String id);

    public boolean isEnabled(String classname);

    public boolean setEnabled(String classname, boolean b);

    public boolean install(String id);

    List<ExtensionAPIStorable> list(ExtensionQueryStorable query);
}
