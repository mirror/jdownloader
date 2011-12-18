package org.jdownloader.api;

import org.appwork.remoteapi.RemoteAPI;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging.Log;
import org.jdownloader.api.cnl2.ExternInterfaceImpl;

public class ExternInterface {

    private static ExternInterface INSTANCE = new ExternInterface();

    public static void main(String[] args) throws InterruptedException {
        ExternInterface.INSTANCE.hashCode();
        Thread.sleep(100000);
    }

    private ExternInterface() {
        if (JsonConfig.create(RemoteAPIConfig.class).getExternInterfaceEnabled()) {
            RemoteAPI remoteAPI = new RemoteAPI();
            try {
                remoteAPI.register(new ExternInterfaceImpl());
                HttpServer.getInstance().registerRequestHandler(9666, JsonConfig.create(RemoteAPIConfig.class).getAPIlocalhost(), remoteAPI);
            } catch (Throwable e) {
                Log.exception(e);
            }
        }
    }

    public static ExternInterface getINSTANCE() {
        return INSTANCE;
    }

}
