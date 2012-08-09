package jd.network.rtmp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.network.rtmp.url.RtmpUrlConnection;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.RTMPDownload;

import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.net.throttledconnection.ThrottledConnection;
import org.appwork.utils.net.throttledconnection.ThrottledConnectionHandler;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.speedmeter.SpeedMeterInterface;
import org.jdownloader.nativ.NativeProcess;
import org.jdownloader.translate._JDT;

public class RtmpDump extends RTMPDownload {

    private static class RTMPCon implements SpeedMeterInterface, ThrottledConnection {

        private ThrottledConnectionHandler handler;

        public ThrottledConnectionHandler getHandler() {
            return this.handler;
        }

        public int getLimit() {
            return 0;
        }

        public long getSpeedMeter() {
            return 0;
        }

        public void putSpeedMeter(final long bytes, final long time) {
        }

        public void resetSpeedMeter() {
        }

        public void setHandler(final ThrottledConnectionHandler manager) {
            this.handler = manager;
        }

        public void setLimit(final int kpsLimit) {
        }

        public long transfered() {
            return 0;
        }

    }

    private Chunk             CHUNK;
    private volatile long     BYTESLOADED = 0l;
    private long              SPEED       = 0l;
    private int               PID         = -1;
    private static String     RTMPDUMP    = null;
    private static String     RTMPVERSION = null;
    private NativeProcess     NP;
    private Process           P;

    private InputStreamReader R;

    public RtmpDump(final PluginForHost plugin, final DownloadLink downloadLink, final String rtmpURL) throws IOException, PluginException {
        super(plugin, downloadLink, rtmpURL);
    }

    /**
     * Attempt to locate a rtmpdump executable. The local tools folder is searched first, then *nix /usr bin folders. If found, the path
     * will is saved to the variable RTMPDUMP.
     * 
     * @return Whether or not rtmpdump executable was found
     */
    private synchronized boolean findRtmpDump() {
        if (RtmpDump.RTMPDUMP != null) { return RtmpDump.RTMPDUMP.length() > 0; }
        if (CrossSystem.isWindows()) {
            RtmpDump.RTMPDUMP = Application.getResource("tools/Windows/rtmpdump/rtmpdump.exe").getAbsolutePath();
        } else if (CrossSystem.isLinux()) {
            RtmpDump.RTMPDUMP = Application.getResource("tools/linux/rtmpdump/rtmpdump").getAbsolutePath();
        } else if (CrossSystem.isMac()) {
            RtmpDump.RTMPDUMP = Application.getResource("tools/mac/rtmpdump/rtmpdump").getAbsolutePath();
        }
        if (RtmpDump.RTMPDUMP != null && !new File(RtmpDump.RTMPDUMP).exists()) {
            RtmpDump.RTMPDUMP = null;
        }
        if (RtmpDump.RTMPDUMP == null && (CrossSystem.isLinux() || CrossSystem.isMac())) {
            if (RtmpDump.RTMPDUMP == null && (RtmpDump.RTMPDUMP = "/usr/bin/rtmpdump") != null && !new File(RtmpDump.RTMPDUMP).exists()) {
                RtmpDump.RTMPDUMP = null;
            }
            if (RtmpDump.RTMPDUMP == null && (RtmpDump.RTMPDUMP = "/usr/local/bin/rtmpdump") != null && !new File(RtmpDump.RTMPDUMP).exists()) {
                RtmpDump.RTMPDUMP = null;
            }
        }
        if (RtmpDump.RTMPDUMP == null) {
            RtmpDump.RTMPDUMP = "";
        }
        return RtmpDump.RTMPDUMP.length() > 0;
    }

    private void getProcessId() {
        try {
            final Field pidField = this.P.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            this.PID = pidField.getInt(this.P);
        } catch (final Exception e) {
            this.PID = -1;
        }
    }

