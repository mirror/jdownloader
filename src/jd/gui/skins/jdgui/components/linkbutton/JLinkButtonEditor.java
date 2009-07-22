package jd.gui.skins.jdgui.components.linkbutton;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;

class JLinkButtonEditor implements TableCellEditor, ActionListener {

    private boolean stop = false;

    public void actionPerformed(ActionEvent e) {
        stop = true;
    }

    public void addCellEditorListener(CellEditorListener l) {
    }

    public void cancelCellEditing() {
    }

    public Object getCellEditorValue() {
        return null;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        stop = false;
        JLink btn = (JLink) value;
        btn.getBroadcaster().addListener(this);
        btn.setOpaque(true);
        return btn;
    }

    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    public void removeCellEditorListener(CellEditorListener l) {
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return false;
    }

    public boolean stopCellEditing() {
        return stop;
    }

}
