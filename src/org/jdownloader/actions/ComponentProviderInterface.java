package org.jdownloader.actions;

import javax.swing.JComponent;

import org.jdownloader.controlling.contextmenu.MenuItemData;

public interface ComponentProviderInterface {
    public JComponent createComponent(MenuItemData menuItemData);

}
