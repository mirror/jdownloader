package org.jdownloader.api.system;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.appwork.utils.Files17;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.bindings.StorageInformationStorable;

public class SystemAPIImpl17 {
    public static List<StorageInformationStorable> getStorageInfos(final String path) {
        final List<StorageInformationStorable> ret = new ArrayList<StorageInformationStorable>();
        final List<String> typeFilters;
        final List<String> pathFilters;
        if (CrossSystem.isUnix()) {
            typeFilters = Arrays.asList("usbfs", "fusectl", "hugetlbfs", "binfmt_misc", "cgroup", "pstore", "sysfs", "tmpfs", "proc", "configfs", "debugfs", "mqueue", "devtmpfs", "devpts", "devfs", "securityfs", "nfsd", "fusectl", "fuse.gvfsd-fuse", "rpc_pipefs", "efivarfs", "fuse.lxcfs");
            pathFilters = Arrays.asList("/proc", "/boot", "/sys", "/dev");
        } else {
            typeFilters = Arrays.asList(new String[0]);
            pathFilters = Arrays.asList(new String[0]);
        }
        final LinkedHashMap<Path, FileStore> roots = new LinkedHashMap<Path, FileStore>();
        Path customPath = null;
        if (StringUtils.isNotEmpty(path)) {
            try {
                final Path pathObj = Paths.get(path);
                roots.put(pathObj, Files.getFileStore(pathObj));
                customPath = pathObj;
            } catch (InvalidPathException e) {
                LogController.CL().log(e);
            } catch (IOException e) {
                LogController.CL().log(e);
            }
        }
        if (roots.isEmpty()) {
            for (final FileStore fileStore : Files17.getFileStores()) {
                final Path fileStorePath = Files17.getPath(fileStore);
                if (fileStorePath != null) {
                    roots.put(fileStorePath, fileStore);
                }
            }
        }
        for (Entry<Path, FileStore> entry : roots.entrySet()) {
            final StorageInformationStorable storage = new StorageInformationStorable();
            final Path root = entry.getKey();
            try {
                final FileStore store = entry.getValue();
                if ((customPath == null || !customPath.equals(root)) && (store.isReadOnly() || typeFilters.contains(store.type()) || isFiltered(pathFilters, root.toString()))) {
                    continue;
                } else {
                    storage.setPath(root.toString());
                    storage.setSize(store.getTotalSpace());
                    storage.setFree(store.getUsableSpace());
                }
            } catch (final IOException e) {
                LogController.CL().log(e);
                if (storage.getPath() == null) {
                    storage.setPath(root.toString());
                }
                storage.setError(e.toString());
            }
            ret.add(storage);
        }
        return ret;
    }

    private static boolean isFiltered(final List<String> filters, final String path) {
        if (filters.size() > 0 && path != null) {
            for (String filter : filters) {
                if (path.startsWith(filter)) {
                    return true;
                }
            }
        }
        return false;
    }
}
