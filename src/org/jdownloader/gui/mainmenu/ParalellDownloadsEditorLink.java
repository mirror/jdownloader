package org.jdownloader.gui.mainmenu;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JComponent;

import jd.gui.swing.jdgui.menu.ParalellDownloadsEditor;

import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.translate._GUI;

public class ParalellDownloadsEditorLink extends MenuItemData implements MenuLink {
    @Override
    public JComponent createSettingsPanel() {
        return null;
    }

    public ParalellDownloadsEditorLink() {
        super();

        setName(_GUI._.ParalellDownloadsEditor_ParalellDownloadsEditor_());
        setIconKey("paralell");
        //
    }

    public JComponent createItem() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {

        return new ParalellDownloadsEditor();

    }

    @Override
    public List<AppAction> createActionsToLink() {
        return null;
    }

}
