package jd.controlling.downloadcontroller;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.jdownloader.settings.GeneralSettings;

public class DiskSpaceManager {

    public static enum DISKSPACERESERVATIONRESULT {
        UNSUPPORTED,
        OK,
        INVALIDDESTINATION,
        FAILED
    }

    private HashMap<DiskSpaceReservation, Object> reservations;
    private final GeneralSettings                 config;

    public DiskSpaceManager() {
        reservations = new HashMap<DiskSpaceReservation, Object>();
        config = JsonConfig.create(GeneralSettings.class);
    }

    public synchronized DISKSPACERESERVATIONRESULT check(DiskSpaceReservation reservation) {
        return checkAndReserve(reservation, null);
    }

    public synchronized DISKSPACERESERVATIONRESULT checkAndReserve(DiskSpaceReservation reservation, Object requestor) {
        if (reservation == null) throw new IllegalArgumentException("reservation must not be null!");
        if (Application.getJavaVersion() < Application.JAVA16) {
            /*
             * File.getUsableSpace is 1.6 only
             */
            return DISKSPACERESERVATIONRESULT.UNSUPPORTED;
        }
        if (!config.isFreeSpaceCheckEnabled()) return DISKSPACERESERVATIONRESULT.OK;
        HashSet<File> reservationPaths = new HashSet<File>();
        long requestedDiskSpace = Math.max(0, reservation.getSize()) + Math.max(0, config.getForcedFreeSpaceOnDisk() * 1024 * 1024);
        File destinationPath = reservation.getDestination();
        File checkPath = null;
        long freeSpace = -1;
        if (destinationPath != null && destinationPath.isFile()) {
            destinationPath = destinationPath.getParentFile();
        }
        if (destinationPath != null) reservationPaths.add(destinationPath);
        while (destinationPath != null) {
            if (destinationPath.exists() && checkPath == null) {
                checkPath = destinationPath;
                freeSpace = checkPath.getUsableSpace();
                if (freeSpace < requestedDiskSpace) { return DISKSPACERESERVATIONRESULT.FAILED; }
            }
            destinationPath = destinationPath.getParentFile();
            if (destinationPath != null) {
                reservationPaths.add(destinationPath);
            }
        }
        if (checkPath == null) { return DISKSPACERESERVATIONRESULT.INVALIDDESTINATION; }

        for (DiskSpaceReservation reserved : reservations.keySet()) {
            destinationPath = reserved.getDestination();
            if (destinationPath != null && destinationPath.isFile()) {
                destinationPath = destinationPath.getParentFile();
            }
            while (destinationPath != null) {
                if (reservationPaths.contains(destinationPath)) {
                    requestedDiskSpace += Math.max(0, reserved.getSize());
                    if (freeSpace < requestedDiskSpace) { return DISKSPACERESERVATIONRESULT.FAILED; }
                    break;
                }
                destinationPath = destinationPath.getParentFile();
            }
        }
        if (requestor != null) {
            reservations.put(reservation, requestor);
        }
        return DISKSPACERESERVATIONRESULT.OK;
    }

    public synchronized boolean free(DiskSpaceReservation reservation, Object requestor) {
        Object currentHolder = getRequestor(reservation);
        if (currentHolder != null && requestor == currentHolder) {
            reservations.remove(reservation);
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean isReservedBy(DiskSpaceReservation reservation, Object requestor) {
        if (requestor == null) return false;
        return reservations.get(reservation) == requestor;
    }

    public synchronized boolean holdsReservations(Object requestor) {
        return reservations.containsValue(requestor);
    }

    public synchronized void freeAllReservationsBy(Object requestor) {
        Iterator<Entry<DiskSpaceReservation, Object>> it = reservations.entrySet().iterator();
        while (it.hasNext()) {
            Entry<DiskSpaceReservation, Object> next = it.next();
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
