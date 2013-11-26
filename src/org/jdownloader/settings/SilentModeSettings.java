package org.jdownloader.settings;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultLongValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.EnumLabel;

public interface SilentModeSettings extends ConfigInterface {
    public static enum AutoSilentModeTrigger {
        @EnumLabel("Auto Mode Disabled")
        NEVER,
        @EnumLabel("Auto-Enable Silentmode when JD is in Tray")
        JD_IN_TRAY,
        @EnumLabel("Auto-Enable Silentmode when JD is minimized to Taskbar")
        JD_IN_TASKBAR
    }

    void setAutoTrigger(AutoSilentModeTrigger action);

    @DefaultEnumValue("NEVER")
    @DescriptionForConfigEntry("Activate Silent Mode Based on Frame Status")
    @AboutConfig
    AutoSilentModeTrigger getAutoTrigger();

    public static enum DialogDuringSilentModeAction {
        @EnumLabel("Wait in Background until window gets focus")
        WAIT_IN_BACKGROUND_UNTIL_WINDOW_GETS_FOCUS,
        @EnumLabel("Wait in Background until window gets focus or timeout is reached")
        WAIT_IN_BACKGROUND_UNTIL_WINDOW_GETS_FOCUS_OR_TIMEOUT,
        @EnumLabel("Cancel the dialog")
        CANCEL_DIALOG;

    }

    void setOnDialogDuringSilentModeAction(DialogDuringSilentModeAction action);

    @DefaultEnumValue("WAIT_IN_BACKGROUND_UNTIL_WINDOW_GETS_FOCUS_OR_TIMEOUT")
    @AboutConfig
    DialogDuringSilentModeAction getOnDialogDuringSilentModeAction();

    void setAutoResetOnStartupEnabled(boolean b);

    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Auto Disable the Silent mode on each startup")
    @AboutConfig
    boolean isAutoResetOnStartupEnabled();

    void setOnDialogDuringSilentModeActionTimeout(long b);

    @DefaultLongValue(30 * 1000l)
    @AboutConfig
    long isOnDialogDuringSilentModeActionTimeout();

    public static enum CaptchaDuringSilentModeAction {
        @EnumLabel("Skip the Captcha")
        SKIP_LINK,
        @EnumLabel("Wait in Background until window gets focus or timeout is reached")
        WAIT_IN_BACKGROUND_UNTIL_WINDOW_GETS_FOCUS_OR_TIMEOUT,
        @EnumLabel("Do not show a dialog, but try auto solver (AntiCaptcha, Exchange Services,...)")
        DISABLE_DIALOG_SOLVER;

    }

    void setOnCaptchaDuringSilentModeAction(CaptchaDuringSilentModeAction action);

    @DefaultEnumValue("DISABLE_DIALOG_SOLVER")
    @AboutConfig
    CaptchaDuringSilentModeAction getOnCaptchaDuringSilentModeAction();

    void setManualEnabled(boolean b);

    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Activate the Silent Mode")
    @AboutConfig
    boolean isManualEnabled();

    @DescriptionForConfigEntry("[ms] After the given time without user interaction, JDownloader will autoactivate the silent mode, and disable it if there is user interaction again.")
    @DefaultLongValue(0)
    @AboutConfig
    long getAutoSilentModeInIdleState();

    void setAutoSilentModeInIdleState(long ms);

}
