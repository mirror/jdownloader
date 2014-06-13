package jd.plugins.download.raf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.download.raf.BytesMappedFile.BytesMappedFileCallback;
import jd.plugins.download.raf.FileBytesMap.FileBytesMapView;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.logging.LogController;

public class BytesMappedFileManager {
    
    private static final class BytesMappedFileStorable implements Storable, FileBytesMapViewInterface {
        private long[][] markedAreas = null;
        private String   name        = null;
        private long     finalSize   = -1;
        
        private BytesMappedFileStorable(/* Storable */) {
        }
        
        public BytesMappedFileStorable(BytesMappedFile bytesMappedFile) {
            this.name = bytesMappedFile.getFile().getName();
            FileBytesMapView view = new FileBytesMapView(bytesMappedFile.getFileBytesMap());
            this.finalSize = view.getFinalSize();
            this.markedAreas = view.getMarkedAreas();
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public long getFinalSize() {
            return finalSize;
        }
        
        public void setFinalSize(long finalSize) {
            this.finalSize = finalSize;
        }
        
        public long[][] getMarkedAreas() {
            return markedAreas;
        }
        
        public void setMarkedAreas(long[][] markedAreas) {
            this.markedAreas = markedAreas;
        }
        
    }
    
    private final HashMap<File, BytesMappedFile> openFiles = new HashMap<File, BytesMappedFile>();
    private static final BytesMappedFileManager  INSTANCE  = new BytesMappedFileManager();
    
    public static BytesMappedFileManager getInstance() {
        return INSTANCE;
    }
    
    private BytesMappedFileManager() {
    }
    
    public synchronized FileBytesMapView getFileBytesMapView(File file) throws IOException {
        final File mapFile = getMapFile(file);
        BytesMappedFile ret = openFiles.get(mapFile);
        if (ret != null) return new FileBytesMapView(ret.getFileBytesMap());
        return new FileBytesMapView(readFileBytesMap(file));
    }
    
    public synchronized BytesMappedFile get(File file) throws IOException {
        final File mapFile = getMapFile(file);
        return openFiles.get(mapFile);
    }
    
    public synchronized BytesMappedFile lock(File file) throws IOException {
        final File mapFile = getMapFile(file);
        BytesMappedFile ret = openFiles.get(mapFile);
        if (ret == null) {
            FileBytesMap fileBytesMap = readFileBytesMap(file);
            final long fileSize = file.length();
            final long finalFileSize = fileBytesMap.getFinalSize();
            final long markedBytes = fileBytesMap.getMarkedBytes();
            if (fileSize < markedBytes || finalFileSize >= 0 && fileSize > finalFileSize) {
                LogController.CL(true).severe("PartFileReset:File:" + file + "|Length:" + file.length() + "|FileBytesMap:" + fileBytesMap);
                fileBytesMap.reset();
            }
            ret = new BytesMappedFile(file, fileBytesMap) {
                
                @Override
                protected void onFlushed() throws IOException {
                    write(this);
                }
            };
            openFiles.put(mapFile, ret);
        }
        LogController.CL(true).severe("lock:File:" + file + "|FileBytesMap:" + ret.getFileBytesMap());
        ret.lock();
        return ret;
    }
    
    public synchronized Boolean unlock(BytesMappedFile mappedFile) throws IOException {
        LogController.CL(true).severe("unlock:File:" + mappedFile.getFile());
        final File mapFile = getMapFile(mappedFile.getFile());
        final BytesMappedFile ret = openFiles.get(mapFile);
        if (ret != null) {
            if (ret.unlock() && ret.isOpen() == false && ret.isLocked() == false) {
                LogController.CL(true).severe("removed:File:" + mappedFile.getFile());
                openFiles.remove(mapFile);
                return true;
            }
            return false;
        }
        return null;
    }
    
