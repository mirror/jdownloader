package org.jdownloader.api.cnl2;

import java.io.IOException;

import org.appwork.remoteapi.RemoteAPI;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging.Log;
import org.jdownloader.api.DeprecatedAPIHttpServerController;
import org.jdownloader.api.RemoteAPIConfig;

public class ExternInterface {

    private static ExternInterface INSTANCE = new ExternInterface();

    private ExternInterface() {
        final RemoteAPIConfig config = JsonConfig.create(RemoteAPIConfig.class);
        if (config.isExternInterfaceEnabled()) {
            final Thread serverInit = new Thread() {
                @Override
                public void run() {
                    final RemoteAPI remoteAPI = new RemoteAPI();
                    try {
                        remoteAPI.register(new ExternInterfaceImpl());
                        while (config.isExternInterfaceEnabled() && !Thread.currentThread().isInterrupted()) {
                            try {
                                DeprecatedAPIHttpServerController.getInstance().registerRequestHandler(9666, config.isExternInterfaceLocalhostOnly(), remoteAPI);
                                break;
                            } catch (IOException e) {
                                Thread.sleep(30 * 1000l);
                            }
                        }
                    } catch (Throwable e) {
                        Log.exception(e);
                    }
                }
            };
            serverInit.setDaemon(true);
            serverInit.setName("ExternInterface: init");
            serverInit.start();
        }
    }

    public static ExternInterface getINSTANCE() {
        return INSTANCE;
    }

}
