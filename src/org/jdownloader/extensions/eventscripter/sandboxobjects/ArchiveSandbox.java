package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.io.File;
import java.util.ArrayList;

import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.multi.ArchiveType;

public class ArchiveSandbox {

    private Archive archive;

    public ArchiveSandbox(Archive archive) {
        this.archive = archive;

        // archive.getName();

    }

    public ArchiveSandbox() {
        // {"variant":null,"host":null,"name":null,"comment":null,"availability":null,"variants":false,"priority":"DEFAULT","packageUUID":-1,"bytesTotal":-1,"uuid":-1,"url":null,"enabled":false}

    }

    public String getArchiveType() {
        if (archive == null) {
            return ArchiveType.RAR_MULTI.name();
        }
        return archive.getArchiveType() + "";
    }

    public String[] getExtractedFiles() {
        if (archive == null || archive.getExtractedFiles() == null || archive.getExtractedFiles().size() == 0) {
            return null;
        }
        ArrayList<String> lst = new ArrayList<String>();
        for (File s : archive.getExtractedFiles()) {
            lst.add(s.getAbsolutePath());
        }

        return lst.toArray(new String[] {});
    }

    public String getName() {
        if (archive == null) {
            return null;
        }
        return archive.getName();
    }

    public String getFolder() {
        if (archive == null) {
            return null;
        }
        return archive.getFolder().getAbsolutePath();
    }
}
