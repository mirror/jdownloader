package org.jdownloader.api.downloads;

import java.util.List;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;

@ApiNamespace("downloads")
public interface DownloadsAPI extends RemoteAPIInterface {

    public List<FilePackageAPIStorable> list();

    // returns a list of all running downloads
    // used in iPhone-App
    public List<FilePackageAPIStorable> running();

    // Sets the enabled flag of a downloadlink
    // used in iPhone-App
    public boolean downloadLinkEnabled(String ID, boolean enabled);

    public boolean stop();

    public boolean start();

    // returns the current downloadspeed
    // used in iPhone-App
    public int speed();

    // returns the current limit
    // used in iPhone-App
    public int limit();

    // returns the current traffic
    // used in iPhone-App
    public long traffic();

    // returns the SpeedMeter from UI without the DownloadSpeed / AverageSpeed Text as an PNG
    // used in iPhone-App
    public void speedMeter(RemoteAPIRequest request, RemoteAPIResponse response);

}
