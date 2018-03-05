package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;

public class AddLinksContextMenuAction extends CustomizableTableContextAppAction {
    private static final long   serialVersionUID = 1901008532686173167L;
    private static final String NAME             = _GUI.T.AddLinksToLinkgrabberAction();
    private static final String TT               = _GUI.T.AddLinksAction_AddLinksAction_tt();

    public AddLinksContextMenuAction() {
        super(true, false);
        setName(NAME);
        setIconKey(IconKey.ICON_ADD);
        setTooltipText(TT);
        setAccelerator(KeyEvent.VK_L);
    }

    public void actionPerformed(ActionEvent e) {
        new AddLinksAction().actionPerformed(e);
    }
}