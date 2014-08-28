package org.jdownloader.api.content.v2;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.APIFileNotFoundException;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.storage.config.annotations.AllowStorage;
import org.jdownloader.myjdownloader.client.json.IconDescriptor;

@ApiNamespace("contentV2")
public interface ContentAPIV2 extends RemoteAPIInterface {

    public void getFavIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String hostername) throws APIFileNotFoundException, InternalApiException;

    public void getFileIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String filename) throws InternalApiException;

    public void getIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String key, int size) throws InternalApiException, APIFileNotFoundException;

    @AllowStorage({ IconDescriptor.class })
    public IconDescriptor getIconDescription(String key) throws InternalApiException;
}
