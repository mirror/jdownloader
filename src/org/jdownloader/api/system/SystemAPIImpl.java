package org.jdownloader.api.system;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.utils.Application;
import org.appwork.utils.ProcMounts;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.myjdownloader.client.bindings.StorageInformationStorable;
import org.jdownloader.myjdownloader.client.bindings.SystemInformationStorable;
import org.jdownloader.myjdownloader.client.bindings.interfaces.SystemInterface;
import org.jdownloader.updatev2.ForcedShutdown;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;
import org.jdownloader.updatev2.SmartRlyRestartRequest;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;

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

    public List<StorageInformationStorable> getStorageInfos(final String path) {
        if (Application.getJavaVersion() >= Application.JAVA17 && DownloadWatchDog.getInstance().getSession().getDiskSpaceManager().isSupported()) {
            return SystemAPIImpl17.getStorageInfos(path);
        } else {
            final List<StorageInformationStorable> ret = new ArrayList<StorageInformationStorable>();
            final List<File> roots = new ArrayList<File>();
            if (StringUtils.isNotEmpty(path)) {
                roots.add(new File(path));
            }
            if (roots.size() == 0) {
                if (CrossSystem.isUnix()) {
                    try {
                        final List<ProcMounts> procMounts = ProcMounts.list();
                        if (procMounts != null) {
                            for (final ProcMounts procMount : procMounts) {
                                if (!procMount.isReadOnly()) {
                                    final String mountPoint = procMount.getMountPoint();
                                    if ("/".equals(mountPoint) || mountPoint.startsWith("/home") || mountPoint.startsWith("/mnt") || mountPoint.startsWith("/media")) {
                                        roots.add(new File(mountPoint));
                                    }
                                }
                            }
                        }
                    } catch (final IOException e) {
                    }
                }
                if (roots.size() == 0) {
                    final File[] fileRoots = File.listRoots();
                    if (fileRoots != null) {
                        roots.addAll(Arrays.asList(fileRoots));
                    }
                }
            }
            for (final File root : roots) {
                final StorageInformationStorable storage = new StorageInformationStorable();
                storage.setPath(root.getPath());
                storage.setSize(root.getTotalSpace());
                storage.setFree(root.getUsableSpace());
                ret.add(storage);
            }
            return ret;
        }
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
        String javaVendor = System.getProperty("java.vm.vendor");
        if (javaVendor == null) {
            javaVendor = System.getProperty("java.vendor");
            if (javaVendor == null) {
                javaVendor = System.getProperty("java.specification.vendor");
            }
        }
        ret.setJavaVendor(javaVendor);
        String javaName = System.getProperty("java.vm.name");
        if (javaName == null) {
            javaName = System.getProperty("java.runtime.name");
        }
        ret.setJavaName(javaName);
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
