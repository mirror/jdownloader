package org.jdownloader.settings;

import java.awt.event.KeyEvent;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;

import org.appwork.storage.Storable;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AbstractCustomValueGetter;
import org.appwork.storage.config.annotations.ConfigEntryKeywords;
import org.appwork.storage.config.annotations.CustomValueGetter;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntArrayValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultLongValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.View;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.LinktablesSearchCategory;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.translate._JDT;

public interface GraphicalUserInterfaceSettings extends ConfigInterface {
    public static enum DownloadFolderChooserDialogSubfolder {
        AUTO,
        ENABLED,
        DISABLED
    }

    @DefaultEnumValue("AUTO")
    @AboutConfig
    DownloadFolderChooserDialogSubfolder getDownloadFolderChooserDialogSubfolder();

    void setDownloadFolderChooserDialogSubfolder(DownloadFolderChooserDialogSubfolder value);

    public static class CustomIsConfigViewVisible extends AbstractCustomValueGetter<Boolean> {
        @Override
        public Boolean getValue(KeyHandler<Boolean> keyHandler, Boolean value) {
            if (CrossSystem.isMac() && Application.getJavaVersion() < Application.JAVA17) {
                return Boolean.TRUE;
            }
            return value;
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

    String getActiveMyJDownloaderPanel();

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

    public static enum CLIPBOARD_SKIP_MODE {
        ON_STARTUP,
        ON_ENABLE,
        NEVER
    }

    @AboutConfig
    @DefaultEnumValue("NEVER")
    CLIPBOARD_SKIP_MODE getClipboardSkipMode();

    void setClipboardSkipMode(CLIPBOARD_SKIP_MODE mode);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Enable/disable HTML-Flavor(Browser selection) Clipboard monitoring")
    @RequiresRestart("A JDownloader Restart is Required")
    boolean isClipboardMonitorProcessHTMLFlavor();

    void setClipboardMonitorProcessHTMLFlavor(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Blacklist(regex) of processes to ignore Clipboard monitoring")
    @RequiresRestart("A JDownloader Restart is Required")
    String[] getClipboardProcessBlacklist();

    void setClipboardProcessBlacklist(String[] blackList);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("If disabled, The Hostercolumn will show gray disabled icons if the link is disabled")
    boolean isColoredIconsForDisabledHosterColumnEnabled();

    @DefaultBooleanValue(false)
    @CustomValueGetter(CustomIsConfigViewVisible.class)
    boolean isConfigViewVisible();

    @DefaultBooleanValue(true)
    boolean isMyJDownloaderViewVisible();

    @AboutConfig
    @DescriptionForConfigEntry("Highlight Table in Downloadview if table is filtered")
    @DefaultBooleanValue(true)
    @RequiresRestart("A JDownloader Restart is Required")
    boolean isFilterHighlightEnabled();

    @AboutConfig
    @DescriptionForConfigEntry("Enable/Disable the DownloadPanel Overview panel ")
    @DefaultBooleanValue(true)
    boolean isDownloadTabOverviewVisible();

    void setDownloadTabOverviewVisible(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Enable/Disable the Linkgrabber Overview panel ")
    @DefaultBooleanValue(true)
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

    void setActiveMyJDownloaderPanel(String name);

    void setActivePluginConfigPanel(String name);

    // void setActiveCESConfigPanel(String name);
    // String getActiveCESConfigPanel();
    void setBalloonNotificationEnabled(boolean b);

    void setCaptchaScaleFactor(int b);

    void setClipboardMonitored(boolean b);

    void setColoredIconsForDisabledHosterColumnEnabled(boolean b);

    void setMyJDownloaderViewVisible(boolean b);

    void setConfigViewVisible(boolean b);

    void setDialogDefaultTimeoutInMS(int ms);

    void setDownloadView(org.jdownloader.gui.views.downloads.View view);

    public void setDownloadViewRefresh(long t);

    void setFilterHighlightEnabled(boolean b);

    public void setLastFrameStatus(FrameStatus status);

    void setLastIfFileExists(IfFileExistsAction value);

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

    void setPresentationModeEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Presentation mode peforms tasks like: account username obstruction throughout GUI")
    boolean isPresentationModeEnabled();

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

    public static enum SIZEUNIT {
        TiB(1024 * 1024 * 1024 * 1024l, true),
        TB(1000 * 1000 * 1000 * 1000l),
        GiB(1024 * 1024 * 1024l, true),
        GB(1000 * 1000 * 1000l),
        MiB(1024 * 1024l, true),
        MB(1000 * 1000l),
        KiB(1024, true),
        KB(1000l),
        B(1);
        private final long    divider;
        private final boolean iecPrefix;

        public final boolean isIECPrefix() {
            return iecPrefix;
        }

        public final long getDivider() {
            return divider;
        }

        private SIZEUNIT(long divider, boolean isIECPrefix) {
            this.divider = divider;
            this.iecPrefix = isIECPrefix;
        }

        private SIZEUNIT(long divider) {
            this(divider, false);
        }

        public static final String formatValue(SIZEUNIT maxSizeUnit, final long fileSize) {
            return formatValue(maxSizeUnit, new DecimalFormat("0.00"), fileSize);
        }

        public static final String formatValue(SIZEUNIT maxSizeUnit, final DecimalFormat formatter, final long fileSize) {
            final boolean isIECPrefix = maxSizeUnit.isIECPrefix();
            switch (maxSizeUnit) {
            case TiB:
                if (fileSize >= 1024 * 1024 * 1024 * 1024l) {
                    return formatter.format(fileSize / (1024 * 1024 * 1024 * 1024.0)).concat(" TiB");
                }
            case TB:
                if (!isIECPrefix && fileSize >= 1000 * 1000 * 1000 * 1000l) {
                    return formatter.format(fileSize / (1000 * 1000 * 1000 * 1000.0)).concat(" TB");
                }
            case GiB:
                if (isIECPrefix && fileSize >= 1024 * 1024 * 1024l) {
                    return formatter.format(fileSize / (1024 * 1024 * 1024.0)).concat(" GiB");
                }
            case GB:
                if (!isIECPrefix && fileSize >= 1000 * 1000 * 1000l) {
                    return formatter.format(fileSize / (1000 * 1000 * 1000.0)).concat(" GB");
                }
            case MiB:
                if (isIECPrefix && fileSize >= 1024 * 1024l) {
                    return formatter.format(fileSize / (1024 * 1024.0)).concat(" MiB");
                }
            case MB:
                if (!isIECPrefix && fileSize >= 1000 * 1000l) {
                    return formatter.format(fileSize / (1000 * 1000.0)).concat(" MB");
                }
            case KiB:
                if (isIECPrefix && fileSize >= 1024l) {
                    return formatter.format(fileSize / 1024.0).concat(" KiB");
                }
            case KB:
                if (!isIECPrefix && fileSize >= 1000l) {
                    return formatter.format(fileSize / 1000.0).concat(" KB");
                }
            default:
                if (fileSize == 0) {
                    return "0 B";
                }
                if (fileSize < 0) {
                    return "~";
                }
                return fileSize + " B";
            }
        }
    }

    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    SIZEUNIT getMaxSizeUnit();

    void setMaxSizeUnit(SIZEUNIT sizeUnit);

    public static enum MacDockProgressDisplay implements LabelInterface {
        TOTAL_PROGRESS() {
            @Override
            public String getLabel() {
                return _GUI.T.DockProgressDisplay_total_progress();
            }
        },
        NOTHING() {
            @Override
            public String getLabel() {
                return _GUI.T.DockProgressDisplay_nothing();
            }
        };
    }

    @DefaultEnumValue("TOTAL_PROGRESS")
    @AboutConfig
    MacDockProgressDisplay getMacDockProgressDisplay();

    void setMacDockProgressDisplay(MacDockProgressDisplay value);

    public static enum WindowsTaskBarProgressDisplay implements LabelInterface {
        TOTAL_PROGRESS() {
            @Override
            public String getLabel() {
                return _GUI.T.DockProgressDisplay_total_progress();
            }
        },
        NOTHING() {
            @Override
            public String getLabel() {
                return _GUI.T.DockProgressDisplay_nothing();
            }
        },
        TOTAL_PROGRESS_AND_CONNECTIONS() {
            @Override
            public String getLabel() {
                return _GUI.T.DockProgressDisplay_connections_and_progress();
            }
        };
    }

    @DefaultEnumValue("TOTAL_PROGRESS")
    @AboutConfig
    WindowsTaskBarProgressDisplay getWindowsTaskbarProgressDisplay();

    void setWindowsTaskbarProgressDisplay(WindowsTaskBarProgressDisplay value);

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
        ALU_OXIDE("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaAluOxideLookAndFeel"),
        BLACK_EYE("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel"),
        BLACK_MOON("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaBlackMoonLookAndFeel"),
        BLACK_STAR("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaBlackStarLookAndFeel"),
        BLUE_ICE("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaBlueIceLookAndFeel"),
        BLUE_LIGHT("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaBlueLightLookAndFeel"),
        BLUE_MOON("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaBlueMoonLookAndFeel"),
        BLUE_STEEL("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaBlueSteelLookAndFeel"),
        CLASSY("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaClassyLookAndFeel"),
        GREEN_DREAM("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaGreenDreamLookAndFeel"),
        MAUVE_METALLIC("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaMauveMetallicLookAndFeel"),
        ORANGE_METALLIC("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaOrangeMetallicLookAndFeel"),
        PLAIN("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaPlainLookAndFeel"),
        SILVER_MOON("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaSilverMoonLookAndFeel"),
        SIMPLE_2D(null, "de.javasoft.plaf.synthetica.SyntheticaSimple2DLookAndFeel"),
        SKY_METALLIC("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaSkyMetallicLookAndFeel"),
        STANDARD("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel"),
        WHITE_VISION("synthetica-themes", "de.javasoft.plaf.synthetica.SyntheticaWhiteVisionLookAndFeel"),
        JD_PLAIN("theme-plain", "org.jdownloader.gui.laf.plain.PlainLookAndFeel"),
        DEFAULT(null, "org.jdownloader.gui.laf.jddefault.JDDefaultLookAndFeel");
        private final String clazz;
        private final String extensionID;

        public final String getExtensionID() {
            return extensionID;
        }

        public final String getClazz() {
            return clazz;
        }

        private LookAndFeelType(String extensionID, String clazz) {
            this.clazz = clazz;
            this.extensionID = extensionID;
        }

        public boolean isAvailable() {
            try {
                // do not use Class.forName here since this would load the class
                final String path = "/" + getClazz().replace(".", "/") + ".class";
                final URL classPath = getClass().getResource(path);
                return classPath != null;
            } catch (Throwable e) {
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
        SWITCH,
        NOTHING,
        TO_FRONT,
        FOCUS
    }

    @AboutConfig
    @DefaultEnumValue("SWITCH")
    NewLinksInLinkgrabberAction getNewLinksActionV2();

    void setNewLinksActionV2(NewLinksInLinkgrabberAction action);

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

    @AboutConfig
    @DefaultIntValue(14)
    @DescriptionForConfigEntry("Days to keep disabled accounts displayed in PremiumBar and AccountToolTip overviews.")
    int getPremiumStatusBarDisabledAccountExpire();

    void setPremiumStatusBarDisabledAccountExpire(int i);

    public static enum PremiumStatusBarDisplay {
        @EnumLabel("Group by Account Type")
        GROUP_BY_ACCOUNT_TYPE,
        @EnumLabel("Group supported Hoster")
        GROUP_BY_SUPPORTED_HOSTS,
        @EnumLabel("Don't Group")
        DONT_GROUP,
        @EnumLabel("Group supported Accounts")
        GROUP_BY_SUPPORTED_ACCOUNTS
    }

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
        @EnumLabel("Open Link or Package Properties Panel")
        OPEN_PROPERTIES_PANEL,
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
        @EnumLabel("Open Link or Package Properties Panel")
        OPEN_PROPERTIES_PANEL,
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
        @EnumLabel("LOW! Show only severe 'Are you sure?' dialogs!")
        /** Only severe ones */
        LOW,
        @EnumLabel("DISABLED! Hide all 'Are you sure?' dialogs! (As far as possible)")
        DISABLED;
    }

    @AboutConfig
    @DescriptionForConfigEntry("Choose how many 'Are you sure?' warnings you want to see (Bug me not).")
    @DefaultEnumValue("NORMAL")
    public RlyWarnLevel getRlyWarnLevel();

    public void setRlyWarnLevel(RlyWarnLevel level);

    public static enum DeleteFileOptions implements LabelInterface {
        REMOVE_LINKS_ONLY() {
            @Override
            public String getLabel() {
                return org.jdownloader.gui.translate._GUI.T.ConfirmDeleteLinksDialog_layoutDialogContent_no_filedelete2();
            }
        },
        REMOVE_LINKS_AND_RECYCLE_FILES() {
            @Override
            public String getLabel() {
                return org.jdownloader.gui.translate._GUI.T.ConfirmDeleteLinksDialog_layoutDialogContent_Recycle_2();
            }
        },
        REMOVE_LINKS_AND_DELETE_FILES() {
            @Override
            public String getLabel() {
                return org.jdownloader.gui.translate._GUI.T.ConfirmDeleteLinksDialog_layoutDialogContent_delete_2();
            }
        };
        @Override
        public String getLabel() {
            return null;
        }
    }

    @DescriptionForConfigEntry("Placeholders: |#TITLE|, | - #SPEED/s|, | - #UPDATENOTIFY|, | - #AVGSPEED|, | - #RUNNING_DOWNLOADS|")
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

    // @AboutConfig
    // @DefaultBooleanValue(true)
    // boolean isOSRSurveyEnabled();
    //
    // void setOSRSurveyEnabled(boolean b);
    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isSpecialDealsEnabled();

    void setSpecialDealsEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isBannerEnabled();

    void setBannerEnabled(boolean b);

    long getBannerChangeTimestamp();

    void setBannerChangeTimestamp(long timestamp);

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
                return _GUI.T.GraphicalUserInterfaceSettings_getLabel_ask_();
            }
        },
        KEEP_IN_LINKGRABBER() {
            @Override
            public String getLabel() {
                return _GUI.T.GraphicalUserInterfaceSettings_getLabel_do_not_move();
            }
        },
        MOVE_TO_DOWNLOADLIST() {
            @Override
            public String getLabel() {
                return _GUI.T.GraphicalUserInterfaceSettings_getLabel_move_anyway();
            }
        },
        DELETE() {
            @Override
            public String getLabel() {
                return _GUI.T.GraphicalUserInterfaceSettings_getLabel_delete();
            }
        }
    }

