package org.jdownloader.api.extensions;

import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("extensions")
public interface ExtensionsAPI extends RemoteAPIInterface {
    @APIParameterNames({ "id" })
    public boolean isInstalled(String id);

    @APIParameterNames({ "classname" })
    public boolean isEnabled(String classname);

    @APIParameterNames({ "classname", "b" })
    public boolean setEnabled(String classname, boolean b);

    @APIParameterNames({ "id" })
    public boolean install(String id);

    @APIParameterNames({ "query" })
    List<ExtensionAPIStorable> list(ExtensionQueryStorable query);
}
