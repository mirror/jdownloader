package org.jdownloader.extensions.shutdown;

import java.util.Arrays;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;

import org.appwork.storage.config.handler.StorageHandler;

public abstract class ShutdownInterface {
    public abstract Mode[] getSupportedModes();

    public boolean isSupported(Mode mode) {
        return Arrays.asList(getSupportedModes()).contains(mode);
    }

    public abstract void requestMode(Mode mode, final boolean force);

    public abstract void prepareMode(Mode mode);

    protected void stopActivity() {
        DownloadWatchDog.getInstance().stopDownloads();
        LinkCollector.getInstance().abort();
        StorageHandler.flushWrites();
    }
}
