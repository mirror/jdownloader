package org.jdownloader.api.jdanywhere.api;

import java.io.FileNotFoundException;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.jdownloader.api.content.ContentAPIImpl;
import org.jdownloader.api.jdanywhere.api.interfaces.IContentApi;

public class ContentApi implements IContentApi {

    ContentAPIImpl ctAPI = new ContentAPIImpl();

    @Override
    public void favicon(RemoteAPIRequest request, RemoteAPIResponse response, String hostername) throws InternalApiException, FileNotFoundException {
        ctAPI.favicon(request, response, hostername);
    }

    public void fileIcon(RemoteAPIRequest request, RemoteAPIResponse response, String extension) throws InternalApiException {
        ctAPI.fileIcon(request, response, extension);
    }
}
