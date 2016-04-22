package org.jdownloader.plugins.components.youtube.configpanel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPopupMenu;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;
import org.appwork.uio.UIOManager;
import org.appwork.utils.CounterMap;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.gui.swing.jdgui.BasicJDTable;

public class LinkTable extends BasicJDTable<Link> {

    public LinkTable() {
        super(new LinkTableModel());
        this.getModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<Link>((LAFOptions.getInstance().getColorForTableAlternateRowForeground()), (LAFOptions.getInstance().getColorForTableAlternateRowBackground()), null) {

            @Override
            protected Color getBackground(Color current) {
                return LAFOptions.getInstance().getColorForPanelHeaderBackground();
            }

            @Override
            public boolean accept(ExtColumn<Link> column, Link value, boolean selected, boolean focus, int row) {
                return value.getGroupingID() != null;
            }

        });

    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, Link contextObject, final List<Link> selection, ExtColumn<Link> column, MouseEvent mouseEvent) {
        popup.add(new AppAction() {
            {
                setSmallIcon(new AbstractIcon(IconKey.ICON_REMOVE, 20));
                setName(_GUI.T.lit_delete());
                setEnabled(false);
                for (Link l : selection) {
                    if (l.getGroupingID() == null) {
                        setEnabled(true);
                        break;
                    }
                }
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                onShortcutDelete(getModel().getSelectedObjects(), null, false);
            }

        });
        return popup;
    }

    @Override
    protected boolean onShortcutDelete(final List<Link> selectedObjects, KeyEvent evt, final boolean direct) {
        if (selectedObjects == null || selectedObjects.size() == 0) {
            return false;
        }
        if (direct || UIOManager.I().showConfirmDialog(0, _GUI.T.lit_are_you_sure(), _GUI.T.lit_are_you_sure())) {
            getSelectionModel().clearSelection();
            List<Link> links = CFG_YOUTUBE.CFG.getLinks();
            for (Link l : selectedObjects) {
                if (l.getGroupingID() == null) {
                    links.remove(l);
                }
            }
            CFG_YOUTUBE.CFG.setLinks(links);
            return true;
        }
        return false;
    }

    public void onEnabledMapUpdate(CounterMap<String> enabledMap) {
        ((LinkTableModel) getModel()).onEnabledMapUpdate(enabledMap);
    }

}
