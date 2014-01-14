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
    private String  name;
    private String  filePath;

    public FileArchiveFile(File f) {
        this.file = f;
        name = file.getName();
        filePath = file.getAbsolutePath();
    }

    public File getFile() {
        return file;
    }

    public boolean isComplete() {
        return file.exists();
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FileArchiveFile)) return false;
        if (obj == this) return true;
        return file.equals(((FileArchiveFile) obj).file);
    }

    public void deleteFile(FileCreationManager.DeleteOption option) {
        FileCreationManager.getInstance().delete(file, option);

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

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public void notifyChanges(Object type) {
    }

}
