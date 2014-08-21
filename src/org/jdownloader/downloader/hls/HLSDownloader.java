package org.jdownloader.downloader.hls;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.downloadcontroller.DiskSpaceReservation;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.FileIsLockedException;
import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
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
import org.jdownloader.controlling.ffmpeg.FFMpegException;
import org.jdownloader.controlling.ffmpeg.FFMpegProgress;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.controlling.ffmpeg.FFmpegSetup;
import org.jdownloader.controlling.ffmpeg.FFprobe;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.plugins.DownloadPluginProgress;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.translate._JDT;

//http://tools.ietf.org/html/draft-pantos-http-live-streaming-13
public class HLSDownloader extends DownloadInterface {

    private long                              bytesWritten = 0l;
    private DownloadLinkDownloadable          downloadable;
    private DownloadLink                      link;
    private long                              startTimeStamp;
    private LogSource                         logger;
    private URLConnectionAdapter              currentConnection;
    private ManagedThrottledConnectionHandler connectionHandler;
    private File                              outputCompleteFile;
    private File                              outputFinalCompleteFile;
    private File                              outputPartFile;

    private PluginException                   caughtPluginException;
    private String                            m3uUrl;
    private HttpServer                        server;
    private Browser                           br;
    private int                               port;
    protected long                            duration;
    protected int                             bitrate      = 0;
    private long                              processID;
    protected MeteredThrottledInputStream     meteredThrottledInputStream;

    public HLSDownloader(final DownloadLink link, Browser br2, String m3uUrl) {

        this.m3uUrl = m3uUrl;
        this.br = br2;
        this.link = link;
        PluginForHost plg = link.getLivePlugin();
        if (plg == null) {
            plg = link.getDefaultPlugin();
        }
        logger = plg.getLogger();

    }

    protected void terminate() {
        if (terminated.getAndSet(true) == false) {
            if (!externalDownloadStop()) {
                logger.severe("A critical Downloaderror occured. Terminate...");
            }
        }
    }

    public StreamInfo getProbe() throws IOException {
        initPipe();
        try {
            FFprobe ffmpeg = new FFprobe();
            this.processID = new UniqueAlltimeID().getID();
            return ffmpeg.getStreamInfo("http://127.0.0.1:" + port + "/m3u8?id=" + processID + "&url=" + Encoding.urlEncode(m3uUrl));

        } finally {

            server.stop();
        }
    }

