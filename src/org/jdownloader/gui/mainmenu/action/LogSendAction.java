package org.jdownloader.gui.mainmenu.action;

import java.awt.event.ActionEvent;

import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

import jd.gui.swing.jdgui.menu.actions.sendlogs.LogAction;

public class LogSendAction extends CustomizableAppAction {

    public LogSendAction() {
        setName(_GUI.T.LogAction());
        setIconKey(IconKey.ICON_LOG);
        setTooltipText(_GUI.T.LogAction_tooltip());

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new LogAction().actionPerformed(e);
    }

}
