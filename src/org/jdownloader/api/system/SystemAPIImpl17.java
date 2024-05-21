package org.jdownloader.api.system;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.appwork.utils.Files17;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.logging.LogController;
import org.jdownloader.myjdownloader.client.bindings.StorageInformationStorable;

public class SystemAPIImpl17 {
    private static Path getPath(final FileStore fileStore) {
        if (CrossSystem.isWindows()) {
            for (final Path rootPath : FileSystems.getDefault().getRootDirectories()) {
                try {
                    if (Files.isDirectory(rootPath) && Files.getFileStore(rootPath).equals(fileStore)) {
                        return rootPath;
                    }
                } catch (NoSuchFileException ignore) {
                } catch (IOException e) {
                    LogController.CL().log(e);
                }
            }
        }
        final String rootPath = new Regex(fileStore.toString(), "^\\s*(.*?) \\(.*?\\)\\s*$").getMatch(0);
        if (rootPath != null) {
            return Paths.get(rootPath);
        } else {
            return null;
        }
    }

    public static List<StorageInformationStorable> getStorageInfos(final String path) {
        final List<StorageInformationStorable> ret = new ArrayList<StorageInformationStorable>();
        final LinkedHashMap<Path, FileStore> roots = new LinkedHashMap<Path, FileStore>();
        Path customPath = null;
        if (StringUtils.isNotEmpty(path)) {
            try {
                final Path pathObj = Paths.get(path);
                roots.put(pathObj, Files.getFileStore(pathObj));
                customPath = pathObj;
            } catch (NoSuchFileException ignore) {
            } catch (InvalidPathException e) {
                LogController.CL().log(e);
            } catch (IOException e) {
                LogController.CL().log(e);
            }
        }
        if (roots.isEmpty()) {
            for (final FileStore fileStore : Files17.getFileStores()) {
                final Path fileStorePath = getPath(fileStore);
                if (fileStorePath != null && Files.isDirectory(fileStorePath)) {
                    roots.put(fileStorePath, fileStore);
                }
            }
        }
        for (Entry<Path, FileStore> entry : roots.entrySet()) {
            final StorageInformationStorable storage = new StorageInformationStorable();
            final Path root = entry.getKey();
            try {
                final FileStore store = entry.getValue();
                if ((customPath == null || !customPath.equals(root)) && (store.isReadOnly() || SystemAPIImpl.isFilteredFileSystem(store.type()) || SystemAPIImpl.isFilteredPath(root.toString()))) {
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
}
