package jd.controlling.downloadcontroller;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Files17;
import org.appwork.utils.ProcMounts;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GeneralSettings;

public class DiskSpaceManager {
    public static enum DISKSPACERESERVATIONRESULT {
        UNSUPPORTED,
        OK,
        INVALIDDESTINATION,
        FAILED
    }

    private final HashMap<DiskSpaceReservation, Object> reservations;
    private final GeneralSettings                       config;
    private final AtomicBoolean                         SUPPORTED = new AtomicBoolean(Application.getJavaVersion() >= Application.JAVA16);

    public DiskSpaceManager() {
        reservations = new HashMap<DiskSpaceReservation, Object>();
        config = JsonConfig.create(GeneralSettings.class);
    }

    public synchronized DISKSPACERESERVATIONRESULT check(DiskSpaceReservation reservation) {
        return checkAndReserve(reservation, null);
    }

    public static String getRootFor(File file) {
        String bestRootMatch = null;
        if (CrossSystem.isUnix()) {
            try {
                final List<ProcMounts> procMounts = ProcMounts.list();
                if (procMounts != null) {
                    final String destination = file.getAbsolutePath();
                    for (final ProcMounts procMount : procMounts) {
                        if (!procMount.isReadOnly() && destination.startsWith(procMount.getMountPoint())) {
                            if (bestRootMatch == null || (procMount.getMountPoint().length() > bestRootMatch.length())) {
                                bestRootMatch = procMount.getMountPoint();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LogController.CL().log(e);
            }
        }
        if (bestRootMatch == null) {
            // fallback to File.listRoots
            final String destination = file.getAbsolutePath();
            if (!destination.startsWith("\\")) {
                final File[] roots = File.listRoots();
                if (roots != null) {
                    for (final File root : roots) {
                        final String rootString = root.getAbsolutePath();
                        final boolean startsWith;
                        if (CrossSystem.isWindows()) {
                            startsWith = StringUtils.startsWithCaseInsensitive(destination, rootString);
                        } else {
                            startsWith = destination.startsWith(rootString);
                        }
                        if (startsWith) {
                            bestRootMatch = rootString;
                            break;
                        }
                    }
                }
            } else {
                // simple unc support (netshares without assigned drive letter)
                File existingFile = file;
                while (existingFile != null) {
                    if (existingFile.exists()) {
                        bestRootMatch = existingFile.getAbsolutePath();
                    }
                    existingFile = existingFile.getParentFile();
                }
            }
        }
        return bestRootMatch;
    }

    public boolean isSupported() {
        return SUPPORTED.get();
    }

    public synchronized DISKSPACERESERVATIONRESULT checkAndReserve(DiskSpaceReservation reservation, Object requestor) {
        if (reservation == null) {
            throw new IllegalArgumentException("reservation must not be null!");
        }
        if (!isSupported()) {
            /*
             * File.getUsableSpace is >=1.6 only
             */
            return DISKSPACERESERVATIONRESULT.UNSUPPORTED;
        }
        if (!config.isFreeSpaceCheckEnabled()) {
            return DISKSPACERESERVATIONRESULT.OK;
        }
        if (reservation.getDestination() == null) {
            return DISKSPACERESERVATIONRESULT.INVALIDDESTINATION;
        }
        String bestRootMatch = null;
        if (Application.getJavaVersion() >= Application.JAVA17 && (CrossSystem.isUnix() || CrossSystem.isMac())) {
            try {
                final File guessRootMatch = Files17.guessRoot(reservation.getDestination());
                if (guessRootMatch != null) {
                    bestRootMatch = guessRootMatch.getAbsolutePath();
                }
            } catch (final IOException e) {
                LogController.CL().log(e);
                if (OperatingSystem.FREEBSD.equals(CrossSystem.getOS()) && StringUtils.containsIgnoreCase(e.getMessage(), "mount point not found")) {
                    LogController.CL().info("Possible FreeBSD Jail detected! Disable DiskSpaceManager!");
                    SUPPORTED.set(false);
                    return DISKSPACERESERVATIONRESULT.UNSUPPORTED;
                }
            }
        }
        if (bestRootMatch == null) {
            bestRootMatch = getRootFor(reservation.getDestination());
        }
        if (bestRootMatch == null || !new File(bestRootMatch).exists()) {
            return DISKSPACERESERVATIONRESULT.INVALIDDESTINATION;
        }
        final long forcedFreeSpaceOnDisk = Math.max(0l, config.getForcedFreeSpaceOnDisk() * 1024l * 1024l);
        long requestedDiskSpace = Math.max(0, reservation.getSize()) + forcedFreeSpaceOnDisk;
        long freeDiskSpace = new File(bestRootMatch).getUsableSpace();
        if (freeDiskSpace == 0) {
            switch (CrossSystem.getOSFamily()) {
            case WINDOWS:
                // File.getUsableSpace fails for subst drives, workaround is to call File.getUsableSpace on one of its directories
                final File[] checks = new File(bestRootMatch).listFiles();
                if (checks != null) {
                    for (final File check : checks) {
                        if (check.isDirectory()) {
                            freeDiskSpace = check.getUsableSpace();
                            break;
                        }
                    }
                }
                break;
            default:
                break;
            }
        }
        if (freeDiskSpace < 0) {
            // unlimited, for example a virtual (distributed) filesystem
            return DISKSPACERESERVATIONRESULT.OK;
        }
        if (freeDiskSpace < requestedDiskSpace) {
            return DISKSPACERESERVATIONRESULT.FAILED;
        }
        for (final DiskSpaceReservation reserved : reservations.keySet()) {
            final boolean startsWith;
            if (CrossSystem.isWindows()) {
                startsWith = StringUtils.startsWithCaseInsensitive(reserved.getDestination().getAbsolutePath(), bestRootMatch);
            } else {
                startsWith = reserved.getDestination().getAbsolutePath().startsWith(bestRootMatch);
            }
            if (startsWith) {
                requestedDiskSpace += Math.max(0, reserved.getSize());
                if (freeDiskSpace < requestedDiskSpace) {
                    return DISKSPACERESERVATIONRESULT.FAILED;
                }
            }
        }
        if (requestor != null) {
            reservations.put(reservation, requestor);
        }
        return DISKSPACERESERVATIONRESULT.OK;
    }

    public synchronized boolean free(DiskSpaceReservation reservation, Object requestor) {
        final Object currentHolder = getRequestor(reservation);
        if (currentHolder != null && requestor == currentHolder) {
            reservations.remove(reservation);
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean isReservedBy(DiskSpaceReservation reservation, Object requestor) {
        if (requestor == null) {
            return false;
        }
        return reservations.get(reservation) == requestor;
    }

    public synchronized boolean holdsReservations(Object requestor) {
        return reservations.containsValue(requestor);
    }

    public synchronized void freeAllReservationsBy(Object requestor) {
        final Iterator<Entry<DiskSpaceReservation, Object>> it = reservations.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<DiskSpaceReservation, Object> next = it.next();
            if (next.getValue() == requestor) {
                it.remove();
            }
        }
    }

    public synchronized Object getRequestor(DiskSpaceReservation reservation) {
        return reservations.get(reservation);
    }

    public synchronized boolean isReserved(DiskSpaceReservation reservation) {
        return reservations.containsKey(reservation);
    }
}
