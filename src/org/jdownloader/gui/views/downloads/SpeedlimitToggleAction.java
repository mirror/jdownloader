package org.jdownloader.gui.views.downloads;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class SpeedlimitToggleAction extends AppAction {

    public SpeedlimitToggleAction() {
        super();
        setName(_GUI._.SpeedlimitToggleAction_SpeedlimitToggleAction());
        setIconKey(IconKey.ICON_SPEED);
        setSelected(CFG_GUI.SPEED_METER_VISIBLE.isEnabled());

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        setSelected(CFG_GUI.SPEED_METER_VISIBLE.toggle());
    }
}
