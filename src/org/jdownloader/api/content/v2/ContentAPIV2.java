package org.jdownloader.api.content.v2;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.ApiDoc;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.APIFileNotFoundException;
import org.appwork.remoteapi.exceptions.BadRequestException;
import org.appwork.remoteapi.exceptions.FileNotFound404Exception;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.storage.config.annotations.AllowStorage;
import org.jdownloader.myjdownloader.client.bindings.interfaces.ContentInterface.Context;
import org.jdownloader.myjdownloader.client.json.IconDescriptor;

@ApiNamespace("contentV2")
public interface ContentAPIV2 extends RemoteAPIInterface {
    @ApiDoc("Get the custom menu structure for the desired context")
    public MyJDMenuItem getMenu(RemoteAPIRequest request, Context context) throws InternalApiException, FileNotFound404Exception;

    @AllowStorage(value = { Object.class })
    @ApiDoc("Invoke a menu action on our selection and get the results.")
    public Object invokeAction(RemoteAPIRequest request, Context context, String id, final long[] linkIds, final long[] packageIds) throws InternalApiException, FileNotFound404Exception;

    public void getFavIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String hostername) throws APIFileNotFoundException, InternalApiException;

    public void getFileIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String filename) throws InternalApiException;

    public void getIcon(RemoteAPIRequest request, final RemoteAPIResponse response, String key, int size) throws InternalApiException, APIFileNotFoundException, BadRequestException;

    @AllowStorage({ IconDescriptor.class })
    public IconDescriptor getIconDescription(String key) throws InternalApiException;
}
