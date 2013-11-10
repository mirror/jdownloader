package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;

import org.appwork.swing.MigPanel;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class LeftRightDividerItem extends MenuItemData implements MenuLink {
    public LeftRightDividerItem() {
        super();
        setName(_GUI._.LeftRightDividerItem_LeftRightDividerItem());
        setVisible(true);
        setIconKey(IconKey.ICON_RIGHT);
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    public JComponent createItem() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        return new MigPanel("ins 0", "[]", "[]");
    }

}
