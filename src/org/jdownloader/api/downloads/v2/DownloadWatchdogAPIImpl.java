package org.jdownloader.api.downloads.v2;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.api.utils.PackageControllerUtils;
import org.jdownloader.myjdownloader.client.bindings.interfaces.DownloadControllerInterface;

public class DownloadWatchdogAPIImpl implements DownloadWatchdogAPI {

    private final PackageControllerUtils<FilePackage, DownloadLink> packageControllerUtils;

    public DownloadWatchdogAPIImpl() {
        RemoteAPIController.validateInterfaces(DownloadWatchdogAPI.class, DownloadControllerInterface.class);
        packageControllerUtils = new PackageControllerUtils<FilePackage, DownloadLink>(DownloadController.getInstance());
    }

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

    @Override
    public int getSpeedInBps() {
        DownloadWatchDog dwd = DownloadWatchDog.getInstance();
        return dwd.getDownloadSpeedManager().getSpeed();
    }

    @Override
    public void forceDownload(final long[] linkIds, final long[] packageIds) {
        DownloadWatchDog dwd = DownloadWatchDog.getInstance();
        dwd.forceDownload(packageControllerUtils.getSelectionInfo(linkIds, packageIds).getChildren());
    }

}
