package org.jdownloader.api.update;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("update")
public interface UpdateAPI extends RemoteAPIInterface {
    public void restartAndUpdate();

    public boolean isUpdateAvailable();
}
