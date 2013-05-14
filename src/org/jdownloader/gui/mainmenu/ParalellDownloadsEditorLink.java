package org.jdownloader.gui.mainmenu;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;

import jd.gui.swing.jdgui.menu.ParalellDownloadsEditor;

import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuItemProperty;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class ParalellDownloadsEditorLink extends MenuItemData implements MenuLink {

    public ParalellDownloadsEditorLink() {
        super(MenuItemProperty.HIDE_ON_MAC);

        setName(_GUI._.ParalellDownloadsEditor_ParalellDownloadsEditor_());
        setIconKey("paralell");
        //
    }

    public JComponent createItem(SelectionInfo<?, ?> selection) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        return new ParalellDownloadsEditor();

    }

}
