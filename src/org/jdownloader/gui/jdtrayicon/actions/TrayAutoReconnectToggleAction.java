package org.jdownloader.gui.jdtrayicon.actions;

import jd.gui.swing.jdgui.components.toolbar.actions.AutoReconnectToggleAction;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;

public class TrayAutoReconnectToggleAction extends AutoReconnectToggleAction {

    public TrayAutoReconnectToggleAction() {
        super();
        setName(_TRAY._.popup_reconnecttoggle());
    }
}
