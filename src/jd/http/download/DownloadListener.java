package jd.http.download;

public interface DownloadListener {

    abstract void onStatus(DownloadEvent downloadEvent);

}
