package org.jdownloader.api.system;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.FileStoreHacks;

import org.appwork.utils.StringUtils;
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
            for (final FileStore fileStore : FileSystems.getDefault().getFileStores()) {
                final Path fileStorePath = FileStoreHacks.getPath(fileStore);
                if (fileStorePath != null) {
                    roots.add(fileStorePath);
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
