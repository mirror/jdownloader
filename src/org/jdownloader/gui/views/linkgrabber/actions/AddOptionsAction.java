package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.laf.jddefault.LAFOptions;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class AddOptionsAction extends AppAction {
    /**
     * 
     */
    private static final long serialVersionUID = -1041794723138925672L;
    private JButton           positionComp;

    public AddOptionsAction(JButton addLinks) {
        setSmallIcon(NewTheme.I().getIcon("popupButton", -1));
        setTooltipText(_GUI._.AddOptionsAction_AddOptionsAction_tt());
        positionComp = addLinks;
    }

    public void actionPerformed(ActionEvent e) {
        JPopupMenu popup = new JPopupMenu();
        AddLinksAction ala = new AddLinksAction((SelectionInfo<CrawledPackage, CrawledLink>) null);
        ala.putValue(AbstractAction.NAME, _GUI._.AddOptionsAction_actionPerformed_addlinks());
        popup.add(new JMenuItem(ala));
        popup.add(new JMenuItem(new AddContainerAction((SelectionInfo<CrawledPackage, CrawledLink>) null)));
        int[] insets = LAFOptions.getInstance().getPopupBorderInsets();

        Dimension pref = popup.getPreferredSize();
        // pref.width = positionComp.getWidth() + ((Component)
        // e.getSource()).getWidth() + insets[1] + insets[3];
        popup.setPreferredSize(pref);

        popup.show(positionComp, -insets[1] - 1, -popup.getPreferredSize().height + insets[2]);
    }

}
