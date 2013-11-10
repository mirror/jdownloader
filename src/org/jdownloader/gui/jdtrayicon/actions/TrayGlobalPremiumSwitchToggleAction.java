package org.jdownloader.gui.jdtrayicon.actions;

import jd.gui.swing.jdgui.components.toolbar.actions.GlobalPremiumSwitchToggleAction;

import org.jdownloader.gui.jdtrayicon.translate._TRAY;

public class TrayGlobalPremiumSwitchToggleAction extends GlobalPremiumSwitchToggleAction {

    public TrayGlobalPremiumSwitchToggleAction() {
        super();
        setName(_TRAY._.popup_premiumtoggle());
    }

}
