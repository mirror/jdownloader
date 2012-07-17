package org.jdownloader.extensions.extraction;

import java.io.File;
import java.util.ArrayList;

import org.appwork.storage.Storable;

public class ExtractionInfo implements Storable {
    private String extractToFolder;

    public String getExtractToFolder() {
        return extractToFolder;
    }

    public void setExtractToFolder(String extractToFolder) {
        this.extractToFolder = extractToFolder;
    }

    public ArrayList<String> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<String> files) {
        this.files = files;
    }

    private ArrayList<String> files;

    public ExtractionInfo(/* storable */) {
    }

    public ExtractionInfo(File extractToFolder, Archive archive) {
        this.extractToFolder = extractToFolder.getAbsolutePath();
        this.files = new ArrayList<String>();
        for (File f : archive.getExtractedFiles()) {
            files.add(f.getAbsolutePath());
        }
    }
}
