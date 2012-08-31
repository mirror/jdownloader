package org.jdownloader.extensions.streaming;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public class PlayToVLCDevice extends PlayToLocalBinary {

    private String    vlcPath;
    private LogSource logger;

    public PlayToVLCDevice(StreamingExtension streamingExtension, String vlcBinary) {
        super(streamingExtension);
        vlcPath = vlcBinary;
        logger = LogController.getInstance().getLogger(StreamingExtension.class.getName());
    }

    @Override
    public String getDisplayName() {
        return "VideoLan Player";
    }

    @Override
    protected String getBinaryPath() {
        return vlcPath;
    }

    @Override
    public String getUniqueDeviceID() {
        return "vlc";
    }

}
