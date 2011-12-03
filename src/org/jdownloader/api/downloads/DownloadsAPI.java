package org.jdownloader.api.downloads;

import java.util.List;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;

@ApiNamespace("downloads")
public interface DownloadsAPI extends RemoteAPIInterface {

    public List<FilePackageAPIStorable> list();

    public DownloadStatusAPIStorable status();

    public boolean stop();

    public boolean start();

    public boolean reconnect();

    @Deprecated
    public boolean reconnectenabled(boolean enable);

    @Deprecated
    public boolean speedlimit(boolean enable, int limit);
}
