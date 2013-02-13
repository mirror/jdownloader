package org.jdownloader.extensions.jdanywhere.api;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.jdownloader.api.content.ContentAPIImpl;
import org.jdownloader.extensions.jdanywhere.api.interfaces.IContentApi;

public class ContentApi implements IContentApi {

    ContentAPIImpl ctAPI = new ContentAPIImpl();

    @Override
    public void favicon(RemoteAPIRequest request, RemoteAPIResponse response, String hostername) {
        ctAPI.favicon(request, response, hostername);
    }

    public void fileIcon(RemoteAPIRequest request, RemoteAPIResponse response, String extension) {
        ctAPI.fileIcon(request, response, extension);
    }
}
