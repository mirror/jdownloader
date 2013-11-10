package org.jdownloader.gui.jdtrayicon.actions;

import jd.gui.swing.jdgui.components.toolbar.actions.PauseDownloadsAction;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;

public class TrayPauseAction extends PauseDownloadsAction {

    public TrayPauseAction() {
        super();
        setName(_TRAY._.popup_pause());
    }

}
