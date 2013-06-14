package org.jdownloader.gui.jdtrayicon.actions;

import jd.gui.swing.jdgui.components.toolbar.actions.UpdateAction;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;
import org.jdownloader.gui.views.SelectionInfo;

public class TrayUpdateAction extends UpdateAction {

    public TrayUpdateAction(SelectionInfo<?, ?> selection) {
        super(selection);
        setName(_TRAY._.popup_update());
    }

}
