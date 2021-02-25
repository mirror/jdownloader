package jd.controlling.downloadcontroller;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.appwork.utils.Files;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.os.CrossSystem;

public class DiskSpaceChecker {
    protected final DiskSpaceReservation         diskSpaceReservation;
    protected final Object                       requestor;
    protected final CopyOnWriteArrayList<String> roots = new CopyOnWriteArrayList<String>();

    public DiskSpaceChecker(final DiskSpaceReservation diskSpaceReservation, final Object requestor) {
        this.diskSpaceReservation = diskSpaceReservation;
        this.requestor = requestor;
        findRoots();
    }

    public String getRoot() {
        if (roots.size() > 0) {
            return roots.get(0);
        } else {
            return null;
        }
    }

    protected void findRoots() {
        final File dest = getDestination();
        final File normalRoot = Files.guessRoot(dest);
        if (normalRoot != null) {
            roots.addIfAbsent(normalRoot.toString());
        }
        try {
            final File realRoot = Files.guessRoot(dest.getCanonicalFile());
            if (realRoot != null) {
                roots.addIfAbsent(realRoot.toString());
            }
        } catch (IOException e) {
            getLogger().log(e);
        }
    }

    public List<String> getRoots() {
        return roots;
    }

    public long getSize() {
        return getDiskSpaceReservation().getSize();
    }

    public long getUsableSpace() {
        return Files.getUsableSpace(getDestination());
    }

    public DiskSpaceReservation getDiskSpaceReservation() {
        return diskSpaceReservation;
    }

    protected File getDestination() {
        return getDiskSpaceReservation().getDestination();
    }

    protected LogInterface getLogger() {
        return getDiskSpaceReservation().getLogger();
    }

    public boolean isSameRoot(final DiskSpaceChecker checker) {
        if (checker == null) {
            return false;
        } else if (checker == this) {
            return true;
        } else {
            final Set<String> remainingRoots = new HashSet<String>(getRoots());
            boolean sameRoot = remainingRoots.removeAll(checker.getRoots());
            if (CrossSystem.isWindows() && !sameRoot) {
                for (final String otherRoot : checker.getRoots()) {
                    String matchingRoot = null;
                    for (final String testRoot : remainingRoots) {
                        if (testRoot.equalsIgnoreCase(otherRoot)) {
                            matchingRoot = testRoot;
                            break;
                        }
                    }
                    if (matchingRoot != null && remainingRoots.remove(matchingRoot)) {
                        break;
                    }
                }
                sameRoot = getRoots().size() > remainingRoots.size();
            }
            return sameRoot;
        }
    }

    public Object getRequestor() {
        return requestor;
    }
}
