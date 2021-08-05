package org.jdownloader.extensions.extraction;

import java.util.List;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.settings.IfFileExistsAction;

public class ArchiveSettings implements Storable {
    public static final TypeRef<ArchiveSettings>                TYPE_REF                           = new TypeRef<ArchiveSettings>() {
    };
    private ArchiveController                                   archiveController;
    private BooleanStatus                                       autoExtract                        = BooleanStatus.UNSET;
    private ExtractionInfo                                      extractionInfo;
    private String                                              extractPath;
    private String                                              finalPassword;
    private IfFileExistsAction                                  ifFileExistsAction                 = null;
    private volatile CopyOnWriteArrayList<String>               passwords                          = new CopyOnWriteArrayList<String>();
    private BooleanStatus                                       removeDownloadLinksAfterExtraction = BooleanStatus.UNSET;
    private BooleanStatus                                       removeFilesAfterExtraction         = BooleanStatus.UNSET;
    public static final String                                  PASSWORD                           = "PASSWORD";
    public static final String                                  AUTO_EXTRACT                       = "AUTO_EXTRACT";
    private String                                              archiveID                          = null;
    private String                                              settingsID                         = null;
    private WeakHashMap<AbstractPackageChildrenNode<?>, Object> assignedLinks                      = null;

    protected WeakHashMap<AbstractPackageChildrenNode<?>, Object> _getAssignedLinks() {
        if (assignedLinks == null) {
            assignedLinks = new WeakHashMap<AbstractPackageChildrenNode<?>, Object>();
        }
        return assignedLinks;
    }

    public String _getArchiveID() {
        return archiveID;
    }

    public String _getSettingsID() {
        return settingsID;
    }

    public ArchiveSettings(/* Storable */) {
    }

    protected void assignController(ArchiveController archiveController, final String archiveID, final String settingsID) {
        this.archiveController = archiveController;
        this.archiveID = archiveID;
        this.settingsID = settingsID;
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
        final IfFileExistsAction ifFileExistsAction = this.ifFileExistsAction;
        if (ifFileExistsAction == null) {
            return null;
        } else {
            return ifFileExistsAction.toString();
        }
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
        if (passwords == null || passwords.size() == 0) {
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

    protected boolean _needsSaving() {
        final String templateInstance = JSonStorage.toString(new ArchiveSettings());
        final String thisInstance = JSonStorage.toString(this);
        return !StringUtils.equals(templateInstance, thisInstance);
    }

    protected Boolean _exists() {
        final WeakHashMap<AbstractPackageChildrenNode<?>, Object> assignedLinks = this.assignedLinks;
        if (assignedLinks == null) {
            return null;
        } else {
            for (final Entry<AbstractPackageChildrenNode<?>, Object> entry : assignedLinks.entrySet()) {
                final AbstractPackageNode parent = (AbstractPackageNode) entry.getKey().getParentNode();
                if (parent != null && parent.getControlledBy() != null) {
                    return true;
                }
            }
            return false;
        }
    }
}
