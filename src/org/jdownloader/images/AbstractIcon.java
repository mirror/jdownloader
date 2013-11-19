package org.jdownloader.images;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class AbstractIcon implements Icon {

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

    private int size;
    private int width;
    private int height;

    public AbstractIcon(String key, int size) {
        this.key = key;
        this.size = size;
        update();
    }

    /**
     * 
     */
    protected void update() {
        ImageIcon icon = NewTheme.I().getIcon(getKey(), getSize());
        width = icon.getIconWidth();
        height = icon.getIconHeight();
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        NewTheme.I().getIcon(getKey(), getSize()).paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

}
