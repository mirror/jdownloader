package org.jdownloader.api.content.v2;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.APIFileNotFoundException;
import org.appwork.remoteapi.exceptions.InternalApiException;

@ApiNamespace("contentV2")
public interface ContentAPIV2 extends RemoteAPIInterface {

    public void getFavIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String hostername) throws APIFileNotFoundException, InternalApiException;

    public void getFileIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String filename) throws InternalApiException;
}