    public void run() throws IOException, PluginException {
        initPipe();
        processID = new UniqueAlltimeID().getID();
        link.setDownloadSize(-1);
        File file = new File(link.getFileOutput() + ".part");
        FFMpegProgress set = new FFMpegProgress() {

        };
        try {
            // link.addPluginProgress(set);
            FFmpegSetup config = JsonConfig.create(FFmpegSetup.class);
            HashMap<String, String> map = config.getExtensionToFormatMap();
            if (map == null) {
                map = new HashMap<String, String>();
            }
            String cust = link.getCustomExtension();
            link.setCustomExtension(null);
            String name = link.getName();
            link.setCustomExtension(cust);
            String extension = Files.getExtension(name);
            String format = map.get(extension.toLowerCase(Locale.ENGLISH));

            FFmpeg ffmpeg = new FFmpeg() {
                protected void parseLine(boolean stdStream, StringBuilder ret, String line) {
                    if (line.trim().startsWith("Duration:")) {
                        String duration = new Regex(line, "Duration\\: (.*?).?\\d*?\\, start").getMatch(0);
                        HLSDownloader.this.duration = formatStringToMilliseconds(duration);
                    } else if (line.trim().startsWith("Stream #")) {
                        String bitrate = new Regex(line, "(\\d+) kb\\/s").getMatch(0);
                        if (bitrate != null) {
                            HLSDownloader.this.bitrate += Integer.parseInt(bitrate);
                        }
                    } else if (line.trim().startsWith("Output #0")) {
                        link.setDownloadSize(((duration / 1000) * bitrate * 1024) / 8);

                    } else if (line.trim().startsWith("frame=")) {
                        String size = new Regex(line, "size=\\s*(\\S+)\\s+").getMatch(0);
                        long newSize = SizeFormatter.getSize(size);

                        bytesWritten = newSize;
                        downloadable.setDownloadBytesLoaded(bytesWritten);
                        String time = new Regex(line, "time=\\s*(\\S+)\\s+").getMatch(0);
                        String bitrate = new Regex(line, "bitrate=\\s*([\\d\\.]+)").getMatch(0);
                        if (time != null && duration > 0) {
                            long rate = bytesWritten / (formatStringToMilliseconds(time) / 1000);
                            link.setDownloadSize(((duration / 1000) * rate));
                        } else {
                            link.setDownloadSize(bytesWritten);
                        }

                    }

                };
            };
            if (format == null) {
                try {
                    ArrayList<String> l = new ArrayList<String>();
                    l.add(ffmpeg.getFullPath());
                    File dummy = Application.getTempResource("ffmpeg_dummy." + extension);
                    l.add(dummy.getAbsolutePath());
                    l.add("-y");
                    ffmpeg.runCommand(null, l);

                } catch (FFMpegException e) {
                    String res = e.getError();
                    format = new Regex(res, "Output \\#0\\, ([^\\,]+)").getMatch(0);
                    if (format == null) {
                        throw e;
                    }
                    synchronized (config) {
                        map = config.getExtensionToFormatMap();
                        if (map == null) {
                            map = new HashMap<String, String>();
                        }
                        map.put(extension.toLowerCase(Locale.ENGLISH), format);
                        config.setExtensionToFormatMap(map);
                    }

                }
            }

            ArrayList<String> l = new ArrayList<String>();
            l.add(ffmpeg.getFullPath());
            l.add("-i");
            l.add("http://127.0.0.1:" + port + "/m3u8?id=" + processID + "&url=" + Encoding.urlEncode(m3uUrl));
            // l.add(m3uUrl);
            // 2.1 aac_adtstoasc
            // Convert MPEG-2/4 AAC ADTS to MPEG-4 Audio Specific Configuration bitstream filter.
            //
            // This filter creates an MPEG-4 AudioSpecificConfig from an MPEG-2/4 ADTS header and removes the ADTS header.
            //
            // This is required for example when copying an AAC stream from a raw ADTS AAC container to a FLV or a MOV/MP4 file.
            //
            //
            if ("mp4".equalsIgnoreCase(extension) || "m4v".equalsIgnoreCase(extension) || "m4a".equalsIgnoreCase(extension) || "mov".equalsIgnoreCase(extension) || "flv".equalsIgnoreCase(extension)) {
                l.add("-bsf:a");
                l.add("aac_adtstoasc");
            }
            l.add("-c");
            l.add("copy");
            l.add("-f");
            l.add(format);
            l.add(outputPartFile.getAbsolutePath());
            l.add("-y");
            l.add("-progress");
            l.add("http://127.0.0.1:" + port + "/progress?id=" + processID);
            ffmpeg.runCommand(set, l);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (FFMpegException e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, e.getMessage());
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (connectionHandler != null && meteredThrottledInputStream != null) {
                connectionHandler.removeThrottledConnection(meteredThrottledInputStream);
            }
            // link.removePluginProgress(set);
            server.stop();
        }
    }

