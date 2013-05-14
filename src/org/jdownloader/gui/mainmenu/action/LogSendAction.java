package org.jdownloader.gui.mainmenu.action;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.menu.actions.sendlogs.LogAction;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class LogSendAction extends AbstractMainMenuAction {

    public LogSendAction(SelectionInfo<?, ?> selection) {
        setName(_GUI._.LogAction());
        setIconKey("log");
        setTooltipText(_GUI._.LogAction_tooltip());

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new LogAction().actionPerformed(e);
    }

}
