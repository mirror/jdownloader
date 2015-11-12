package org.jdownloader.images;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import org.appwork.resources.AWUTheme;
import org.appwork.resources.Theme;
import org.appwork.swing.components.CheckBoxIcon;
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
        AWUTheme.I().setDelegate(this);
    }

    protected String getCacheKey(final Object... objects) {

        if (objects != null) {
            if (objects.length == 1 && objects[0] != null) {
                return objects[0].toString();

            }
            final StringBuilder sb = new StringBuilder();
            for (final Object o : objects) {
                if (o != null) {
                    if (sb.length() > 0) {
                        sb.append("_");
                    }
                    sb.append(o.toString());
                }
            }
            return sb.toString();
        }
        return null;
    }

    @Override
    public Image getImage(String key, int size, boolean useCache) {
        Icon ico = this.getIcon(key, size, useCache);
        if (ico instanceof IdentifierWrapperIcon) {
            return IconIO.toBufferedImage(((IdentifierWrapperIcon) ico).getIcon());
        }
        return IconIO.toBufferedImage(ico);

    }

    public Icon getCheckBoxImage(String path, boolean selected, int size) {
        return getCheckBoxImage(path, selected, size, null);
    }

    /**
     * Returns a Icon which contains a checkbox.
     *
     * @param path
     * @param selected
     * @param size
     * @param red
     * @return
     */
    public Icon getCheckBoxImage(String path, boolean selected, int size, Color red) {
        Icon ret = null;
        String key = this.getCacheKey(path + "/" + red, size, selected);
        ret = getCached(key);
        if (ret == null) {
            Icon back = getIcon(path, size - 5, false);
            Icon checkBox = selected ? CheckBoxIcon.TRUE : CheckBoxIcon.FALSE;
            if (red != null) {// works for synthetica default LAF only
                if (UIManager.getLookAndFeel().getClass().getSimpleName().equals("JDDefaultLookAndFeel")) {
                    checkBox = IconIO.replaceColor(checkBox, new Color(!selected ? 0xFFF0F0F0 : 0xFFEBEBEB), 50, red, true);
                } else {
                    BufferedImage img = IconIO.toBufferedImage(checkBox);
                    Graphics2D g2 = (Graphics2D) img.getGraphics();

                    Point2D center = new Point2D.Float(img.getWidth() / 2 - 1, img.getHeight() / 2 - 1);
                    float radius = img.getWidth() / (float) 2;
                    float[] dist = { 0.4f, 1.0f };

                    Color[] colors = { new Color(red.getRed(), red.getGreen(), red.getBlue(), red.getAlpha()), new Color(red.getRed(), red.getGreen(), red.getBlue(), 0) };
                    RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors, CycleMethod.NO_CYCLE);
                    g2.setPaint(p);
                    g2.fillOval(0, 0, img.getWidth() + 2, img.getHeight() + 2);
                    g2.dispose();
                    checkBox = new ImageIcon(img);
                }

            }

            ret = new ImageIcon(ImageProvider.merge(back, checkBox, 5, 0, 0, back.getIconHeight() - checkBox.getIconHeight() + 5));
            cache(ret, key);
        }
        return ret;
    }

}
