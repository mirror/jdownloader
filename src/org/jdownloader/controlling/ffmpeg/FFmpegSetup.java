package org.jdownloader.controlling.ffmpeg;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultStringArrayValue;

public interface FFmpegSetup extends ConfigInterface {
    @AboutConfig
    String getBinaryPath();

    void setBinaryPath(String path);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%video", "-i", "%audio", "-map", "0:0", "-c:v", "copy", "-map", "1:0", "-c:a", "copy", "-f", "mp4", "%out", "-y" })
    String[] getMuxToMp4Command();

    void setMuxToMp4Command(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-f", "mp4", "-c:a", "copy", "%out", "-y" })
    String[] getDash2M4aCommand();

    void setDash2M4aCommand(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-f", "adts", "-c:a", "copy", "%out", "-y" })
    String[] getDash2AacCommand();

    void setDash2AacCommand(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-vn", "-f", "adts", "-c:a", "copy", "%out", "-y" })
    String[] getDemux2AacCommand();

    void setDemux2AacCommand(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-vn", "-f", "mp4", "-c:a", "copy", "%out", "-y" })
    String[] getDemux2M4aCommand();

    void setDemux2M4aCommand(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-vn", "-f", "mp3", "-c:a", "copy", "%out", "-y" })
    String[] getDemux2Mp3Command();

    void setDemux2Mp3Command(String[] command);

}
