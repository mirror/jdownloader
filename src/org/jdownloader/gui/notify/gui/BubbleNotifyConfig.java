package org.jdownloader.gui.notify.gui;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultLongValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.EnumLabel;
import org.appwork.storage.config.annotations.SpinnerValidator;

public interface BubbleNotifyConfig extends ConfigInterface {

    public static enum Anchor {
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        TOP_LEFT,
        TOP_RIGHT,
        SYSTEM_DEFAULT
    }

    @AboutConfig
    @DefaultEnumValue("SYSTEM_DEFAULT")
    @DescriptionForConfigEntry("Position Anchor for the First Bubble. TOP_RIGHT means, that the topright corner of the bubble is the reference anchor")
    public Anchor getFinalPositionAnchor();

    @AboutConfig
    @DefaultIntValue(-1)
    @DescriptionForConfigEntry("X Position of the first bubble. 0 is left screen edge -1 is right screen edge")
    public int getFinalPositionX();

    @AboutConfig
    @DefaultIntValue(-1)
    @DescriptionForConfigEntry("Y Position of the first bubble. 0 is top screen edge -1 is bottom screen edge")
    public int getFinalPositionY();

    @AboutConfig
    @DefaultEnumValue("SYSTEM_DEFAULT")
    @DescriptionForConfigEntry("Animation End Anchor. TOP_RIGHT means, that the topright corner of the bubble is the reference anchor")
    public Anchor getAnimationEndPositionAnchor();

    @AboutConfig
    @DefaultIntValue(-1)
    @DescriptionForConfigEntry("Animation End X Position. 0 is left screen edge -1 is right screen edge")
    public int getAnimationEndPositionX();

    @AboutConfig
    @DefaultIntValue(-1)
    @DescriptionForConfigEntry("Animation End Y Position. 0 is top screen edge -1 is bottom screen edge")
    public int getAnimationEndPositionY();

    @DefaultStringValue("\\display9999")
    @AboutConfig
    @DescriptionForConfigEntry("The bubbles iuse the current active screen. That means the screen that is used by the Main Window. You can set a hardcoded screen ID here. Like \\display0 for your main screen")
    public String getScreenID();

    @AboutConfig
    @DefaultEnumValue("SYSTEM_DEFAULT")
    @DescriptionForConfigEntry("Animation Start Anchor. TOP_RIGHT means, that the topright corner of the bubble is the reference anchor")
    public Anchor getAnimationStartPositionAnchor();

    @AboutConfig
    @DefaultIntValue(-1)
    @DescriptionForConfigEntry("Animation Start X Position. 0 is left screen edge -1 is right screen edge")
    public int getAnimationStartPositionX();

    @AboutConfig
    @DefaultIntValue(-1)
    @DescriptionForConfigEntry("Animation Start Y Position. 0 is top screen edge -1 is bottom screen edge")
    public int getAnimationStartPositionY();

    @DefaultBooleanValue(true)
    @AboutConfig
    public boolean isBubbleNotifyOnNewLinkgrabberLinksEnabled();

    @DefaultBooleanValue(true)
    @AboutConfig
    public boolean isBubbleNotifyOnReconnectStartEnabled();

    @DefaultBooleanValue(true)
    @AboutConfig
    public boolean isBubbleNotifyOnUpdateAvailableEnabled();

    public void setFinalPositionAnchor(Anchor a);

    public void setFinalPositionX(int i);

    public void setFinalPositionY(int i);

    public void setBubbleNotifyOnNewLinkgrabberLinksEnabled(boolean b);

    public void setBubbleNotifyOnReconnectStartEnabled(boolean b);

    public void setBubbleNotifyOnUpdateAvailableEnabled(boolean b);

    public void setAnimationEndPositionAnchor(Anchor a);

    public void setAnimationEndPositionX(int i);

    public void setAnimationEndPositionY(int i);

    public void setScreenID(String string);

    public void setAnimationStartPositionAnchor(Anchor a);

    public void setAnimationStartPositionX(int i);

    public void setAnimationStartPositionY(int i);

