package org.jdownloader.gui.views.downloads;

import javax.swing.Icon;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public enum View {
    RUNNING(_GUI._.downloadview_running(), "media-playback-start"),
    ALL(_GUI._.downloadview_all(), "download"),
    FAILED(_GUI._.downloadview_failed(), "error"),
    SUCCESSFUL(_GUI._.downloadview_successful(), "ok"),
    TODO(_GUI._.downloadview_todo(), IconKey.ICON_WAIT);
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
