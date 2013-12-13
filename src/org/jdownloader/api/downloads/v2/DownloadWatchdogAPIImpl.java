package org.jdownloader.api.downloads.v2;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.myjdownloader.client.bindings.interfaces.DownloadControllerInterface;

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
        RemoteAPIController.validateInterfaces(DownloadWatchdogAPI.class, DownloadControllerInterface.class);
    }

    @Override
    public int getSpeedInBps() {
        DownloadWatchDog dwd = DownloadWatchDog.getInstance();
        return dwd.getDownloadSpeedManager().getSpeed();
    }

    @Override
    public void forceDownload(final long[] linkIds, final long[] packageIds) {

        DownloadWatchDog dwd = DownloadWatchDog.getInstance();

        dwd.forceDownload(org.jdownloader.api.downloads.v2.DownloadsAPIV2Impl.getSelectionInfo(linkIds, packageIds).getChildren());

    }

}
