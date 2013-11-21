package org.jdownloader.controlling.ffmpeg;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;

public interface FFmpegSetup extends ConfigInterface {
    @AboutConfig
    String getBinaryPath();

    void setBinaryPath(String path);

}
