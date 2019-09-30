package org.jdownloader.extensions.extraction;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.appwork.storage.Storable;
import org.jdownloader.settings.IfFileExistsAction;

public class ArchiveSettings implements Storable {
    private ArchiveController                     archiveController;
    private BooleanStatus                         autoExtract                        = BooleanStatus.UNSET;
    private ExtractionInfo                        extractionInfo;
    private String                                extractPath;
    private String                                finalPassword;
    private IfFileExistsAction                    ifFileExistsAction                 = null;
    private volatile CopyOnWriteArrayList<String> passwords                          = new CopyOnWriteArrayList<String>();
    private BooleanStatus                         removeDownloadLinksAfterExtraction = BooleanStatus.UNSET;
    private BooleanStatus                         removeFilesAfterExtraction         = BooleanStatus.UNSET;
    private volatile boolean                      needsSaving                        = false;
    public static final String                    PASSWORD                           = "PASSWORD";
    public static final String                    AUTO_EXTRACT                       = "AUTO_EXTRACT";

    public ArchiveSettings(/* Storable */) {
    }

    public void assignController(ArchiveController archiveController) {
        this.archiveController = archiveController;
        needsSaving = false;
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

    public String getIfFileExistsAction() {
        if (ifFileExistsAction == null) {
            return null;
        }
        return ifFileExistsAction.toString();
    }

    public IfFileExistsAction _getIfFileExistsAction() {
        return ifFileExistsAction;
    }

    public List<String> getPasswords() {
        return passwords;
    }

    public BooleanStatus getRemoveDownloadLinksAfterExtraction() {
        return removeDownloadLinksAfterExtraction;
    }

    public BooleanStatus getRemoveFilesAfterExtraction() {
        return removeFilesAfterExtraction;
    }

    private void fireUpdate() {
        if (archiveController != null) {
            archiveController.update(this);
            needsSaving = true;
        }
    }

    public void setAutoExtract(BooleanStatus overwriteFiles) {
        if (overwriteFiles == null) {
            overwriteFiles = BooleanStatus.UNSET;
        }
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

    public void setIfFileExistsAction(String overwriteFiles) {
        try {
            if (overwriteFiles == null) {
                return;
            }
            _setIfFileExistsAction(IfFileExistsAction.valueOf(overwriteFiles));
        } catch (Exception e) {
        }
    }

    public void _setIfFileExistsAction(IfFileExistsAction overwriteFiles) {
        this.ifFileExistsAction = overwriteFiles;
        fireUpdate();
    }

    public void setPasswords(List<String> passwords) {
        if (passwords == null) {
            this.passwords = new CopyOnWriteArrayList<String>();
        } else {
            this.passwords = new CopyOnWriteArrayList<String>(passwords);
        }
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

    public boolean needsSaving() {
        return needsSaving;
    }
}
