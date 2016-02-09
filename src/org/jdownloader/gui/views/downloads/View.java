package org.jdownloader.gui.views.downloads;

import javax.swing.Icon;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public enum View {
    RUNNING(_GUI.T.downloadview_running(), "media-playback-start"),
    ALL(_GUI.T.downloadview_all(), "download"),
    SKIPPED(_GUI.T.downloadview_skipped(), "skipped"),
    FAILED(_GUI.T.downloadview_failed(), "error"),
    EXISTS(_GUI.T.downloadview_exists(), "false"),
    OFFLINE(_GUI.T.downloadview_offline(), "false"),
    SUCCESSFUL(_GUI.T.downloadview_successful(), "ok"),
    TODO(_GUI.T.downloadview_todo(), IconKey.ICON_WAIT);
    private String label;
    private String iconKey;

    public String getLabel() {
        return label;
    }

    private View(String translation, String iconKey) {
        this.label = translation;
        this.iconKey = iconKey;
    }

    public Icon getIcon() {
        return NewTheme.I().getIcon(iconKey, 18);
    }

    public String getIconKey() {
        return iconKey;
    }
}
