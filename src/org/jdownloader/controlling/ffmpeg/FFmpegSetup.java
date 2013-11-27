package org.jdownloader.controlling.ffmpeg;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultStringArrayValue;

public interface FFmpegSetup extends ConfigInterface {
    @AboutConfig
    String getBinaryPath();

    void setBinaryPath(String path);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%video", "-i", "%audio", "-map", "0:0", "-c:v", "copy", "-map", "1:0", "-c:a", "copy", "-f", "mp4", "%out" })
    String[] getMergeCommand();

    void setMergeCommand(String[] command);

}
