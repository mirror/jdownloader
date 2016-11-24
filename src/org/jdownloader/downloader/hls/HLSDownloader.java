package org.jdownloader.downloader.hls;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
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
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.HttpServer;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
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
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.translate._JDT;

//http://tools.ietf.org/html/draft-pantos-http-live-streaming-13
public class HLSDownloader extends DownloadInterface {

    private volatile long                           bytesWritten         = 0l;
    private final DownloadLinkDownloadable          downloadable;
    private final DownloadLink                      link;
    private long                                    startTimeStamp       = -1;
    private final LogInterface                      logger;
    private URLConnectionAdapter                    currentConnection;
    private final ManagedThrottledConnectionHandler connectionHandler;
    private File                                    outputCompleteFile;
    private File                                    outputFinalCompleteFile;
    private File                                    outputPartFile;

    private PluginException                         caughtPluginException;
    private final String                            m3uUrl;
    private HttpServer                              server;

    private final Browser                           sourceBrowser;

    protected volatile long                         duration             = -1l;
    protected volatile int                          bitrate              = -1;
    private long                                    processID;
    protected MeteredThrottledInputStream           meteredThrottledInputStream;
    protected final AtomicReference<byte[]>         instanceBuffer       = new AtomicReference<byte[]>();
    private final List<M3U8Playlist>                m3u8Playlists;
    private final AtomicInteger                     currentPlayListIndex = new AtomicInteger(0);

    public int getCurrentPlayListIndex() {
        return currentPlayListIndex.get();
    }

    public M3U8Playlist getCurrentPlayList() {
        return getPlayLists().get(getCurrentPlayListIndex());
    }

    public List<M3U8Playlist> getPlayLists() {
        return m3u8Playlists;
    }

    public static final String ENCRYPTED_FLAG = "ENCRYPTED";

    public HLSDownloader(final DownloadLink link, Browser br2, String m3uUrl) throws IOException {
        this.m3uUrl = Request.getLocation(m3uUrl, br2.getRequest());
        this.sourceBrowser = br2.cloneBrowser();
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
        if (isEncrypted()) {
            link.setProperty(ENCRYPTED_FLAG, true);
        }
    }

    public long getEstimatedSize() {
        return M3U8Playlist.getEstimatedSize(m3u8Playlists);
    }

    public boolean isEncrypted() {
        if (m3u8Playlists != null) {
            for (M3U8Playlist playlist : m3u8Playlists) {
                if (playlist.isEncrypted) {
                    return true;
                }
            }
        }
        return false;
    }

