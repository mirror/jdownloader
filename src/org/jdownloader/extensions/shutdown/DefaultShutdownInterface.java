package org.jdownloader.extensions.shutdown;

import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;

public class DefaultShutdownInterface extends ShutdownInterface {
    public DefaultShutdownInterface(ShutdownExtension shutdownExtension) {
    }

    @Override
    public Mode[] getSupportedModes() {
        return new Mode[] { Mode.CLOSE };
    }

    @Override
    public void requestMode(Mode mode, boolean force) {
        switch (mode) {
        case CLOSE:
            stopActivity();
            RestartController.getInstance().exitAsynch(new SmartRlyExitRequest(true));
            break;
        default:
            break;
        }
    }

    @Override
    public void prepareMode(Mode mode) {
    }
}
