package org.jdownloader.controlling.ffmpeg;

import java.io.File;
import java.util.ArrayList;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.logging.LogController;

public class FFprobe extends AbstractFFmpegBinary {
    public FFprobe(Browser br) {
        super(br);
        config = JsonConfig.create(FFmpegSetup.class);
        logger = LogController.getInstance().getLogger(FFprobe.class.getName());
        path = config.getBinaryPathProbe();
        if (path != null && !validatePaths()) {
            config.setBinaryPath(null);
            path = null;
        }
    }

    public FFprobe() {
        this(null);
    }

    private boolean validatePaths() {
        File root = Application.getResource("");
        String relative = Files.getRelativePath(root, new File(path));
        logger.info("Validate Relative Path: " + relative);
        if (relative != null) {
            File correctPath = FFMpegInstallThread.getFFmpegPath("ffprobe");
            String relativeCorrect = Files.getRelativePath(root, correctPath);
            logger.info("Validate Relative Correct Path: " + relativeCorrect);
            if (relativeCorrect != null) {
                if (!StringUtils.equals(relative, relativeCorrect)) {
                    if (!Application.getResource(relativeCorrect).exists() && Application.getResource(relative).exists()) {
                        // relative path doesn't have to match our expectation of where ffmpeg should be installed to.. Users will install
                        // to a directory structure of there own liking
                        logger.info("Validate Relative Path: User has installed to there own path!");
                        return true;
                    } else if (!StringUtils.equals(relative, relativeCorrect)) {
                        logger.info("Mismatch. validation failed");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public StreamInfo getStreamInfo(String url) {
        try {
            initPipe();
            this.processID = new UniqueAlltimeID().getID();
            final ArrayList<String> commandLine = new ArrayList<String>();
            commandLine.add(getFullPath());
            commandLine.add("-loglevel");
            commandLine.add("48");
            commandLine.add("-show_format");
            commandLine.add("-show_streams");
            commandLine.add("-probesize");
            commandLine.add("24000");
            commandLine.add("-of");
            commandLine.add("json");
            commandLine.add("-i");
            if (server != null && server.isRunning()) {
                commandLine.add("http://127.0.0.1:" + server.getPort() + "/download?id=" + processID + "&url=" + Encoding.urlEncode(url));
            } else {
                commandLine.add(url);
            }
            final String ret = runCommand(null, commandLine);
            final StreamInfo data = JSonStorage.restoreFromString(ret, new TypeRef<StreamInfo>() {
            });
            return data;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            closePipe();
        }
    }

    public StreamInfo getStreamInfo(File dummy) {
        if (dummy != null) {
            return getStreamInfo(dummy.toString());
        } else {
            return null;
        }
    }

}
