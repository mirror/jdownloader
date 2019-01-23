package org.jdownloader.controlling.ffmpeg;

import java.io.File;
import java.util.ArrayList;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;

public abstract class FFprobe extends AbstractFFmpegBinary {
    public FFprobe(Browser br) {
        super(br);
        final String path = config.getBinaryPathProbe();
        this.setPath(path);
    }

    public FFprobe() {
        this(null);
    }

    @Override
    public boolean isCompatible() {
        if (CrossSystem.isWindows()) {
            if (!CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_7)) {
                final String sha256 = Hash.getFileHash(new File(getFullPath()), Hash.HASH_TYPE_SHA256);
                if (StringUtils.equalsIgnoreCase("d88cc4d2cf122c98e26a3cce9bb0457e97e6445da02b2874b8a407c5fe95c4b8", sha256) || StringUtils.equalsIgnoreCase("d740da4d80d7add22c3538918ab35f62725df920fa9631035678330ec6b9b31a", sha256)) {
                    getLogger().severe("ffprobe binary(" + getFullPath() + ") requires minimum Windows 7!");
                    return false;
                }
            }
        }
        return super.isCompatible();
    }

    public StreamInfo getStreamInfo(String url) {
        try {
            if (!isAvailable() || !isCompatible()) {
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
            getLogger().log(e);
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
