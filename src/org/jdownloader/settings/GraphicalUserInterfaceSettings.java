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
import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.storage.config.annotations.LookUpKeys;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.annotations.ValidatorFactory;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.View;
import org.jdownloader.gui.laf.jddefault.JDDefaultLookAndFeel;
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

    @DefaultIntValue(20000)
    @AboutConfig
    int getDialogDefaultTimeoutInMS();

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
    @DefaultBooleanValue(false)
    boolean isSkipClipboardMonitorFirstRound();

    void setSkipClipboardMonitorFirstRound(boolean b);

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
    @DefaultBooleanValue(true)
    @LookUpKeys({ "linkgrabberhighlighonnewlinksenabled" })
    @DescriptionForConfigEntry("If enabled, JDownloader GUI switch to Linkgrabber Tab when new links are added")
    boolean isSwitchToLinkgrabberTabOnNewLinksAddedEnabled();

    public void setSwitchToLinkgrabberTabOnNewLinksAddedEnabled(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Enable/Disable the Linkgrabber Sidebar")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isLinkgrabberSidebarEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("Enable/Disable the DownloadPanel Overview panel ")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isDownloadPanelOverviewVisible();

    void setDownloadPanelOverviewVisible(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Enable/Disable the Linkgrabber Overview panel ")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isLinkgrabberOverviewVisible();

    void setLinkgrabberOverviewVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelTotalInfoVisible();

    void setOverviewPanelTotalInfoVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelVisibleOnlyInfoVisible();

    void setOverviewPanelVisibleOnlyInfoVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isDownloadPanelOverviewSettingsVisible();

    void setDownloadPanelOverviewSettingsVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelSelectedInfoVisible();

    void setOverviewPanelSelectedInfoVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelSmartInfoVisible();

    void setOverviewPanelSmartInfoVisible(boolean b);

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

    void setDialogDefaultTimeoutInMS(int ms);

    void setDownloadView(org.jdownloader.gui.views.downloads.View view);

    public void setDownloadViewRefresh(long t);

    void setFilterHighlightEnabled(boolean b);

    public void setLastFrameStatus(FrameStatus status);

    void setLastIfFileExists(IfFileExistsAction value);

    void setLinkgrabberAutoTabSwitchEnabled(boolean b);

    void setLinkgrabberSidebarEnabled(boolean b);

    void setLinkgrabberSidebarToggleButtonEnabled(boolean b);

    void setLinkgrabberSidebarVisible(boolean b);

    void setLogViewVisible(boolean b);

    public void setPassword(String password);

    public void setPasswordProtectionEnabled(boolean b);

    void setPremiumAlertETAColumnEnabled(boolean b);

    void setPremiumAlertSpeedColumnEnabled(boolean b);

    void setPremiumAlertTaskColumnEnabled(boolean b);

    void setSaveDownloadViewCrossSessionEnabled(boolean b);

    @DefaultEnumValue("FILENAME")
    void setSelectedDownloadSearchCategory(LinktablesSearchCategory selectedCategory);

    void setSortColumnHighlightEnabled(boolean b);

    void setThemeID(String themeID);

    void setTooltipEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(2000)
    @SpinnerValidator(min = 50, max = 5000, step = 50)
    @RequiresRestart
    int getTooltipDelay();

    void setTooltipDelay(int t);

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
        @EnumLabel("Clear List only")
        CLEAR_LIST,
        @EnumLabel("Reset full Panel")
        RESET_PANEL
    }

    @DefaultEnumValue("CLEAR_LIST")
    @AboutConfig
    @RequiresRestart
    ClearAction getLinkgrabberDefaultClearAction();

    void setLinkgrabberDefaultClearAction(ClearAction action);

    public static enum DeleteLinksDialogOption {
        @EnumLabel("Always hide Dialog and never delete Files from Harddisk")
        HIDE_ALWAYS_AND_NEVER_DELETE_ANY_LINKS_FROM_HARDDISK,
        @EnumLabel("Hide Dialog if CTRL is pressed and never delete Files from Harddisk")
        HIDE_IF_CTRL_IS_PRESSED_AND_NEVER_DELETE_ANY_LINKS_FROM_HARDDISK,
        @EnumLabel("Hide Dialog if CTRL is NOT pressed and never delete Files from Harddisk")
        HIDE_IF_CTRL_IS_NOT_PRESSED_AND_NEVER_DELETE_ANY_LINKS_FROM_HARDDISK;
    }

    @DefaultEnumValue("HIDE_IF_CTRL_IS_PRESSED_AND_NEVER_DELETE_ANY_LINKS_FROM_HARDDISK")
    @AboutConfig
    DeleteLinksDialogOption getShowDeleteLinksDialogOption();

    void setShowDeleteLinksDialogOption(DeleteLinksDialogOption option);

    public static enum MacDockProgressDisplay {
        @EnumLabel("Total Progress")
        TOTAL_PROGRESS,
        @EnumLabel("Nothing")
        NOTHING;
    }

    @DefaultEnumValue("TOTAL_PROGRESS")
    @AboutConfig
    MacDockProgressDisplay getMacDockProgressDisplay();

    void setMacDockProgressDisplay(MacDockProgressDisplay value);

    @DefaultEnumValue("MAINFRAME_IS_MAXIMIZED")
    @DescriptionForConfigEntry("When should the Mainframe come to Front if new Links were added?")
    @AboutConfig
    WindowState getMainframePopupTriggerWhenNewLinksWereAdded();

    void setMainframePopupTriggerWhenNewLinksWereAdded(WindowState action);

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isTaskBarFlashEnabled();

    void setTaskBarFlashEnabled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isCaptchaDebugModeEnabled();

    void setCaptchaDebugModeEnabled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isCaptchaDialogAOTWorkaroundEnabled();

    void setCaptchaDialogAOTWorkaroundEnabled(boolean b);

    @DefaultEnumValue("MAINFRAME_IS_MAXIMIZED_OR_ICONIFIED_OR_TOTRAY")
    @DescriptionForConfigEntry("When should a Captcha Dialog get the Input Focus?")
    @AboutConfig
    WindowState getFocusTriggerForCaptchaDialogs();

    void setFocusTriggerForCaptchaDialogs(WindowState action);

    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Requirment: Java 1.7 / Set to true if you want JDownloader to steal focus when the window pops up")
    @AboutConfig
    boolean isWindowsRequestFocusOnActivationEnabled();

    void setWindowsRequestFocusOnActivationEnabled(boolean b);

    public static enum StartButtonAction {
        @EnumLabel("Add all Linkgrabber links and start Downloads.")
        ADD_ALL_LINKS_AND_START_DOWNLOADS,
        @EnumLabel("Start Downloads only")
        START_DOWNLOADS_ONLY,
        @EnumLabel("Do Nothing - Disable Action")
        DISABLED
    }

    @DefaultEnumValue("ADD_ALL_LINKS_AND_START_DOWNLOADS")
    @DescriptionForConfigEntry("Choose what should happen when you click on the [Start Downloads] Button when you are in the Linkgrabber Tab")
    @AboutConfig
    StartButtonAction getStartButtonActionInLinkgrabberContext();

    void setStartButtonActionInLinkgrabberContext(StartButtonAction b);

    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Use horizontal Scrollbars in DownloadTable")
    @AboutConfig
    boolean isHorizontalScrollbarsInDownloadTableEnabled();

    void setHorizontalScrollbarsInDownloadTableEnabled(boolean b);

    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Use horizontal Scrollbars in Linkgrabber")
    @AboutConfig
    boolean isHorizontalScrollbarsInLinkgrabberTableEnabled();

    void setHorizontalScrollbarsInLinkgrabberTableEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(0)
    @DescriptionForConfigEntry("by default, table row's height dynamicly adapts to the fontsize. Set a value>0 to set your own custom row height.")
    int getCustomTableRowHeight();

    void setCustomTableRowHeight(int height);

    @RequiresRestart
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Every  odd row get's a light shadow if enabled")
    @AboutConfig
    boolean isTableAlternateRowHighlightEnabled();

    void setTableAlternateRowHighlightEnabled(boolean b);

    @RequiresRestart
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("The row that is 'touched' by the mouse cursor gets a darker shadow")
    @AboutConfig
    boolean isTableMouseOverHighlightEnabled();

    void setTableMouseOverHighlightEnabled(boolean b);

    @RequiresRestart
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Packages get a different background color if enabled")
    @AboutConfig
    boolean isPackagesBackgroundHighlightEnabled();

    void setPackagesBackgroundHighlightEnabled(boolean b);

    @AboutConfig
    @RequiresRestart
    @DefaultEnumValue("DEFAULT")
    LookAndFeelType getLookAndFeelTheme();

    void setLookAndFeelTheme(LookAndFeelType type);

    // org.jdownloader.gui.laf.jddefault.JDDefaultLookAndFeel
    public static enum LookAndFeelType {
        BLACK_EYE("de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel"),
        BLACK_MOON("de.javasoft.plaf.synthetica.SyntheticaBlackMoonLookAndFeel"),
        BLACK_STAR("de.javasoft.plaf.synthetica.SyntheticaBlackStarLookAndFeel"),
        BLUE_ICE("de.javasoft.plaf.synthetica.SyntheticaBlueIceLookAndFeel"),
        BLUE_MOON("de.javasoft.plaf.synthetica.SyntheticaBlueMoonLookAndFeel"),
        BLUE_STEEL("de.javasoft.plaf.synthetica.SyntheticaBlueSteelLookAndFeel"),
        GREEN_DREAM("de.javasoft.plaf.synthetica.SyntheticaGreenDreamLookAndFeel"),
        MAUVE_METALLIC("de.javasoft.plaf.synthetica.SyntheticaMauveMetallicLookAndFeel"),
        ORANGE_METALLIC("de.javasoft.plaf.synthetica.SyntheticaOrangeMetallicLookAndFeel"),
        SILVER_MOON("de.javasoft.plaf.synthetica.SyntheticaSilverMoonLookAndFeel"),
        SIMPLE_2D("de.javasoft.plaf.synthetica.SyntheticaSimple2DLookAndFeel"),
        SKY_METALLIC("de.javasoft.plaf.synthetica.SyntheticaSkyMetallicLookAndFeel"),
        STANDARD("de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel"),
        WHITE_VISION("de.javasoft.plaf.synthetica.SyntheticaWhiteVisionLookAndFeel"),

        DEFAULT(JDDefaultLookAndFeel.class.getName());
        private String clazz;

        public String getClazz() {
            return clazz;
        }

        private LookAndFeelType(String clazz) {

            this.clazz = clazz;
        }

        public boolean isAvailable() {
            try {
                Class<?> c = Class.forName(clazz);
                return c != null;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

}
