package jd.http.download;

import jd.plugins.download.DownloadInterface;

public abstract class DownloadListener {

    public abstract void onPropertyChanged(DownloadInterface downloadInterface, int id, Object contentLength);

}
