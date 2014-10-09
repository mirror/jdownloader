package org.jdownloader.api.system;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
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
    public void shutdownOS(final boolean force) {
        stopJD();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public int getHookPriority() {
                return Integer.MIN_VALUE;
            }

            @Override
            public void onShutdown(ShutdownRequest shutdownRequest) {
                CrossSystem.shutdownSystem(force);
            }
        });
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
        int maxWait = 5 * 1000;
        while (DownloadWatchDog.getInstance().isIdle() == false && maxWait >= 0) {
            try {
                maxWait -= 500;
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    @Override
    public void restartJD() {
        stopJD();
        RestartController.getInstance().asyncRestart(new SmartRlyRestartRequest());
    }

    @Override
    public void exitJD() {
        stopJD();
        RestartController.getInstance().exitAsynch(new SmartRlyExitRequest());
    }

}