    @AboutConfig
    @DefaultEnumValue("ASK")
    ConfirmIncompleteArchiveAction getConfirmIncompleteArchiveAction();

    void setConfirmIncompleteArchiveAction(ConfirmIncompleteArchiveAction action);

    @AboutConfig
    @RequiresRestart("Restart is Required")
    String getCustomLookAndFeelClass();

    void setCustomLookAndFeelClass(String clazz);

    @AboutConfig
    @RequiresRestart("Restart is Required")
    String getDateTimeFormatAccountManagerExpireDateColumn();

    void setDateTimeFormatAccountManagerExpireDateColumn(String df);

    @AboutConfig
    @RequiresRestart("Restart is Required")
    String getDateTimeFormatDownloadListAddedDateColumn();

    void setDateTimeFormatDownloadListAddedDateColumn(String df);

    @AboutConfig
    @RequiresRestart("Restart is Required")
    String getDateTimeFormatDownloadListFinishedDateColumn();

    void setDateTimeFormatDownloadListFinishedDateColumn(String df);

    @AboutConfig
    @RequiresRestart("Restart is Required")
    @DefaultBooleanValue(true)
    boolean isSpecialDealOboomDialogVisibleOnStartup();

    void setSpecialDealOboomDialogVisibleOnStartup(boolean b);

