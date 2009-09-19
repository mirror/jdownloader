package jd.controlling;

import jd.plugins.DownloadLink;

public interface LinkGrabberPackagingEvent {
    public void attachToPackagesFirstStage(DownloadLink link);

    public void attachToPackagesSecondStage(DownloadLink link);

}
