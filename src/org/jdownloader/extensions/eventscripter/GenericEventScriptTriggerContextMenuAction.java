package org.jdownloader.extensions.eventscripter;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;

import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;

public class GenericEventScriptTriggerContextMenuAction extends CustomizableTableContextAppAction {
    public GenericEventScriptTriggerContextMenuAction() {
        setName("EventScripter Trigger");
        setIconKey(IconKey.ICON_EVENT);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final LazyExtension extension = ExtensionController.getInstance().getExtension(EventScripterExtension.class);
        if (extension != null && extension._isEnabled()) {
            final SelectionInfo selection = getSelection();
            final EventScripterExtension ext = ((EventScripterExtension) extension._getExtension());
            final View view = MainTabbedPane.getInstance().getSelectedView();
            if (view instanceof DownloadsView) {
                ext.triggerAction(getName(), getIconKey(), getShortCutString(), EventTrigger.DOWNLOAD_TABLE_CONTEXT_MENU_BUTTON, selection);
            } else if (view instanceof LinkGrabberView) {
                ext.triggerAction(getName(), getIconKey(), getShortCutString(), EventTrigger.LINKGRABBER_TABLE_CONTEXT_MENU_BUTTON, selection);
            }
        }
    }
}
