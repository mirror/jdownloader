package org.jdownloader.api.cnl2;

import org.appwork.remoteapi.RemoteAPI;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging.Log;
import org.jdownloader.api.HttpServer;
import org.jdownloader.api.RemoteAPIConfig;

public class ExternInterface {

    private static ExternInterface INSTANCE = new ExternInterface();

    private ExternInterface() {
        if (JsonConfig.create(RemoteAPIConfig.class).isExternInterfaceEnabled()) {
            RemoteAPI remoteAPI = new RemoteAPI();
            try {
                remoteAPI.register(new ExternInterfaceImpl());
                HttpServer.getInstance().registerRequestHandler(9666, true, remoteAPI);
            } catch (Throwable e) {
                Log.exception(e);
            }
        }
    }

    public static ExternInterface getINSTANCE() {
        return INSTANCE;
    }

}
