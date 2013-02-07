package org.jdownloader.extensions.jdanywhere.api.downloads;

import java.util.List;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;

@ApiNamespace("mobile/downloads")
public interface DownloadsMobileAPI extends RemoteAPIInterface {
    boolean start();

    boolean stop();

    boolean pause(Boolean value);

    String getState();

    // Sets the enabled flag of a downloadPackage
    public boolean downloadPackageEnabled(String ID, boolean enabled);

    public boolean removeDownloadLink(String ID);

    public boolean removeDownloadPackage(String ID);

    public DownloadLinkAPIStorable getDownloadLink(long ID);

    public FilePackageAPIStorable getFilePackage(long ID);

    public String getPackageIDFromLinkID(long ID);

    public List<FilePackageAPIStorable> listPackages();

    public List<DownloadLinkAPIStorable> listDownloadLinks(long ID);

    // returns a list of all running downloads
    // public List<FilePackageAPIStorable> running();
    List<RunningObjectAPIStorable> runningLinks();

    // Sets the enabled flag of a downloadlink
    public boolean downloadLinkEnabled(String ID, boolean enabled);

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
