package org.jdownloader.images;

import java.awt.Image;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.appwork.resources.AWUTheme;
import org.appwork.resources.Theme;
import org.appwork.swing.components.CheckBoxIcon;
import org.appwork.swing.components.IdentifierInterface;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;

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

    protected Icon modify(Icon ret, String relativePath) {
        if (ret instanceof ImageIcon) {
            return new IdentifierImageIcon(((ImageIcon) ret).getImage(), relativePath);
        }
        return new IdentifierWrapperIcon(ret, relativePath);

    };

    /**
     * Create a new instance of NewTheme.I(). This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private NewTheme() {
        super("org/jdownloader/");
        AWUTheme.getInstance().setNameSpace(getNameSpace());
    }

    protected String getCacheKey(final Object... objects) {
        if (objects.length == 1) {
            return objects[0].toString();
        }
        final StringBuilder sb = new StringBuilder();
        for (final Object o : objects) {
            if (sb.length() > 0) {
                sb.append("_");
            }
            if (o instanceof IdentifierInterface) {
                sb.append(((IdentifierInterface) o).toIdentifier());
            } else {
                sb.append(o.toString());
            }
        }
        return sb.toString();
    }

    @Override
    public Image getImage(String key, int size, boolean useCache) {
        Icon ico = this.getIcon(key, size, useCache);
        if (ico instanceof IdentifierWrapperIcon) {
            return IconIO.toBufferedImage(((IdentifierWrapperIcon) ico).getIcon());
        }
        return IconIO.toBufferedImage(ico);

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
            Icon back = getIcon(path, size - 5, false);
            CheckBoxIcon checkBox = selected ? CheckBoxIcon.TRUE : CheckBoxIcon.FALSE;

            ret = new ImageIcon(ImageProvider.merge(back, checkBox, 5, 0, 0, back.getIconHeight() - checkBox.getIconHeight() + 5));
            cache(ret, key);
        }
        return ret;
    }

}
