package org.jdownloader.extensions.streaming.mediaarchive.categories;

import javax.swing.Icon;

import org.jdownloader.images.NewTheme;

public class RootCategory {

    private String iconKey;

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    private String label;

    public RootCategory(String label, String iconKey) {
        this.label = label;
        this.iconKey = iconKey;
    }

    public Icon getIcon() {
        return NewTheme.I().getIcon(getIconKey(), 32);
    }

}
