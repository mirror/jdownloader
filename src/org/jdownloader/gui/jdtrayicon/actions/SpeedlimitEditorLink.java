package org.jdownloader.gui.jdtrayicon.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JComponent;

import jd.gui.swing.jdgui.menu.SpeedlimitEditor;

import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.translate._GUI;

public class SpeedlimitEditorLink extends MenuItemData implements MenuLink {
    @Override
    public JComponent createSettingsPanel() {
        return null;
    }

    @Override
    public List<AppAction> createActionsToLink() {
        return null;
    }

    public SpeedlimitEditorLink() {
        super();
        setName(_GUI._.SpeedlimitEditor_SpeedlimitEditor_());
        setIconKey("speed");
        //
    }

    public JComponent createItem() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        return new SpeedlimitEditor();

    }

}
