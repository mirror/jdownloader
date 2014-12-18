package org.jdownloader.api.extraction;

import java.util.HashMap;

import org.appwork.storage.Storable;

public class ArchiveStatusStorable implements Storable {

    private String                             archiveId   = null;
    private String                             archiveName = null;
    private HashMap<String, ArchiveFileStatus> states      = null;

    public ArchiveStatusStorable(/* Storable */) {

    }

    public ArchiveStatusStorable(HashMap<String, ArchiveFileStatus> states) {
        this(null, null, states);
    }

    public ArchiveStatusStorable(String archiveId, String name, HashMap<String, ArchiveFileStatus> states) {
        this.setStates(states);
        this.setArchiveId(archiveId);
        this.archiveName = name;
    }

    public String getArchiveId() {
        return archiveId;
    }

    public void setArchiveId(String archiveId) {
        this.archiveId = archiveId;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    public HashMap<String, ArchiveFileStatus> getStates() {
        return states;
    }

    public void setStates(HashMap<String, ArchiveFileStatus> states) {
        this.states = states;
    }

    public static enum ArchiveFileStatus {
        COMPLETE,
        INCOMPLETE,
        MISSING;
    }

}
