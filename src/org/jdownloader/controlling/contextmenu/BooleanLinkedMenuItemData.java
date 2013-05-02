package org.jdownloader.controlling.contextmenu;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.ExtensionNotLoadedException;
import org.jdownloader.gui.views.SelectionInfo;

public class BooleanLinkedMenuItemData extends MenuItemData {

    private BooleanKeyHandler keyhandler;
    private boolean           value;

    public BooleanLinkedMenuItemData(BooleanKeyHandler keyhandler, boolean value, Class<? extends AppAction> class1) {
        super(new ActionData(class1));
        this.keyhandler = keyhandler;
        this.value = value;
    }

    @Override
    public JComponent addTo(JComponent root, SelectionInfo<?, ?> selection) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, SecurityException, ExtensionNotLoadedException {
        if (keyhandler.isEnabled() == value) { return super.addTo(root, selection); }
        return null;
    }

}
