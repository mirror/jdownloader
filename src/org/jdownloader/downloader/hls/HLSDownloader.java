package org.jdownloader.downloader.hls;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;
import jd.plugins.download.raf.FileBytesMap;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.SkippingLimitedOutputStream;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.net.httpserver.HttpServer;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.speedmeter.AverageSpeedMeter;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.controlling.ffmpeg.AbstractFFmpegBinary;
import org.jdownloader.controlling.ffmpeg.FFMpegException;
import org.jdownloader.controlling.ffmpeg.FFMpegProgress;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.controlling.ffmpeg.FFmpegMetaData;
import org.jdownloader.controlling.ffmpeg.FFmpegSetup;
import org.jdownloader.controlling.ffmpeg.FFprobe;
import org.jdownloader.controlling.ffmpeg.json.Stream;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.M3U8Playlist.M3U8Segment;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.translate._JDT;

//http://tools.ietf.org/html/draft-pantos-http-live-streaming-13
public class HLSDownloader extends DownloadInterface {
    private class PartFile {
        private final File          file;
        private final AtomicBoolean flag = new AtomicBoolean(false);

        private PartFile(File file) {
            this.file = file;
        }
    }

    private final AtomicLong                       bytesWritten         = new AtomicLong(0);
    private DownloadLinkDownloadable               downloadable;
    private DownloadLink                           link;
    private long                                   startTimeStamp       = -1;
    private LogInterface                           logger;
    private URLConnectionAdapter                   currentConnection;
    private ManagedThrottledConnectionHandler      connectionHandler;
    private File                                   outputCompleteFile;
    private PluginException                        caughtPluginException;
    private String                                 m3uUrl;
    private String                                 persistentParameters;
    private HttpServer                             server;
    private Browser                                sourceBrowser;
    private long                                   processID;
    protected volatile MeteredThrottledInputStream meteredThrottledInputStream;
    protected final AtomicReference<byte[]>        instanceBuffer       = new AtomicReference<byte[]>();
    private List<M3U8Playlist>                     m3u8Playlists;
    private final List<PartFile>                   outputPartFiles      = new ArrayList<PartFile>();
    private final AtomicInteger                    currentPlayListIndex = new AtomicInteger(0);
    private final HashMap<String, SecretKeySpec>   aes128Keys           = new HashMap<String, SecretKeySpec>();
    private final boolean                          isJared              = Application.isJared(HLSDownloader.class);

    public int getCurrentPlayListIndex() {
        return currentPlayListIndex.get();
    }

    public M3U8Playlist getCurrentPlayList() {
        return getPlayLists().get(getCurrentPlayListIndex());
    }

    public List<M3U8Playlist> getPlayLists() {
        return m3u8Playlists;
    }

