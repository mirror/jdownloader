package org.jdownloader.images;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

import org.appwork.swing.components.IDIcon;
import org.appwork.swing.components.IconIdentifier;

public class IdentifierWrapperIcon implements Icon, IDIcon {

    private Icon   _icon;
    private String key;

    public IdentifierWrapperIcon(Icon ret, String relativePath) {
        _icon = ret;
        key = relativePath;
    }

    public Icon getIcon() {
        return _icon;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        _icon.paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
        return _icon.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return _icon.getIconHeight();
    }

    @Override
    public IconIdentifier getIdentifier() {
        return new IconIdentifier(null, key);
    }

}
