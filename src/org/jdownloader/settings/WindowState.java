package org.jdownloader.settings;

import org.appwork.storage.config.annotations.EnumLabel;

public enum WindowState {
    @EnumLabel("Never")
    NEVER,
    @EnumLabel("Mainwindow is in normal state (not iconified or tray)")
    MAINFRAME_IS_MAXIMIZED,
    @EnumLabel("Mainwindow is not in tray")
    MAINFRAME_IS_MAXIMIZED_OR_ICONIFIED,
    @EnumLabel("Always")
    MAINFRAME_IS_MAXIMIZED_OR_ICONIFIED_OR_TOTRAY
}
