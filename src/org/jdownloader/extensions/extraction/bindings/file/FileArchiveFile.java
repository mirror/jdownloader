package org.jdownloader.extensions.extraction.bindings.file;

import java.awt.Color;
import java.io.File;

import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ArchiveFile;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionStatus;

public class FileArchiveFile implements ArchiveFile {

    private File    file;
    private Archive archive;

    public FileArchiveFile(File f) {
        this.file = f;
    }

    public File getFile() {
        return file;
    }

    public boolean isComplete() {
        return true;
    }

    public String getFilePath() {
        return file.getAbsolutePath();
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FileArchiveFile)) return false;
        return file.equals(((FileArchiveFile) obj).file);
    }

    public boolean isValid() {
        return exists();
    }

    public boolean deleteFile() {
        return FileCreationManager.getInstance().delete(file);
    }

    public boolean exists() {
        return file.exists();
    }

    public String toString() {
        return file.getAbsolutePath();
    }

    public String getName() {
        return file.getName();
    }

    public void setStatus(ExtractionStatus error) {
    }

    public void setMessage(String plugins_optional_extraction_status_notenoughspace) {
    }

    public void setProgress(long value, long max, Color color) {
    }

    @Override
    public long getFileSize() {
        return file.length();
    }

    @Override
    public void deleteLink() {
    }

    @Override
    public void onCleanedUp(ExtractionController controller) {
    }

    @Override
    public void setArchive(Archive archive) {
        this.archive = archive;
    }

    public Archive getArchive() {
        return archive;
    }

}