    @AboutConfig()
    @DefaultBooleanValue(false)
    public boolean isBubbleNotifyEnabledDuringSilentMode();

    public void setBubbleNotifyEnabledDuringSilentMode(boolean b);

    public static enum BubbleNotifyEnabledState {
        @EnumLabel("Always")
        ALWAYS,
        @EnumLabel("Never")
        NEVER,
        @EnumLabel("Only if JDownloader is not the active window")
        JD_NOT_ACTIVE,
        @EnumLabel("Only if JDownloader is minimized to tray or taskbar")
        TRAY_OR_TASKBAR,
        @EnumLabel("Only if JDownloader is minimized to tray")
        TRAY,
        @EnumLabel("Only if JDownloader is minimized to taskbar")
        TASKBAR

    }

    @AboutConfig()
    @DefaultEnumValue("ALWAYS")
    public BubbleNotifyEnabledState getBubbleNotifyEnabledState();

    public void setBubbleNotifyEnabledState(BubbleNotifyEnabledState b);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isDownloadStartedBubbleContentSaveToVisible();

    public void setDownloadStartedBubbleContentSaveToVisible(boolean b);

    @AboutConfig()
    @DefaultBooleanValue(false)
    public boolean isBubbleNotifyStartStopDownloadsEnabled();

    public void setBubbleNotifyStartStopDownloadsEnabled(boolean b);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isBubbleNotifyStartPauseStopEnabled();

    public void setBubbleNotifyStartPauseStopEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(15000)
    public int getDefaultTimeout();

    public void setDefaultTimeout(int ms);

    @AboutConfig
    @DefaultIntValue(900)
    public int getFadeAnimationDuration();

    public void setFadeAnimationDuration(int ms);

    @AboutConfig
    @DefaultLongValue(10000)
    public long getDownloadStartEndNotifyDelay();

    public void setDownloadStartEndNotifyDelay(long ms);

    @AboutConfig
    @DefaultIntValue(100)
    @DescriptionForConfigEntry("Transparency of the Bubbles. 0 = invisible 100= no Transparency")
    @SpinnerValidator(min = 1, max = 100, step = 1)
    public int getTransparency();

    public void setTransparency(int percent);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isBubbleNotifyOnCaptchaInBackgroundEnabled();

    public void setBubbleNotifyOnCaptchaInBackgroundEnabled(boolean b);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isDownloadStartedBubbleContentStatusVisible();

    public void setDownloadStartedBubbleContentStatusVisible(boolean b);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isDownloadStartedBubbleContentProxyVisible();

    public void setDownloadStartedBubbleContentProxyVisible(boolean b);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isDownloadStartedBubbleContentAccountVisible();

    public void setDownloadStartedBubbleContentAccountVisible(boolean b);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isDownloadStartedBubbleContentHosterVisible();

    public void setDownloadStartedBubbleContentHosterVisible(boolean b);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isCrawlerBubbleContentDurationVisible();

    public void setCrawlerBubbleContentDurationVisible(boolean b);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isCrawlerBubbleContentLinkCountVisible();

    public void setCrawlerBubbleContentLinkCountVisible(boolean b);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isCrawlerBubbleContentPackageCountVisible();

    public void setCrawlerBubbleContentPackageCountVisible(boolean b);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isCrawlerBubbleContentOfflineCountVisible();

    public void setCrawlerBubbleContentOfflineCountVisible(boolean b);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isCrawlerBubbleContentOnlineCountVisible();

    public void setCrawlerBubbleContentOnlineCountVisible(boolean b);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isCrawlerBubbleContentStatusVisible();

    public void setCrawlerBubbleContentStatusVisible(boolean b);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isCrawlerBubbleContentAnimatedIconVisible();

    public void setCrawlerBubbleContentAnimatedIconVisible(boolean b);

    @AboutConfig()
    @DefaultBooleanValue(true)
    public boolean isDownloadStartedBubbleContentFilenameVisible();

    public void setDownloadStartedBubbleContentFilenameVisible(boolean b);

}
