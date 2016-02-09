package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.updatev2.gui.LAFOptions;

public class AddOptionsAction extends AppAction {
    /**
     *
     */
    private static final long serialVersionUID = -1041794723138925672L;
    private JButton           positionComp;

    public AddOptionsAction(JButton addLinks) {
        setSmallIcon(new AbstractIcon(IconKey.ICON_POPUPSMALL, -1));
        setTooltipText(_GUI.T.AddOptionsAction_AddOptionsAction_tt());
        positionComp = addLinks;
    }

    public void actionPerformed(ActionEvent e) {
        JPopupMenu popup = new JPopupMenu();
        AddLinksAction ala = new AddLinksAction();
        ala.putValue(AbstractAction.NAME, _GUI.T.AddOptionsAction_actionPerformed_addlinks());
        popup.add(new JMenuItem(ala));
        popup.add(new JMenuItem(new AddContainerAction()));
        Insets insets = LAFOptions.getInstance().getExtension().customizePopupBorderInsets();

        Dimension pref = popup.getPreferredSize();
        // pref.width = positionComp.getWidth() + ((Component)
        // e.getSource()).getWidth() + insets[1] + insets[3];
        popup.setPreferredSize(pref);

        popup.show(positionComp, -insets.left - 1, -popup.getPreferredSize().height + insets.bottom);
    }

}
