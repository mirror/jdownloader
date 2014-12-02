package org.jdownloader.controlling.ffmpeg;

import java.util.HashMap;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultStringArrayValue;

public interface FFmpegSetup extends ConfigInterface {
    @AboutConfig
    String getBinaryPath();

    void setBinaryPath(String path);

    @AboutConfig
    String getBinaryPathProbe();

    void setBinaryPathProbe(String path);

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

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-vn", "-f", "ogg", "-acodec", "libvorbis", "-aq", "4", "%out", "-y" })
    String[] getDemuxAndConvert2Ogg();

    void setDemuxAndConvert2Ogg(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "%map", "-acodec", "copy", "-vn", "%out", "-y" })
    String[] getDemuxGenericCommand();

    void setDemuxGenericCommand(String[] command);

    @AboutConfig
    HashMap<String, String> getExtensionToFormatMap();

    void setExtensionToFormatMap(HashMap<String, String> map);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%video", "-i", "%audio", "-map", "0:0", "-c:v", "copy", "-map", "1:0", "-c:a", "copy", "-f", "webm", "%out", "-y" })
    String[] getMuxToWebmCommand();

    void setMuxToWebmCommand(String[] command);

    @AboutConfig
    @DefaultStringArrayValue({ "-i", "%audio", "-f", "ogg", "-c:a", "copy", "%out", "-y" })
    String[] getDash2OggAudioCommand();

    void setDash2OggAudioCommand(String[] command);

}
