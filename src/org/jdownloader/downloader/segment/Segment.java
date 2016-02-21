package org.jdownloader.downloader.segment;

public class Segment {

    private String url;

    public String getUrl() {
        return url;
    }

    public Segment(String url) {
        this.url = url;
    }

    public Segment(String baseUrl, String s) {
        this.url = baseUrl + s;
    }

}
