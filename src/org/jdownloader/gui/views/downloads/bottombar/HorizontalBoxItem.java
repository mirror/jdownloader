package org.jdownloader.gui.views.downloads.bottombar;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;

import org.appwork.swing.MigPanel;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class HorizontalBoxItem extends MenuItemData implements MenuLink, SelfLayoutInterface {
    public HorizontalBoxItem() {
        super();
        setName(_GUI._.HorizontalBoxItem_HorizontalBoxItem());
        setVisible(false);
        setIconKey(IconKey.ICON_RIGHT);
    }

    public JComponent createItem() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        return new MigPanel("ins 0", "[]", "[]");
    }

    @Override
    public String createConstraints() {
        return "height 24!,aligny top,gapleft 2,pushx,growx";
    }
}
