package org.jdownloader.extensions.streaming.dataprovider.transcode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import jd.parser.Regex;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpserver.HttpHandlerInfo;
import org.appwork.utils.net.httpserver.handler.HttpRequestHandler;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.PostRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.appwork.utils.net.meteredconnection.MeteredInputStream;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.speedmeter.AverageSpeedMeter;
import org.jdownloader.api.HttpServer;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.extensions.streaming.ByteRange;
import org.jdownloader.extensions.streaming.StreamLinker;
import org.jdownloader.extensions.streaming.dataprovider.StreamFactoryInterface;
import org.jdownloader.logging.LogController;

public abstract class Transcoder {

    private int               port;
    private HttpHandlerInfo   streamHandler;
    private String            sessionID;
    private LogSource         logger;
    private ArrayList<String> commands;

    public Transcoder(int port) {
        this.port = port;
        logger = LogController.getInstance().getLogger(Transcoder.class.getName());
        sessionID = new UniqueAlltimeID().toString();

    }

    public String toString() {
        return "Transcoder: " + commands;
    }

    public void close() {
        logger.info("Closed: " + this);
        if (streamHandler != null) {
            HttpServer.getInstance().unregisterRequestHandler(streamHandler);
        }
        if (process != null) {
            process.destroy();
        }
        exception = null;
    }

    public void run() throws Exception {

        String path = getFFMpegPath();

        initStreamHandler();
        //
        // "g:\\audio" + sessionID + ".mp3"
        try {
            /*
             * [mpeg4 @ 0000000001e4d860] Invalid and inefficient vfw-avi packed B frames detected [NULL @ 0000000001e43d00] Unable to find
             * a suitable output format for 'http://127.0.0.1:3128/ffmpeg/1346828391987' http://127.0.0.1:3128/ffmpeg/1346828391987: Invalid
             * argument
             */

            String[] results;

            ArrayList<String> ffmpegcommands = new ArrayList<String>();
            ffmpegcommands.add(path);
            for (String s : getFFMpegCommandLine("http://127.0.0.1:" + port + "/ffmpeg/" + sessionID)) {
                ffmpegcommands.add(s);
            }
            commands = ffmpegcommands;
            results = execute(ffmpegcommands.toArray(new String[] {}));

        } catch (Exception e) {

            addException(e);

        } finally {
            HttpServer.getInstance().unregisterRequestHandler(streamHandler);
            if (exception != null) throw exception;
        }
    }

    protected abstract void addException(Exception e);

    protected abstract String[] getFFMpegCommandLine(String string);

