package org.jdownloader.http.download;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import jd.http.Browser;
import jd.http.URLConnectionAdapter;

import org.appwork.utils.net.DownloadProgress;
import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
import org.appwork.utils.speedmeter.AverageSpeedMeter;

public class DownloadClient {
    private Browser                     httpClient;
    private DownloadProgress            progressCallback;
    private MeteredThrottledInputStream input;

    public DownloadProgress getProgressCallback() {
        return progressCallback;
    }

    public void setProgressCallback(DownloadProgress progressCallback) {
        this.progressCallback = progressCallback;
    }

    public DownloadClient(Browser br) {
        this.httpClient = br;
    }

    public void download(String url) throws IOException, InterruptedException {
        jd.http.requests.GetRequest request = httpClient.createGetRequest(url);
        download(request);
    }

    private void download(jd.http.Request request) throws IOException, InterruptedException {
        URLConnectionAdapter connection = null;
        try {
            if (getResumePosition() >= 0) {
                request.getHeaders().put("Range", "bytes=" + getResumePosition() + "-");
            }
            final DownloadProgress progress = getProgressCallback();
            if (progress != null) {
                progress.onConnect();
            }
            connection = httpClient.openRequestConnection(request, true);
            if (progress != null) {
                progress.setTotal(connection.getCompleteContentLength());
            }
            long[] range = connection.getRange();
            if (getResumePosition() >= 0) {
                if (range == null || range.length == 0 || range[0] != getResumePosition()) {
                    throw new IOException("Resume Failed");
                }
            }
            validateConnection(connection);
            download(connection);
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Throwable e) {
            }
        }
    }

    protected void validateConnection(URLConnectionAdapter connection) throws IOException {
        switch (connection.getResponseCode()) {
        case 200:
        case 206:
            return;
        default:
            throw new IOException("Invalid ResponseCode: " + connection);
        }
    }

    private void download(URLConnectionAdapter connection) throws IOException, InterruptedException {
        try {
            input = new MeteredThrottledInputStream(connection.getInputStream(), new AverageSpeedMeter());
            onInputStream(input);
            OutputStream output = getOutputStream();
            DownloadProgress progress = getProgressCallback();
            if (connection.getCompleteContentLength() >= 0) {
                if (progress != null) {
                    progress.setTotal(connection.getCompleteContentLength());
                }
            } else {
                /* no contentLength is known */
            }
            final byte[] b = new byte[32 * 1024];
            int len = 0;
            long loaded = Math.max(0, getResumePosition());
            if (progress != null) {
                progress.setLoaded(loaded);
            }
            while (true) {
                if ((len = input.read(b)) == -1) {
                    break;
                }
                if (!doContinue()) {
                    throw new InterruptedException();
                }
                if (len > 0) {
                    if (progress != null) {
                        progress.onBytesLoaded(b, len);
                    }
                    output.write(b, 0, len);
                    loaded += len;
                    if (progress != null) {
                        progress.increaseLoaded(len);
                    }
                }
            }
            if (connection.getCompleteContentLength() >= 0) {
                if (loaded != connection.getCompleteContentLength()) {
                    throw new IOException("Incomplete download! " + loaded + " from " + connection.getCompleteContentLength());
                }
            }
        } finally {
            getOutputStream().close();
        }
    }

    protected void onInputStream(MeteredThrottledInputStream input2) {
    }

    protected boolean doContinue() {
        return !Thread.interrupted();
    }

    private long         resumePosition = 0;
    private OutputStream outputStream;

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public long getResumePosition() {
        return resumePosition;
    }

    public void setResumePosition(long resumePosition) {
        this.resumePosition = resumePosition;
    }

    public void setOutputFile(final File file) throws IOException {
        setOutputStream(null);
        if (file != null) {
            file.getParentFile().mkdirs();
            final BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file, true));
            setOutputStream(outputStream);
            setResumePosition(file.length());
        }
    }

    private void setOutputStream(OutputStream outputStream) throws IOException {
        if (this.outputStream != null) {
            this.outputStream.close();
        }
        this.outputStream = outputStream;
    }

    public long getSpeedInBps() {
        if (input == null) {
            return 0;
        }
        return input.getSpeedMeter();
    }
}
