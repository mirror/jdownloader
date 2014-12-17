package org.jdownloader.extensions.extraction;

import java.awt.Color;

import org.jdownloader.controlling.FileCreationManager.DeleteOption;

public class MissingArchiveFile implements ArchiveFile {

    private final String name;
    private final String filePath;

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public long getFileSize() {
        return -1;
    }

    public MissingArchiveFile(final String name, final String filePath) {
        this.name = name;
        this.filePath = filePath;
    }

    @Override
    public void deleteFile(DeleteOption option) {
    }

    @Override
    public void deleteLink() {
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setStatus(ExtractionController controller, ExtractionStatus error) {
    }

    @Override
    public void setMessage(ExtractionController controller, String plugins_optional_extraction_status_notenoughspace) {
    }

    @Override
    public void setProgress(ExtractionController controller, long value, long max, Color color) {
    }

    @Override
    public void removePluginProgress(ExtractionController controller) {
    }

    @Override
    public void onCleanedUp(ExtractionController controller) {
    }

    @Override
    public void setArchive(Archive archive) {
    }

    @Override
    public void notifyChanges(Object type) {
    }

}
