package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

import jd.gui.swing.jdgui.menu.actions.AddContainerAction;

public class AddContainerContextMenuAction extends CustomizableTableContextAppAction {

    private static final long serialVersionUID = 1901008532686173167L;

    public AddContainerContextMenuAction() {
        super(true, false);

        setName(_GUI.T.action_addcontainer());
        setTooltipText(_GUI.T.action_addcontainer_tooltip());
        setIconKey(IconKey.ICON_LOAD);
        setAccelerator(KeyEvent.VK_L);

    }

    public void actionPerformed(ActionEvent e) {
        new AddContainerAction().actionPerformed(e);
    }

}