    public synchronized BytesMappedFile open(BytesMappedFile mappedFile, BytesMappedFileCallback callback) throws IOException {
        final File mapFile = getMapFile(mappedFile.getFile());
        final BytesMappedFile ret = openFiles.get(mapFile);
        if (ret != null && mappedFile == ret) {
            LogController.CL(true).severe("open:File:" + mappedFile.getFile() + "|FileBytesMap:" + ret.getFileBytesMap());
            ret.open(callback);
            return ret;
        }
        return null;
    }
    
    public synchronized BytesMappedFile open(File file, BytesMappedFileCallback callback) throws IOException {
        final File mapFile = getMapFile(file);
        BytesMappedFile ret = openFiles.get(mapFile);
        if (ret == null) {
            FileBytesMap fileBytesMap = readFileBytesMap(file);
            final long fileSize = file.length();
            final long finalFileSize = fileBytesMap.getFinalSize();
            final long markedBytes = fileBytesMap.getMarkedBytes();
            if (fileSize < markedBytes || finalFileSize >= 0 && fileSize > finalFileSize) {
                LogController.CL(true).severe("PartFileReset:File:" + file + "|Length:" + file.length() + "|FileBytesMap:" + fileBytesMap);
                fileBytesMap.reset();
            }
            ret = new BytesMappedFile(file, fileBytesMap) {
                @Override
                protected void onFlushed() throws IOException {
                    write(this);
                }
            };
            openFiles.put(mapFile, ret);
        }
        LogController.CL(true).severe("open:File:" + file + "|FileBytesMap:" + ret.getFileBytesMap());
        ret.open(callback);
        return ret;
    }
    
    public synchronized Boolean close(BytesMappedFile mappedFile, BytesMappedFileCallback callback) throws IOException {
        LogController.CL(true).severe("close:File:" + mappedFile.getFile());
        final File mapFile = getMapFile(mappedFile.getFile());
        final BytesMappedFile ret = openFiles.get(mapFile);
        if (ret != null) {
            if (ret.close(callback) && ret.isLocked() == false) {
                LogController.CL(true).severe("removed:File:" + mappedFile.getFile());
                openFiles.remove(mapFile);
                return true;
            }
            return false;
        }
        return null;
    }
    
    public static String getFileBytesMapID(File file) {
        return getFileBytesMapID(file.getName());
    }
    
    public static String getFileBytesMapID(String fileName) {
        final String name;
        if (CrossSystem.isWindows() || DownloadWatchDog.getInstance().isForceMirrorDetectionCaseInsensitive()) {
            /* windows filesystem is case insensitive */
            name = fileName.toLowerCase(Locale.ENGLISH);
        } else {
            name = fileName;
        }
        String ext = Files.getExtension(name);
        return name.length() + "-" + Hash.getMD5(name) + "." + ext + ".jdresume";
    }
    
    public static File getMapFile(File file) {
        File parentFile = file.getParentFile();
        return new File(parentFile, getFileBytesMapID(file));
    }
    
    protected FileBytesMap readFileBytesMap(File file) throws IOException {
        File mapFile = getMapFile(file);
        final FileBytesMap ret = new FileBytesMap();
        if (mapFile.exists()) {
            try {
                byte[] bytes = IO.readFile(mapFile);
                BytesMappedFileStorable storable = JSonStorage.stringToObject(new String(bytes, "UTF-8"), new TypeRef<BytesMappedFileStorable>() {
                }, null);
                if (storable != null) ret.set(storable);
            } catch (final IOException e) {
                LogController.CL(true).log(e);
                throw e;
            }
        }
        return ret;
    }
    
    public void write(BytesMappedFile bytesMappedFile) throws IOException {
        synchronized (bytesMappedFile) {
            File file = bytesMappedFile.getFile();
            File mapFile = getMapFile(file);
            try {
                IO.secureWrite(mapFile, JSonStorage.serializeToJson(new BytesMappedFileStorable(bytesMappedFile)).getBytes("UTF-8"), IO.SYNC.META_AND_DATA);
            } catch (final IOException e) {
                LogController.CL(true).log(e);
                throw e;
            }
        }
    }
}
