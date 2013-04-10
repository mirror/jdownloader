package org.jdownloader.settings;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AbstractValidator;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultLongValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.annotations.ValidatorFactory;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.View;
import org.jdownloader.gui.views.components.LinktablesSearchCategory;

public interface GraphicalUserInterfaceSettings extends ConfigInterface {

    // Static Mappings for interface

    // org.jdownloader.settings.GraphicalUserInterfaceSettings

    class ThemeValidator extends AbstractValidator<String> {

        @Override
        public void validate(String themeID) throws ValidationException {
            if (!Application.getResource("themes/" + themeID).exists()) {
                throw new ValidationException(Application.getResource("themes/" + themeID) + " must exist");
            } else if (!Application.getResource("themes/" + themeID).isDirectory()) { throw new ValidationException(Application.getResource("themes/" + themeID) + " must be a directory"); }
        }

    }

    String getActivePluginConfigPanel();

    String getActiveConfigPanel();

    @AboutConfig
    @DescriptionForConfigEntry("Captcha Dialog Image scale Faktor in %")
    @DefaultIntValue(100)
    @SpinnerValidator(min = 50, max = 500, step = 10)
    int getCaptchaScaleFactor();

    @DefaultIntValue(20)
    @AboutConfig
    int getDialogDefaultTimeout();

    @DefaultIntValue(20)
    @AboutConfig
    int getCaptchaDialogTimeout();

    public void setCaptchaDialogTimeout(int i);

    @DefaultEnumValue("ALL")
    org.jdownloader.gui.views.downloads.View getDownloadView();

    @AboutConfig
    @DescriptionForConfigEntry("Refreshrate in ms for the DownloadView")
    @DefaultLongValue(500)
    @SpinnerValidator(min = 50, max = 5000, step = 25)
    @RequiresRestart
    public long getDownloadViewRefresh();

    FrameStatus getLastFrameStatus();

    @DefaultEnumValue("SKIP_FILE")
    IfFileExistsAction getLastIfFileExists();

    @AboutConfig
    String getLookAndFeel();

    @AboutConfig
    public String getPassword();

    LinktablesSearchCategory getSelectedDownloadSearchCategory();

    @DefaultStringValue("standard")
    @AboutConfig
    @DescriptionForConfigEntry("Icon Theme ID. Make sure that ./themes/<ID>/ exists")
    @ValidatorFactory(ThemeValidator.class)
    String getThemeID();

    // @AboutConfig
    // @Description("Enable/Disable the Linkgrabber Sidebar")
    // @DefaultBooleanValue(true)
    // @RequiresRestart
    // boolean isDownloadViewSidebarEnabled();
    //
    // @AboutConfig
    // @Description("Enable/Disable the DownloadView Sidebar QuicktoggleButton")
    // @DefaultBooleanValue(true)
    // @RequiresRestart
    // boolean isDownloadViewSidebarToggleButtonEnabled();
    //
    // @DefaultBooleanValue(true)
    // @RequiresRestart
    // boolean isDownloadViewSidebarVisible();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isBalloonNotificationEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("Enable/disable Enable/disable Clipboard monitoring")
    @DefaultBooleanValue(true)
    boolean isClipboardMonitored();

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("If disabled, The Hostercolumn will show gray disabled icons if the link is disabled")
    boolean isColoredIconsForDisabledHosterColumnEnabled();

    @DefaultBooleanValue(false)
    boolean isConfigViewVisible();

    @AboutConfig
    @DescriptionForConfigEntry("Highlight Table in Downloadview if table is filtered")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isFilterHighlightEnabled();

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("If enabled, The User Interface will switch to Linkgrabber Tab if a new job has been added")
    boolean isLinkgrabberAutoTabSwitchEnabled();

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("If enabled, JDownloader GUI will come to top when new links are added")
    boolean isLinkgrabberFrameToTopOnNewLinksEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("Enable/Disable the Linkgrabber Sidebar")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isLinkgrabberSidebarEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("Enable/Disable the Linkgrabber Sidebar")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isDownloadPanelOverviewVisible();

    void setDownloadPanelOverviewVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isDownloadPanelOverviewTotalInfoVisible();

    void setDownloadPanelOverviewTotalInfoVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isDownloadPanelOverviewVisibleOnlyInfoVisible();

    void setDownloadPanelOverviewVisibleOnlyInfoVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isDownloadPanelOverviewSettingsVisible();

    void setDownloadPanelOverviewSettingsVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isDownloadPanelOverviewSelectedInfoVisible();

    void setDownloadPanelOverviewSelectedInfoVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isDownloadPanelOverviewSmartInfoVisible();

