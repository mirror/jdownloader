package jd.controlling.downloadcontroller;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.JVMVersion;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.settings.GeneralSettings;

public class DiskSpaceManager {
    public static enum DISKSPACERESERVATIONRESULT {
        UNSUPPORTED,
        OK,
        INVALIDDESTINATION,
        FAILED
    }

    private final List<DiskSpaceChecker> reservations = new ArrayList<DiskSpaceChecker>();
    private final GeneralSettings        config;

    public DiskSpaceManager() {
        config = JsonConfig.create(GeneralSettings.class);
    }

    public synchronized DISKSPACERESERVATIONRESULT check(DiskSpaceReservation reservation) {
        return checkAndReserve(reservation, null);
    }

    private DISKSPACERESERVATIONRESULT handle(final DiskSpaceChecker checker, final DISKSPACERESERVATIONRESULT result, final Long requestedDiskSpace) {
        final DiskSpaceReservation reservation = checker.getDiskSpaceReservation();
        final LogInterface logger = reservation.getLogger();
        logger.info("DiskSpaceManager:Result:" + result + "|File:" + reservation.getDestination() + "|Root(s):" + checker.getRoots() + "|Requestor:" + checker.getRequestor() + "|RequestedSpace:" + (requestedDiskSpace != null ? SizeFormatter.formatBytes(requestedDiskSpace.longValue()) : null) + "|UsableSpace:" + SizeFormatter.formatBytes(checker.getUsableSpace()));
        return result;
    }

    public synchronized DISKSPACERESERVATIONRESULT checkAndReserve(final DiskSpaceReservation reservation, final Object requestor) {
        if (reservation == null) {
            throw new IllegalArgumentException("reservation must not be null!");
        } else {
            final DiskSpaceChecker checker;
            if (JVMVersion.isMinimum(JVMVersion.JAVA_1_7)) {
                checker = new DiskSpaceChecker17(reservation, requestor);
            } else {
                checker = new DiskSpaceChecker(reservation, requestor);
            }
            if (false) {
                return handle(checker, DISKSPACERESERVATIONRESULT.UNSUPPORTED, null);
            } else if (!config.isFreeSpaceCheckEnabled()) {
                return handle(checker, DISKSPACERESERVATIONRESULT.OK, null);
            } else if (reservation.getDestination() == null) {
                return handle(checker, DISKSPACERESERVATIONRESULT.INVALIDDESTINATION, null);
            } else {
                final String bestRootMatch = checker.getRoot();
                if (bestRootMatch == null || !new File(bestRootMatch).isDirectory()) {
                    return handle(checker, DISKSPACERESERVATIONRESULT.INVALIDDESTINATION, null);
                }
                final long forcedFreeSpaceOnDisk = Math.max(0l, config.getForcedFreeSpaceOnDisk() * 1024l * 1024l);
                long requestedDiskSpace = Math.max(0, reservation.getSize()) + forcedFreeSpaceOnDisk;
                for (final DiskSpaceChecker reservedDiskSpace : reservations) {
                    if (reservedDiskSpace.isSameRoot(checker)) {
                        requestedDiskSpace += Math.max(0, reservedDiskSpace.getSize());
                    }
                }
                final long freeDiskSpace = checker.getUsableSpace();
                // freeDiskSpace <0 -> unlimited, for example a virtual (distributed) filesystem
                if (freeDiskSpace >= 0 && freeDiskSpace < requestedDiskSpace) {
                    return handle(checker, DISKSPACERESERVATIONRESULT.FAILED, requestedDiskSpace);
                } else {
                    if (requestor != null) {
                        reservations.add(checker);
                    }
                    return handle(checker, DISKSPACERESERVATIONRESULT.OK, requestedDiskSpace);
                }
            }
        }
    }

    public synchronized boolean free(final DiskSpaceReservation reservation, final Object requestor) {
        final DiskSpaceChecker reservedDiskSpace = getDiskSpaceChecker(reservation);
        return reservedDiskSpace != null && reservedDiskSpace.getRequestor() == requestor && reservations.remove(reservedDiskSpace);
    }

    public synchronized boolean isReservedBy(final DiskSpaceReservation reservation, final Object requestor) {
        final DiskSpaceChecker reservedDiskSpace = getDiskSpaceChecker(reservation);
        return reservedDiskSpace != null && reservedDiskSpace.getRequestor() == requestor;
    }

    public synchronized boolean holdsReservations(final Object requestor) {
        for (final DiskSpaceChecker reservedDiskSpace : reservations) {
            if (reservedDiskSpace.getRequestor() == requestor) {
                return true;
            }
        }
        return false;
    }

    public synchronized void freeAllReservationsBy(final Object requestor) {
        final Iterator<DiskSpaceChecker> it = reservations.iterator();
        while (it.hasNext()) {
            final DiskSpaceChecker reservedDiskSpace = it.next();
            if (reservedDiskSpace != null && reservedDiskSpace.getRequestor() == requestor) {
                it.remove();
            }
        }
    }

    private synchronized DiskSpaceChecker getDiskSpaceChecker(final DiskSpaceReservation reservation) {
        if (reservation != null) {
            for (final DiskSpaceChecker reservedDiskSpace : reservations) {
                if (reservedDiskSpace.getDiskSpaceReservation() == reservation) {
                    return reservedDiskSpace;
                }
            }
        }
        return null;
    }

    public synchronized boolean isReserved(final DiskSpaceReservation reservation) {
        return getDiskSpaceChecker(reservation) != null;
    }
}
