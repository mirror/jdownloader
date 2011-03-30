package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import jd.controlling.proxy.ProxyInfo;

import org.appwork.utils.swing.table.ExtTable;

public class ProxyContextMenu extends MouseAdapter {

    private final ExtTable<ProxyInfo> table;

    public ProxyContextMenu(final ExtTable<ProxyInfo> table) {
        this.table = table;
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        JPopupMenu popup = null;
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            final int row = this.table.rowAtPoint(e.getPoint());
            final Object obj = ((ProxyTableModel) this.table.getModel()).getObjectbyRow(row);
            if (obj == null || row == -1) {
                /* no object under mouse, lets clear the selection */
                this.table.clearSelection();
                popup = this.showProxyPopup(e, null, null);
                if (popup != null) {
                    popup.show(this.table, e.getPoint().x, e.getPoint().y);
                }
                return;
            } else {
                /* check if we need to select object */
                if (!this.table.isRowSelected(row)) {
                    this.table.clearSelection();
                    this.table.addRowSelectionInterval(row, row);
                }
            }
            final ArrayList<ProxyInfo> selected = ((ProxyTableModel) this.table.getModel()).getSelectedObjects();

            if (obj instanceof ProxyInfo) {
                popup = this.showProxyPopup(e, (ProxyInfo) obj, selected);
            }
            if (popup != null) {
                popup.show(this.table, e.getPoint().x, e.getPoint().y);
            }

        }
    }

    private JPopupMenu showProxyPopup(final MouseEvent e, final ProxyInfo obj, final ArrayList<ProxyInfo> selected) {
        final JPopupMenu popup = new JPopupMenu();
        popup.add(new JMenuItem(new ProxyAddAction()));
        popup.add(new JMenuItem(new ProxyDeleteAction(selected)));
        return popup;
    }
}
