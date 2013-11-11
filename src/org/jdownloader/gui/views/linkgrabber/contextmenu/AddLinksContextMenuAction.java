package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;

public class AddLinksContextMenuAction extends CustomizableTableContextAppAction {

    private static final long serialVersionUID = 1901008532686173167L;

    public AddLinksContextMenuAction() {
        super(true, false);
        setName(_GUI._.AddLinksToLinkgrabberAction());
        setIconKey("add");
        setTooltipText(_GUI._.AddLinksAction_AddLinksAction_tt());
        setAccelerator(KeyEvent.VK_O);

    }

    public void actionPerformed(ActionEvent e) {
        new AddLinksAction().actionPerformed(e);

    }

}