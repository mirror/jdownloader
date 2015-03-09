package org.jdownloader.extensions.extraction.bindings.file;

import java.awt.Color;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionStatus;

public class FileArchiveFile implements ArchiveFile {

    private final File                     file;
    private final String                   name;
    private final String                   filePath;
    private boolean                        isFirstArchiveFile = false;
    private final int                      hashCode;
    private final AtomicReference<Boolean> exists             = new AtomicReference<Boolean>(null);

    public boolean isFirstArchiveFile() {
        return isFirstArchiveFile;
    }

    public void setFirstArchiveFile(boolean isFirstArchiveFile) {
        this.isFirstArchiveFile = isFirstArchiveFile;
    }

    protected FileArchiveFile(File f) {
        this.file = f;
        exists.set(Boolean.TRUE);
        name = file.getName();
        filePath = file.getAbsolutePath();
        hashCode = (getClass() + name).hashCode();
    }

    public File getFile() {
        return file;
    }

    public boolean isComplete() {
        return exists();
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null) {
            if (obj instanceof FileArchiveFile) {
                return getFile().equals(((FileArchiveFile) obj).getFile());
            } else if (obj instanceof File) {
                return getFile().equals((obj));
            }
        }
        return false;
    }

    public void deleteFile(FileCreationManager.DeleteOption option) {
        FileCreationManager.getInstance().delete(getFile(), option);
        invalidateExists();
    }

    public String toString() {
        return "File: " + filePath + " Complete:" + isComplete();
    }

    public String getName() {
        return name;
    }

    public void setStatus(ExtractionController controller, ExtractionStatus error) {
    }

    public void setMessage(ExtractionController controller, String plugins_optional_extraction_status_notenoughspace) {
    }

    public void setProgress(ExtractionController controller, long value, long max, Color color) {
    }

    @Override
    public long getFileSize() {
        return getFile().length();
    }

    @Override
    public void onCleanedUp(ExtractionController controller) {
    }

    @Override
    public void setArchive(Archive archive) {
    }

    @Override
    public boolean exists() {
        Boolean ret = exists.get();
        if (ret == null) {
            ret = getFile().exists();
            exists.compareAndSet(null, ret);
        }
        return ret;
    }

    @Override
    public void notifyChanges(Object type) {
    }

    @Override
    public void removePluginProgress(ExtractionController controller) {
    }

    @Override
    public void invalidateExists() {
        exists.set(null);
    }

}
