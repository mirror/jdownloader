package org.jdownloader.gui.jdtrayicon.actions;

import jd.gui.swing.jdgui.components.toolbar.actions.ReconnectAction;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;
import org.jdownloader.gui.views.SelectionInfo;

public class TrayReconnectAction extends ReconnectAction {

    public TrayReconnectAction(SelectionInfo<?, ?> selection) {
        super(selection);
        setName(_TRAY._.popup_reconnect());
    }

}
