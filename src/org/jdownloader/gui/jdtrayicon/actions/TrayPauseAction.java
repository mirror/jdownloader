package org.jdownloader.gui.jdtrayicon.actions;

import jd.gui.swing.jdgui.components.toolbar.actions.PauseDownloadsAction;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;
import org.jdownloader.gui.views.SelectionInfo;

public class TrayPauseAction extends PauseDownloadsAction {

    public TrayPauseAction(SelectionInfo<?, ?> selection) {
        super(selection);
        setName(_TRAY._.popup_pause());
    }

}
