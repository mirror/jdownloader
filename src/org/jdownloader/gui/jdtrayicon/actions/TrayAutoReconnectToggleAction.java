package org.jdownloader.gui.jdtrayicon.actions;

import jd.gui.swing.jdgui.components.toolbar.actions.AutoReconnectToggleAction;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;
import org.jdownloader.gui.views.SelectionInfo;

public class TrayAutoReconnectToggleAction extends AutoReconnectToggleAction {

    public TrayAutoReconnectToggleAction(SelectionInfo<?, ?> selection) {
        super(selection);
        setName(_TRAY._.popup_reconnecttoggle());
    }
}
