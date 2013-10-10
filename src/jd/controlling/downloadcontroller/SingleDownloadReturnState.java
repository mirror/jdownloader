package jd.controlling.downloadcontroller;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public class SingleDownloadReturnState {

    private final SingleDownloadController controller;

    public SingleDownloadController getController() {
        return controller;
    }

    public Throwable getCaughtThrowable() {
        return caughtThrowable;
    }

    public PluginForHost getLatestPlugin() {
        return latestPlugin;
    }

    private final Throwable     caughtThrowable;
    private final PluginForHost latestPlugin;
    private final long          timeStamp = System.currentTimeMillis();

    public long getTimeStamp() {
        return timeStamp;
    }

    protected SingleDownloadReturnState(SingleDownloadController controller, Throwable caughtThrowable, PluginForHost latestPlugin) {
        this.controller = controller;
        this.caughtThrowable = caughtThrowable;
        this.latestPlugin = latestPlugin;
    }

    public DownloadLinkCandidate getDownloadLinkCandidate() {
        return controller.getDownloadLinkCandidate();
    }

    public DownloadLink getDownloadLink() {
        return controller.getDownloadLink();
    }
}
