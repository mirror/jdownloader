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
    public void shutdownOS(boolean force) throws InterruptedException;;

    public void standbyOS() throws InterruptedException;;

    public void hibernateOS() throws InterruptedException;

    public void restartJD() throws InterruptedException;;

    public void exitJD() throws InterruptedException;;

    @AllowNonStorableObjects
    public SystemInformationStorable getSystemInfos();

    @AllowNonStorableObjects
    @APIParameterNames({ "path" })
    public List<StorageInformationStorable> getStorageInfos(final String path);
}
