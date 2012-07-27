package org.jdownloader.gui.shortcuts;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

public interface ShortcutSettings extends ConfigInterface {
    @AboutConfig
    @DefaultStringValue("Ctrl+Enter")
    @DescriptionForConfigEntry("Confirm All Button in the Linkgrabber Tab")
    String getLinkgrabberActionsConfirmAll();

    @AboutConfig
    @DefaultStringValue("Ctrl+U")
    @DescriptionForConfigEntry("Upload Changes in Translator Module")
    String getTranslatorExtensionUpload();

    @AboutConfig
    @DefaultStringValue("Ctrl+S")
    @DescriptionForConfigEntry("Save Changes in Translator Module")
    String getTranslatorExtensionSaveLocally();

    @AboutConfig
    @DefaultStringValue("Ctrl+R")
    String getToolbarAutoReconnectToggle();

    @AboutConfig
    @DefaultStringValue("Ctrl+C")
    String getToolbarClipboardToggle();

    @AboutConfig
    String getGlobalPremiumToggleAction();

    @AboutConfig
    String getOpenDefaultDownloadFolderAction();

    @AboutConfig
    @DefaultStringValue("Ctrl+B")
    String getPauseDownloadsToggleAction();

    @AboutConfig
    @DefaultStringValue("Ctrl+I")
    String getDoReconnectAction();

    @AboutConfig
    @DefaultStringValue("Ctrl+S")
    String getStartDownloadsAction();

    @AboutConfig
    @DefaultStringValue("Ctrl+S")
    String getStopDownloadsAction();

    @AboutConfig
    String getDoUpdateCheckAction();

    @AboutConfig
    String getAddContainerAction();

    @AboutConfig
    @DefaultStringValue("Ctrl+Q")
    String getExitJDownloaderAction();

    @AboutConfig
    String getOpenKnowledgebaseAction();

    @AboutConfig
    String getOpenLatestChangesAction();

    @AboutConfig
    String getRestartJDownloaderAction();

    @AboutConfig
    @DefaultStringValue("Ctrl+COMMA")
    String getOpenSettingsAction();

    @AboutConfig
    String getExtractionUnpackFilesAction();

    @AboutConfig
    String getShutdownExtensionToggleShutdownAction();

    //

    void setLinkgrabberActionsConfirmAll(String shortcut);

    void setTranslatorExtensionUpload(String shortcut);

    void setTranslatorExtensionSaveLocally(String shortcut);

    void setToolbarAutoReconnectToggle(String shortcut);

    void setToolbarClipboardToggle(String shortcut);

    void setGlobalPremiumToggleAction(String shortcut);

    void setOpenDefaultDownloadFolderAction(String shortcut);

    void setPauseDownloadsToggleAction(String shortcut);

    void setDoReconnectAction(String shortcut);

    void setStartDownloadsAction(String shortcut);

    void setStopDownloadsAction(String shortcut);

    void setDoUpdateCheckAction(String shortcut);

    void setAddContainerAction(String shortcut);

    void setExitJDownloaderAction(String shortcut);

    void setOpenKnowledgebaseAction(String shortcut);

    void setOpenLatestChangesAction(String shortcut);

    void setRestartJDownloaderAction(String shortcut);

    void setOpenSettingsAction(String shortcut);

    void setExtractionUnpackFilesAction(String shortcut);

    void setShutdownExtensionToggleShutdownAction(String shortcut);

}
