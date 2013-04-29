package org.jdownloader.actions;

import org.appwork.swing.action.BasicAction;
import org.jdownloader.images.NewTheme;

/**
 * This abstract class is the parent class for all actions in JDownloader
 * 
 * @author thomas
 * 
 */
public abstract class AppAction extends BasicAction {

    private String iconKey;

    private int    size;

    public AppAction() {
        super();

    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
        setIconSizes(18);
    }

    public String getIconKey() {
        return iconKey;
    }

    public AppAction setIconSizes(int size) {
        this.size = size;
        return this;
    }

    public Object getValue(String key) {
        if (iconKey != null && LARGE_ICON_KEY.equalsIgnoreCase(key)) {
            return NewTheme.I().getIcon(iconKey, size);
        } else if (iconKey != null && SMALL_ICON.equalsIgnoreCase(key)) {
            //
            return NewTheme.I().getIcon(iconKey, size);
        }
        return super.getValue(key);
    }

}
