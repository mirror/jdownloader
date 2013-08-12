package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberPanel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ClearFilteredLinksAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.RemoveAllLinkgrabberAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.RemoveIncompleteArchives;
import org.jdownloader.gui.views.linkgrabber.contextmenu.RemoveNonSelectedAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.RemoveOfflineAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.RemoveSelectionLinkgrabberAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ResetPopupAction;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class RemoveOptionsAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = 7579020566025178078L;
    private JButton           positionComp;
    private LinkGrabberTable  table;
    private LinkGrabberPanel  panel;

    {
        putValue(SMALL_ICON, NewTheme.I().getIcon("popupButton", -1));

    }

    public RemoveOptionsAction(LinkGrabberPanel linkGrabberPanel, JButton addLinks) {
        positionComp = addLinks;
        panel = linkGrabberPanel;
        this.table = panel.getTable();
    }

    public void actionPerformed(ActionEvent e) {
        JPopupMenu popup = new JPopupMenu();
        final boolean hasData = table.getModel().getTableData().size() > 0;
        java.util.List<AbstractNode> selection = table.getModel().getSelectedObjects();

        SelectionInfo<CrawledPackage, CrawledLink> si = new SelectionInfo<CrawledPackage, CrawledLink>(null, selection, null, null, e, table);
        popup.add(new RemoveAllLinkgrabberAction(null));
        popup.add(new RemoveSelectionLinkgrabberAction(si) {
            {
                setName(_GUI._.RemoveOptionsAction_actionPerformed_selection_());
            }
        });
        popup.add(new RemoveNonSelectedAction(si) {
            @Override
            public boolean isEnabled() {
                if (hasData == false) return false;
                return super.isEnabled();
            }
        });
        popup.add(new RemoveOfflineAction(si) {
            @Override
            public boolean isEnabled() {
                if (hasData == false) return false;
                return super.isEnabled();
            }
        });

        popup.add(new RemoveIncompleteArchives(new SelectionInfo<CrawledPackage, CrawledLink>(null, new ArrayList<AbstractNode>(LinkGrabberTableModel.getInstance().getAllPackageNodes()), null, null, e, table)));
        popup.add(new ClearFilteredLinksAction(null));
        popup.add(new JSeparator());
        popup.add(new ResetPopupAction(null));
        int[] insets = LAFOptions.getInstance().getPopupBorderInsets();

        Dimension pref = popup.getPreferredSize();
        // pref.width = positionComp.getWidth() + ((Component)
        // e.getSource()).getWidth() + insets[1] + insets[3];
        popup.setPreferredSize(pref);

        popup.show(positionComp, -insets[1] - 1, -popup.getPreferredSize().height + insets[2]);
    }
}
