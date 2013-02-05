package org.jdownloader.api.mobile.content;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.jdownloader.api.content.ContentAPIImpl;

public class ContentMobileAPIImpl implements ContentMobileAPI {

    ContentAPIImpl ctAPI = new ContentAPIImpl();

    @Override
    public void favicon(RemoteAPIRequest request, RemoteAPIResponse response, String hostername) {
        ctAPI.favicon(request, response, hostername);
    }

    public void fileIcon(RemoteAPIRequest request, RemoteAPIResponse response, String extension) {
        ctAPI.fileIcon(request, response, extension);
    }
}
