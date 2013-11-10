package org.jdownloader.gui.jdtrayicon.actions;

import jd.gui.swing.jdgui.components.toolbar.actions.ClipBoardToggleAction;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;

public class TrayClipBoardToggleAction extends ClipBoardToggleAction {

    public TrayClipBoardToggleAction() {
        super();
        setName(_TRAY._.popup_clipboardtoggle());
    }

}
