package jd.network.rtmp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.lang.reflect.Field;

import jd.network.rtmp.url.RtmpUrlConnection;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.RTMPDownload;
import jd.utils.JDUtilities;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.nativ.NativeProcess;
import org.jdownloader.translate._JDT;

public class RtmpDump extends RTMPDownload {

    private Chunk             chunk;
    private long              speed = 0l;
    private int               PID   = -1;
    private String            RTMPDUMP;
    private NativeProcess     np;
    private Process           p;
    private InputStreamReader r;

    public RtmpDump(final PluginForHost plugin, final DownloadLink downloadLink, final String rtmpURL) throws IOException, PluginException {
        super(plugin, downloadLink, rtmpURL);
    }

    private void getProcessId() {
        try {
            final Field pidField = p.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            PID = pidField.getInt(p);
        } catch (final Exception e) {
            PID = -1;
        }
    }

    private void sendSIGINT() {
        getProcessId();
        if (PID >= 0) {
            try {
                Runtime.getRuntime().exec("kill -SIGINT " + String.valueOf(PID));
            } catch (final Exception e1) {
            }
        }
    }

    public boolean start(final RtmpUrlConnection rtmpConnection) throws Exception {
        if (CrossSystem.isWindows()) {
            RTMPDUMP = JDUtilities.getResourceFile("tools/Windows/rtmpdump/rtmpdump.exe").getAbsolutePath();
        } else if (CrossSystem.isLinux()) {
            RTMPDUMP = JDUtilities.getResourceFile("tools/linux/rtmpdump/rtmpdump").getAbsolutePath();
        } else if (CrossSystem.isMac()) {
            RTMPDUMP = JDUtilities.getResourceFile("tools/mac/rtmpdump/rtmpdump").getAbsolutePath();
        } else {
            return false;
        }
        if (!new File(RTMPDUMP).exists()) {
            if (CrossSystem.isLinux() || CrossSystem.isMac()) {
                RTMPDUMP = "/usr/bin/rtmpdump";
                if (!new File(RTMPDUMP).exists()) {
                    RTMPDUMP = "/usr/local/bin/rtmpdump";
                }
            }
        }
        if (!new File(RTMPDUMP).exists()) { throw new PluginException(LinkStatus.ERROR_FATAL, "Error " + RTMPDUMP + " not found!"); }
        try {
            addChunksDownloading(1);
            chunk = new Chunk(0, 0, null, null) {
                @Override
                public long getSpeed() {
                    return speed;
                }
            };
            chunk.setInProgress(true);
            getChunks().add(chunk);
            downloadLink.getLinkStatus().addStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);

            rtmpConnection.connect();

            File tmpFile = new File(downloadLink.getFileOutput() + ".part");
            if (!CrossSystem.isWindows()) {
                tmpFile = new File(downloadLink.getFileOutput().replaceAll("\\s", "\\\\s") + ".part");
            }
            String line = "", error = "";
            long iSize = 0;
            long before = 0;
            long lastTime = System.currentTimeMillis();
            long bytesLoaded = 0l;

            String cmd = rtmpConnection.getCommandLineParameter();
            if (CrossSystem.isWindows()) {
                // MAX_PATH Fix --> \\?\ + Path
                if (String.valueOf(tmpFile).length() >= 260) {
                    cmd += " -o \"\\\\?\\" + String.valueOf(tmpFile) + "\"";
                } else {
                    cmd += " -o \"" + String.valueOf(tmpFile) + "\"";
                }
            } else {
                cmd = cmd.replaceAll("\"", "") + " -o " + String.valueOf(tmpFile);
            }

            if (cmd.contains(" -e ")) {
                setResume(true);
            }

            try {
                if (CrossSystem.isWindows()) {
                    np = new NativeProcess(RTMPDUMP, cmd);
                    r = new InputStreamReader(np.getErrorStream());
                } else {
                    p = Runtime.getRuntime().exec(RTMPDUMP + cmd);
                    r = new InputStreamReader(p.getErrorStream());
                }
                final BufferedReader br = new BufferedReader(r);
                int sizeCalulateBuffer = 0;
                while ((line = br.readLine()) != null) {
                    error = line;
                    if (!new Regex(line, "^[0-9]").matches()) {
                        if (line.contains("length")) {
                            final String size = new Regex(line, ".*?(\\d.+)").getMatch(0);
                            iSize += SizeFormatter.getSize(size);
                        }
                    } else {
                        if (downloadLink.getDownloadSize() == 0) {
                            downloadLink.setDownloadSize(iSize);
                        }
                        final int pos1 = line.indexOf("(");
                        final int pos2 = line.indexOf(")");
                        if (pos1 != -1 && pos2 != -1 && line.toUpperCase().contains("KB")) {
                            final float progressFloat = Float.parseFloat(line.substring(pos1 + 1, pos2 - 1));
                            bytesLoaded = SizeFormatter.getSize(line.substring(0, line.toLowerCase().indexOf("kb") + 2));
                            if (Thread.currentThread().isInterrupted()) {
                                if (CrossSystem.isWindows()) {
                                    np.sendCtrlCSignal();
                                } else {
                                    sendSIGINT();
                                }
                                throw new InterruptedIOException();
                            }

                            downloadLink.setDownloadCurrent(bytesLoaded);
                            if (sizeCalulateBuffer > 6) {
                                downloadLink.setDownloadSize((long) (bytesLoaded * 100.0F / progressFloat));
                            } else {
                                sizeCalulateBuffer++;
                            }
                            if (System.currentTimeMillis() - lastTime > 1000) {
                                speed = (bytesLoaded - before) / (System.currentTimeMillis() - lastTime) * 1000l;
                                lastTime = System.currentTimeMillis();
                                before = bytesLoaded;
                                downloadLink.requestGuiUpdate();
                                downloadLink.setChunksProgress(new long[] { bytesLoaded });
                            }
                        }
                    }
                    if (!line.toLowerCase().contains("download complete")) {
                        continue;
                    }
                    Thread.sleep(500);
                    break;
                }
            } finally {
                rtmpConnection.disconnect();
            }
            if (line != null && line.toLowerCase().contains("download complete")) {
                downloadLink.setDownloadSize(bytesLoaded);
                logger.finest("no errors : rename");
                if (!tmpFile.renameTo(new File(downloadLink.getFileOutput()))) {
                    logger.severe("Could not rename file " + tmpFile + " to " + downloadLink.getFileOutput());
                    error(LinkStatus.ERROR_LOCAL_IO, _JDT._.system_download_errors_couldnotrename());
                }
                downloadLink.getLinkStatus().addStatus(LinkStatus.FINISHED);
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, " rtmpdump-Output: " + error);
            }
            return true;
        } finally {
            downloadLink.getLinkStatus().removeStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
            downloadLink.setDownloadInstance(null);
            downloadLink.getLinkStatus().setStatusText(null);
            chunk.setInProgress(false);
        }
    }
}
