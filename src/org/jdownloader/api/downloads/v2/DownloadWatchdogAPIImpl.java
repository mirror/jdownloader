package org.jdownloader.api.downloads.v2;

import jd.controlling.downloadcontroller.DownloadWatchDog;

public class DownloadWatchdogAPIImpl implements DownloadWatchdogAPI {

    public boolean start() {
        DownloadWatchDog.getInstance().startDownloads();
        return true;
    }

    public boolean stop() {
        DownloadWatchDog.getInstance().stopDownloads();
        return true;
    }

    public boolean pause(boolean value) {
        DownloadWatchDog.getInstance().pauseDownloadWatchDog(value);
        return true;
    }

    @Override
    public String getCurrentState() {
        return DownloadWatchDog.getInstance().getStateMachine().getState().getLabel();
    }

    public DownloadWatchdogAPIImpl() {

    }

    @Override
    public int getSpeedInBps() {
        DownloadWatchDog dwd = DownloadWatchDog.getInstance();
        return dwd.getDownloadSpeedManager().getSpeed();
    }

    @Override
    public void forceDownload(final long[] linkIds, final long[] packageIds) {

        DownloadWatchDog dwd = DownloadWatchDog.getInstance();

        dwd.forceDownload(org.jdownloader.api.downloads.v2.DownloadsAPIImplV2.getSelectionInfo(linkIds, packageIds).getChildren());

    }

}
