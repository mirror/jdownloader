package org.jdownloader.api.system;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.myjdownloader.client.bindings.interfaces.SystemInterface;
import org.jdownloader.updatev2.ForcedShutdown;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;
import org.jdownloader.updatev2.SmartRlyRestartRequest;

public class SystemAPIImpl implements SystemAPI {

    public SystemAPIImpl() {
        RemoteAPIController.validateInterfaces(SystemAPI.class, SystemInterface.class);
    }

    @Override
    public void shutdownOS(boolean force) {
        stopJD();
        CrossSystem.shutdownSystem(force);
        RestartController.getInstance().exitAsynch(new ForcedShutdown());
    }

    @Override
    public void standbyOS() {
        stopJD();
        CrossSystem.standbySystem();
    }

    @Override
    public void hibernateOS() {
        stopJD();
        CrossSystem.hibernateSystem();
    }

    private void stopJD() {
        DownloadWatchDog.getInstance().stopDownloads();
        LinkCollector.getInstance().abort();
    }

    @Override
    public void restartJD() {
        DownloadWatchDog.getInstance().stopDownloads();
        LinkCollector.getInstance().abort();
        RestartController.getInstance().asyncRestart(new SmartRlyRestartRequest());
    }

    @Override
    public void exitJD() {
        DownloadWatchDog.getInstance().stopDownloads();
        LinkCollector.getInstance().abort();
        RestartController.getInstance().exitAsynch(new SmartRlyExitRequest());
    }

}
