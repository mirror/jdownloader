package org.jdownloader.extensions.jdanywhere.api.interfaces;

import java.io.FileNotFoundException;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.InternalApiException;

@ApiNamespace("jdanywhere/content")
public interface IContentApi extends RemoteAPIInterface {

    public void favicon(RemoteAPIRequest request, final RemoteAPIResponse response, String hostername) throws InternalApiException, FileNotFoundException;

    public void fileIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String filename) throws InternalApiException;
}
