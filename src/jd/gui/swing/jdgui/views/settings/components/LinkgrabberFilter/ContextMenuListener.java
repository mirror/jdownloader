package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

public class ContextMenuListener implements MouseListener {

    private FilterTable       table;
    private LinkgrabberFilter linkgrabberFilter;

    public ContextMenuListener(LinkgrabberFilter linkgrabberFilter) {
        this.table = linkgrabberFilter.getTable();
        this.linkgrabberFilter = linkgrabberFilter;

    }

    public void mouseClicked(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {

        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            int row = table.rowAtPoint(e.getPoint());
            Object obj = table.getExtTableModel().getObjectbyRow(row);
            if (obj == null || row == -1) {
                /* no object under mouse, lets clear the selection */
                table.clearSelection();
                showPopupEmpty().show(table, e.getPoint().x, e.getPoint().y);
                return;
            } else {
                /* check if we need to select object */
                if (!table.isRowSelected(row)) {
                    table.clearSelection();
                    table.addRowSelectionInterval(row, row);
                }
                showPopup().show(table, e.getPoint().x, e.getPoint().y);
            }
        }
    }

    private JPopupMenu showPopup() {
        return showPopupEmpty();
    }

    private JPopupMenu showPopupEmpty() {
        JPopupMenu popup = new JPopupMenu();
        ArrayList<LinkFilter> selected = table.getExtTableModel().getSelectedObjects();
        popup.add(new JMenuItem(new NewAction(table)));
        popup.add(new JMenuItem(new RemoveAction(selected)));
        popup.add(new JSeparator());
        popup.add(new JMenuItem(new TestAction(linkgrabberFilter)));
        return popup;
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

}
