package org.jdownloader.settings;

import java.awt.event.KeyEvent;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AbstractValidator;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntArrayValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultLongValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.LookUpKeys;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.annotations.ValidatorFactory;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.View;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.gui.laf.jddefault.JDDefaultLookAndFeel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.LinktablesSearchCategory;
import org.jdownloader.settings.advanced.ActionClass;

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

    /**
     * How many ms the speedmeter shall show/record. Please note that big Timeframes and high fps values may cause high CPU usage
     * 
     * @return
     */
    @AboutConfig
    @DefaultIntValue(30000)
    int getSpeedMeterTimeFrame();

    void setSpeedMeterTimeFrame(int i);

    /**
     * How many refreshes and datasamples the speedmeter uses. Please note that big Timeframes and high fps values may cause high CPU usage
     * 
     * @return
     */
    @AboutConfig
    @DefaultIntValue(4)
    int getSpeedMeterFramesPerSecond();

    void setSpeedMeterFramesPerSecond(int i);

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
    @RequiresRestart("A JDownloader Restart is Required")
    public long getDownloadViewRefresh();

    FrameStatus getLastFrameStatus();

    @DefaultEnumValue("SKIP_FILE")
    IfFileExistsAction getLastIfFileExists();

    @AboutConfig
    public String getPassword();

    LinktablesSearchCategory getSelectedDownloadSearchCategory();

    LinktablesSearchCategory getSelectedLinkgrabberSearchCategory();

    @DefaultStringValue("standard")
    @AboutConfig
    @DescriptionForConfigEntry("Icon Theme ID. Make sure that ./themes/<ID>/ exists")
    @ValidatorFactory(ThemeValidator.class)
    String getThemeID();

    // @AboutConfig
    // @Description("Enable/Disable the Linkgrabber Sidebar")
    // @DefaultBooleanValue(true)
    // @RequiresRestart("A JDownloader Restart is Required")
    // boolean isDownloadViewSidebarEnabled();
    //
    // @AboutConfig
    // @Description("Enable/Disable the DownloadView Sidebar QuicktoggleButton")
    // @DefaultBooleanValue(true)
    // @RequiresRestart("A JDownloader Restart is Required")
    // boolean isDownloadViewSidebarToggleButtonEnabled();
    //
    // @DefaultBooleanValue(true)
    // @RequiresRestart("A JDownloader Restart is Required")
    // boolean isDownloadViewSidebarVisible();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isBalloonNotificationEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("Enable/disable Clipboard monitoring")
    @DefaultBooleanValue(true)
    boolean isClipboardMonitored();

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isSkipClipboardMonitorFirstRound();

    void setSkipClipboardMonitorFirstRound(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Enable/disable HTML-Flavor(Browser selection) Clipboard monitoring")
    @RequiresRestart("A JDownloader Restart is Required")
    boolean isClipboardMonitorProcessHTMLFlavor();

    void setClipboardMonitorProcessHTMLFlavor(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("If disabled, The Hostercolumn will show gray disabled icons if the link is disabled")
    boolean isColoredIconsForDisabledHosterColumnEnabled();

    @DefaultBooleanValue(false)
    boolean isConfigViewVisible();

    @AboutConfig
    @DescriptionForConfigEntry("Highlight Table in Downloadview if table is filtered")
    @DefaultBooleanValue(true)
    @RequiresRestart("A JDownloader Restart is Required")
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
    @DescriptionForConfigEntry("Enable/Disable the DownloadPanel Overview panel ")
    @DefaultBooleanValue(true)
    @RequiresRestart("A JDownloader Restart is Required")
    boolean isDownloadTabOverviewVisible();

    void setDownloadTabOverviewVisible(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Enable/Disable the Linkgrabber Overview panel ")
    @DefaultBooleanValue(true)
    @RequiresRestart("A JDownloader Restart is Required")
    boolean isLinkgrabberTabOverviewVisible();

    void setLinkgrabberTabOverviewVisible(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Enable/Disable the Linkgrabber properties panel ")
    @DefaultBooleanValue(true)
    boolean isLinkgrabberTabPropertiesPanelVisible();

    void setLinkgrabberTabPropertiesPanelVisible(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Enable/Disable the Downloads properties panel ")
    @DefaultBooleanValue(true)
    boolean isDownloadsTabPropertiesPanelVisible();

    void setDownloadsTabPropertiesPanelVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelTotalInfoVisible();

    void setOverviewPanelTotalInfoVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelVisibleOnlyInfoVisible();

    void setOverviewPanelVisibleOnlyInfoVisible(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Include disabled links in the size calculation")
    @DefaultBooleanValue(true)
    boolean isOverviewPanelLinkgrabberIncludeDisabledLinks();

    void setOverviewPanelLinkgrabberIncludeDisabledLinks(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Include disabled links in the totalbytes and loadedbytes calculation")
    boolean isOverviewPanelDownloadPanelIncludeDisabledLinks();

    void setOverviewPanelDownloadPanelIncludeDisabledLinks(boolean b);

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

    @DefaultBooleanValue(true)
    @RequiresRestart("A JDownloader Restart is Required")
    boolean isLinkgrabberSidebarVisible();

    @DefaultBooleanValue(false)
    boolean isLogViewVisible();

    @DefaultBooleanValue(false)
    @AboutConfig
    public boolean isPasswordProtectionEnabled();

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("If true, ETAColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.")
    boolean isPremiumAlertETAColumnEnabled();

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("If true, SpeedColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.")
    boolean isPremiumAlertSpeedColumnEnabled();

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("If true, TaskColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.")
    boolean isPremiumAlertTaskColumnEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("Set to true of you want jd to remember the latest selected download view")
    @DefaultBooleanValue(false)
    boolean isSaveDownloadViewCrossSessionEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("Highlight Column in Downloadview if table is not in downloadsortorder")
    @DefaultBooleanValue(true)
    @RequiresRestart("A JDownloader Restart is Required")
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
    @RequiresRestart("A JDownloader Restart is Required")
    boolean isUseD3D();

    public void setUseD3D(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isSpeedMeterVisible();

    public void setSpeedMeterVisible(boolean b);

    public void setShowFullHostname(boolean b);

    // void setDownloadViewSidebarEnabled(boolean b);
    //
    // void setDownloadViewSidebarToggleButtonEnabled(boolean b);
    //
    // void setDownloadViewSidebarVisible(boolean b);

    void setActiveConfigPanel(String name);

    void setActivePluginConfigPanel(String name);

    // void setActiveCESConfigPanel(String name);

    // String getActiveCESConfigPanel();

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

    @DefaultEnumValue("FILENAME")
    void setSelectedLinkgrabberSearchCategory(LinktablesSearchCategory selectedCategory);

    void setSortColumnHighlightEnabled(boolean b);

    void setThemeID(String themeID);

    void setTooltipEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(2000)
    @SpinnerValidator(min = 50, max = 5000, step = 50)
    @RequiresRestart("A JDownloader Restart is Required")
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
    @RequiresRestart("A JDownloader Restart is Required")
    boolean isFileCountInSizeColumnVisible();

    void setFileCountInSizeColumnVisible(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Paint all labels/text with or without antialias. Default value is false.")
    @DefaultBooleanValue(false)
    @RequiresRestart("A JDownloader Restart is Required")
    boolean isSpeedmeterAntiAliasingEnabled();

    void setSpeedmeterAntiAliasingEnabled(boolean b);

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

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isTaskBarFlashEnabled();

    void setTaskBarFlashEnabled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isCaptchaDebugModeEnabled();

    void setCaptchaDebugModeEnabled(boolean b);

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

    @RequiresRestart("A JDownloader Restart is Required")
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Every  odd row get's a light shadow if enabled")
    @AboutConfig
    boolean isTableAlternateRowHighlightEnabled();

    void setTableAlternateRowHighlightEnabled(boolean b);

    @RequiresRestart("A JDownloader Restart is Required")
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("The row that is 'touched' by the mouse cursor gets a darker shadow")
    @AboutConfig
    boolean isTableMouseOverHighlightEnabled();

    void setTableMouseOverHighlightEnabled(boolean b);

    @RequiresRestart("A JDownloader Restart is Required")
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Packages get a different background color if enabled")
    @AboutConfig
    boolean isPackagesBackgroundHighlightEnabled();

    void setPackagesBackgroundHighlightEnabled(boolean b);

    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
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

    @AboutConfig
    @DefaultEnumValue("TO_FRONT")
    FrameState getNewDialogFrameState();

    void setNewDialogFrameState(FrameState state);

    public static enum NewLinksInLinkgrabberAction {
        NOTHING,
        TO_FRONT,
        FOCUS
    }

    @AboutConfig
    @DefaultEnumValue("NOTHING")
    NewLinksInLinkgrabberAction getNewLinksAction();

    void setNewLinksAction(NewLinksInLinkgrabberAction action);

    @AboutConfig
    @DescriptionForConfigEntry("JDownloader uses a workaround to bring it's window or dialogs to focused to front. It simulates an ALT key shortcut. \r\nIf disabled, you will get focus problems")
    @DefaultBooleanValue(true)
    boolean isWindowsWindowManagerAltKeyWorkaroundEnabled();

    void setWindowsWindowManagerAltKeyWorkaroundEnabled(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("JDownloader uses a workaround to bring it's window or dialogs to focused to front. It simulates an ALT key shortcut. \r\nIf disabled, you will get focus problems")
    @DefaultIntArrayValue({ KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_SHIFT })
    int[] getWindowsWindowManagerAltKeyCombi();

    void setWindowsWindowManagerAltKeyCombi(int[] keys);

    @AboutConfig
    @RequiresRestart("A Windows Reboot is Required")
    @DescriptionForConfigEntry("This value is read from the windows registry. if you set it, JDownloader will write it back to the registry.")
    void setWindowsWindowManagerForegroundLockTimeout(int readForegroundLockTimeout);

    int getWindowsWindowManagerForegroundLockTimeout();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isUpdateButtonFlashingEnabled();

    void setUpdateButtonFlashingEnabled(boolean b);

    public static enum ShowSpeedInWindowTitleTrigger {
        @EnumLabel("Never")
        NEVER,
        @EnumLabel("Only if JD is minimized to Taskbar")
        WHEN_WINDOW_IS_MINIMIZED,
        @EnumLabel("Always")
        ALWAYS
    }

    @AboutConfig
    @DefaultEnumValue("WHEN_WINDOW_IS_MINIMIZED")
    ShowSpeedInWindowTitleTrigger getSpeedInWindowTitle();

    void setSpeedInWindowTitle(ShowSpeedInWindowTitleTrigger b);

    public static enum PremiumStatusBarDisplay {
        @EnumLabel("Group by Account Type")
        GROUP_BY_ACCOUNT_TYPE,
        @EnumLabel("Group supported Hoster")
        GROUP_BY_SUPPORTED_HOSTS,
        @EnumLabel("Don't Group")
        DONT_GROUP

    }

    @AboutConfig
    @ActionClass(ImportAllMenusAdvancedAction.class)
    boolean getImportAllMenusAction();

    void setImportAllMenusAction(boolean type);

    @AboutConfig
    @ActionClass(ExportAllMenusAdvancedAction.class)
    boolean getExportAllMenusAction();

    void setExportAllMenusAction(boolean type);

    @AboutConfig
    @DefaultEnumValue("GROUP_BY_ACCOUNT_TYPE")
    PremiumStatusBarDisplay getPremiumStatusBarDisplay();

    void setPremiumStatusBarDisplay(PremiumStatusBarDisplay type);

    public static enum PackageDoubleClickAction {
        @EnumLabel("Expand or collapse Folder")
        EXPAND_COLLAPSE_TOGGLE,
        @EnumLabel("Open Downloadfolder")
        OPEN_FOLDER,
        @EnumLabel("Rename package")
        RENAME,
        @EnumLabel("Do nothing")
        NOTHING
    }

    @AboutConfig
    @DefaultEnumValue("OPEN_FOLDER")
    PackageDoubleClickAction getPackageDoubleClickAction();

    void setPackageDoubleClickAction(PackageDoubleClickAction action);

    public static enum LinkDoubleClickAction {

        @EnumLabel("Open Downloadfolder")
        OPEN_FOLDER,
        @EnumLabel("Open File")
        OPEN_FILE,
        @EnumLabel("Rename File")
        RENAME,
        @EnumLabel("Do nothing")
        NOTHING
    }

    @AboutConfig
    @DefaultEnumValue("OPEN_FILE")
    LinkDoubleClickAction getLinkDoubleClickAction();

    void setLinkDoubleClickAction(LinkDoubleClickAction action);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isLinkPropertiesPanelCommentVisible();

    void setLinkPropertiesPanelCommentVisible(boolean v);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isLinkPropertiesPanelFilenameVisible();

    void setLinkPropertiesPanelFilenameVisible(boolean v);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isLinkPropertiesPanelPackagenameVisible();

    void setLinkPropertiesPanelPackagenameVisible(boolean v);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isLinkPropertiesPanelDownloadPasswordVisible();

    void setLinkPropertiesPanelDownloadPasswordVisible(boolean v);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isLinkPropertiesPanelChecksumVisible();

    void setLinkPropertiesPanelChecksumVisible(boolean v);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isLinkPropertiesPanelArchivepasswordVisible();

    void setLinkPropertiesPanelArchivepasswordVisible(boolean v);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isLinkPropertiesPanelSaveToVisible();

    void setLinkPropertiesPanelSaveToVisible(boolean v);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isLinkPropertiesPanelDownloadFromVisible();

    void setLinkPropertiesPanelDownloadFromVisible(boolean v);

    //

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isDownloadsPropertiesPanelCommentVisible();

    void setDownloadsPropertiesPanelCommentVisible(boolean v);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isDownloadsPropertiesPanelFilenameVisible();

    void setDownloadsPropertiesPanelFilenameVisible(boolean v);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isDownloadsPropertiesPanelPackagenameVisible();

    void setDownloadsPropertiesPanelPackagenameVisible(boolean v);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isDownloadsPropertiesPanelDownloadPasswordVisible();

    void setDownloadsPropertiesPanelDownloadPasswordVisible(boolean v);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isDownloadsPropertiesPanelChecksumVisible();

    void setDownloadsPropertiesPanelChecksumVisible(boolean v);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isDownloadsPropertiesPanelArchivepasswordVisible();

    void setDownloadsPropertiesPanelArchivepasswordVisible(boolean v);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isDownloadsPropertiesPanelSaveToVisible();

    void setDownloadsPropertiesPanelSaveToVisible(boolean v);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isDownloadsPropertiesPanelDownloadFromVisible();

    void setDownloadsPropertiesPanelDownloadFromVisible(boolean v);

    public static enum RlyWarnLevel {
        @EnumLabel("HIGH! Show all 'Are you sure?' dialogs!")
        HIGH,

        @EnumLabel("NORMAL! Show the most important 'Are you sure?' dialogs!")
        NORMAL,
        @EnumLabel("LOW! Show only servere 'Are you sure?' dialogs!")
        /** Only severe ones */
        LOW,
        @EnumLabel("DISABLED! Hide all 'Are you sure?' dialogs! (As far as possible)")
        DISABLED;
    }

    @AboutConfig
    @DefaultEnumValue("NORMAL")
    public RlyWarnLevel getRlyWarnLevel();

    public void setRlyWarnLevel(RlyWarnLevel level);

    public static enum DeleteFileOptions implements LabelInterface {

        REMOVE_LINKS_ONLY() {
            @Override
            public String getLabel() {
                return org.jdownloader.gui.translate._GUI._.ConfirmDeleteLinksDialog_layoutDialogContent_no_filedelete2();
            }
        },

        REMOVE_LINKS_AND_RECYCLE_FILES() {
            @Override
            public String getLabel() {
                return org.jdownloader.gui.translate._GUI._.ConfirmDeleteLinksDialog_layoutDialogContent_Recycle_2();
            }
        },

        REMOVE_LINKS_AND_DELETE_FILES() {
            @Override
            public String getLabel() {
                return org.jdownloader.gui.translate._GUI._.ConfirmDeleteLinksDialog_layoutDialogContent_delete_2();
            }
        };

        @Override
        public String getLabel() {
            return null;
        }

    }

    @DescriptionForConfigEntry("Placeholders: |#TITLE|, | - #SPEED/s|, | - #UPDATENOTIFY|")
    @DefaultStringValue("|#TITLE|| - #SPEED/s|| - #UPDATENOTIFY|")
    @AboutConfig
    public String getTitlePattern();

    public void setTitlePattern(String pattern);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isBypassAllRlyDeleteDialogsEnabled();

    void setBypassAllRlyDeleteDialogsEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isHelpDialogsEnabled();

    void setHelpDialogsEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isHateCaptchasTextInCaptchaDialogVisible();

    void setHateCaptchasTextInCaptchaDialogVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isPropertiesPanelHeightNormalized();

    void setPropertiesPanelHeightNormalized(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOSRSurveyEnabled();

    void setOSRSurveyEnabled(boolean b);

    //
    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelDownloadPackageCountVisible();

    void setOverviewPanelDownloadPackageCountVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelLinkgrabberHosterCountVisible();

    void setOverviewPanelLinkgrabberHosterCountVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelDownloadLinkCountVisible();

    void setOverviewPanelDownloadLinkCountVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelDownloadTotalBytesVisible();

    void setOverviewPanelDownloadTotalBytesVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isOverviewPanelDownloadLinksFailedCountVisible();

    void setOverviewPanelDownloadLinksFailedCountVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isOverviewPanelDownloadLinksFinishedCountVisible();

    void setOverviewPanelDownloadLinksFinishedCountVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isOverviewPanelDownloadLinksSkippedCountVisible();

    void setOverviewPanelDownloadLinksSkippedCountVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelDownloadRunningDownloadsCountVisible();

    void setOverviewPanelDownloadRunningDownloadsCountVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelDownloadSpeedVisible();

    void setOverviewPanelDownloadSpeedVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelDownloadETAVisible();

    void setOverviewPanelDownloadETAVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelDownloadConnectionsVisible();

    void setOverviewPanelDownloadConnectionsVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelDownloadBytesLoadedVisible();

    void setOverviewPanelDownloadBytesLoadedVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelLinkgrabberPackageCountVisible();

    void setOverviewPanelLinkgrabberPackageCountVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelLinkgrabberLinksCountVisible();

    void setOverviewPanelLinkgrabberLinksCountVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelLinkgrabberTotalBytesVisible();

    void setOverviewPanelLinkgrabberTotalBytesVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelLinkgrabberStatusUnknownVisible();

    void setOverviewPanelLinkgrabberStatusUnknownVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelLinkgrabberStatusOnlineVisible();

    void setOverviewPanelLinkgrabberStatusOnlineVisible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isOverviewPanelLinkgrabberStatusOfflineVisible();

    void setOverviewPanelLinkgrabberStatusOfflineVisible(boolean b);

    public static enum ConfirmIncompleteArchiveAction implements LabelInterface {
        ASK() {

            @Override
            public String getLabel() {
                return _GUI._.GraphicalUserInterfaceSettings_getLabel_ask_();
            }

        },
        KEEP_IN_LINKGRABBER() {

            @Override
            public String getLabel() {
                return _GUI._.GraphicalUserInterfaceSettings_getLabel_do_not_move();
            }

        },
        MOVE_TO_DOWNLOADLIST() {
            @Override
            public String getLabel() {
                return _GUI._.GraphicalUserInterfaceSettings_getLabel_move_anyway();
            }

        },
        DELETE() {
            @Override
            public String getLabel() {
                return _GUI._.GraphicalUserInterfaceSettings_getLabel_delete();
            }

        }
    }

    @AboutConfig
    @DefaultEnumValue("ASK")
    ConfirmIncompleteArchiveAction getConfirmIncompleteArchiveAction();

    void setConfirmIncompleteArchiveAction(ConfirmIncompleteArchiveAction action);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("If Enabled, Variants will get a more detailed, technical name.")
    boolean isExtendedVariantNamesEnabled();

    void setExtendedVariantNamesEnabled(boolean b);
}