    protected void initStreamHandler() throws IOException {
        streamHandler = HttpServer.getInstance().registerRequestHandler(port, true, new HttpRequestHandler() {

            @Override
            public boolean onPostRequest(PostRequest request, HttpResponse response) {
                if (!request.getRequestedURL().equals("/ffmpeg/" + sessionID)) return false;
                logger.info("Incoming transcoded data\r\n" + request);
                MeteredInputStream input;
                try {
                    input = new MeteredInputStream(request.getInputStream(), new AverageSpeedMeter(10));

                    int len;
                    byte[] buffer = new byte[100 * 1024];
                    long oldspeed = 0;
                    while ((len = input.read(buffer)) != -1) {
                        if (len > 0) {

                            write(buffer, len);

                            long speed = (input.getSpeedMeter() / 1000);
                            if (speed != oldspeed) {
                                oldspeed = speed;
                                System.out.println("Transcoded Data Speed: " + speed + " kb/s");
                            }

                        }
                    }

                    response.setResponseCode(ResponseCode.SUCCESS_OK);
                    response.closeConnection();

                    return true;
                } catch (IOException e) {
                    if (e.getMessage().contains("Connection reset")) {

                        // ffmpeg killed;
                        logger.info("FFMPeg killed transcoded Stream");
                        return true;
                    }

                    addException(e);
                    try {
                        response.getOutputStream().write(Exceptions.getStackTrace(e).getBytes("UTF-8"));
                    } catch (Exception e1) {

                    }

                }

                return true;
            }

            @Override
            public boolean onGetRequest(GetRequest request, HttpResponse response) {

                if (!request.getRequestedURL().equals("/ffmpeg/" + sessionID)) return false;

                try {
                    new StreamLinker(response).run(getStreamFactory(), new ByteRange(request));

                } catch (Exception e) {
                    if (e.getMessage().contains("socket write error")) {
                        // output closed by ffmpeg
                        return true;
                    }
                    addException(e);

                    try {
                        response.getOutputStream().write(Exceptions.getStackTrace(e).getBytes());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    response.setResponseCode(ResponseCode.SERVERERROR_INTERNAL);

                }
                return true;
            }
        });
    }

    protected abstract StreamFactoryInterface getStreamFactory();

    protected Exception exception;
    private Process     process;

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    protected void setException(Exception e) {
        if (this.exception == null) {
            exception = e;
        }
    }

    protected abstract void write(byte[] buffer, int length) throws IOException;

    public String readInputStreamToString(final InputStream fis) throws UnsupportedEncodingException, IOException, InterruptedException {
        BufferedReader f = null;
        final StringBuilder ret = new StringBuilder();
        try {
            f = new BufferedReader(new InputStreamReader(fis, "UTF8"));
            String line;

            final String sep = System.getProperty("line.separator");
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

            return ret.toString();
        } catch (IOException e) {

            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } finally {

            // don't close streams this might ill the process
        }
    }

    private String[] execute(String... cmds) throws InterruptedException, IOException {
        Thread reader1 = null;
        Thread reader2 = null;
        final ProcessBuilder pb = ProcessBuilderFactory.create(cmds);
        StringBuilder scb = new StringBuilder();
        for (String s : cmds) {
            scb.append(s);
            scb.append(" ");
        }
        final StringBuilder sb = new StringBuilder();
        final StringBuilder sb2 = new StringBuilder();
        process = pb.start();

        reader1 = new Thread("ffmpegReader") {
            public void run() {
                try {
                    sb.append(readInputStreamToString(process.getInputStream()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        reader2 = new Thread("ffmpegReader") {
            public void run() {
                try {
                    sb2.append(readInputStreamToString(process.getErrorStream()));

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        reader1.start();
        reader2.start();
        try {
            int exitCode = process.waitFor();

            logger.info("Exit FFMPEG: " + exitCode);
            logger.info("Output: \r\n" + sb);
            logger.info("ErrorOutput: \r\n" + sb2);
            String[][] matches = new Regex(sb2.toString(), "video\\:(\\d+\\w+) audio\\:(\\d+\\w+) subtitle\\:(\\d+) global headers\\:(\\d+\\w+) ").getMatches();
            if (matches == null) {
                //
                throw new IOException("FFMPEG Exception " + sb2.toString());
            }
        } catch (InterruptedException e) {
            try {
                process.getInputStream().close();
            } catch (Exception e2) {

            }
            try {
                process.getErrorStream().close();
            } catch (Exception e2) {

            }
            // try {
            //
            // reader1.interrupt();
            // } catch (Exception e2) {
            // }
            // try {
            //
            // reader2.interrupt();
            // } catch (Exception e2) {
            // }
            throw e;
        }
        return new String[] { sb.toString(), sb2.toString() };

    }

    private String getFFMpegPath() {

        if (CrossSystem.isWindows()) {
            File ffprobe = Application.getResource("tools\\Windows\\ffmpeg\\" + (CrossSystem.is64BitOperatingSystem() ? "x64" : "i386") + "\\bin\\ffmpeg.exe");
            if (ffprobe.exists()) return ffprobe.getAbsolutePath();
        } else if (CrossSystem.isLinux()) {
            File ffprobe = Application.getResource("tools\\linux\\ffmpeg\\" + (CrossSystem.is64BitOperatingSystem() ? "x64" : "i386") + "\\ffmpeg");
            if (ffprobe.exists()) return ffprobe.getAbsolutePath();
        } else if (CrossSystem.isMac()) {
            File ffprobe = Application.getResource("tools\\mac\\ffmpeg\\" + (CrossSystem.is64BitOperatingSystem() ? "x64" : "i386") + "\\ffmpeg");
            if (ffprobe.exists()) return ffprobe.getAbsolutePath();
        }
        return "ffmpeg";
    }
}
