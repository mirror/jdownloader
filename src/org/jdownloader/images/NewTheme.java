package org.jdownloader.images;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.appwork.resources.AWUTheme;
import org.appwork.resources.Theme;
import org.appwork.swing.components.CheckBoxIcon;
import org.appwork.utils.ImageProvider.ImageProvider;

/**
 * New JDownloader Icon Theme Support
 * 
 * @author thomas
 * 
 */
public class NewTheme extends Theme {
    private static final NewTheme INSTANCE = new NewTheme();

    /**
     * get the only existing instance of NewTheme.I(). This is a singleton
     * 
     * @return
     */
    public static NewTheme getInstance() {
        return NewTheme.INSTANCE;
    }

    public static NewTheme I() {
        return NewTheme.INSTANCE;
    }

    /**
     * Create a new instance of NewTheme.I(). This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private NewTheme() {
        super("org/jdownloader/");
        AWUTheme.getInstance().setNameSpace(getNameSpace());
    }

    /**
     * Returns a Icon which contains a checkbox.
     * 
     * @param path
     * @param selected
     * @param size
     * @return
     */
    public Icon getCheckBoxImage(String path, boolean selected, int size) {
        Icon ret = null;
        String key = this.getCacheKey(path, size, selected);
        ret = getCached(key);
        if (ret == null) {
            ImageIcon back = getIcon(path, size - 5, false);
            CheckBoxIcon checkBox = selected ? CheckBoxIcon.TRUE : CheckBoxIcon.FALSE;

            ret = new ImageIcon(ImageProvider.merge(back, checkBox, 5, 0, 0, back.getIconHeight() - checkBox.getIconHeight() + 5));
            cache(ret, key);
        }
        return ret;
    }

}
