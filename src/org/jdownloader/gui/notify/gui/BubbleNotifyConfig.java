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
        TOP_RIGHT
    }

    @AboutConfig
    @DefaultEnumValue("TOP_RIGHT")
    @DescriptionForConfigEntry("Position Anchor for the First Bubble. TOP_RIGHT means, that the topright corner of the bubble is the reference anchor")
    public Anchor getAnchor();

    @AboutConfig
    @DefaultIntValue(-1)
    @DescriptionForConfigEntry("X Position of the first bubble. 0 is left screen edge -1 is right screen edge")
    public int getAnchorX();

    @AboutConfig
    @DefaultIntValue(0)
    @DescriptionForConfigEntry("Y Position of the first bubble. 0 is top screen edge -1 is bottom screen edge")
    public int getAnchorY();

    @AboutConfig
    @DefaultEnumValue("BOTTOM_RIGHT")
    @DescriptionForConfigEntry("Animation End Anchor. TOP_RIGHT means, that the topright corner of the bubble is the reference anchor")
    public Anchor getEndAnchor();

    @AboutConfig
    @DefaultIntValue(-1)
    @DescriptionForConfigEntry("Animation End X Position. 0 is left screen edge -1 is right screen edge")
    public int getEndX();

    @AboutConfig
    @DefaultIntValue(0)
    @DescriptionForConfigEntry("Animation End Y Position. 0 is top screen edge -1 is bottom screen edge")
    public int getEndY();

    @DefaultStringValue("\\display9999")
    @AboutConfig
    @DescriptionForConfigEntry("The bubbles iuse the current active screen. That means the screen that is used by the Main Window. You can set a hardcoded screen ID here. Like \\display0 for your main screen")
    public String getScreenID();

    @AboutConfig
    @DefaultEnumValue("TOP_RIGHT")
    @DescriptionForConfigEntry("Animation Start Anchor. TOP_RIGHT means, that the topright corner of the bubble is the reference anchor")
    public Anchor getStartAnchor();

    @AboutConfig
    @DefaultIntValue(-1)
    @DescriptionForConfigEntry("Animation Start X Position. 0 is left screen edge -1 is right screen edge")
    public int getStartX();

    @AboutConfig
    @DefaultIntValue(-1)
    @DescriptionForConfigEntry("Animation Start Y Position. 0 is top screen edge -1 is bottom screen edge")
    public int getStartY();

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

    public void setAnchor(Anchor a);

    public void setAnchorX(int i);

    public void setAnchorY(int i);

    public void setBubbleNotifyOnNewLinkgrabberLinksEnabled(boolean b);

    public void setBubbleNotifyOnNewLinkgrabberPackageEnabled(boolean b);

    public void setBubbleNotifyOnReconnectEndEnabled(boolean b);

    public void setBubbleNotifyOnReconnectStartEnabled(boolean b);

    public void setBubbleNotifyOnUpdateAvailableEnabled(boolean b);

    public void setEndAnchor(Anchor a);

    public void setEndX(int i);

    public void setEndY(int i);

    public void setScreenID(String string);

    public void setStartAnchor(Anchor a);

    public void setStartX(int i);

    public void setStartY(int i);
}