    public LogInterface initLogger(final DownloadLink link) {
        PluginForHost plg = link.getLivePlugin();
        if (plg == null) {
            plg = link.getDefaultPlugin();
        }
        return plg == null ? null : plg.getLogger();
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

    public StreamInfo getProbe() throws IOException {
        try {
            final FFprobe ffprobe = new FFprobe();
            if (!ffprobe.isAvailable()) {
                return null;
            } else {
                this.processID = new UniqueAlltimeID().getID();
                initPipe(ffprobe);
                return ffprobe.getStreamInfo("http://127.0.0.1:" + server.getPort() + "/m3u8?id=" + processID);
            }
        } finally {
            if (server != null) {
                server.stop();
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

    protected String getFFmpegFormat(final FFmpeg ffmpeg) throws PluginException, IOException, InterruptedException, FFMpegException {
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

    public void run() throws IOException, PluginException {
        link.setDownloadSize(-1);
        final FFMpegProgress set = new FFMpegProgress();
        try {
            final FFmpeg ffmpeg = new FFmpeg() {
                protected void parseLine(boolean stdStream, StringBuilder ret, String line) {
                    try {
                        final String trimmedLine = line.trim();
                        if (trimmedLine.startsWith("Duration:")) {
                            if (!line.contains("Duration: N/A")) {
                                String duration = new Regex(line, "Duration\\: (.*?).?\\d*?\\, start").getMatch(0);
                                HLSDownloader.this.duration = formatStringToMilliseconds(duration);
                            }
                        } else if (trimmedLine.startsWith("Stream #")) {
                            final String bitrate = new Regex(line, "(\\d+) kb\\/s").getMatch(0);
                            if (bitrate != null) {
                                if (HLSDownloader.this.bitrate == -1) {
                                    HLSDownloader.this.bitrate = 0;
                                }
                                HLSDownloader.this.bitrate += Integer.parseInt(bitrate);
                            }
                        } else if (trimmedLine.startsWith("Output #0")) {
                            if (duration > 0 && bitrate > 0) {
                                link.setDownloadSize(((duration / 1000) * bitrate * 1024) / 8);
                            }
                        } else if (trimmedLine.startsWith("frame=") || trimmedLine.startsWith("size=")) {
                            final String size = new Regex(line, "size=\\s*(\\S+)\\s+").getMatch(0);
                            long newSize = SizeFormatter.getSize(size);
                            bytesWritten = newSize;
                            downloadable.setDownloadBytesLoaded(bytesWritten);
                            final String time = new Regex(line, "time=\\s*(\\S+)\\s+").getMatch(0);
                            // final String bitrate = new Regex(line, "bitrate=\\s*([\\d\\.]+)").getMatch(0);
                            long timeInSeconds = (formatStringToMilliseconds(time) / 1000);
                            if (timeInSeconds > 0 && duration > 0) {
                                long rate = bytesWritten / timeInSeconds;
                                link.setDownloadSize(((duration / 1000) * rate));
                            } else {
                                link.setDownloadSize(bytesWritten);
                            }
                        }
                    } catch (Throwable e) {
                        if (logger != null) {
                            logger.log(e);
                        }
                    }
                };
            };
            final String format = getFFmpegFormat(ffmpeg);
            final String out = outputPartFile.getAbsolutePath();
            try {
                processID = new UniqueAlltimeID().getID();
                initPipe(ffmpeg);
                runFF(set, format, ffmpeg, out);
            } catch (FFMpegException e) {
                // some systems have problems with special chars to find the in or out file.
                if (e.getError() != null && e.getError().contains("No such file or directory")) {
                    final File tmpOut = Application.getTempResource("ffmpeg_out" + UniqueAlltimeID.create());
                    runFF(set, format, ffmpeg, tmpOut.getAbsolutePath());
                    outputPartFile.delete();
                    tmpOut.renameTo(outputPartFile);
                } else {
                    throw e;
                }
            }
        } catch (InterruptedException e) {
            if (logger != null) {
                logger.log(e);
            }
        } catch (final FFMpegException e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, e.getMessage(), -1, e);
        } catch (Throwable e) {
            if (logger != null) {
                logger.log(e);
            }
        } finally {
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

    protected ArrayList<String> buildCommandLine(String format, FFmpeg ffmpeg, String out) {
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
        if (requiresAdtstoAsc(format, ffmpeg)) {
            l.add("-bsf:a");
            l.add("aac_adtstoasc");
        }
        l.add("-c");
        l.add("copy");
        l.add("-f");
        l.add(format);
        l.add(out);
        l.add("-y");
        l.add("-progress");
        l.add("http://127.0.0.1:" + server.getPort() + "/progress?id=" + processID);
        return l;
    }

    protected void runFF(FFMpegProgress set, String format, FFmpeg ffmpeg, String out) throws IOException, InterruptedException, FFMpegException {
        ffmpeg.runCommand(set, buildCommandLine(format, ffmpeg, out));
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

    protected List<M3U8Playlist> getM3U8Playlists() throws IOException {
        final Browser br = getRequestBrowser();
        // work around for longggggg m3u pages
        final int was = br.getLoadLimit();
        // lets set the connection limit to our required request
        br.setLoadLimit(Integer.MAX_VALUE);
        try {
            return M3U8Playlist.loadM3U8(m3uUrl, br);
        } finally {
            br.setLoadLimit(was);
        }
    }

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
                try {
                    if (logger != null) {
                        logger.info(request.toString());
                    }
                    if (processID != Long.parseLong(request.getParameterbyKey("id"))) {
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
                }
                return false;
            }

            @Override
            public boolean onGetRequest(GetRequest request, HttpResponse response) {
                boolean requestOkay = false;
                try {
                    if (logger != null) {
                        logger.info("START " + request.getRequestedURL());
                    }
                    if (logger != null) {
                        logger.info(request.toString());
                    }
                    final String id = request.getParameterbyKey("id");
                    if (id == null) {
                        return false;
                    }
                    if (processID != Long.parseLong(request.getParameterbyKey("id"))) {
                        return false;
                    }
                    if ("/meta".equals(request.getRequestedPath())) {
                        ffmpeg.updateLastUpdateTimestamp();
                        final FFmpegMetaData ffMpegMetaData = getFFmpegMetaData();
                        final byte[] bytes;
                        if (ffMpegMetaData != null) {
                            final String content = ffMpegMetaData.getFFmpegMetaData();
                            bytes = content.getBytes("UTF-8");
                        } else {
                            bytes = new byte[0];
                        }
                        if (bytes.length > 0) {
                            response.setResponseCode(HTTPConstants.ResponseCode.get(200));
                        } else {
                            response.setResponseCode(HTTPConstants.ResponseCode.get(404));
                        }
                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/plain; charset=utf-8"));
                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH, String.valueOf(bytes.length)));
                        final OutputStream out = response.getOutputStream(true);
                        if (bytes.length > 0) {
                            out.write(bytes);
                            out.flush();
                        }
                        requestOkay = true;
                        return true;
                    } else if ("/m3u8".equals(request.getRequestedPath())) {
                        ffmpeg.updateLastUpdateTimestamp();
                        final M3U8Playlist m3u8 = getCurrentPlayList();
                        if (m3u8.isEncrypted()) {
                            response.setResponseCode(ResponseCode.get(404));
                        } else {
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
                            byte[] bytes = sb.toString().getBytes("UTF-8");
                            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH, String.valueOf(bytes.length)));
                            OutputStream out = response.getOutputStream(true);
                            out.write(bytes);
                            out.flush();
                        }
                        requestOkay = true;
                        return true;
                    } else if ("/download".equals(request.getRequestedPath())) {
                        final String url = request.getParameterbyKey("url");
                        final String indexString = request.getParameterbyKey("ts_index");
                        if (indexString == null && url == null) {
                            return false;
                        }
                        final String downloadURL;
                        final M3U8Segment segment;
                        if (url != null) {
                            segment = null;
                            downloadURL = url;
                            return false;// disabled in HLSDownloader! do not allow access to other urls than hls segments
                        } else {
                            try {
                                final int index = Integer.parseInt(indexString);
                                segment = getCurrentPlayList().getSegment(index);
                                if (segment == null) {
                                    throw new IndexOutOfBoundsException("Unknown segment:" + index);
                                } else {
                                    if (logger != null) {
                                        logger.info("Forward segment:" + (index + 1) + "/" + m3u8Playlists.size());
                                    }
                                    downloadURL = segment.getUrl();
                                }
                            } catch (final NumberFormatException e) {
                                if (logger != null) {
                                    logger.log(e);
                                }
                                return false;
                            } catch (final IndexOutOfBoundsException e) {
                                if (logger != null) {
                                    logger.log(e);
                                }
                                return false;
                            }
                        }
                        OutputStream outputStream = null;
                        final FileBytesMap fileBytesMap = new FileBytesMap();
                        final Browser br = getRequestBrowser();
                        retryLoop: for (int retry = 0; retry < 10; retry++) {
                            try {
                                br.disconnect();
                            } catch (final Throwable e) {
                            }
                            final jd.http.requests.GetRequest getRequest = new jd.http.requests.GetRequest(downloadURL);
                            if (fileBytesMap.getFinalSize() > 0) {
                                if (logger != null) {
                                    logger.info("Resume(" + retry + "): " + fileBytesMap.toString());
                                }
                                final List<Long[]> unMarkedAreas = fileBytesMap.getUnMarkedAreas();
                                getRequest.getHeaders().put(HTTPConstants.HEADER_REQUEST_RANGE, "bytes=" + unMarkedAreas.get(0)[0] + "-" + unMarkedAreas.get(0)[1]);
                            }
                            URLConnectionAdapter connection = null;
                            try {
                                ffmpeg.updateLastUpdateTimestamp();
                                connection = br.openRequestConnection(getRequest);
                                if (connection.getResponseCode() != 200 && connection.getResponseCode() != 206) {
                                    throw new IOException("ResponseCode must be 200 or 206!");
                                }
                            } catch (IOException e) {
                                onSegmentException(connection, e);
                                if (logger != null) {
                                    logger.log(e);
                                }
                                if (connection == null || connection.getResponseCode() == 504) {
                                    Thread.sleep(250 + (retry * 50));
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
                            final long length = connection.getCompleteContentLength();
                            try {
                                if (outputStream == null) {
                                    response.setResponseCode(HTTPConstants.ResponseCode.get(br.getRequest().getHttpConnection().getResponseCode()));
                                    if (length > 0) {
                                        fileBytesMap.setFinalSize(length);
                                        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_LENGTH, Long.toString(length)));
                                    }
                                    response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, connection.getContentType()));
                                    outputStream = response.getOutputStream(true);
                                }
                                if (meteredThrottledInputStream == null) {
                                    meteredThrottledInputStream = new MeteredThrottledInputStream(connection.getInputStream(), new AverageSpeedMeter(10));
                                    if (connectionHandler != null) {
                                        connectionHandler.addThrottledConnection(meteredThrottledInputStream);
                                    }
                                } else {
                                    meteredThrottledInputStream.setInputStream(connection.getInputStream());
                                }
                                long position = fileBytesMap.getMarkedBytes();
                                while (true) {
                                    int len = -1;
                                    try {
                                        len = meteredThrottledInputStream.read(readWriteBuffer);
                                    } catch (IOException e) {
                                        if (fileBytesMap.getFinalSize() > 0) {
                                            Thread.sleep(250 + (retry * 50));
                                            continue retryLoop;
                                        } else {
                                            throw e;
                                        }
                                    }
                                    if (len > 0) {
                                        ffmpeg.updateLastUpdateTimestamp();
                                        outputStream.write(readWriteBuffer, 0, len);
                                        if (segment != null) {
                                            segment.setLoaded(true);
                                        }
                                        fileBytesMap.mark(position, len);
                                        position += len;
                                    } else if (len == -1) {
                                        break;
                                    }
                                }
                                outputStream.flush();
                                outputStream.close();
                                if (fileBytesMap.getSize() > 0) {
                                    requestOkay = fileBytesMap.getUnMarkedBytes() == 0;
                                } else {
                                    requestOkay = true;
                                }
                                return true;
                            } finally {
                                if (segment != null && (connection.getResponseCode() == 200 || connection.getResponseCode() == 206)) {
                                    segment.setSize(Math.max(connection.getCompleteContentLength(), fileBytesMap.getSize()));
                                }
                                if (instanceBuffer) {
                                    HLSDownloader.this.instanceBuffer.compareAndSet(null, readWriteBuffer);
                                }
                                connection.disconnect();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    if (logger != null) {
                        logger.log(e);
                    }
                } catch (IOException e) {
                    if (logger != null) {
                        logger.log(e);
                    }
                } finally {
                    if (logger != null) {
                        logger.info("END:" + requestOkay + ">" + request.getRequestedURL());
                    }
                }
                return true;
            }
        });

    }

    protected void onSegmentException(URLConnectionAdapter connection, IOException e) {
    }

    public long getBytesLoaded() {
        return bytesWritten;
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
        try {
            downloadable.setDownloadInterface(this);
            DownloadPluginProgress downloadPluginProgress = null;
            downloadable.setConnectionHandler(this.getManagedConnetionHandler());
            final DiskSpaceReservation reservation = downloadable.createDiskSpaceReservation();
            try {
                if (!downloadable.checkIfWeCanWrite(new ExceptionRunnable() {

                    @Override
                    public void run() throws Exception {
                        downloadable.checkAndReserve(reservation);
                        createOutputChannel();
                        try {
                            downloadable.lockFiles(outputCompleteFile, outputFinalCompleteFile, outputPartFile);
                        } catch (FileIsLockedException e) {
                            downloadable.unlockFiles(outputCompleteFile, outputFinalCompleteFile, outputPartFile);
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
                run();
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
            downloadable.unlockFiles(outputCompleteFile, outputFinalCompleteFile, outputPartFile);
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
        final boolean renameOkay = downloadable.rename(outputPartFile, outputCompleteFile);
        if (!renameOkay) {
            error(new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT.T.system_download_errors_couldnotrename(), LinkStatus.VALUE_LOCAL_IO_ERROR));
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
        }
        if (!isAcceptDownloadStopAsValidEnd()) {
            for (final M3U8Playlist m3u8Playlist : m3u8Playlists) {
                for (int index = 0; index < m3u8Playlist.size(); index++) {
                    if (!m3u8Playlist.isSegmentLoaded(index)) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                    }
                }
            }
        }
        if (caughtPluginException == null) {
            downloadable.setLinkStatus(LinkStatus.FINISHED);
            final long fileSize = outputCompleteFile.length();
            downloadable.setDownloadBytesLoaded(fileSize);
            downloadable.setVerifiedFileSize(fileSize);
            return true;
        } else {
            throw caughtPluginException;
        }
    }

    private void createOutputChannel() throws SkipReasonException {
        try {
            final String fileOutput = downloadable.getFileOutput();
            if (logger != null) {
                logger.info("createOutputChannel for " + fileOutput);
            }
            final String finalFileOutput = downloadable.getFinalFileOutput();
            outputCompleteFile = new File(fileOutput);
            outputFinalCompleteFile = outputCompleteFile;
            if (!fileOutput.equals(finalFileOutput)) {
                outputFinalCompleteFile = new File(finalFileOutput);
            }
            outputPartFile = new File(downloadable.getFileOutputPart());
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
