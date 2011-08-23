package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class AddOptionsAction extends AbstractAction {
    private JButton positionComp;

    {
        putValue(SMALL_ICON, NewTheme.I().getIcon("popupButton", -1));

    }

    public AddOptionsAction(JButton addLinks) {
        positionComp = addLinks;
    }

    public void actionPerformed(ActionEvent e) {
        JPopupMenu popup = new JPopupMenu();
        AddLinksAction ala = new AddLinksAction();
        ala.putValue(AbstractAction.NAME, _GUI._.AddOptionsAction_actionPerformed_addlinks());
        popup.add(new JMenuItem(ala));
        popup.add(new JMenuItem(new AddContainerAction()));

        popup.show(positionComp, 0, -popup.getPreferredSize().height);
    }

}
