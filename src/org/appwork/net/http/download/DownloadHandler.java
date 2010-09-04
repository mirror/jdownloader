package org.appwork.net.http.download;

import java.io.File;

import jd.http.Request;

import org.appwork.net.http.download.event.DownloadEvent;
import org.appwork.utils.event.DefaultEventSender;

public class DownloadHandler {

    private DefaultEventSender<DownloadEvent> eventSender;
    private File outputFile;

    private Request request;

    public DownloadHandler(Request request, File outputFile) {
        eventSender = new DefaultEventSender<DownloadEvent>();
        this.outputFile = outputFile;
        this.request = request;
    }

    public DefaultEventSender<DownloadEvent> getEventSender() {
        return eventSender;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public Request getRequest() {
        return request;
    }

}
