package org.jdownloader.api.device;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.jdownloader.myjdownloader.client.json.DirectConnectionInfos;

@ApiNamespace("device")
public interface DeviceAPI extends RemoteAPIInterface {
    
    public boolean ping();
    
    @AllowNonStorableObjects
    public DirectConnectionInfos getDirectConnectionInfos(RemoteAPIRequest request);
    
}
