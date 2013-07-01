package org.jdownloader.updatev2;

import org.appwork.shutdown.BasicShutdownRequest;
import org.appwork.shutdown.ShutdownVetoListener;

public class ForcedShutdown extends BasicShutdownRequest {

    @Override
    public boolean askForVeto(ShutdownVetoListener listener) {
        return false;
    }

}
