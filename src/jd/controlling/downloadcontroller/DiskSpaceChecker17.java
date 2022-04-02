package jd.controlling.downloadcontroller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import org.appwork.utils.Exceptions;
import org.appwork.utils.Files17;
import org.appwork.utils.StringUtils;

import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadLinkDownloadable;

public class DiskSpaceChecker17 extends DiskSpaceChecker {
    protected static boolean MOUNT_POINT_NOT_FOUND_IN_FSTAB = false;
    private boolean          invalidPathExceptionThrown     = false;

    public DiskSpaceChecker17(DiskSpaceReservation diskSpaceReservation, final Object requestor) {
        super(diskSpaceReservation, requestor);
    }

    protected Path toPath(final File file) {
        try {
            return file.toPath();
        } catch (InvalidPathException e) {
            Path alternativeFile = null;
            try {
                if (file.equals(getDestination())) {
                    final Object owner = diskSpaceReservation.getOwner();
                    if (owner instanceof DownloadLinkDownloadable) {
                        final DownloadLinkDownloadable downloadLink = (DownloadLinkDownloadable) owner;
                        alternativeFile = new File(downloadLink.getDownloadLink().getDownloadLinkController().getSessionDownloadDirectory()).toPath();
                    } else if (owner instanceof PluginForHost) {
                        final PluginForHost plugin = (PluginForHost) owner;
                        alternativeFile = new File(plugin.getDownloadLink().getDownloadLinkController().getSessionDownloadDirectory()).toPath();
                    } else if (owner instanceof SingleDownloadController) {
                        final SingleDownloadController controller = (SingleDownloadController) owner;
                        alternativeFile = new File(controller.getSessionDownloadDirectory()).toPath();
                    }
                }
            } catch (InvalidPathException e2) {
                throw Exceptions.addSuppressed(e2, e);
            }
            if (alternativeFile == null) {
                throw e;
            } else {
                getLogger().exception("fallback to download folder:" + alternativeFile, e);
                return alternativeFile;
            }
        }
    }

    protected void log(Throwable e) {
        if (e instanceof InvalidPathException) {
            if (invalidPathExceptionThrown) {
                return;
            } else {
                invalidPathExceptionThrown = true;
            }
        }
        getLogger().log(e);
    }

    @Override
    protected void findRoots() {
        if (!MOUNT_POINT_NOT_FOUND_IN_FSTAB) {
            try {
                final File dest = getDestination();
                final Path normalRoot = Files17.guessRoot(toPath(dest));
                if (normalRoot != null) {
                    roots.addIfAbsent(normalRoot.toString());
                    Path currentPath = toPath(dest);
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
                log(e);
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
                final Long ret = Files17.getUsableSpace(toPath(dest));
                if (ret != null) {
                    return ret.longValue();
                }
            } catch (Throwable e) {
                log(e);
            }
        }
        return super.getUsableSpace();
    }
}
