package org.jdownloader.api.system;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.utils.Application;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.myjdownloader.client.bindings.SystemInformationStorable;
import org.jdownloader.myjdownloader.client.bindings.interfaces.SystemInterface;
import org.jdownloader.updatev2.ForcedShutdown;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;
import org.jdownloader.updatev2.SmartRlyRestartRequest;

public class SystemAPIImpl implements SystemAPI {

    private final long startupTimeStamp = System.currentTimeMillis();

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

    @Override
    public SystemInformationStorable getSystemInfos() {
        final SystemInformationStorable ret = new SystemInformationStorable();
        final OperatingSystem os = CrossSystem.getOS();
        ret.setOperatingSystem(os.name());
        ret.setOsFamily(os.getFamily().name());
        ret.setOsString(CrossSystem.getOSString());

        ret.setArchFamily(CrossSystem.getARCHFamily().name());
        ret.setArchString(CrossSystem.getARCHString());

        ret.setJavaVersion(Application.getJavaVersion());
        ret.setJavaVersionString(Application.getJVMVersion());
        ret.setJvm64Bit(Application.is64BitJvm());

        ret.setHeadless(Application.isHeadless());

        ret.setOs64Bit(CrossSystem.is64BitOperatingSystem());
        ret.setArch64Bit(CrossSystem.is64BitArch());

        ret.setStartupTimeStamp(startupTimeStamp);

        try {
            final java.lang.management.MemoryUsage memory = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            if (memory != null) {
                ret.setHeapUsed(memory.getUsed());
                ret.setHeapCommitted(memory.getCommitted());
                ret.setHeapMax(memory.getMax());
            }
        } catch (final Throwable e) {
            LoggerFactory.getDefaultLogger().log(e);
        }

        return ret;
    }

}
