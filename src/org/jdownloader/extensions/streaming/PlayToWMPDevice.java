package org.jdownloader.extensions.streaming;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public class PlayToWMPDevice extends PlayToLocalBinary {

    private StreamingExtension extension;
    private String             path;
    private LogSource          logger;

    public PlayToWMPDevice(StreamingExtension streamingExtension, String path) {
        super(streamingExtension);
        this.extension = streamingExtension;
        this.path = path;
        logger = LogController.getInstance().getLogger(PlayToWMPDevice.class.getName());
    }

    @Override
    public String getDisplayName() {
        return "Windows Media Player";
    }

    @Override
    protected String getBinaryPath() {
        return path;
    }

    @Override
    public String getUniqueDeviceID() {
        return "wmp";
    }

    @Override
    public String getUserAgentPattern() {
        return null;
    }
}
