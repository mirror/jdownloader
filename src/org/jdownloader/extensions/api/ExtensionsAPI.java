package org.jdownloader.extensions.api;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("extensions")
public interface ExtensionsAPI extends RemoteAPIInterface {

    public boolean isInstalled(String id);

    public boolean isEnabled(String classname);

    public void setEnabled(String classname, boolean b);

    public void install(String id);
}
