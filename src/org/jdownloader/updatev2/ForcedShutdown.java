package org.jdownloader.updatev2;

import org.appwork.shutdown.ShutdownVetoFilter;
import org.appwork.shutdown.ShutdownVetoListener;

public class ForcedShutdown implements ShutdownVetoFilter {

    @Override
    public boolean askForVeto(ShutdownVetoListener listener) {
        return false;
    }

    @Override
    public void gotVetoFrom(ShutdownVetoListener listener) {
    }

}
