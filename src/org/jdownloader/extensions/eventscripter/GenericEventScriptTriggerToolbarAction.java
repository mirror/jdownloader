package org.jdownloader.extensions.eventscripter;

import java.awt.event.ActionEvent;

import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;
import org.jdownloader.gui.toolbar.action.AbstractToolBarAction;

public class GenericEventScriptTriggerToolbarAction extends AbstractToolBarAction {

    public GenericEventScriptTriggerToolbarAction() {
        setName("EventScripter Trigger");
        setIconKey(IconKey.ICON_EVENT);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LazyExtension extension = ExtensionController.getInstance().getExtension(EventScripterExtension.class);
        boolean isToolbar = getMenuItemData()._getRoot() == MenuManagerMainToolbar.getInstance().getMenuData();
        if (extension != null && extension._isEnabled()) {
            ((EventScripterExtension) extension._getExtension()).triggerAction(getName(), getIconKey(), getShortCutString(), EventTrigger.TOOLBAR_BUTTON, null);
        }

    }

    @Override
    protected String createTooltip() {
        return null;
    }

}
