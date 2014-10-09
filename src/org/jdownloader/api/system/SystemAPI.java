package org.jdownloader.api.system;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace(org.jdownloader.myjdownloader.client.bindings.interfaces.SystemInterface.NAMESPACE)
public interface SystemAPI extends RemoteAPIInterface {

    public void shutdownOS(boolean force);

    public void standbyOS();

    public void hibernateOS();

    public void restartJD();

    public void exitJD();
}
