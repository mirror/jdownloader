package org.jdownloader.controlling.ffmpeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jd.controlling.downloadcontroller.ManagedThrottledConnectionHandler;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.PluginProgress;
import jd.plugins.download.raf.FileBytesMap;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.HttpServer;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.speedmeter.AverageSpeedMeter;

public class AbstractFFmpegBinary {

    protected FFmpegSetup config;
    private Browser       obr;

    public AbstractFFmpegBinary(Browser br) {
        this.obr = br;
    }

    protected String[] execute(final int timeout, PluginProgress progess, File runin, String... cmds) throws InterruptedException, IOException {
        final ProcessBuilder pb = ProcessBuilderFactory.create(cmds);
        if (runin != null) {
            pb.directory(runin);
        }
        final StringBuilder inputStream = new StringBuilder();
        final StringBuilder errorStream = new StringBuilder();
        final Process process = pb.start();

        final Thread reader1 = new Thread("ffmpegReader") {
            public void run() {
                try {
                    readInputStreamToString(inputStream, process.getInputStream(), true);
                } catch (Throwable e) {
                    logger.log(e);
                }
            }
        };

        final Thread reader2 = new Thread("ffmpegReader") {
            public void run() {
                try {
                    readInputStreamToString(errorStream, process.getErrorStream(), false);
                } catch (Throwable e) {
                    logger.log(e);
                }
            }
        };
        if (CrossSystem.isWindows()) {
            reader1.setPriority(Thread.NORM_PRIORITY + 1);
            reader2.setPriority(Thread.NORM_PRIORITY + 1);
        }
        reader1.start();
        reader2.start();
        if (timeout > 0) {
            final AtomicBoolean timeoutReached = new AtomicBoolean(false);
            final AtomicBoolean processAlive = new AtomicBoolean(true);
            Thread timouter = new Thread("ffmpegReaderTimeout") {
                public void run() {
                    try {
                        Thread.sleep(timeout);
                    } catch (InterruptedException e) {
                        return;
                    }
                    if (processAlive.get()) {
                        timeoutReached.set(true);
                        process.destroy();
                    }
                }
            };
            timouter.start();
            logger.info("ExitCode1: " + process.waitFor());
            processAlive.set(false);
            timouter.interrupt();
            if (timeoutReached.get()) {
                throw new InterruptedException("Timeout!");
            }
            reader1.join();
            reader2.join();
            return new String[] { inputStream.toString(), errorStream.toString() };
        } else {
            logger.info("ExitCode2: " + process.waitFor());

            reader1.join();
            reader2.join();
            return new String[] { inputStream.toString(), errorStream.toString() };
        }

    }

    private String readInputStreamToString(StringBuilder ret, final InputStream fis, boolean b) throws IOException {
        BufferedReader f = null;
        try {
            f = new BufferedReader(new InputStreamReader(fis, "UTF8"));
            String line;
            final String sep = System.getProperty("line.separator");
            final boolean isInstantFlush = logger.isInstantFlush();
            while ((line = f.readLine()) != null) {
                if (ret != null) {
                    if (isInstantFlush) {
                        logger.info(b + ":" + line);
                    }
                    synchronized (ret) {
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
                        parseLine(b, ret, line);
                    }
                }
            }
            if (ret == null) {
                return null;
            }
            return ret.toString();
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            if (e instanceof Error) {
                throw (Error) e;
            }
            throw new RuntimeException(e);
        }
    }

    protected void parseLine(boolean stdStream, StringBuilder ret, String line) {
    }

    protected LogSource  logger;
    protected String     path;
    protected HttpServer server;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isAvailable() {
        return getFullPath() != null;
    }

    public String getFullPath() {
        try {
            if (StringUtils.isEmpty(path)) {
                return null;
            }
            File file = new File(path);
            if (!file.isAbsolute()) {
                file = Application.getResource(path);
            }
            if (!file.exists()) {
                return null;
            }
            if (Application.getJavaVersion() >= Application.JAVA16) {
                if (!file.canExecute()) {
                    file.setExecutable(true);
                }
            }
            return file.getCanonicalPath();
        } catch (Exception e) {
            logger.log(e);
            return null;
        }
    }

    /**
     * @param out
     * @param videoIn
     * @param audioIn
     * @param map
     *            TODO
     * @param mc
     * @return
     */
    public ArrayList<String> fillCommand(String out, String videoIn, String audioIn, HashMap<String, String[]> map, String... mc) {
        ArrayList<String> commandLine = new ArrayList<String>();
        commandLine.add(getFullPath());
        main: for (int i = 0; i < mc.length; i++) {
            String param = mc[i];
            param = param.replace("%video", videoIn == null ? "" : videoIn);
            param = param.replace("%audio", audioIn == null ? "" : audioIn);
            param = param.replace("%out", out);

            if (map != null) {
                for (Entry<String, String[]> es : map.entrySet()) {
                    if (param.equals(es.getKey())) {
                        for (String s : es.getValue()) {
                            commandLine.add(s);
                        }
                        continue main;
                    }

                }
            }

            commandLine.add(param);

        }

        return commandLine;
    }

    protected long                            processID;
    protected final AtomicReference<byte[]>   instanceBuffer = new AtomicReference<byte[]>();
    protected MeteredThrottledInputStream     meteredThrottledInputStream;
    private ManagedThrottledConnectionHandler connectionHandler;

    protected void closePipe() {
        if (server != null) {
            server.stop();
        }

    }