    private void initPipe() throws IOException {
        int port = 9667;
        while (port < 65000) {
            try {

                server = new HttpServer(port);
                server.setLocalhostOnly(true);
                server.start();
                this.port = port;
                break;
            } catch (java.net.BindException e) {
                port++;
                // try next port
            }
        }
        server.registerRequestHandler(new HttpRequestHandler() {

            @Override
            public boolean onPostRequest(PostRequest request, HttpResponse response) {
                try {

                    if (processID != Long.parseLong(request.getParameterbyKey("id"))) {
                        return false;
                    }

                    if ("/progress".equals(request.getRequestedPath())) {

                        BufferedReader f = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
                        final StringBuilder ret = new StringBuilder();
                        final String sep = System.getProperty("line.separator");
                        String line;
                        while ((line = f.readLine()) != null) {
                            if (ret.length() > 0) {
                                ret.append(sep);
                            } else if (line.startsWith("\uFEFF")) {
                                /*
                                 * Workaround for this bug: http://bugs.sun.com/view_bug.do?bug_id=4508058
                                 * http://bugs.sun.com/view_bug.do?bug_id=6378911
                                 */

                                line = line.substring(1);
                            }

                            ret.append(line);
                        }
                        response.setResponseCode(ResponseCode.SUCCESS_OK);
                        return true;

                    }

                } catch (Exception e) {
                    logger.log(e);
                }
                return false;
            }

            @Override
            public boolean onGetRequest(GetRequest request, HttpResponse response) {
                try {
                    String id = request.getParameterbyKey("id");
                    if (id == null) {
                        System.out.println(1);
                        return false;
                    }
                    if (processID != Long.parseLong(request.getParameterbyKey("id"))) {
                        return false;
                    }
                    if ("/m3u8".equals(request.getRequestedPath())) {
                        String url = request.getParameterbyKey("url");
                        if (url == null) {

                            return false;
                        }
                        if (StringUtils.equals(m3uUrl, url)) {
                            br.getPage(m3uUrl);

                            response.setResponseCode(HTTPConstants.ResponseCode.get(br.getRequest().getHttpConnection().getResponseCode()));
                            String playlist = br.toString();
                            StringBuilder sb = new StringBuilder();
                            for (String s : Regex.getLines(playlist)) {
                                if (sb.length() > 0) {
                                    sb.append("\r\n");
                                }
                                if (s.startsWith("http://")) {
                                    sb.append("http://127.0.0.1:" + HLSDownloader.this.port + "/download?id=" + processID + "&url=" + Encoding.urlEncode(s));
                                } else if (!s.trim().startsWith("#")) {
                                    sb.append("http://127.0.0.1:" + HLSDownloader.this.port + "/download?id=" + processID + "&url=" + Encoding.urlEncode(br.getBaseURL() + s));
                                } else {
                                    sb.append(s);
                                }
                            }
                            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, br.getRequest().getHttpConnection().getContentType()));
                            OutputStream out = response.getOutputStream(true);
                            out.write(sb.toString().getBytes("UTF-8"));
                            out.flush();
                            return true;

                        }
                        return false;
                    } else if ("/download".equals(request.getRequestedPath())) {

                        String url = request.getParameterbyKey("url");
                        if (url == null) {
                            return false;
                        }

                        URLConnectionAdapter connection = br.openGetConnection(url);
                        try {
                            response.setResponseCode(HTTPConstants.ResponseCode.get(br.getRequest().getHttpConnection().getResponseCode()));
                            long length = connection.getCompleteContentLength();
                            if (length > 0) {
                                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, length + ""));
                            }
                            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, connection.getContentType()));

                            OutputStream out = response.getOutputStream(true);

                            if (meteredThrottledInputStream == null) {
                                meteredThrottledInputStream = new MeteredThrottledInputStream(connection.getInputStream(), new AverageSpeedMeter(10));
                                if (connectionHandler != null) {
                                    connectionHandler.addThrottledConnection(meteredThrottledInputStream);
                                }

                            } else {
                                meteredThrottledInputStream.setInputStream(connection.getInputStream());
                            }

                            try {

                                byte[] buffer = new byte[32 * 1024];

                                int len;

                                while ((len = meteredThrottledInputStream.read(buffer)) != -1) {
                                    if (len > 0) {

                                        out.write(buffer, 0, len);

                                    }
                                }
                            } finally {

                            }
                            out.flush();
                            out.close();
                            return true;
                        } finally {
                            connection.disconnect();

                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return true;
            }
        });

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
            downloadable.setDownloadInterface(this);

            String fileOutput = downloadable.getFileOutput();
            String finalFileOutput = downloadable.getFinalFileOutput();
            outputCompleteFile = new File(fileOutput);
            outputFinalCompleteFile = outputCompleteFile;
            if (!fileOutput.equals(finalFileOutput)) {
                outputFinalCompleteFile = new File(finalFileOutput);
            }
            outputPartFile = new File(downloadable.getFileOutputPart());

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
                    downloadable.addDownloadTime(System.currentTimeMillis() - getStartTimeStamp());
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
        if (!handleErrors()) {
            return;
        }
        // link.setVerifiedFileSize(bytesWritten);

        boolean renameOkay = downloadable.rename(outputPartFile, outputCompleteFile);
        if (!renameOkay) {

            error(new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, _JDT._.system_download_errors_couldnotrename(), LinkStatus.VALUE_LOCAL_IO_ERROR));
        }

    }

    protected void cleanupDownladInterface() {
        try {
            downloadable.removeConnectionHandler(this.getManagedConnetionHandler());
        } catch (final Throwable e) {
        }
        try {
            if (currentConnection != null) {
                currentConnection.disconnect();
            }
        } catch (Throwable e) {
        }
        closeOutputChannel();
    }

    private void closeOutputChannel() {
        try {

        } catch (Throwable e) {
            LogSource.exception(logger, e);
        } finally {

        }
    }

    private boolean handleErrors() throws PluginException {
        if (externalDownloadStop()) {
            return false;
        }

        if (caughtPluginException == null) {
            downloadable.setLinkStatus(LinkStatus.FINISHED);
            downloadable.setVerifiedFileSize(outputCompleteFile.length());
            return true;
        } else {
            throw caughtPluginException;
        }
    }

    private void createOutputChannel() throws SkipReasonException {
        try {
            String fileOutput = downloadable.getFileOutput();
            logger.info("createOutputChannel for " + fileOutput);
            String finalFileOutput = downloadable.getFinalFileOutput();
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
            logger.info("externalStop recieved");
            terminate();
        }
    }

    private AtomicBoolean abort      = new AtomicBoolean(false);
    private AtomicBoolean terminated = new AtomicBoolean(false);

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

}
