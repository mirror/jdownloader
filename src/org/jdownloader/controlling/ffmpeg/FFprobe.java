package org.jdownloader.controlling.ffmpeg;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.logging.LogController;

public class FFprobe extends AbstractFFmpegBinary {

    public FFprobe() {
        config = JsonConfig.create(FFmpegSetup.class);
        logger = LogController.getInstance().getLogger(FFprobe.class.getName());
        path = config.getBinaryPathProbe();

    }

    public FFprobe(String path) {
        this();
        this.path = path;
    }

    public StreamInfo getStreamInfo(String dllink) {
        ArrayList<String> commandLine = new ArrayList<String>();
        commandLine.add(getFullPath());
        commandLine.add("-show_format");
        commandLine.add("-show_streams");
        commandLine.add("-probesize");
        commandLine.add("24000");
        commandLine.add("-of");
        commandLine.add("json");
        commandLine.add("-i");
        commandLine.add(dllink);

        try {
            String ret = runCommand(null, commandLine);
            StreamInfo data = JSonStorage.restoreFromString(ret, new TypeRef<StreamInfo>() {
            });
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (FFMpegException e) {
            e.printStackTrace();
        }
        return null;
    }

    public StreamInfo getStreamInfo(File dummy) {
        return getStreamInfo(dummy.toString());
    }

}
