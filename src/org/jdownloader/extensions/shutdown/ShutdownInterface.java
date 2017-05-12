package org.jdownloader.extensions.shutdown;

import java.util.Arrays;

public abstract class ShutdownInterface {
    public abstract Mode[] getSupportedModes();

    public boolean isSupported(Mode mode) {
        return Arrays.asList(getSupportedModes()).contains(mode);
    }

    public abstract void requestMode(Mode mode, final boolean force);

    public abstract void prepareMode(Mode mode);
}
