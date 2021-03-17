package jd.controlling.downloadcontroller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.appwork.utils.Files17;
import org.appwork.utils.StringUtils;

public class DiskSpaceChecker17 extends DiskSpaceChecker {
    protected static boolean MOUNT_POINT_NOT_FOUND_IN_FSTAB = false;

    public DiskSpaceChecker17(DiskSpaceReservation diskSpaceReservation, final Object requestor) {
        super(diskSpaceReservation, requestor);
    }

    @Override
    protected void findRoots() {
        if (!MOUNT_POINT_NOT_FOUND_IN_FSTAB) {
            try {
                final File dest = getDestination();
                final Path normalRoot = Files17.guessRoot(dest.toPath());
                if (normalRoot != null) {
                    roots.addIfAbsent(normalRoot.toString());
                    Path currentPath = dest.toPath();
                    Path realPath = null;
                    while (currentPath != null) {
                        if (Files.exists(currentPath)) {
                            realPath = currentPath.toRealPath();
                            break;
                        } else if (currentPath.equals(normalRoot)) {
                            break;
                        } else {
                            currentPath = currentPath.getParent();
                        }
                    }
                    if (realPath != null) {
                        final Path realRoot = Files17.guessRoot(realPath);
                        if (realRoot != null) {
                            roots.addIfAbsent(realRoot.toString());
                        }
                    }
                }
            } catch (Throwable e) {
                if (e instanceof IOException && StringUtils.contains(e.getMessage(), "Mount point not found in fstab")) {
                    MOUNT_POINT_NOT_FOUND_IN_FSTAB = true;
                }
                getLogger().log(e);
            }
        }
        if (roots.size() == 0) {
            super.findRoots();
        }
    }

    @Override
    public long getUsableSpace() {
        if (!MOUNT_POINT_NOT_FOUND_IN_FSTAB) {
            final File dest = getDestination();
            try {
                final Long ret = Files17.getUsableSpace(dest.toPath());
                if (ret != null) {
                    return ret.longValue();
                }
            } catch (Throwable e) {
                getLogger().log(e);
            }
        }
        return super.getUsableSpace();
    }
}
