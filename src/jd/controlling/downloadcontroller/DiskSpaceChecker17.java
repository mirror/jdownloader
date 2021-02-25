package jd.controlling.downloadcontroller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.appwork.utils.Files17;

public class DiskSpaceChecker17 extends DiskSpaceChecker {
    public DiskSpaceChecker17(DiskSpaceReservation diskSpaceReservation, final Object requestor) {
        super(diskSpaceReservation, requestor);
    }

    @Override
    public String getRoot() {
        return super.getRoot();
    }

    @Override
    protected void findRoots() {
        super.findRoots();
        final File dest = getDestination();
        try {
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
            getLogger().log(e);
        }
    }

    @Override
    public long getUsableSpace() {
        final File dest = getDestination();
        try {
            final Long ret = Files17.getUsableSpace(dest.toPath());
            if (ret != null) {
                return ret.longValue();
            }
        } catch (Throwable e) {
            getLogger().log(e);
        }
        return super.getUsableSpace();
    }
}
