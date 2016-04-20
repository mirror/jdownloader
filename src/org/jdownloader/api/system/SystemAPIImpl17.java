package org.jdownloader.api.system;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.myjdownloader.client.bindings.StorageInformationStorable;

public class SystemAPIImpl17 {

    public static List<StorageInformationStorable> getStorageInfos(final String path) {
        final List<StorageInformationStorable> ret = new ArrayList<StorageInformationStorable>();
        final List<Path> roots = new ArrayList<Path>();
        final boolean customPath;
        if (StringUtils.isNotEmpty(path)) {
            roots.add(new File(path).toPath());
            customPath = true;
        } else {
            customPath = false;
        }
        if (roots.size() == 0) {
            if (!CrossSystem.isWindows()) {
                try {
                    final List<ProcMounts> procMounts = ProcMounts.list();
                    if (procMounts != null) {
                        for (final ProcMounts procMount : procMounts) {
                            if (!procMount.isReadOnly()) {
                                final String mountPoint = procMount.getMountPoint();
                                if ("/".equals(mountPoint) || mountPoint.startsWith("/home") || mountPoint.startsWith("/mnt") || mountPoint.startsWith("/media")) {
                                    roots.add(new File(mountPoint).toPath());
                                }
                            }
                        }
                    }
                } catch (final IOException e) {
                }
            }
            if (roots.size() == 0) {
                for (final Path root : FileSystems.getDefault().getRootDirectories()) {
                    roots.add(root);
                }
            }
        }
        for (final Path root : roots) {
            final StorageInformationStorable storage = new StorageInformationStorable();
            try {
                final FileStore store = Files.getFileStore(root);
                if (customPath == false && store.isReadOnly()) {
                    continue;
                }
                storage.setPath(root.toString());
                storage.setSize(store.getTotalSpace());
                storage.setFree(store.getUsableSpace());
            } catch (final IOException e) {
                if (storage.getPath() == null) {
                    storage.setPath(root.toString());
                }
                storage.setError(e.toString());
            }
            ret.add(storage);
        }
        return ret;
    }

}