    protected void initPipe() throws IOException {
        if (obr == null) {
            return;
        }
        server = new HttpServer(0);
        server.setLocalhostOnly(true);
        final HttpServer finalServer = server;
        server.start();
        instanceBuffer.set(new byte[512 * 1024]);
        finalServer.registerRequestHandler(new HttpRequestHandler() {

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
                    String id = request.getParameterbyKey("id");
                    if (id == null) {

                        return false;
                    }
                    if (processID != Long.parseLong(request.getParameterbyKey("id"))) {
                        return false;
                    }
                    if ("/download".equals(request.getRequestedPath())) {
                        String url = request.getParameterbyKey("url");
                        if (url == null) {
                            return false;
                        }
                        OutputStream outputStream = null;
                        final FileBytesMap fileBytesMap = new FileBytesMap();
                        retryLoop: for (int retry = 0; retry < 10; retry++) {
                            final Browser br = obr.cloneBrowser();
                            final jd.http.requests.GetRequest getRequest = new jd.http.requests.GetRequest(url);
                            if (fileBytesMap.getFinalSize() > 0) {
                                if (logger != null) {
                                    logger.info("Resume(" + retry + "): " + fileBytesMap.toString());
                                }
                                final List<Long[]> unMarkedAreas = fileBytesMap.getUnMarkedAreas();
                                getRequest.getHeaders().put(HTTPConstants.HEADER_REQUEST_RANGE, "bytes=" + unMarkedAreas.get(0)[0] + "-" + unMarkedAreas.get(0)[1]);
                            }
                            final URLConnectionAdapter connection;
                            try {
                                connection = br.openRequestConnection(getRequest);
                            } catch (IOException e) {
                                Thread.sleep(250);
                                continue retryLoop;
                            }
                            byte[] readWriteBuffer = instanceBuffer.getAndSet(null);
                            final boolean instanceBuffer;
                            if (readWriteBuffer != null) {
                                instanceBuffer = true;
                            } else {
                                instanceBuffer = false;
                                readWriteBuffer = new byte[32 * 1024];
                            }
                            try {
                                if (outputStream == null) {
                                    response.setResponseCode(HTTPConstants.ResponseCode.get(br.getRequest().getHttpConnection().getResponseCode()));
                                    final long length = connection.getCompleteContentLength();
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
                                            Thread.sleep(250);
                                            continue retryLoop;
                                        } else {
                                            throw e;
                                        }
                                    }
                                    if (len > 0) {
                                        outputStream.write(readWriteBuffer, 0, len);
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
                                if (instanceBuffer) {
                                    AbstractFFmpegBinary.this.instanceBuffer.compareAndSet(null, readWriteBuffer);
                                }
                                connection.disconnect();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (logger != null) {
                        logger.info("END:" + requestOkay + ">" + request.getRequestedURL());
                    }
                }
                return true;
            }
        });

    }

    public String runCommand(FFMpegProgress progress, ArrayList<String> commandLine) throws IOException, InterruptedException, FFMpegException {
        final ProcessBuilder pb = ProcessBuilderFactory.create(commandLine);
        final Process process = pb.start();
        try {
            final StringBuilder sdtStream = new StringBuilder();
            final Thread reader1 = new Thread("ffmpegReader") {
                public void run() {
                    try {
                        readInputStreamToString(sdtStream, process.getInputStream(), true);
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                }
            };
            final StringBuilder errorStream = new StringBuilder();
            final Thread reader2 = new Thread("ffmpegReader") {
                public void run() {
                    try {
                        readInputStreamToString(errorStream, process.getErrorStream(), false);
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                }
            };
            if (CrossSystem.isWindows()) {
                reader1.setPriority(Thread.NORM_PRIORITY + 1);
                reader2.setPriority(Thread.NORM_PRIORITY + 1);
            }
            reader1.start();
            reader2.start();
            long lastUpdate = System.currentTimeMillis();
            long lastDuration = -1;
            while (true) {
                final String errorStreamString;
                synchronized (errorStream) {
                    errorStreamString = errorStream.toString();
                    errorStream.setLength(0);
                }
                if (StringUtils.isNotEmpty(errorStreamString)) {
                    lastUpdate = System.currentTimeMillis();
                }
                final String duration = new Regex(errorStreamString, "Duration\\: (.*?).?\\d*?\\, start").getMatch(0);
                if (duration != null) {
                    lastDuration = formatStringToMilliseconds(duration);
                }
                if (lastDuration > 0) {
                    final String[] times = new Regex(errorStreamString, "time=(.*?).?\\d*? ").getColumn(0);
                    if (times != null && times.length > 0) {
                        final long msDone = formatStringToMilliseconds(times[times.length - 1]);
                        System.out.println(msDone + "/" + lastDuration);
                        if (progress != null) {
                            progress.updateValues(msDone, lastDuration);
                        }
                    }
                }
                try {
                    final int exitCode = process.exitValue();
                    reader2.join();
                    logger.info("LastErrorStream:" + errorStreamString);
                    logger.info("LastStdStream:" + sdtStream.toString());
                    logger.info("ExitCode:" + exitCode);
                    final boolean okay = exitCode == 0;
                    if (!okay) {

                        throw new FFMpegException("FFmpeg Failed", sdtStream.toString(), errorStreamString);
                    } else {
                        return sdtStream.toString();
                    }
                } catch (IllegalThreadStateException e) {
                    // still running;
                }
                if (System.currentTimeMillis() - lastUpdate > 60000) {
                    // 60 seconds without any ffmpeg update. interrupt
                    throw new InterruptedException("FFMPeg does not answer");
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            logger.log(e);
            throw e;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    public static long formatStringToMilliseconds(final String text) {
        final String[] found = new Regex(text, "(\\d+):(\\d+):(\\d+)").getRow(0);
        if (found == null) {
            return 0;
        }
        int hours = Integer.parseInt(found[0]);
        int minutes = Integer.parseInt(found[1]);
        int seconds = Integer.parseInt(found[2]);

        return hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000;
    }

}