    private final void init(final DownloadLink link, Browser br, String m3uUrl, final String persistantParameters) throws Exception {
        setPersistentParameters(persistantParameters);
        this.m3uUrl = Request.getLocation(m3uUrl, br.getRequest());
        this.sourceBrowser = br.cloneBrowser();
        this.link = link;
        logger = initLogger(link);
        connectionHandler = new ManagedThrottledConnectionHandler();
        downloadable = new DownloadLinkDownloadable(link) {
            @Override
            public boolean isResumable() {
                return link.getBooleanProperty("RESUME", true);
            }

            @Override
            public void setResumeable(boolean value) {
                link.setProperty("RESUME", value);
                super.setResumeable(value);
            }
        };
        m3u8Playlists = getM3U8Playlists();
        if (m3u8Playlists.size() == 0) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public HLSDownloader(final DownloadLink link, Browser br, String m3uUrl, final String persistentParameters) throws Exception {
        init(link, br, m3uUrl, persistentParameters);
    }

    public HLSDownloader(final DownloadLink link, final Browser br, final String m3uUrl) throws Exception {
        init(link, br, m3uUrl, null);
    }

    protected void setPersistentParameters(final String persistentParameters) {
        if (StringUtils.isEmpty(persistentParameters)) {
            this.persistentParameters = null;
        } else {
            final String parameter = persistentParameters.trim();
            if (parameter.startsWith("?") || parameter.startsWith("&")) {
                this.persistentParameters = parameter.substring(1);
            } else {
                this.persistentParameters = parameter;
            }
        }
    }

    protected String buildDownloadUrl(final String url) {
        final String persistentParameters = this.persistentParameters;
        if (persistentParameters == null) {
            return url;
        } else {
            try {
                return URLHelper.parseLocation(new URL(url), "&" + persistentParameters);
            } catch (MalformedURLException e) {
                if (url.contains("?")) {
                    return url + "&" + persistentParameters;
                } else {
                    return url + "?" + persistentParameters;
                }
            }
        }
    }

    public long getEstimatedSize() {
        return M3U8Playlist.getEstimatedSize(getPlayLists());
    }

    public boolean isEncrypted() {
        if (isJared) {
            if (m3u8Playlists != null) {
                for (final M3U8Playlist playlist : m3u8Playlists) {
                    if (playlist.isEncrypted()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected boolean isSupported(M3U8Playlist m3u8) {
        if (isJared) {
            if (m3u8 != null && m3u8.isEncrypted()) {
                return false;
            } else {
                return !isEncrypted();
            }
        }
        return true;
    }

    public LogInterface initLogger(final DownloadLink link) {
        final PluginForHost plg = link.getLivePlugin();
        LogInterface log = null;
        if (plg != null) {
            log = plg.getLogger();
        }
        if (log == null && sourceBrowser != null) {
            log = sourceBrowser.getLogger();
        }
        if (log == null) {
            log = LogController.TRASH;
        }
        return log;
    }

    protected void terminate() {
        if (terminated.getAndSet(true) == false) {
            if (!externalDownloadStop()) {
                if (logger != null) {
                    logger.severe("A critical Downloaderror occured. Terminate...");
                }
            }
        }
    }

    public StreamInfo getProbe() throws Exception {
        return getProbe(0);
    }

    public StreamInfo getProbe(int index) throws Exception {
        try {
            if (index < getM3U8Playlists().size()) {
                currentPlayListIndex.set(index);
            } else {
                throw new IllegalArgumentException("Index " + index + " > m3u8 playlist size " + getM3U8Playlists().size());
            }
            final FFprobe ffprobe = new FFprobe();
            if (!ffprobe.isAvailable()) {
                logger.info("FFProbe is not available");
                return null;
            } else {
                this.processID = new UniqueAlltimeID().getID();
                initPipe(ffprobe);
                return ffprobe.getStreamInfo("http://127.0.0.1:" + server.getPort() + "/m3u8?id=" + processID);
            }
        } finally {
            final HttpServer server = this.server;
            this.server = null;
            if (server != null) {
                server.stop();
                waitForProcessingRequests();
            }
        }
    }

    protected void waitForProcessingRequests() throws InterruptedException {
        while (true) {
            synchronized (requestsInProcess) {
                if (requestsInProcess.get() == 0) {
                    break;
                }
                requestsInProcess.wait(50);
            }
        }
    }

    protected String guessFFmpegFormat(final StreamInfo streamInfo) {
        if (streamInfo != null && streamInfo.getStreams() != null) {
            for (final Stream s : streamInfo.getStreams()) {
                if ("video".equalsIgnoreCase(s.getCodec_type())) {
                    return "mp4";
                }
            }
        }
        return null;
    }

    protected String getFFmpegFormat(final FFmpeg ffmpeg) throws Exception {
        String name = link.getForcedFileName();
        if (StringUtils.isEmpty(name)) {
            name = link.getFinalFileName();
            if (StringUtils.isEmpty(name)) {
                name = link.getRawName();
            }
            if (StringUtils.isEmpty(name)) {
                final String url = link.getContentUrlOrPatternMatcher();
                name = Plugin.extractFileNameFromURL(url);
            }
        }
        String format = ffmpeg.getDefaultFormatByFileName(name);
        if (format == null) {
            final StreamInfo streamInfo = getProbe();
            format = guessFFmpegFormat(streamInfo);
            if (format == null) {
                final String extension = Files.getExtension(name);
                if (extension == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String extensionID = extension.toLowerCase(Locale.ENGLISH);
                final FFmpegSetup config = JsonConfig.create(FFmpegSetup.class);
                synchronized (HLSDownloader.class) {
                    HashMap<String, String> map = config.getExtensionToFormatMap();
                    if (map == null) {
                        map = new HashMap<String, String>();
                    } else {
                        map = new HashMap<String, String>(map);
                    }
                    try {
                        format = map.get(extensionID);
                        if (format == null) {
                            final ArrayList<String> queryDefaultFormat = new ArrayList<String>();
                            queryDefaultFormat.add(ffmpeg.getFullPath());
                            final File dummy = Application.getTempResource("ffmpeg_dummy-" + System.currentTimeMillis() + "." + extension);
                            try {
                                queryDefaultFormat.add(dummy.getAbsolutePath());
                                queryDefaultFormat.add("-y");
                                ffmpeg.runCommand(null, queryDefaultFormat);
                            } finally {
                                dummy.delete();
                            }
                        }
                    } catch (FFMpegException e) {
                        final String res = e.getError();
                        format = new Regex(res, "Output \\#0\\, ([^\\,]+)").getMatch(0);
                        if (format == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, -1, e);
                        }
                        map.put(extensionID, format);
                        config.setExtensionToFormatMap(map);
                    }
                }
            }
        }
        return format;
    }

    private void runConcat() throws IOException, PluginException {
        try {
            final FFmpeg ffmpeg = new FFmpeg() {
                @Override
                protected void parseLine(boolean isStdout, String line) {
                }
            };
            processID = new UniqueAlltimeID().getID();
            initPipe(ffmpeg);
            final AtomicReference<File> outputFile = new AtomicReference<File>();
            final FFMpegProgress progress = new FFMpegProgress() {
                final long total;
                {
                    long total = 0;
                    for (final PartFile partFile : outputPartFiles) {
                        total += partFile.file.length();
                    }
                    this.total = total;
                }

                @Override
                public void setTotal(long total) {
                }

                public void updateValues(long current, long total) {
                };

                @Override
                public long getCurrent() {
                    final File file = outputFile.get();
                    if (file != null) {
                        return file.length();
                    } else {
                        return 0;
                    }
                };

                @Override
                public long getTotal() {
                    return total;
                }

                @Override
                public String getMessage(Object requestor) {
                    if (requestor instanceof ETAColumn) {
                        final long eta = getETA();
                        if (eta > 0) {
                            return Formatter.formatSeconds(eta);
                        }
                        return null;
                    }
                    return getDetaultMessage();
                }

                @Override
                public long getETA() {
                    final long runtime = System.currentTimeMillis() - startedTimestamp;
                    if (runtime > 0) {
                        final double speed = getCurrent() / (double) runtime;
                        if (speed > 0) {
                            return (long) ((getTotal() - getCurrent()) / speed);
                        } else {
                            return -1;
                        }
                    }
                    return -1;
                }

                @Override
                protected String getDetaultMessage() {
                    return _GUI.T.FFMpegProgress_getMessage_concat();
                }
            };
            progress.setProgressSource(this);
            boolean deleteOutput = true;
            final String concatFormat;
            if (CrossSystem.isWindows() && m3u8Playlists.size() > 1) {
                concatFormat = getFFmpegFormat(ffmpeg);
            } else {
                concatFormat = null;
            }
            try {
                downloadable.addPluginProgress(progress);
                try {
                    outputFile.set(outputCompleteFile);
                    ffmpeg.runCommand(null, buildConcatCommandLine(concatFormat, ffmpeg, outputCompleteFile.getAbsolutePath()));
                    deleteOutput = false;
                } catch (FFMpegException e) {
                    // some systems have problems with special chars to find the in or out file.
                    if (e.getError() != null && e.getError().contains("No such file or directory")) {
                        final File tmpOut = Application.getTempResource("ffmpeg_out" + UniqueAlltimeID.create());
                        outputFile.set(tmpOut);
                        ffmpeg.runCommand(null, buildConcatCommandLine(concatFormat, ffmpeg, tmpOut.getAbsolutePath()));
                        outputCompleteFile.delete();
                        if (tmpOut.renameTo(outputCompleteFile)) {
                            deleteOutput = false;
                        }
                    } else {
                        throw e;
                    }
                }
            } finally {
                downloadable.removePluginProgress(progress);
                if (deleteOutput) {
                    outputCompleteFile.delete();
                } else {
                    for (final PartFile partFile : outputPartFiles) {
                        partFile.file.delete();
                    }
                }
            }
        } catch (final FFMpegException e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, e.getMessage(), -1, e);
        } catch (Throwable e) {
            if (logger != null) {
                logger.log(e);
            }
        } finally {
            // link.removePluginProgress(set);
            final HttpServer server = this.server;
            this.server = null;
            if (server != null) {
                server.stop();
            }
        }
    }

    private void runDownload() throws Exception {
        link.setDownloadSize(-1);
        try {
            final AtomicLong lastBitrate = new AtomicLong(-1);
            final AtomicLong lastBytesWritten = new AtomicLong(0);
            final AtomicLong lastTime = new AtomicLong(0);
            final AtomicLong completeTime = new AtomicLong(0);
            final long estimatedDuration = M3U8Playlist.getEstimatedDuration(m3u8Playlists) / 1000;
            final FFmpeg ffmpeg = new FFmpeg() {
                @Override
                protected int exitProcess(Process process, String stdout, String stderr) throws IllegalThreadStateException {
                    try {
                        return process.exitValue();
                    } catch (IllegalThreadStateException e) {
                        if (stderr != null && stderr.matches("(?s).*video:\\d+\\w*\\s*audio:\\d+\\w*\\s*subtitle:\\d+\\w*.+")) {
                            return 0;
                        } else {
                            throw e;
                        }
                    }
                }

                @Override
                protected void parseLine(boolean isStdout, String line) {
                    try {
                        final String trimmedLine = line.trim();
                        if (trimmedLine.startsWith("Duration:")) {
                            // if (!line.contains("Duration: N/A")) {
                            // final String durationString = new Regex(line, "Duration\\: (.*?).?\\d*?\\, start").getMatch(0);
                            // if (durationString != null) {
                            // final long duration = formatStringToMilliseconds(durationString);
                            // }
                            // }
                        } else if (trimmedLine.startsWith("Stream #")) {
                            final String bitrateString = new Regex(line, "(\\d+) kb\\/s").getMatch(0);
                            if (bitrateString != null) {
                                if (lastBitrate.get() == -1) {
                                    lastBitrate.set(Integer.parseInt(bitrateString));
                                } else {
                                    lastBitrate.addAndGet(Integer.parseInt(bitrateString));
                                }
                                final long bitrate = lastBitrate.get();
                                if (estimatedDuration > 0 && bitrate > 0) {
                                    final long estimatedSize = ((estimatedDuration) * bitrate * 1024) / 8;
                                    downloadable.setDownloadTotalBytes(Math.max(M3U8Playlist.getEstimatedSize(m3u8Playlists), estimatedSize));
                                }
                            }
                        } else if (trimmedLine.startsWith("Output #0")) {
                            final long bitrate = lastBitrate.get();
                            if (estimatedDuration > 0 && bitrate > 0) {
                                final long estimatedSize = ((estimatedDuration) * bitrate * 1024) / 8;
                                downloadable.setDownloadTotalBytes(Math.max(M3U8Playlist.getEstimatedSize(m3u8Playlists), estimatedSize));
                            }
                        } else if (trimmedLine.startsWith("frame=") || trimmedLine.startsWith("size=")) {
                            final String sizeString = new Regex(line, "size=\\s*(\\S+)\\s+").getMatch(0);
                            final long size = SizeFormatter.getSize(sizeString);
                            final long currentBytesWritten = lastBytesWritten.get() + size;
                            bytesWritten.set(currentBytesWritten);
                            downloadable.setDownloadBytesLoaded(currentBytesWritten);
                            final String timeString = new Regex(line, "time=\\s*(\\S+)\\s+").getMatch(0);
                            long time = (formatStringToMilliseconds(timeString) / 1000);
                            lastTime.set(time);
                            time += completeTime.get();
                            final long estimatedSize;
                            if (time > 0 && estimatedDuration > 0) {
                                final long rate = currentBytesWritten / time;
                                estimatedSize = (estimatedDuration) * rate;
                            } else {
                                estimatedSize = currentBytesWritten;
                            }
                            downloadable.setDownloadTotalBytes(Math.max(M3U8Playlist.getEstimatedSize(m3u8Playlists), estimatedSize));
                        }
                    } catch (Throwable e) {
                        if (logger != null) {
                            logger.log(e);
                        }
                    }
                };
            };
            currentPlayListIndex.set(0);
            final String downloadFormat;
            if (CrossSystem.isWindows() && m3u8Playlists.size() > 1) {
                downloadFormat = "mpegts";
            } else {
                downloadFormat = getFFmpegFormat(ffmpeg);
            }
            processID = new UniqueAlltimeID().getID();
            initPipe(ffmpeg);
            for (int index = 0; index < m3u8Playlists.size(); index++) {
                final PartFile partFile = outputPartFiles.get(index);
                final File destination = partFile.file;
                try {
                    completeTime.addAndGet(lastTime.get());
                    lastBitrate.set(-1);
                    lastBytesWritten.set(bytesWritten.get());
                    currentPlayListIndex.set(index);
                    partFile.flag.set(true);
                    ffmpeg.runCommand(null, buildDownloadCommandLine(downloadFormat, ffmpeg, destination.getAbsolutePath()));
                } catch (FFMpegException e) {
                    // some systems have problems with special chars to find the in or out file.
                    if (e.getError() != null && e.getError().contains("No such file or directory")) {
                        final File tmpOut = Application.getTempResource("ffmpeg_out" + UniqueAlltimeID.create());
                        ffmpeg.runCommand(null, buildDownloadCommandLine(downloadFormat, ffmpeg, tmpOut.getAbsolutePath()));
                        destination.delete();
                        tmpOut.renameTo(destination);
                    } else {
                        throw e;
                    }
                }
            }
        } catch (final FFMpegException e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, e.getMessage(), -1, e);
        } catch (Exception e) {
            if (logger != null) {
                logger.log(e);
            }
            throw e;
        } finally {
            currentPlayListIndex.set(0);
            try {
                if (meteredThrottledInputStream != null) {
                    connectionHandler.removeThrottledConnection(meteredThrottledInputStream);
                }
            } finally {
                // link.removePluginProgress(set);
                final HttpServer server = this.server;
                this.server = null;
                if (server != null) {
                    server.stop();
                }
            }
        }
    }

    protected boolean isMapMetaDataEnabled() {
        return false;
    }

    protected boolean requiresAdtstoAsc(final String format, final FFmpeg ffmpeg) {
        return ffmpeg.requiresAdtstoAsc(format);
    }

    protected ArrayList<String> buildConcatCommandLine(final String format, FFmpeg ffmpeg, String out) {
        final ArrayList<String> l = new ArrayList<String>();
        l.add(ffmpeg.getFullPath());
        if (CrossSystem.isWindows()) {
            // workaround to support long path lengths
            // https://trac.ffmpeg.org/wiki/Concatenate
            l.add("-i");
            final StringBuilder sb = new StringBuilder();
            sb.append("concat:");
            boolean seperator = false;
            for (final PartFile outputPartFile : outputPartFiles) {
                if (seperator) {
                    sb.append("|");
                } else {
                    seperator = true;
                }
                sb.append("\\\\?\\" + outputPartFile.file.getAbsolutePath());
            }
            l.add(sb.toString());
        } else {
            l.add("-f");
            l.add("concat");
            l.add("-i");
            l.add("http://127.0.0.1:" + server.getPort() + "/concat?id=" + processID);
        }
        if (isMapMetaDataEnabled()) {
            final FFmpegMetaData ffMpegMetaData = getFFmpegMetaData();
            if (ffMpegMetaData != null && !ffMpegMetaData.isEmpty()) {
                l.add("-i");
                l.add("http://127.0.0.1:" + server.getPort() + "/meta?id=" + processID);
                l.add("-map_metadata");
                l.add("1");
            }
        }
        l.add("-c");
        l.add("copy");
        applyBitStreamFilter(l, format, ffmpeg);
        if (CrossSystem.isWindows() && out.length() > 259) {
            // https://msdn.microsoft.com/en-us/library/aa365247.aspx
            l.add("\\\\?\\" + out);
        } else {
            l.add(out);
        }
        l.add("-y");
        return l;
    }

    protected ArrayList<String> buildDownloadCommandLine(String format, FFmpeg ffmpeg, String out) {
        final ArrayList<String> l = new ArrayList<String>();
        l.add(ffmpeg.getFullPath());
        l.add("-analyzeduration");// required for low bandwidth streams!
        l.add("15000000");// 15 secs
        l.add("-i");
        l.add("http://127.0.0.1:" + server.getPort() + "/m3u8?id=" + processID);
        if (isMapMetaDataEnabled()) {
            final FFmpegMetaData ffMpegMetaData = getFFmpegMetaData();
            if (ffMpegMetaData != null && !ffMpegMetaData.isEmpty()) {
                l.add("-i");
                l.add("http://127.0.0.1:" + server.getPort() + "/meta?id=" + processID);
                l.add("-map_metadata");
                l.add("1");
            }
        }
        applyBitStreamFilter(l, format, ffmpeg);
        l.add("-c:v");
        l.add("copy");
        l.add("-c:a");
        l.add("copy");
        l.add("-f");
        l.add(format);
        if (CrossSystem.isWindows() && out.length() > 259) {
            // https://msdn.microsoft.com/en-us/library/aa365247.aspx
            l.add("\\\\?\\" + out);
        } else {
            l.add(out);
        }
        l.add("-y");
        // l.add("-progress");
        // l.add("http://127.0.0.1:" + server.getPort() + "/progress?id=" + processID);
        return l;
    }

    protected void applyBitStreamFilter(List<String> cmdLine, String format, FFmpeg ffmpeg) {
        if (format != null && "mpegts".equals(format)) {
            cmdLine.add("-bsf:v");
            cmdLine.add("h264_mp4toannexb");
        }
        if (format != null && requiresAdtstoAsc(format, ffmpeg)) {
            cmdLine.add("-bsf:a");
            cmdLine.add("aac_adtstoasc");
        }
    }

    protected FFmpegMetaData getFFmpegMetaData() {
        return null;
    }

    protected Browser getRequestBrowser() {
        final Browser ret = sourceBrowser.cloneBrowser();
        ret.setConnectTimeout(30 * 1000);
        ret.setReadTimeout(30 * 1000);
        return ret;
    }

    protected List<M3U8Playlist> getM3U8Playlists() throws Exception {
        final Browser br = getRequestBrowser();
        // work around for longggggg m3u pages
        final int was = br.getLoadLimit();
        // lets set the connection limit to our required request
        br.setLoadLimit(Integer.MAX_VALUE);
        try {
            return M3U8Playlist.loadM3U8(buildDownloadUrl(m3uUrl), br);
        } finally {
            br.setLoadLimit(was);
        }
    }

    private final AtomicInteger requestsInProcess = new AtomicInteger(0);

    private void initPipe(final AbstractFFmpegBinary ffmpeg) throws IOException {
        server = new HttpServer(0);
        server.setLocalhostOnly(true);
        final HttpServer finalServer = server;
        server.start();
        instanceBuffer.set(new byte[512 * 1024]);
        finalServer.registerRequestHandler(new HttpRequestHandler() {
            final byte[] readBuf = new byte[512];

            @Override
            public boolean onPostRequest(PostRequest request, HttpResponse response) {
                requestsInProcess.incrementAndGet();
                try {
                    if (logger != null) {
                        logger.info(request.toString());
                    }
                    if (!validateID(request)) {
                        return false;
                    }
                    if ("/progress".equals(request.getRequestedPath())) {
                        while (request.getInputStream().read(readBuf) != -1) {
                        }
                        response.setResponseCode(ResponseCode.SUCCESS_OK);
                        return true;
                    }
                } catch (Exception e) {
                    if (logger != null) {
                        logger.log(e);
                    }
                } finally {
                    synchronized (requestsInProcess) {
                        requestsInProcess.decrementAndGet();
                        requestsInProcess.notifyAll();
                    }
                }
                return false;
            }

            private final boolean validateID(HttpRequest request) throws IOException {
                final String id = request.getParameterbyKey("id");
                if (id == null) {
                    return false;
                }
                if (processID != Long.parseLong(request.getParameterbyKey("id"))) {
                    return false;
                }
                return true;
            }

            @Override
            public boolean onGetRequest(GetRequest request, HttpResponse response) {
                boolean requestOkay = false;
                final LogSource requestLogger = new LogSource(request.getRequestedURL());
                requestsInProcess.incrementAndGet();
                try {
                    requestLogger.info("START " + request.getRequestedURL());
                    requestLogger.info(request.toString());
                    if (!validateID(request)) {
                        requestLogger.info("invalid ID");
                        return false;
                    } else if ("/concat".equals(request.getRequestedPath())) {
                        final StringBuilder sb = new StringBuilder();
                        for (final PartFile partFile : outputPartFiles) {
                            if (sb.length() > 0) {
                                sb.append("\r\n");
                            }
                            sb.append("file '");
                            if (CrossSystem.isWindows()) {
                                // https://trac.ffmpeg.org/ticket/2702
                                // NOTE: this does not work for long path lengths! see different way in buildConcatCommandLine
                                sb.append("file:" + partFile.file.getAbsolutePath().replaceAll("\\\\", "/"));
                            } else {
                                sb.append("file://" + partFile.file.getAbsolutePath());
                            }
                            sb.append("'");
                        }
                        final byte[] bytes = sb.toString().getBytes("UTF-8");
                        response.setResponseCode(ResponseCode.get(200));
                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/plain"));
                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH, String.valueOf(bytes.length)));
                        final OutputStream out = response.getOutputStream(true);
                        out.write(bytes);
                        out.flush();
                        requestOkay = true;
                        return true;
                    } else if ("/meta".equals(request.getRequestedPath())) {
                        ffmpeg.updateLastUpdateTimestamp();
                        final FFmpegMetaData ffMpegMetaData = getFFmpegMetaData();
                        if (ffMpegMetaData != null && !ffMpegMetaData.isEmpty()) {
                            final String content = ffMpegMetaData.getFFmpegMetaData();
                            final byte[] bytes = content.getBytes("UTF-8");
                            response.setResponseCode(HTTPConstants.ResponseCode.get(200));
                            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/plain; charset=utf-8"));
                            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH, String.valueOf(bytes.length)));
                            final OutputStream out = response.getOutputStream(true);
                            if (bytes.length > 0) {
                                out.write(bytes);
                                out.flush();
                            }
                        } else {
                            response.setResponseCode(HTTPConstants.ResponseCode.get(404));
                        }
                        requestOkay = true;
                        return true;
                    } else if ("/m3u8".equals(request.getRequestedPath())) {
                        ffmpeg.updateLastUpdateTimestamp();
                        final M3U8Playlist m3u8 = getCurrentPlayList();
                        if (isSupported(m3u8)) {
                            final StringBuilder sb = new StringBuilder();
                            sb.append("#EXTM3U\r\n");
                            sb.append("#EXT-X-VERSION:3\r\n");
                            sb.append("#EXT-X-MEDIA-SEQUENCE:0\r\n");
                            if (m3u8.getTargetDuration() > 0) {
                                sb.append("#EXT-X-TARGETDURATION:");
                                sb.append(m3u8.getTargetDuration());
                                sb.append("\r\n");
                            }
                            for (int index = 0; index < m3u8.size(); index++) {
                                final M3U8Segment segment = m3u8.getSegment(index);
                                sb.append("#EXTINF:" + M3U8Segment.toExtInfDuration(segment.getDuration()));
                                sb.append("\r\nhttp://127.0.0.1:" + finalServer.getPort() + "/download?id=" + processID + "&ts_index=" + index + "\r\n");
                            }
                            sb.append("#EXT-X-ENDLIST\r\n");
                            response.setResponseCode(ResponseCode.get(200));
                            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "application/x-mpegURL"));
                            final byte[] bytes = sb.toString().getBytes("UTF-8");
                            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH, String.valueOf(bytes.length)));
                            final OutputStream out = response.getOutputStream(true);
                            out.write(bytes);
                            out.flush();
                        } else {
                            response.setResponseCode(ResponseCode.get(404));
                        }
                        requestOkay = true;
                        return true;
                    } else if ("/download".equals(request.getRequestedPath())) {
                        final String url = request.getParameterbyKey("url");
                        final String segmentIndex = request.getParameterbyKey("ts_index");
                        if (segmentIndex == null && url == null) {
                            return false;
                        }
                        final String downloadURL;
                        final M3U8Segment segment;
                        final M3U8Playlist playList;
                        if (url != null) {
                            // disabled in HLSDownloader! do not allow access to other urls than hls segments
                            segment = null;
                            downloadURL = url;
                            playList = null;
                            return false;
                        } else {
                            playList = getCurrentPlayList();
                            try {
                                final int index = Integer.parseInt(segmentIndex);
                                segment = playList.getSegment(index);
                                if (segment == null) {
                                    throw new IndexOutOfBoundsException("Unknown segment:" + index);
                                } else {
                                    requestLogger.info("Forward segment:" + (index + 1) + "/" + playList.size());
                                    downloadURL = segment.getUrl();
                                }
                            } catch (final NumberFormatException e) {
                                requestLogger.log(e);
                                return false;
                            } catch (final IndexOutOfBoundsException e) {
                                requestLogger.log(e);
                                return false;
                            }
                        }
                        final FileBytesMap fileBytesMap = new FileBytesMap();
                        final Browser br = getRequestBrowser();
                        br.setLogger(requestLogger);
                        OutputStream requestOutputStream = null;
                        retryLoop: for (int retry = 0; retry < 10; retry++) {
                            try {
                                final jd.http.requests.GetRequest getRequest = new jd.http.requests.GetRequest(buildDownloadUrl(downloadURL));
                                final long byteRange[] = segment.getByteRange();
                                if (fileBytesMap.getFinalSize() > 0) {
                                    requestLogger.info("Resume(" + retry + "): " + fileBytesMap.toString());
                                    final List<Long[]> unMarkedAreas = fileBytesMap.getUnMarkedAreas();
                                    final long startByteRange = byteRange != null ? byteRange[1] : 0;
                                    getRequest.getHeaders().put(HTTPConstants.HEADER_REQUEST_RANGE, "bytes=" + startByteRange + unMarkedAreas.get(0)[0] + "-" + startByteRange + unMarkedAreas.get(0)[1]);
                                } else if (byteRange != null) {
                                    getRequest.getHeaders().put(HTTPConstants.HEADER_REQUEST_RANGE, "bytes=" + byteRange[1] + "-" + (byteRange[1] + byteRange[0] - 1));
                                }
                                URLConnectionAdapter connection = null;
                                try {
                                    ffmpeg.updateLastUpdateTimestamp();
                                    connection = br.openRequestConnection(getRequest);
                                    if (connection.getResponseCode() != 200 && connection.getResponseCode() != 206) {
                                        throw new IOException("ResponseCode(" + connection.getResponseCode() + ") must be 200 or 206!");
                                    }
                                } catch (IOException e) {
                                    requestLogger.log(e);
                                    onSegmentException(connection, e);
                                    if (connection == null || connection.getResponseCode() == 504 || connection.getResponseCode() == 502 || connection.getResponseCode() == 999) {
                                        Thread.sleep(250 + (retry * 250));
                                        continue retryLoop;
                                    } else {
                                        return false;
                                    }
                                }
                                ffmpeg.updateLastUpdateTimestamp();
                                byte[] readWriteBuffer = HLSDownloader.this.instanceBuffer.getAndSet(null);
                                final boolean instanceBuffer;
                                if (readWriteBuffer != null) {
                                    instanceBuffer = true;
                                } else {
                                    instanceBuffer = false;
                                    readWriteBuffer = new byte[32 * 1024];
                                }
                                final long length;
                                if (fileBytesMap.getFinalSize() > 0) {
                                    length = fileBytesMap.getFinalSize();
                                } else if (byteRange != null) {
                                    length = connection.getContentLength();
                                } else {
                                    length = connection.getCompleteContentLength();
                                }
                                try {
                                    if (requestOutputStream == null) {
                                        response.setResponseCode(ResponseCode.SUCCESS_OK);
                                        if (length > 0) {
                                            fileBytesMap.setFinalSize(length);
                                            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH, Long.toString(length)));
                                        }
                                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, connection.getContentType()));
                                        requestOutputStream = response.getOutputStream(true);
                                    }
                                    final InputStream inputStream;
                                    if (segment != null && segment.isEncrypted()) {
                                        // FIXME: resume(range request) not supported yet
                                        final String keyURI = segment.getxKeyURI();
                                        if (M3U8Segment.X_KEY_METHOD.AES_128.equals(segment.getxKeyMethod()) && keyURI != null) {
                                            SecretKeySpec key = aes128Keys.get(keyURI);
                                            if (key == null) {
                                                final Browser br2 = getRequestBrowser();
                                                br2.setLogger(requestLogger);
                                                br2.setFollowRedirects(true);
                                                final URLConnectionAdapter con = br2.openGetConnection(buildDownloadUrl(keyURI));
                                                try {
                                                    if (con.getResponseCode() == 200) {
                                                        final byte[] buf = IO.readStream(20, con.getInputStream());
                                                        if (buf.length == 16) {
                                                            key = new SecretKeySpec(buf, "AES");
                                                            aes128Keys.put(keyURI, key);
                                                        }
                                                    }
                                                } finally {
                                                    con.disconnect();
                                                }
                                                if (key == null) {
                                                    throw new IOException("Failed to fetch #EXT-X-KEY:URI=" + keyURI);
                                                }
                                            }
                                            /*
                                             * https://tools.ietf.org/html/draft-pantos-http-live-streaming-20#section-5.2
                                             */
                                            final IvParameterSpec ivSpec;
                                            if (segment.getxKeyIV() != null) {
                                                ivSpec = new IvParameterSpec(HexFormatter.hexToByteArray(segment.getxKeyIV()));
                                            } else {
                                                final int sequenceNumber = playList.getMediaSequenceOffset() + playList.indexOf(segment);
                                                final byte[] iv = new byte[16];
                                                iv[15] = (byte) (sequenceNumber >>> 0 & 0xFF);
                                                iv[14] = (byte) (sequenceNumber >>> 8 & 0xFF);
                                                iv[13] = (byte) (sequenceNumber >>> 16 & 0xFF);
                                                ivSpec = new IvParameterSpec(iv);
                                            }
                                            try {
                                                final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
                                                inputStream = new javax.crypto.CipherInputStream(connection.getInputStream(), cipher);
                                            } catch (Exception e) {
                                                throw new IOException(e);
                                            }
                                        } else {
                                            throw new IOException("Unsupported Method #EXT-X-KEY:METHOD=" + segment.getxKeyMethod());
                                        }
                                    } else {
                                        inputStream = connection.getInputStream();
                                    }
                                    if (meteredThrottledInputStream == null) {
                                        meteredThrottledInputStream = new MeteredThrottledInputStream(inputStream, new AverageSpeedMeter(10));
                                        if (connectionHandler != null) {
                                            connectionHandler.addThrottledConnection(meteredThrottledInputStream);
                                        }
                                    } else {
                                        meteredThrottledInputStream.setInputStream(inputStream);
                                    }
                                    long position = fileBytesMap.getMarkedBytes();
                                    final OutputStream outputStream;
                                    if (position > 0) {
                                        if (connection.getResponseCode() == 206) {
                                            outputStream = requestOutputStream;
                                        } else {
                                            outputStream = new SkippingLimitedOutputStream(requestOutputStream, position);
                                        }
                                    } else {
                                        outputStream = requestOutputStream;
                                    }
                                    boolean writeToOutputStream = true;
                                    while (true) {
                                        final int len;
                                        try {
                                            len = meteredThrottledInputStream.read(readWriteBuffer);
                                            if (segment != null) {
                                                segment.setLoaded(true);
                                            }
                                        } catch (IOException e) {
                                            requestLogger.log(e);
                                            if (fileBytesMap.getFinalSize() > 0) {
                                                Thread.sleep(250 + (retry * 50));
                                                continue retryLoop;
                                            } else {
                                                throw e;
                                            }
                                        }
                                        if (len > 0) {
                                            ffmpeg.updateLastUpdateTimestamp();
                                            if (writeToOutputStream) {
                                                try {
                                                    outputStream.write(readWriteBuffer, 0, len);
                                                } catch (IOException e) {
                                                    requestLogger.log(e);
                                                    if (length != -1) {
                                                        throw e;
                                                    } else {
                                                        writeToOutputStream = false;
                                                    }
                                                }
                                            }
                                            fileBytesMap.mark(position, len);
                                            position += len;
                                        } else if (len == -1) {
                                            break;
                                        }
                                    }
                                    if (writeToOutputStream) {
                                        outputStream.flush();
                                        outputStream.close();
                                    }
                                    if (fileBytesMap.getSize() > 0) {
                                        requestOkay = fileBytesMap.getUnMarkedBytes() == 0;
                                    } else {
                                        requestOkay = true;
                                    }
                                    return true;
                                } finally {
                                    try {
                                        if (segment != null) {
                                            requestLogger.info("Segment:" + segment.getUrl() + "|Loaded:" + segment.isLoaded());
                                            if (connection.getResponseCode() == 200 || connection.getResponseCode() == 206) {
                                                segment.setSize(Math.max(length, fileBytesMap.getSize()));
                                            }
                                            if (segment.isLoaded() == false) {
                                                System.out.println("WTF");
                                            }
                                        }
                                        requestLogger.info(fileBytesMap.toString());
                                        if (instanceBuffer) {
                                            HLSDownloader.this.instanceBuffer.compareAndSet(null, readWriteBuffer);
                                        }
                                    } finally {
                                        connection.disconnect();
                                    }
                                }
                            } finally {
                                br.disconnect();
                            }
                        }
                    } else {
                        requestLogger.info("unhandled request:" + request.getRequestedURL());
                    }
                } catch (Throwable e) {
                    requestLogger.log(e);
                } finally {
                    requestLogger.info("END:" + requestOkay + ">" + request.getRequestedURL());
                    if (logger != null) {
                        logger.info(requestLogger.toString());
                    }
                    synchronized (requestsInProcess) {
                        requestsInProcess.decrementAndGet();
                        requestsInProcess.notifyAll();
                    }
                }
                return true;
            }
        });
    }

    protected void onSegmentException(URLConnectionAdapter connection, IOException e) {
    }

    public long getBytesLoaded() {
        return bytesWritten.get();
    }

    @Override
    public ManagedThrottledConnectionHandler getManagedConnetionHandler() {
        return connectionHandler;
    }

    @Override
    public URLConnectionAdapter connect(Browser br) throws Exception {
        throw new WTFException("Not needed");
    }

    @Override
    public long getTotalLinkBytesLoadedLive() {
        return getBytesLoaded();
    }

    @Override
    public boolean startDownload() throws Exception {
        if (isEncrypted()) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Encrypted HLS is not supported");
        }
        final List<File> requiredFiles = new ArrayList<File>();
        try {
            downloadable.setDownloadInterface(this);
            DownloadPluginProgress downloadPluginProgress = null;
            downloadable.setConnectionHandler(this.getManagedConnetionHandler());
            final DiskSpaceReservation reservation = downloadable.createDiskSpaceReservation();
            // TODO: update to handle 2x disk space usage (download + concat)
            try {
                if (!downloadable.checkIfWeCanWrite(new ExceptionRunnable() {
                    @Override
                    public void run() throws Exception {
                        downloadable.checkAndReserve(reservation);
                        requiredFiles.addAll(createOutputChannel());
                        try {
                            downloadable.lockFiles(requiredFiles.toArray(new File[0]));
                        } catch (FileIsLockedException e) {
                            downloadable.unlockFiles(requiredFiles.toArray(new File[0]));
                            throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
                        }
                    }
                }, null)) {
                    throw new SkipReasonException(SkipReason.INVALID_DESTINATION);
                }
                startTimeStamp = System.currentTimeMillis();
                downloadPluginProgress = new DownloadPluginProgress(downloadable, this, Color.GREEN.darker());
                downloadable.addPluginProgress(downloadPluginProgress);
                downloadable.setAvailable(AvailableStatus.TRUE);
                // TODO: add resume to continue with unfished playlist
                runDownload();
                if (outputPartFiles.size() > 1) {
                    for (PartFile partFile : outputPartFiles) {
                        if (!partFile.flag.get()) {
                            logger.severe("PartFile:" + partFile.file + " not loaded");
                            throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, "PartFile:" + partFile.file + " not loaded");
                        }
                    }
                    runConcat();
                }
            } finally {
                try {
                    downloadable.free(reservation);
                } catch (final Throwable e) {
                    LogSource.exception(logger, e);
                }
                try {
                    final long startTimeStamp = getStartTimeStamp();
                    if (startTimeStamp > 0) {
                        downloadable.addDownloadTime(System.currentTimeMillis() - getStartTimeStamp());
                    }
                } catch (final Throwable e) {
                }
                downloadable.removePluginProgress(downloadPluginProgress);
            }
            onDownloadReady();
            return handleErrors();
        } finally {
            downloadable.unlockFiles(requiredFiles.toArray(new File[0]));
            cleanupDownladInterface();
        }
    }

    protected void error(PluginException pluginException) {
        synchronized (this) {
            /* if we recieved external stop, then we dont have to handle errors */
            if (externalDownloadStop()) {
                return;
            }
            LogSource.exception(logger, pluginException);
            if (caughtPluginException == null) {
                caughtPluginException = pluginException;
            }
        }
        terminate();
    }

    protected void onDownloadReady() throws Exception {
        cleanupDownladInterface();
        if (!handleErrors() && !isAcceptDownloadStopAsValidEnd()) {
            return;
        }
        if (outputPartFiles.size() == 1) {
            final boolean renameOkay = downloadable.rename(outputPartFiles.get(0).file, outputCompleteFile);
            if (!renameOkay) {
                error(new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT.T.system_download_errors_couldnotrename(), LinkStatus.VALUE_LOCAL_IO_ERROR));
            }
        }
    }

    protected void cleanupDownladInterface() {
        try {
            downloadable.removeConnectionHandler(this.getManagedConnetionHandler());
        } catch (final Throwable e) {
        }
        try {
            final URLConnectionAdapter currentConnection = getConnection();
            if (currentConnection != null) {
                currentConnection.disconnect();
            }
        } catch (Throwable e) {
        }
    }

    private boolean handleErrors() throws PluginException {
        if (externalDownloadStop()) {
            return false;
        } else if (caughtPluginException != null) {
            throw caughtPluginException;
        } else if (!isAcceptDownloadStopAsValidEnd()) {
            for (final M3U8Playlist m3u8Playlist : m3u8Playlists) {
                for (int index = 0; index < m3u8Playlist.size(); index++) {
                    if (!m3u8Playlist.isSegmentLoaded(index)) {
                        logger.severe("Segment:" + index + ":" + m3u8Playlist.getSegment(index).getUrl() + " not loaded");
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Segment:" + index + " not loaded");
                    }
                }
            }
        }
        for (final PartFile partFile : outputPartFiles) {
            if (!partFile.flag.get()) {
                logger.severe("PartFile:" + partFile.file + " not loaded");
                throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE, "PartFile:" + partFile.file + " not loaded");
            }
        }
        if (outputPartFiles.size() == 1 && outputPartFiles.get(0).file.exists()) {
            downloadable.setLinkStatus(LinkStatus.FINISHED);
            final long fileSize = outputPartFiles.get(0).file.length();
            downloadable.setDownloadBytesLoaded(fileSize);
            downloadable.setVerifiedFileSize(fileSize);
            return true;
        } else if (outputCompleteFile.exists()) {
            downloadable.setLinkStatus(LinkStatus.FINISHED);
            final long fileSize = outputCompleteFile.length();
            downloadable.setDownloadBytesLoaded(fileSize);
            downloadable.setVerifiedFileSize(fileSize);
            return true;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private List<File> createOutputChannel() throws SkipReasonException {
        try {
            final List<File> requiredFiles = new ArrayList<File>();
            final String fileOutput = downloadable.getFileOutput();
            outputCompleteFile = new File(fileOutput);
            requiredFiles.add(outputCompleteFile);
            outputPartFiles.clear();
            if (m3u8Playlists.size() > 1) {
                for (int index = 0; index < m3u8Playlists.size(); index++) {
                    outputPartFiles.add(new PartFile(new File(downloadable.getFileOutputPart() + index + ".part")));
                }
            } else {
                outputPartFiles.add(new PartFile(new File(downloadable.getFileOutputPart())));
            }
            for (final PartFile partFile : outputPartFiles) {
                requiredFiles.add(partFile.file);
            }
            return requiredFiles;
        } catch (Exception e) {
            LogSource.exception(logger, e);
            throw new SkipReasonException(SkipReason.INVALID_DESTINATION, e);
        }
    }

    @Override
    public URLConnectionAdapter getConnection() {
        return currentConnection;
    }

    @Override
    public void stopDownload() {
        if (abort.getAndSet(true) == false) {
            if (logger != null) {
                logger.info("externalStop recieved");
            }
            terminate();
        }
    }

    private final AtomicBoolean abort                        = new AtomicBoolean(false);
    private final AtomicBoolean terminated                   = new AtomicBoolean(false);
    /**
     * if set to true, external Stops will finish and rename the file, else the file will be handled as unfinished. This is usefull for live
     * streams since
     */
    private boolean             acceptDownloadStopAsValidEnd = false;

    @Override
    public boolean externalDownloadStop() {
        return abort.get();
    }

    @Override
    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    @Override
    public void close() {
        final URLConnectionAdapter currentConnection = getConnection();
        if (currentConnection != null) {
            currentConnection.disconnect();
        }
    }

    @Override
    public Downloadable getDownloadable() {
        return downloadable;
    }

    @Override
    public boolean isResumedDownload() {
        return false;
    }

    public void setAcceptDownloadStopAsValidEnd(boolean b) {
        this.acceptDownloadStopAsValidEnd = b;
    }

    public boolean isAcceptDownloadStopAsValidEnd() {
        return acceptDownloadStopAsValidEnd;
    }
}
