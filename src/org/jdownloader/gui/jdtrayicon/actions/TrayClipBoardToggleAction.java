package org.jdownloader.gui.jdtrayicon.actions;

import jd.gui.swing.jdgui.components.toolbar.actions.ClipBoardToggleAction;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;
import org.jdownloader.gui.views.SelectionInfo;

public class TrayClipBoardToggleAction extends ClipBoardToggleAction {

    public TrayClipBoardToggleAction(SelectionInfo<?, ?> selection) {
        super(selection);
        setName(_TRAY._.popup_clipboardtoggle());
    }

}
