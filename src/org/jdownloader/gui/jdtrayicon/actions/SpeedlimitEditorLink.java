package org.jdownloader.gui.jdtrayicon.actions;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;

import jd.gui.swing.jdgui.menu.SpeedlimitEditor;

import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class SpeedlimitEditorLink extends MenuItemData implements MenuLink {

    public SpeedlimitEditorLink() {
        super();
        setName(_GUI._.SpeedlimitEditor_SpeedlimitEditor_());
        setIconKey("speed");
        //
    }

    public JComponent createItem(SelectionInfo<?, ?> selection) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        return new SpeedlimitEditor();

    }

}