    /**
     * Attempt to locate a rtmpdump executable and parse the version number from the 'rtmpdump -h' output.
     * 
     * @return The version number of the RTMPDump executable
     */
    public synchronized String getRtmpDumpVersion() throws Exception {
        if (RtmpDump.RTMPVERSION != null) { return RtmpDump.RTMPVERSION; }
        if (!this.findRtmpDump()) { throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDump not found!"); }
        final String arg = " -h";
        NativeProcess verNP = null;
        Process verP = null;
        InputStreamReader verR = null;
        try {
            if (CrossSystem.isWindows()) {
                verNP = new NativeProcess(RtmpDump.RTMPDUMP, arg);
                verR = new InputStreamReader(verNP.getErrorStream());
            } else {
                verP = Runtime.getRuntime().exec(RtmpDump.RTMPDUMP + arg);
                verR = new InputStreamReader(verP.getErrorStream());
            }
            final BufferedReader br = new BufferedReader(verR);
            final Pattern reg = Pattern.compile("RTMPDump v([0-9.]+)");
            String line = null;
            while ((line = br.readLine()) != null) {
                final Matcher match = reg.matcher(line);
                if (match.find()) {
                    RtmpDump.RTMPVERSION = match.group(1);
                    return RtmpDump.RTMPVERSION;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "Error " + RtmpDump.RTMPDUMP + " version not found!");
        } finally {
            try {
                /* make sure we destroyed the process */
                verP.destroy();
            } catch (final Throwable e) {
            }
            try {
                /* close InputStreamReader */
                verR.close();
            } catch (final Throwable e) {
            }
            if (verNP != null) {
                /* close Streams from native */
                verNP.closeStreams();
            }
        }
    }

    @Override
    public long getTotalLinkBytesLoadedLive() {
        return this.BYTESLOADED;
    }

    private void sendSIGINT() {
        this.getProcessId();
        if (this.PID >= 0) {
            try {
                Runtime.getRuntime().exec("kill -SIGINT " + String.valueOf(this.PID));
            } catch (final Throwable e1) {
            }
        }
    }

    public boolean start(final RtmpUrlConnection rtmpConnection) throws Exception {
        if (!this.findRtmpDump()) { throw new PluginException(LinkStatus.ERROR_FATAL, "Error " + RtmpDump.RTMPDUMP + " not found!"); }
        final ThrottledConnection tcon = new RTMPCon() {
            @Override
            public long getSpeedMeter() {
                return RtmpDump.this.SPEED;
            }

            @Override
            public long transfered() {
                return RtmpDump.this.BYTESLOADED;
            }
        };
        try {
            this.getManagedConnetionHandler().addThrottledConnection(tcon);
            this.addChunksDownloading(1);
            this.downloadLink.getLinkStatus().addStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
            try {
                this.downloadLink.getDownloadLinkController().getConnectionHandler().addConnectionHandler(this.getManagedConnetionHandler());
            } catch (final Throwable e) {
            }
            rtmpConnection.connect();

            File tmpFile = new File(this.downloadLink.getFileOutput() + ".part");
            if (!CrossSystem.isWindows()) {
                tmpFile = new File(this.downloadLink.getFileOutput().replaceAll("\\s", "\\\\s") + ".part");
            }
            String line = "", error = "";
            long iSize = 0;
            long before = 0;
            long lastTime = System.currentTimeMillis();

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
                this.setResume(true);
            }

            try {
                if (CrossSystem.isWindows()) {
                    this.NP = new NativeProcess(RtmpDump.RTMPDUMP, cmd);
                    this.R = new InputStreamReader(this.NP.getErrorStream());
                } else {
                    this.P = Runtime.getRuntime().exec(RtmpDump.RTMPDUMP + cmd);
                    this.R = new InputStreamReader(this.P.getErrorStream());
                }
                final BufferedReader br = new BufferedReader(this.R);
                float progressFloat = 0;
                while ((line = br.readLine()) != null) {
                    if (!line.equals("")) {
                        error = line;
                    }
                    if (!new Regex(line, "^[0-9]").matches()) {
                        if (line.contains("length") || line.contains("lastkeyframelocation")) {
                            String size = new Regex(line, ".*?(\\d.+)").getMatch(0);
                            iSize += SizeFormatter.getSize(size);
                        }
                    } else {
                        if (this.downloadLink.getDownloadSize() == 0) {
                            this.downloadLink.setDownloadSize(iSize);
                        }
                        // is resumed
                        if (iSize == 0) iSize = downloadLink.getDownloadSize();

                        if (line.toUpperCase().matches("\\d+\\.\\d+\\sKB\\s/\\s\\d+\\.\\d+\\sSEC(\\s\\(\\d+\\.\\d%\\))?")) {
                            this.BYTESLOADED = SizeFormatter.getSize(line.substring(0, line.toUpperCase().indexOf("KB") + 2));
                            progressFloat = (float) (Math.round(this.BYTESLOADED * 100.0F / (float) iSize * 10) / 10.0F);

                            if (Thread.currentThread().isInterrupted()) {
                                if (CrossSystem.isWindows()) {
                                    this.NP.sendCtrlCSignal();
                                } else {
                                    this.sendSIGINT();
                                }
                                // throw new InterruptedIOException();
                                return true;
                            }

                            if (System.currentTimeMillis() - lastTime > 1000) {
                                this.SPEED = (this.BYTESLOADED - before) / (System.currentTimeMillis() - lastTime) * 1000l;
                                lastTime = System.currentTimeMillis();
                                before = this.BYTESLOADED;
                                // downloadLink.requestGuiUpdate();
                                this.downloadLink.setChunksProgress(new long[] { this.BYTESLOADED });
                            }
                        }
                    }
                    if (!line.toLowerCase().contains("download complete")) {
                        continue;
                    }
                    // autoresuming when FMS sends NetStatus.Play.Stop and
                    // progress less than 100%
                    if (progressFloat < 99.8) {
                        System.out.println("Versuch Nr.: " + this.downloadLink.getLinkStatus().getRetryCount() + " ::: " + this.plugin.getMaxRetries(this.downloadLink, null));
                        if (this.downloadLink.getLinkStatus().getRetryCount() >= this.plugin.getMaxRetries(this.downloadLink, null)) {
                            this.downloadLink.getLinkStatus().setRetryCount(0);
                        }
                        this.downloadLink.getLinkStatus().setStatus(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE);
                    }
                    Thread.sleep(500);
                    break;
                }
            } finally {
                rtmpConnection.disconnect();
                try {
                    /* make sure we destroyed the process */
                    this.P.destroy();
                } catch (final Throwable e) {
                }
                try {
                    /* close InputStreamReader */
                    this.R.close();
                } catch (final Throwable e) {
                }
                if (this.NP != null) {
                    /* close Streams from native */
                    this.NP.closeStreams();
                }
            }
            if (this.downloadLink.getLinkStatus().getStatus() == LinkStatus.ERROR_DOWNLOAD_INCOMPLETE) {
                return false;
            } else if (line != null && line.toLowerCase().contains("download complete")) {
                this.downloadLink.setDownloadSize(this.BYTESLOADED);
                this.logger.finest("no errors : rename");
                if (!tmpFile.renameTo(new File(this.downloadLink.getFileOutput()))) {
                    this.logger.severe("Could not rename file " + tmpFile + " to " + this.downloadLink.getFileOutput());
                    this.error(LinkStatus.ERROR_LOCAL_IO, _JDT._.system_download_errors_couldnotrename());
                }
                this.downloadLink.getLinkStatus().addStatus(LinkStatus.FINISHED);
            } else {
                this.logger.severe("cmd: " + cmd);
                this.logger.severe(error);
                throw new PluginException(LinkStatus.ERROR_FATAL, error);
            }
            return true;
        } finally {
            this.downloadLink.setDownloadCurrent(this.BYTESLOADED);
            this.downloadLink.getLinkStatus().removeStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
            this.downloadLink.setDownloadInstance(null);
            this.downloadLink.getLinkStatus().setStatusText(null);
            this.getManagedConnetionHandler().removeThrottledConnection(tcon);
            try {
                this.downloadLink.getDownloadLinkController().getConnectionHandler().removeConnectionHandler(this.getManagedConnetionHandler());
            } catch (final Throwable e) {
            }
        }
    }

}
