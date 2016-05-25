package jd.controlling.downloadcontroller;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.appwork.utils.os.CrossSystem;

//from http://stackoverflow.com/questions/10678363/find-the-directory-for-a-filestore, 25.05.2016
public class FileStoreHacks {
    /**
     * Stores the known hacks.
     */
    private static final Map<Class<? extends FileStore>, Hacks> hacksMap = new HashMap<Class<? extends FileStore>, Hacks>();
    static {
        if (CrossSystem.isWindows()) {
            try {
                final Class<? extends FileStore> fileStoreClass = Class.forName("sun.nio.fs.WindowsFileStore").asSubclass(FileStore.class);
                hacksMap.put(fileStoreClass, new WindowsFileStoreHacks(fileStoreClass));
            } catch (ClassNotFoundException e) {
                // Probably not running on Windows.
            }
        } else {
            try {
                final Class<? extends FileStore> fileStoreClass = Class.forName("sun.nio.fs.UnixFileStore").asSubclass(FileStore.class);
                hacksMap.put(fileStoreClass, new UnixFileStoreHacks(fileStoreClass));
            } catch (ClassNotFoundException e) {
                // Probably not running on UNIX.
            }
            try {
                final Class<? extends FileStore> fileStoreClass = Class.forName("sun.nio.fs.LinuxFileStore").asSubclass(FileStore.class);
                hacksMap.put(fileStoreClass, new UnixFileStoreHacks(fileStoreClass));
            } catch (ClassNotFoundException e) {
                // Probably not running on LINUX.
            }
            try {
                final Class<? extends FileStore> fileStoreClass = Class.forName("sun.nio.fs.SolarisFileStore").asSubclass(FileStore.class);
                hacksMap.put(fileStoreClass, new UnixFileStoreHacks(fileStoreClass));
            } catch (ClassNotFoundException e) {
                // Probably not running on SOLARIS.
            }
            try {
                final Class<? extends FileStore> fileStoreClass = Class.forName("sun.nio.fs.BsdFileStore").asSubclass(FileStore.class);
                hacksMap.put(fileStoreClass, new UnixFileStoreHacks(fileStoreClass));
            } catch (ClassNotFoundException e) {
                // Probably not running on BSD.
            }
        }
    }

    private FileStoreHacks() {
    }

    /**
     * Gets the path from a file store. For some reason, NIO2 only has a method to go in the other direction.
     *
     * @param store
     *            the file store.
     * @return the path.
     */
    public static Path getPath(FileStore store) {
        final Hacks hacks = hacksMap.get(store.getClass());
        if (hacks == null) {
            return null;
        } else {
            return hacks.getPath(store);
        }
    }

    private static interface Hacks {
        Path getPath(FileStore store);
    }

    private static class WindowsFileStoreHacks implements Hacks {
        private final Field field;

        public WindowsFileStoreHacks(Class<?> fileStoreClass) {
            try {
                field = fileStoreClass.getDeclaredField("root");
                field.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("file field not found", e);
            }
        }

        @Override
        public Path getPath(FileStore store) {
            try {
                String root = (String) field.get(store);
                return FileSystems.getDefault().getPath(root);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Denied access", e);
            }
        }
    }

    private static class UnixFileStoreHacks implements Hacks {
        private final Field field;

        private UnixFileStoreHacks(Class<?> fileStoreClass) {
            Field field = null;
            try {
                field = fileStoreClass.getDeclaredField("file");
                field.setAccessible(true);
            } catch (NoSuchFieldException e) {
                try {
                    field = fileStoreClass.getSuperclass().getDeclaredField("file");
                    field.setAccessible(true);
                } catch (NoSuchFieldException e2) {
                    throw new IllegalStateException("file field not found", e2);
                }
            }
            this.field = field;
        }

        @Override
        public Path getPath(FileStore store) {
            try {
                return (Path) field.get(store);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Denied access", e);
            }
        }
    }

    public static String getRootFor(File file) throws IOException {
        FileStore fileFileStore = null;
        File existingFile = file;
        while (existingFile != null) {
            if (existingFile.exists()) {
                fileFileStore = Files.getFileStore(existingFile.toPath());
                break;
            } else {
                existingFile = existingFile.getParentFile();
            }
        }
        if (fileFileStore != null) {
            for (FileStore fileStore : FileSystems.getDefault().getFileStores()) {
                if (fileStore.equals(fileFileStore)) {
                    final Path fileStorePath = FileStoreHacks.getPath(fileStore);
                    if (fileStorePath != null) {
                        return fileStorePath.toFile().getAbsolutePath();
                    }
                }
            }
        }
        return null;
    }
}
