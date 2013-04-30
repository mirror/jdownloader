package org.jdownloader.actions;

import javax.swing.ImageIcon;

import org.appwork.swing.action.BasicAction;
import org.appwork.utils.ImageProvider.ImageProvider;
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

    public static ImageIcon merge(String a, String b) {
        return new ImageIcon(ImageProvider.merge(NewTheme.I().getImage(a, 18), NewTheme.I().getImage(b, 12), 0, 0, 10, 10).getSubimage(0, 0, 18, 18));
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
