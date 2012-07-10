package jd.plugins.download;

import jd.http.Request;
import jd.plugins.DownloadLink;

public interface DownloadInterfaceFactory {

    public DownloadInterface getDownloadInterface(DownloadLink downloadLink, Request request) throws Exception;

    public DownloadInterface getDownloadInterface(DownloadLink downloadLink, Request request, boolean resume, int chunks) throws Exception;
}
