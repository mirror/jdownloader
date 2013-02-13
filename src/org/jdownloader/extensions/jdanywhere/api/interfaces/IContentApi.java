package org.jdownloader.extensions.jdanywhere.api.interfaces;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.ApiSessionRequired;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;

@ApiNamespace("jdanywhere/content")
@ApiSessionRequired
public interface IContentApi extends RemoteAPIInterface {

    public void favicon(RemoteAPIRequest request, final RemoteAPIResponse response, String hostername);

    public void fileIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String filename);
}
