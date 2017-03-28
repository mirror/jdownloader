package org.jdownloader.controlling.ffmpeg;

import java.io.File;
import java.util.ArrayList;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.logging.LogController;

public class FFprobe extends AbstractFFmpegBinary {
    public FFprobe(Browser br) {
        super(br);
        logger = LogController.getInstance().getLogger(FFprobe.class.getName());
        final String path = config.getBinaryPathProbe();
        if (path != null && !validatePaths(path)) {
            config.setBinaryPath(null);
            setPath(null);
        } else {
            this.setPath(path);
        }
    }

    public FFprobe() {
        this(null);
    }

    private boolean validatePaths(final String path) {
        final File root = Application.getResource("");
        final String relative = Files.getRelativePath(root, new File(path));
        logger.info("Validate Relative Path: " + relative);
        if (relative != null) {
            final File correctPath = FFMpegInstallThread.getFFmpegPath("ffprobe");
            final String relativeCorrect = Files.getRelativePath(root, correctPath);
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
            if (!isAvailable()) {
                return null;
            } else {
                if (StringUtils.endsWithCaseInsensitive(url, ".m3u8")) {
                    initPipe(url);
                } else {
                    initPipe(null);
                }
                this.processID = new UniqueAlltimeID().getID();
                final ArrayList<String> commandLine = new ArrayList<String>();
                commandLine.add(getFullPath());
                commandLine.add("-loglevel");
                commandLine.add("48");
                commandLine.add("-show_format");
                commandLine.add("-show_streams");
                commandLine.add("-analyzeduration");
                commandLine.add("15000000");// 15 secs
                commandLine.add("-of");
                commandLine.add("json");
                commandLine.add("-i");
                if (server != null && server.isRunning()) {
                    if (StringUtils.endsWithCaseInsensitive(url, ".m3u8")) {
                        commandLine.add("http://127.0.0.1:" + server.getPort() + "/m3u8?id=" + processID);
                    } else {
                        commandLine.add("http://127.0.0.1:" + server.getPort() + "/download?id=" + processID + "&url=" + Encoding.urlEncode(url));
                    }
                } else {
                    commandLine.add(url);
                }
                final String ret = runCommand(null, commandLine);
                final StreamInfo data = JSonStorage.restoreFromString(ret, new TypeRef<StreamInfo>() {
                });
                return data;
            }
        } catch (Throwable e) {
            if (logger != null) {
                logger.log(e);
            }
            return null;
        } finally {
            closePipe();
        }
    }

    public StreamInfo getStreamInfo(final File dummy) {
        if (dummy != null) {
            return getStreamInfo(dummy.toString());
        } else {
            return null;
        }
    }

}
