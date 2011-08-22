package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class AddLinksAction extends AppAction {
    {
        putValue(SMALL_ICON, NewTheme.I().getIcon("add", 16));
        putValue(NAME, _GUI._.AddLinksAction_());

    }

    public void actionPerformed(ActionEvent e) {
    }

}