    void setDownloadPanelOverviewSmartInfoVisible(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Enable/Disable the Linkgrabber Sidebar QuicktoggleButton")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isLinkgrabberSidebarToggleButtonEnabled();

    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isLinkgrabberSidebarVisible();

    @DefaultBooleanValue(false)
    boolean isLogViewVisible();

    @DefaultBooleanValue(false)
    @AboutConfig
    public boolean isPasswordProtectionEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("If true, ETAColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.")
    boolean isPremiumAlertETAColumnEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("If true, SpeedColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.")
    boolean isPremiumAlertSpeedColumnEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("If true, TaskColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.")
    boolean isPremiumAlertTaskColumnEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("Set to true of you want jd to remember the latest selected download view")
    @DefaultBooleanValue(false)
    boolean isSaveDownloadViewCrossSessionEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("True if move button should be visible in downloadview")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isShowMoveDownButton();

    @DescriptionForConfigEntry("True if move button should be visible in downloadview")
    @AboutConfig
    @RequiresRestart
    @DefaultBooleanValue(false)
    boolean isShowMoveToBottomButton();

    @DescriptionForConfigEntry("True if move button should be visible in downloadview")
    @RequiresRestart
    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isShowMoveToTopButton();

    @DescriptionForConfigEntry("True if move button should be visible in downloadview")
    @AboutConfig
    @RequiresRestart
    @DefaultBooleanValue(false)
    boolean isShowMoveUpButton();

    @AboutConfig
    @DescriptionForConfigEntry("Highlight Column in Downloadview if table is not in downloadsortorder")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isSortColumnHighlightEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("If false, Most of the Tooltips will be disabled")
    @DefaultBooleanValue(true)
    boolean isTooltipEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("If true, hostcolumn will also show full hostname")
    @DefaultBooleanValue(false)
    boolean isShowFullHostname();

    @AboutConfig
    @DescriptionForConfigEntry("If true, java will try to use D3D for graphics")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isUseD3D();

    public void setUseD3D(boolean b);

    public void setShowFullHostname(boolean b);

    // void setDownloadViewSidebarEnabled(boolean b);
    //
    // void setDownloadViewSidebarToggleButtonEnabled(boolean b);
    //
    // void setDownloadViewSidebarVisible(boolean b);

    void setActiveConfigPanel(String name);

    void setActivePluginConfigPanel(String name);

    void setBalloonNotificationEnabled(boolean b);

    void setCaptchaScaleFactor(int b);

    void setClipboardMonitored(boolean b);

    void setColoredIconsForDisabledHosterColumnEnabled(boolean b);

    void setConfigViewVisible(boolean b);

    void setDialogDefaultTimeout(int value);

    void setDownloadView(org.jdownloader.gui.views.downloads.View view);

    public void setDownloadViewRefresh(long t);

    void setFilterHighlightEnabled(boolean b);

    public void setLastFrameStatus(FrameStatus status);

    void setLastIfFileExists(IfFileExistsAction value);

    void setLinkgrabberAutoTabSwitchEnabled(boolean b);

    void setLinkgrabberFrameToTopOnNewLinksEnabled(boolean b);

    void setLinkgrabberSidebarEnabled(boolean b);

    void setLinkgrabberSidebarToggleButtonEnabled(boolean b);

    void setLinkgrabberSidebarVisible(boolean b);

    void setLogViewVisible(boolean b);

    void setLookAndFeel(String laf);

    public void setPassword(String password);

    public void setPasswordProtectionEnabled(boolean b);

    void setPremiumAlertETAColumnEnabled(boolean b);

    void setPremiumAlertSpeedColumnEnabled(boolean b);

    void setPremiumAlertTaskColumnEnabled(boolean b);

    void setSaveDownloadViewCrossSessionEnabled(boolean b);

    @DefaultEnumValue("FILENAME")
    void setSelectedDownloadSearchCategory(LinktablesSearchCategory selectedCategory);

    void setShowMoveDownButton(boolean b);

    void setShowMoveToBottomButton(boolean b);

    void setShowMoveToTopButton(boolean b);

    void setShowMoveUpButton(boolean b);

    void setSortColumnHighlightEnabled(boolean b);

    void setThemeID(String themeID);

    void setTooltipEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(2500)
    @SpinnerValidator(min = 50, max = 5000, step = 50)
    @RequiresRestart
    int getTooltipTimeout();

    void setTooltipTimeout(int t);

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isCaptchaDialogUniquePositionByHosterEnabled();

    void setCaptchaDialogUniquePositionByHosterEnabled(boolean b);

    @DefaultEnumValue("DETAILS")
    @AboutConfig
    void setFileChooserView(View view);

    View getFileChooserView();

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry("Hide the package in case it only contains one child")
    boolean isHideSingleChildPackages();

    void setHideSingleChildPackages(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart
    boolean isFileCountInSizeColumnVisible();

    void setFileCountInSizeColumnVisible(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Paint all labels/text with or without antialias. Default value is false.")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isSpeedmeterAntiAliasingEnabled();

    void setSpeedmeterAntiAliasingEnabled(boolean b);

    public static enum ClearAction {
        CLEAR_LIST,
        RESET_PANEL
    }

    @DefaultEnumValue("CLEAR_LIST")
    @AboutConfig
    @RequiresRestart
    ClearAction getLinkgrabberDefaultClearAction();

    void setLinkgrabberDefaultClearAction(ClearAction action);

}
