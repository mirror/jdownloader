package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.laf.LookAndFeelController;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.images.NewTheme;

public class ConfirmOptionsAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = 8513573481628755833L;
    private JButton           positionComp;
    private LinkGrabberTable  table;

    {
        putValue(SMALL_ICON, NewTheme.I().getIcon("popupButton", -1));

    }

    public ConfirmOptionsAction(LinkGrabberTable table, JButton addLinks) {
        positionComp = addLinks;
        this.table = table;
    }

    public void actionPerformed(ActionEvent e) {
        JPopupMenu popup = new JPopupMenu();
        JMenu all = new JMenu(_GUI._.ConfirmOptionsAction_actionPerformed_all());
        JMenu selected = new JMenu(_GUI._.ConfirmOptionsAction_actionPerformed_selected());
        all.setIcon(NewTheme.I().getIcon("confirmAll", 16));
        selected.setIcon(NewTheme.I().getIcon("confirmSelectedLinks", 16));
        all.add(new JMenuItem(new ConfirmAllAction(false).setAccelerator(null)));
        all.add(new JMenuItem(new ConfirmAllAction(true).setAccelerator(null)));
        SelectionInfo<CrawledPackage, CrawledLink> selection = new SelectionInfo<CrawledPackage, CrawledLink>(table.getExtTableModel().getSelectedObjects());
        selected.add(new JMenuItem(new ConfirmAction(selection).setAutostart(false).setAccelerator(null)));
        ConfirmAction action = new ConfirmAction(selection);
        action.setAutostart(true);
        selected.add(new JMenuItem(action.setAccelerator(null)));
        int[] insets = LookAndFeelController.getInstance().getLAFOptions().getPopupBorderInsets();
        popup.add(all);
        popup.add(selected);
        Dimension pref = popup.getPreferredSize();
        pref.width = positionComp.getWidth() + ((Component) e.getSource()).getWidth() + insets[1] + insets[3];
        popup.setPreferredSize(pref);
        popup.show((Component) e.getSource(), -popup.getPreferredSize().width + ((Component) e.getSource()).getWidth() + insets[3], -popup.getPreferredSize().height + insets[2]);
    }
}
