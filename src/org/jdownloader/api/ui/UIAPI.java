package org.jdownloader.api.ui;

import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.ApiDoc;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.FileNotFound404Exception;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.storage.config.annotations.AllowStorage;
import org.jdownloader.api.content.v2.MyJDMenuItem;
import org.jdownloader.myjdownloader.client.bindings.interfaces.UIInterface.Context;

@ApiNamespace("ui")
public interface UIAPI extends RemoteAPIInterface {
    @ApiDoc("Get the custom menu structure for the desired context")
    @APIParameterNames({ "request", "context" })
    public MyJDMenuItem getMenu(RemoteAPIRequest request, Context context) throws InternalApiException, FileNotFound404Exception;

    @AllowStorage(value = { Object.class })
    @ApiDoc("Invoke a menu action on our selection and get the results.")
    @APIParameterNames({ "request", "context", "id", "linkIds", "packageIds" })
    public Object invokeAction(RemoteAPIRequest request, Context context, String id, final long[] linkIds, final long[] packageIds) throws InternalApiException, FileNotFound404Exception;
}
