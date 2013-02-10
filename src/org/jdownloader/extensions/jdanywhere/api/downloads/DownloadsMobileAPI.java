package org.jdownloader.extensions.jdanywhere.api.downloads;

import java.util.List;

import org.appwork.remoteapi.ApiNamespace;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;

@ApiNamespace("mobile/downloads")
public interface DownloadsMobileAPI extends RemoteAPIInterface {
    boolean start(final String username, final String password);

    boolean stop(final String username, final String password);

    boolean pause(Boolean value, final String username, final String password);

    String getState(final String username, final String password);

    // Sets the enabled flag of a downloadPackage
    public boolean downloadPackageEnabled(String ID, boolean enabled, final String username, final String password);

    public boolean removeDownloadLink(String ID, final String username, final String password);

    public boolean removeDownloadPackage(String ID, final String username, final String password);

    public DownloadLinkAPIStorable getDownloadLink(long ID, final String username, final String password);

    public FilePackageAPIStorable getFilePackage(long ID, final String username, final String password);

    public String getPackageIDFromLinkID(long ID, final String username, final String password);

    public List<FilePackageAPIStorable> listPackages(final String username, final String password);

    public List<DownloadLinkAPIStorable> listDownloadLinks(long ID, final String username, final String password);

    public boolean setLimitspeed(int speed, final String username, final String password);

    public boolean activateLimitspeed(boolean activate, final String username, final String password);

    public boolean resetDownloadLink(String ID, final String username, final String password);

    public boolean resetPackage(String ID, final String username, final String password);

    // returns a list of all running downloads
    // public List<FilePackageAPIStorable> running();
    List<RunningObjectAPIStorable> runningLinks(final String username, final String password);

    // Sets the enabled flag of a downloadlink
    public boolean downloadLinkEnabled(String ID, boolean enabled, final String username, final String password);

    // returns the current downloadspeed
    // used in iPhone-App
    public int speed(final String username, final String password);

    // returns the current limit
    // used in iPhone-App
    public int limit(final String username, final String password);

    // returns the current traffic
    // used in iPhone-App
    public long traffic(final String username, final String password);

    // returns the SpeedMeter from UI without the DownloadSpeed / AverageSpeed Text as an PNG
    // used in iPhone-App
    public void speedMeter(RemoteAPIRequest request, RemoteAPIResponse response, final String username, final String password);
}
