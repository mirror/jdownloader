package org.jdownloader.images;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

import org.appwork.swing.components.IDIcon;
import org.appwork.swing.components.IconIdentifier;

public class AbstractIcon implements Icon, IDIcon {

    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
        update();
    }

    public int getSize() {

        return size;
    }

    public void setSize(int size) {
        this.size = size;
        update();
    }

    private int     size;
    private int     width;
    private int     height;
    private boolean autoDisabledIconEnabled;

    public AbstractIcon(String key, int size) {
        this.key = key;
        this.size = size;
        update();
    }

    /**
     *
     */
    protected void update() {
        Icon icon = NewTheme.I().getIcon(getKey(), getSize());
        width = icon.getIconWidth();
        height = icon.getIconHeight();
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
        if (c != null && !c.isEnabled() && isAutoDisabledIconEnabled()) {
            org.jdownloader.images.NewTheme.I().getDisabledIcon(icon).paintIcon(c, g, x, y);
        } else {
            icon.paintIcon(c, g, x, y);
        }
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
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
