package org.jdownloader.gui.jdtrayicon.actions;

import jd.gui.swing.jdgui.components.toolbar.actions.ReconnectAction;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;

public class TrayReconnectAction extends ReconnectAction {

    public TrayReconnectAction() {
        super();
        setName(_TRAY._.popup_reconnect());
    }

}
