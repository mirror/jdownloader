package org.jdownloader.gui.notify.gui;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

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
    public boolean isBubbleNotifyOnNewLinkgrabberPackageEnabled();

    @DefaultBooleanValue(true)
    @AboutConfig
    public boolean isBubbleNotifyOnReconnectEndEnabled();

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

    public void setBubbleNotifyOnNewLinkgrabberPackageEnabled(boolean b);

    public void setBubbleNotifyOnReconnectEndEnabled(boolean b);

    public void setBubbleNotifyOnReconnectStartEnabled(boolean b);

    public void setBubbleNotifyOnUpdateAvailableEnabled(boolean b);

    public void setAnimationEndPositionAnchor(Anchor a);

    public void setAnimationEndPositionX(int i);

    public void setAnimationEndPositionY(int i);

    public void setScreenID(String string);

    public void setAnimationStartPositionAnchor(Anchor a);

    public void setAnimationStartPositionX(int i);

    public void setAnimationStartPositionY(int i);
}
