package org.jdownloader.gui.jdtrayicon.actions;

import jd.gui.swing.jdgui.components.toolbar.actions.GlobalPremiumSwitchToggleAction;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;
import org.jdownloader.gui.views.SelectionInfo;

public class TrayGlobalPremiumSwitchToggleAction extends GlobalPremiumSwitchToggleAction {

    public TrayGlobalPremiumSwitchToggleAction(SelectionInfo<?, ?> selection) {
        super(selection);
        setName(_TRAY._.popup_premiumtoggle());
    }

}
