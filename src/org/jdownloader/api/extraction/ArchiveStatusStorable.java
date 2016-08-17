package org.jdownloader.api.extraction;

import java.util.HashMap;

import org.appwork.remoteapi.annotations.ApiDoc;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.jdownloader.extensions.extraction.Archive;

public class ArchiveStatusStorable implements Storable {
    public static void main(String[] args) {
        System.out.println(JSonStorage.serializeToJson(new ArchiveStatusStorable()));
    }

    private String           archiveId        = null;
    private long             controllerId     = -1;
    private ControllerStatus controllerStatus = ControllerStatus.NA;
    private String           type             = null;

    @ApiDoc("The status of the controller")
    public ControllerStatus getControllerStatus() {
        return controllerStatus;
    }

    public void setControllerStatus(ControllerStatus controllerStatus) {
        this.controllerStatus = controllerStatus;
    }

    @ApiDoc("-1 or the controller ID if any controller is active. Used in cancelExtraction?<ControllerID> ")
    public long getControllerId() {
        return controllerId;
    }

    public void setControllerId(long controllerId) {
        this.controllerId = controllerId;
    }

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

    public ArchiveStatusStorable(Archive archive, HashMap<String, ArchiveFileStatus> states) {
        this.setStates(states);
        this.setArchiveId(archive.getArchiveID());
        this.setArchiveName(archive.getName());
        if (archive.getArchiveType() != null) {
            this.setType(archive.getArchiveType().name());
        } else if (archive.getSplitType() != null) {
            this.setType(archive.getSplitType().name());
        }
    }

    @ApiDoc("ID to adress the archive. Used for example for extraction/getArchiveSettings?[<ARCHIVE_ID_1>,<ARCHIVE_ID_2>,...]")
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

    @ApiDoc("Map Keys: Filename of the Part-File. Values: ArchiveFileStatus\r\nExample: \r\n{\r\n\"archive.part1.rar\":\"COMPLETE\",\r\n\"archive.part2.rar\":\"MISSING\"\r\n}")
    public HashMap<String, ArchiveFileStatus> getStates() {
        return states;
    }

    public void setStates(HashMap<String, ArchiveFileStatus> states) {
        this.states = states;
    }

    @ApiDoc("Type of the archive. e.g. \"GZIP_SINGLE\", \"RAR_MULTI\",\"RAR_SINGLE\",.... ")
    public String getType() {
        return type;
    }

    public void setType(String archiveType) {
        this.type = archiveType;
    }

    public static enum ArchiveFileStatus {
        @ApiDoc("File is available  for extraction") COMPLETE,
        @ApiDoc("File exists, but is incomplete") INCOMPLETE,
        @ApiDoc("File does not exist") MISSING;
    }

    public static enum ControllerStatus {
        @ApiDoc("Extraction is currently running") RUNNING,
        @ApiDoc("Archive is queued for extraction and will run as soon as possible") QUEUED,
        @ApiDoc("No controller assigned") NA
    }
}
