package org.jdesktop.swingx.renderer;

import java.awt.Color;

import javax.swing.JTable;

/**
 * Table specific <code>CellContext</code>.
 */
public class TableCellContext extends CellContext<JTable> {

    /**
     * Returns the cell's editable property as returned by table.isCellEditable
     * or false if the table is null.
     * 
     * @return the cell's editable property. 
     */
    @Override
    public boolean isEditable() {
        return getComponent() != null ? getComponent().isCellEditable(
                getRow(), getColumn()) : false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Color getSelectionBackground() {
        return getComponent() != null ? getComponent()
                .getSelectionBackground() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Color getSelectionForeground() {
        return getComponent() != null ? getComponent()
                .getSelectionForeground() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getUIPrefix() {
        return "Table.";
    }

}