package org.jdownloader.api.system;

import java.util.List;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.jdownloader.myjdownloader.client.bindings.StorageInformationStorable;
import org.jdownloader.myjdownloader.client.bindings.SystemInformationStorable;

@ApiNamespace(org.jdownloader.myjdownloader.client.bindings.interfaces.SystemInterface.NAMESPACE)
public interface SystemAPI extends RemoteAPIInterface {
    @APIParameterNames({ "force" })
    public void shutdownOS(boolean force);

    public void standbyOS();

    public void hibernateOS();

    public void restartJD();

    public void exitJD();

    @AllowNonStorableObjects
    public SystemInformationStorable getSystemInfos();

    @AllowNonStorableObjects
    @APIParameterNames({ "path" })
    public List<StorageInformationStorable> getStorageInfos(final String path);
}
