package org.jdownloader.extensions.streaming.gui.categories;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.images.NewTheme;

public abstract class RootCategory {

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

    private String             label;
    private StreamingExtension extension;

    public RootCategory(StreamingExtension plg, String label, String iconKey) {
        this.label = label;
        this.iconKey = iconKey;
        this.extension = plg;
    }

    public StreamingExtension getExtension() {
        return extension;
    }

    public Icon getIcon() {
        return NewTheme.I().getIcon(getIconKey(), 32);
    }

    public abstract JComponent getView();

}
