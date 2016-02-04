package org.jdownloader.images;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

import org.appwork.swing.components.IDIcon;
import org.appwork.swing.components.IconIdentifier;

public class AbstractIcon implements Icon, IDIcon {

    private final String key;

    public String getKey() {
        return key;
    }

    public int getSize() {
        return size;
    }

    private final int size;
    private int       width  = -1;
    private int       height = 1;
    private boolean   autoDisabledIconEnabled;

    public AbstractIcon(String key, int size) {
        this.key = key;
        this.size = size;
    }

    /**
     *
     */
    protected void lazyUpdate() {
        if (width == -1 || height == -1) {
            final Icon icon = NewTheme.I().getIcon(getKey(), getSize());
            width = icon.getIconWidth();
            height = icon.getIconHeight();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AbstractIcon)) {
            return false;
        }
        return key.equals(((AbstractIcon) obj).key) && size == ((AbstractIcon) obj).size;
    }

    @Override
    public int hashCode() {
        return key.hashCode() + size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        final Icon icon = NewTheme.I().getIcon(getKey(), getSize());
        if (width == -1 || height == -1) {
            width = icon.getIconWidth();
            height = icon.getIconHeight();
        }
        if (c != null && !c.isEnabled() && isAutoDisabledIconEnabled()) {
            org.jdownloader.images.NewTheme.I().getDisabledIcon(icon).paintIcon(c, g, x, y);
        } else {
            icon.paintIcon(c, g, x, y);
        }
    }

    @Override
    public int getIconWidth() {
        lazyUpdate();
        return width;
    }

    @Override
    public int getIconHeight() {
        lazyUpdate();
        return height;
    }

    @Override
    public IconIdentifier getIdentifier() {
        return new IconIdentifier(null, key);
    }

    public void setAutoDisabledIconEnabled(boolean b) {
        autoDisabledIconEnabled = b;
    }

    public boolean isAutoDisabledIconEnabled() {
        return autoDisabledIconEnabled;
    }

}
