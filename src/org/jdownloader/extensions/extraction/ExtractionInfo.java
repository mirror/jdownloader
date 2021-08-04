package org.jdownloader.extensions.extraction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.Storable;

public class ExtractionInfo implements Storable {
    private String extractToFolder;

    public String getExtractToFolder() {
        return extractToFolder;
    }

    public void setExtractToFolder(String extractToFolder) {
        this.extractToFolder = extractToFolder;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    private List<String> files;

    public ExtractionInfo(/* storable */) {
    }

    public ExtractionInfo(File extractToFolder, Archive archive) {
        final ArrayList<String> files = new ArrayList<String>();
        for (final File file : archive.getExtractedFiles()) {
            files.add(file.getAbsolutePath());
        }
        this.extractToFolder = extractToFolder.getAbsolutePath();
        this.files = files;
    }
}