    @RequiresRestart("Restart is Required")
    @AboutConfig
    HashMap<String, Long> getPremiumExpireWarningMapV2();

    void setPremiumExpireWarningMapV2(HashMap<String, Long> value);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isPremiumExpireWarningEnabled();

    void setPremiumExpireWarningEnabled(boolean b);

    @AboutConfig
    @RequiresRestart("Restart is Required")
    @DefaultBooleanValue(true)
    boolean isStatusBarAddPremiumButtonVisible();

    void setStatusBarAddPremiumButtonVisible(boolean b);

    @AboutConfig
    @RequiresRestart("Restart is Required")
    @DefaultBooleanValue(true)
    boolean isTableWrapAroundEnabled();

    void setTableWrapAroundEnabled(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("The last used the Regex option for 'Rename Filename/Packagename' Dialog")
    @DefaultBooleanValue(false)
    boolean isRenameActionRegexEnabled();

    void setRenameActionRegexEnabled(boolean b);

    @DescriptionForConfigEntry("If enabled ctrl+A first of all selects all children in all current packages, and in a second step all packages")
    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isTwoStepCtrlASelectionEnabled();

    void setTwoStepCtrlASelectionEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isDownloadControlColumnAutoShowEnabled();

    void setDownloadControlColumnAutoShowEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isPriorityColumnAutoShowEnabled();

    void setPriorityColumnAutoShowEnabled(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Shortcut for the  Refresh Button in the captcha dialog")
    @DefaultStringValue("pressed F5")
    String getShortcutForCaptchaDialogRefresh();

    void setShortcutForCaptchaDialogRefresh(String b);

    @AboutConfig
    @RequiresRestart("Restart JDownloader if you changed this value")
    @DescriptionForConfigEntry("Interval of the Downloads Table Refresher. Default 1000ms (once per second). Decreasing this value will cost CPU Power")
    @DefaultLongValue(1000)
    long getDownloadsTableRefreshInterval();

    void setDownloadsTableRefreshInterval(long ms);

    public static class Position implements Storable {
        public Position(/* storable */) {
        }

        private int x = -1;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        private int y = -1;
    }

    @AboutConfig
    @RequiresRestart("Restart JDownloader if you changed this value")
    @DescriptionForConfigEntry("Customize the order of the Overview Panel Entries in x and y position")
    HashMap<String, Position> getOverviewPositions();

    void setOverviewPositions(HashMap<String, Position> map);

    public static enum DockingPosition implements LabelInterface {
        NORTH() {
            @Override
            public String getLabel() {
                return _GUI.T.setOverviewPositions_north();
            }
        },
        SOUTH() {
            public String getLabel() {
                return _GUI.T.setOverviewPositions_south();
            }
        }
    }

    @DefaultEnumValue("SOUTH")
    @AboutConfig
    DockingPosition getLinkgrabberBottombarPosition();

    void setLinkgrabberBottombarPosition(DockingPosition pos);

    public static enum DownloadFolderChooserDialogDefaultPath implements LabelInterface {
        CURRENT_PATH {
            @Override
            public String getLabel() {
                return _JDT.T.DownloadFolderChooserDialogDefaultPath_CURRENT_PATH();
            }
        },
        GLOBAL_DOWNLOAD_DIRECTORY {
            @Override
            public String getLabel() {
                return _JDT.T.DownloadFolderChooserDialogDefaultPath_GLOBAL_DOWNLOAD_DIRECTORY(CFG_GENERAL.DEFAULT_DOWNLOAD_FOLDER.getValue());
            }
        },
        LAST_USED_PATH {
            @Override
            public String getLabel() {
                return _JDT.T.DownloadFolderChooserDialogDefaultPath_LAST_USED_PATH();
            }
        }
    }

    @AboutConfig
    @DefaultEnumValue("CURRENT_PATH")
    DownloadFolderChooserDialogDefaultPath getDownloadFolderChooserDefaultPath();

    void setDownloadFolderChooserDefaultPath(DownloadFolderChooserDialogDefaultPath path);

    public static enum DonateButtonState implements LabelInterface {
        AUTO_HIDDEN {
            @Override
            public String getLabel() {
                return _JDT.T.DonateButtonState_AUTO_HIDDEN();
            }
        },
        AUTO_VISIBLE {
            @Override
            public String getLabel() {
                return _JDT.T.DonateButtonState_AUTO_VISIBLE();
            }
        },
        CUSTOM_VISIBLE {
            @Override
            public String getLabel() {
                return _JDT.T.DonateButtonState_CUSTOM_VISIBLE();
            }
        },
        CUSTOM_HIDDEN {
            @Override
            public String getLabel() {
                return _JDT.T.DonateButtonState_CUSTOM_HIDDEN();
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("AUTO_VISIBLE")
    @ConfigEntryKeywords({ "donat", "contribut" })
    DonateButtonState getDonateButtonState();

    void setDonateButtonState(DonateButtonState state);

    void setDonateButtonLatestAutoChange(long currentTimeMillis);

    long getDonateButtonLatestAutoChange();

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("If disabled, the captcha has no border painted, and the dialog looks like in jd09")
    boolean isCaptchaDialogBorderAroundImageEnabled();

    void setCaptchaDialogBorderAroundImageEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isAvailableColumnTextVisible();

    void setAvailableColumnTextVisible(boolean b);

    @AboutConfig
    String getDonationNotifyID();

    void setDonationNotifyID(String id);

    void setDownloadListScrollPosition(int[] is);

    int[] getDownloadListScrollPosition();

    void setLinkgrabberListScrollPosition(int[] is);

    int[] getLinkgrabberListScrollPosition();

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Set to false to invert the sort Order for the Download & Linkgrabber Tables.")
    boolean isPrimaryTableSorterDesc();

    void setPrimaryTableSorterDesc(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isPremiumDisabledWarningFlashEnabled();

    void setPremiumDisabledWarningFlashEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isClipboardDisabledWarningFlashEnabled();

    void setClipboardDisabledWarningFlashEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("If Enabled, JDownloader will try to be always on top of all other windows")
    boolean isMainWindowAlwaysOnTop();

    void setMainWindowAlwaysOnTop(boolean b);
}
