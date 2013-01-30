package org.jdownloader.api.content;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;

@ApiNamespace("content")
public interface ContentAPI extends RemoteAPIInterface {

    public void favicon(RemoteAPIRequest request, final RemoteAPIResponse response, String hostername);

    public void fileIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String filename);
}
