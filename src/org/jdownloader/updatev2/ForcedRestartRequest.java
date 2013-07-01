package org.jdownloader.updatev2;

import org.appwork.shutdown.ShutdownVetoListener;

public class ForcedRestartRequest extends BasicRestartRequest {

    public ForcedRestartRequest(String... arguments) {
        super(arguments);
    }

    @Override
    public boolean askForVeto(ShutdownVetoListener listener) {
        return false;
    }
}
