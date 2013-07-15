package org.jdownloader.extensions.extraction;

import java.util.ArrayList;
import java.util.HashSet;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;

public class ArchiveSettings implements Storable {
    private ArchiveController      archiveController;
    private ArrayList<ArchiveItem> archiveItems;
    private BooleanStatus          autoExtract                        = BooleanStatus.UNSET;
    private ExtractionInfo         extractionInfo;
    private String                 extractPath;
    private String                 finalPassword;
    private BooleanStatus          getOverwriteFiles                  = BooleanStatus.UNSET;
    private HashSet<String>        passwords;
    private BooleanStatus          removeDownloadLinksAfterExtraction = BooleanStatus.UNSET;
    private BooleanStatus          removeFilesAfterExtraction         = BooleanStatus.UNSET;
    private boolean                needsSaving                        = false;

    public ArchiveSettings(/* Storable */) {

    }

    public void assignController(ArchiveController archiveController) {
        this.archiveController = archiveController;
        needsSaving = false;
    }

    public ArrayList<ArchiveItem> getArchiveItems() {
        return archiveItems;
    }

    public BooleanStatus getAutoExtract() {
        return autoExtract;
    }

    public ExtractionInfo getExtractionInfo() {
        return extractionInfo;
    }

    public String getExtractPath() {
        return extractPath;
    }

    public String getFinalPassword() {
        return finalPassword;
    }

    public BooleanStatus getOverwriteFiles() {
        return getOverwriteFiles;
    }

    public HashSet<String> getPasswords() {
        return passwords;
    }

    public BooleanStatus getRemoveDownloadLinksAfterExtraction() {

        return removeDownloadLinksAfterExtraction;
    }

    public BooleanStatus getRemoveFilesAfterExtraction() {
        return removeFilesAfterExtraction;
    }

    public void setArchiveItems(ArrayList<ArchiveItem> files) {
        this.archiveItems = files;
        fireUpdate();

    }

    private void fireUpdate() {
        if (archiveController != null) {
            archiveController.update(this);
            needsSaving = true;
        }

    }

    public void setAutoExtract(BooleanStatus overwriteFiles) {
        this.autoExtract = overwriteFiles;
        fireUpdate();
    }

    public void setExtractionInfo(ExtractionInfo extractionInfo) {
        this.extractionInfo = extractionInfo;
        fireUpdate();
    }

    public void setExtractPath(String extractPath) {
        this.extractPath = extractPath;
        fireUpdate();
    }

    public void setFinalPassword(String password) {
        this.finalPassword = password;
        fireUpdate();
    }

    public void setOverwriteFiles(BooleanStatus overwriteFiles) {
        this.getOverwriteFiles = overwriteFiles;
        fireUpdate();
    }

    public void setPasswords(HashSet<String> passwords) {
        this.passwords = passwords;
        fireUpdate();
    }

    public void setRemoveDownloadLinksAfterExtraction(BooleanStatus overwriteFiles) {
        this.removeDownloadLinksAfterExtraction = overwriteFiles;
        fireUpdate();
    }

    public void setRemoveFilesAfterExtraction(BooleanStatus overwriteFiles) {
        this.removeFilesAfterExtraction = overwriteFiles;
        fireUpdate();
    }

    public static final TypeRef<ArchiveSettings> TYPEREF = new TypeRef<ArchiveSettings>() {
                                                         };

    public boolean needsSaving() {
        return needsSaving;
    }

    public ArchiveSettings createClone() {
        ArchiveSettings ret = JSonStorage.restoreFromString(JSonStorage.toString(this), TYPEREF);
        ret.needsSaving = needsSaving;
        ret.archiveController = archiveController;
        return ret;
    }

}
