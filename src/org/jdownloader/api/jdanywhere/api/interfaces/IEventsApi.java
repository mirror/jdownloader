package org.jdownloader.api.jdanywhere.api.interfaces;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("jdanywhere/events")
public interface IEventsApi extends RemoteAPIInterface {

    public boolean RegisterCaptchaPush(String host, String path, String query);

    public boolean RegisterCaptchaPush_v2(String deviceID, String host, String path, String query, boolean withSound);

    public boolean IsRegistered(String deviceID);

    public boolean IsSoundEnabled(String deviceID);

    public boolean UnRegisterCaptchaPush(String deviceID);
}
