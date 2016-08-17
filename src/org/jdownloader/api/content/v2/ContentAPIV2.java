package org.jdownloader.api.content.v2;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.APIFileNotFoundException;
import org.appwork.remoteapi.exceptions.BadRequestException;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.storage.config.annotations.AllowStorage;
import org.jdownloader.myjdownloader.client.json.IconDescriptor;

@ApiNamespace("contentV2")
public interface ContentAPIV2 extends RemoteAPIInterface {
    @APIParameterNames({ "request", "response", "hostername" })
    public void getFavIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String hostername) throws APIFileNotFoundException, InternalApiException;

    @APIParameterNames({ "request", "response", "filename" })
    public void getFileIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String filename) throws InternalApiException;

    @APIParameterNames({ "request", "response", "key", "size" })
    public void getIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String key, int size) throws InternalApiException, APIFileNotFoundException, BadRequestException;

    @AllowStorage({ IconDescriptor.class })
    @APIParameterNames({ "key" })
    public IconDescriptor getIconDescription(String key) throws InternalApiException;
}
