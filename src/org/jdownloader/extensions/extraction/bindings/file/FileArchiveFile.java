package org.jdownloader.extensions.extraction.bindings.file;

import java.awt.Color;
import java.io.File;

import org.jdownloader.extensions.extraction.ArchiveFile;

public class FileArchiveFile implements ArchiveFile {

    private File file;

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

    public boolean isValid() {
        return exists();
    }

    public boolean delete() {
        return file.delete();
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

    public void setStatus(Status error) {
    }

    public void setMessage(String plugins_optional_extraction_status_notenoughspace) {
    }

    public void setProgress(long value, long max, Color color) {
    }

}
