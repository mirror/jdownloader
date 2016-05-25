package jd.controlling.downloadcontroller;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.api.system.ProcMounts;
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
    private final boolean                               SUPPORTED = Application.getJavaVersion() >= Application.JAVA16;

    public DiskSpaceManager() {
        reservations = new HashMap<DiskSpaceReservation, Object>();
        config = JsonConfig.create(GeneralSettings.class);
    }

    public synchronized DISKSPACERESERVATIONRESULT check(DiskSpaceReservation reservation) {
        return checkAndReserve(reservation, null);
    }

    public synchronized DISKSPACERESERVATIONRESULT checkAndReserve(DiskSpaceReservation reservation, Object requestor) {
        if (reservation == null) {
            throw new IllegalArgumentException("reservation must not be null!");
        }
        if (!SUPPORTED) {
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
        if (CrossSystem.isUnix()) {
            try {
                final List<ProcMounts> procMounts = ProcMounts.list();
                if (procMounts != null) {
                    final String destination = reservation.getDestination().getAbsolutePath();
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
            final String destination = reservation.getDestination().getAbsolutePath();
            if (!destination.startsWith("\\")) {
                final File[] roots = File.listRoots();
                if (roots != null) {
                    for (final File root : roots) {
                        final String rootString = root.getAbsolutePath();
                        if (destination.startsWith(rootString)) {
                            bestRootMatch = rootString;
                            break;
                        }
                    }
                }
            } else {
                // simple unc support (netshares without assigned drive letter)
                File existingFile = reservation.getDestination();
                while (existingFile != null) {
                    if (existingFile.exists()) {
                        bestRootMatch = existingFile.getAbsolutePath();
                        break;
                    } else {
                        existingFile = existingFile.getParentFile();
                    }
                }
            }
        }
        if (bestRootMatch == null || !new File(bestRootMatch).exists()) {
            return DISKSPACERESERVATIONRESULT.INVALIDDESTINATION;
        }
        final long forcedFreeSpaceOnDisk = Math.max(0l, config.getForcedFreeSpaceOnDisk() * 1024l * 1024l);
        long requestedDiskSpace = Math.max(0, reservation.getSize()) + forcedFreeSpaceOnDisk;
        final long freeDiskSpace = new File(bestRootMatch).getUsableSpace();
        if (freeDiskSpace < requestedDiskSpace) {
            return DISKSPACERESERVATIONRESULT.FAILED;
        }
        for (final DiskSpaceReservation reserved : reservations.keySet()) {
            if (reserved.getDestination().getAbsolutePath().startsWith(bestRootMatch)) {
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
