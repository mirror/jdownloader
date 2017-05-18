package org.jdownloader.api;

import java.io.IOException;

import org.appwork.remoteapi.InterfaceHandler;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.exceptions.BasicRemoteAPIException;
import org.appwork.utils.net.httpserver.requests.HttpRequest;

public class DeprecatedRemoteAPIRequest extends RemoteAPIRequest {
    public DeprecatedRemoteAPIRequest(InterfaceHandler<?> iface, String methodName, String[] parameters, DeprecatedAPIRequestInterface request, String callback) throws IOException, BasicRemoteAPIException {
        super(iface, methodName, parameters, (HttpRequest) request, callback);
    }

    public DeprecatedAPIRequestInterface getRequest() {
        return (DeprecatedAPIRequestInterface) super.getHttpRequest();
    }
}
