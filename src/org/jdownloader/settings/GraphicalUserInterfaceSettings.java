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
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.storage.config.annotations.ValidatorFactory;
import org.appwork.utils.Application;
import org.appwork.utils.swing.dialog.View;
import org.jdownloader.gui.views.components.SearchCategory;

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

    String getActiveConfigPanel();

    @AboutConfig
    @Description("Captcha Dialog Image scale Faktor in %")
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
    @Description("Refreshrate in ms for the DownloadView")
    @DefaultLongValue(500)
    @SpinnerValidator(min = 50, max = 5000, step = 25)
    @RequiresRestart
    public long getDownloadViewRefresh();

    @AboutConfig
    @Description("Font to be used. Default value is default. For foreign chars use e.g. Dialog")
    @DefaultStringValue("default")
    @RequiresRestart
    String getFontName();

    @AboutConfig
    @Description("Font scale factor in percent. Default value is 100 which means no font scaling.")
    @DefaultIntValue(100)
    @RequiresRestart
    int getFontScaleFactor();

    FrameStatus getLastFrameStatus();

    @DefaultEnumValue("SKIP_FILE")
    IfFileExistsAction getLastIfFileExists();

    @AboutConfig
    String getLookAndFeel();

    @AboutConfig
    public String getPassword();

    SearchCategory getSelectedDownloadSearchCategory();

    @DefaultStringValue("standard")
    @AboutConfig
    @Description("Icon Theme ID. Make sure that ./themes/<ID>/ exists")
    @ValidatorFactory(ThemeValidator.class)
    String getThemeID();

    @AboutConfig
    @Description("Disable animation and all animation threads. Optional value. Default value is true.")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isAnimationEnabled();

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
    @Description("Enable/disable Enable/disable Clipboard monitoring")
    @DefaultBooleanValue(true)
    boolean isClipboardMonitored();

    @AboutConfig
    @DefaultBooleanValue(true)
    @Description("If disabled, The Hostercolumn will show gray disabled icons if the link is disabled")
    boolean isColoredIconsForDisabledHosterColumnEnabled();

    @DefaultBooleanValue(false)
    boolean isConfigViewVisible();

    @AboutConfig
    @Description("Highlight Table in Downloadview if table is filtered")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isFilterHighlightEnabled();

    @AboutConfig
    @Description("Enable/disable support for system DPI settings. Default value is true.")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isFontRespectsSystemDPI();

    @AboutConfig
    @DefaultBooleanValue(true)
    @Description("If enabled, The User Interface will switch to Linkgrabber Tab if a new job has been added")
    boolean isLinkgrabberAutoTabSwitchEnabled();

    @AboutConfig
    @DefaultBooleanValue(false)
    @Description("If enabled, JDownloader GUI will come to top when new links are added")
    boolean isLinkgrabberFrameToTopOnNewLinksEnabled();

    @AboutConfig
    @Description("Enable/Disable the Linkgrabber Sidebar")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isLinkgrabberSidebarEnabled();

    @AboutConfig
    @Description("Enable/Disable the Linkgrabber Sidebar QuicktoggleButton")
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
    @Description("If true, ETAColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.")
    boolean isPremiumAlertETAColumnEnabled();

    @AboutConfig
    @Description("If true, SpeedColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.")
    boolean isPremiumAlertSpeedColumnEnabled();

    @AboutConfig
    @Description("If true, TaskColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.")
    boolean isPremiumAlertTaskColumnEnabled();

    @AboutConfig
    @Description("Set to true of you want jd to remember the latest selected download view")
    @DefaultBooleanValue(false)
    boolean isSaveDownloadViewCrossSessionEnabled();

    @AboutConfig
    @Description("True if move button should be visible in downloadview")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isShowMoveDownButton();

    @Description("True if move button should be visible in downloadview")
    @AboutConfig
    @RequiresRestart
    @DefaultBooleanValue(false)
    boolean isShowMoveToBottomButton();

    @Description("True if move button should be visible in downloadview")
    @RequiresRestart
    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isShowMoveToTopButton();

    @Description("True if move button should be visible in downloadview")
    @AboutConfig
    @RequiresRestart
    @DefaultBooleanValue(false)
    boolean isShowMoveUpButton();

    @AboutConfig
    @Description("Highlight Column in Downloadview if table is not in downloadsortorder")
    @DefaultBooleanValue(true)
    @RequiresRestart
    boolean isSortColumnHighlightEnabled();

    @AboutConfig
    @Description("Paint all labels/text with or without antialias. Default value is false.")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isTextAntiAliasEnabled();

    @AboutConfig
    @Description("If false, Most of the Tooltips will be disabled")
    @DefaultBooleanValue(true)
    boolean isTooltipEnabled();

    @AboutConfig
    @Description("If true, hostcolumn will also show full hostname")
    @DefaultBooleanValue(false)
    boolean isShowFullHostname();

    @AboutConfig
    @Description("If true, java will try to use D3D for graphics")
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

    @AboutConfig
    @Description("Enable/disable window opacity on Java 6u10 and above. A value of 'false' disables window opacity which means that the window corner background which is visible for non-rectangular windows disappear. Furthermore the shadow for popupMenus makes use of real translucent window. Some themes like SyntheticaSimple2D support translucent titlePanes if opacity is disabled. The property is ignored on JRE's below 6u10. Note: It is recommended to activate this feature only if your graphics hardware acceleration is supported by the JVM - a value of 'false' can affect application performance. Default value is false which means the translucency feature is enabled")
    @DefaultBooleanValue(false)
    @RequiresRestart
    boolean isWindowOpaque();

    void setActiveConfigPanel(String name);

    void setAnimationEnabled(boolean b);

    void setBalloonNotificationEnabled(boolean b);

    void setCaptchaScaleFactor(int b);

    void setClipboardMonitored(boolean b);

    void setColoredIconsForDisabledHosterColumnEnabled(boolean b);

    void setConfigViewVisible(boolean b);

    void setDialogDefaultTimeout(int value);

    void setDownloadView(org.jdownloader.gui.views.downloads.View view);

    public void setDownloadViewRefresh(long t);

    void setFilterHighlightEnabled(boolean b);

    void setFontName(String name);

    void setFontRespectsSystemDPI(boolean b);

    void setFontScaleFactor(int b);

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
    void setSelectedDownloadSearchCategory(SearchCategory selectedCategory);

    void setShowMoveDownButton(boolean b);

    void setShowMoveToBottomButton(boolean b);

    void setShowMoveToTopButton(boolean b);

    void setShowMoveUpButton(boolean b);

    void setSortColumnHighlightEnabled(boolean b);

    void setTextAntiAliasEnabled(boolean b);

    void setThemeID(String themeID);

    void setTooltipEnabled(boolean b);

    void setWindowOpaque(boolean b);

    void setLanguage(String lng);

    @AboutConfig
    String getLanguage();

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
    @Description("Hide the package in case it only contains one child")
    boolean isHideSingleChildPackages();

    void setHideSingleChildPackages(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart
    boolean isFileCountInSizeColumnVisible();

    void setFileCountInSizeColumnVisible(boolean b);
}
