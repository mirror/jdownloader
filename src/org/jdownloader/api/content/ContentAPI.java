package org.jdownloader.api.content;

import java.io.FileNotFoundException;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.InternalApiException;

@ApiNamespace("content")
public interface ContentAPI extends RemoteAPIInterface {

    public void favicon(RemoteAPIRequest request, final RemoteAPIResponse response, String hostername) throws FileNotFoundException, InternalApiException;

    public void fileIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String filename) throws InternalApiException;
}
