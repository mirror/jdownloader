/*
 * $Id: ComboBoxCellEditor.java,v 1.7 2007/11/02 14:26:47 kschaefe Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx.autocomplete;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

/**
 * <p>This is a cell editor that can be used when a combo box (that has been set
 * up for automatic completion) is to be used in a JTable. The
 * {@link javax.swing.DefaultCellEditor DefaultCellEditor} won't work in this
 * case, because each time an item gets selected it stops cell editing and hides
 * the combo box.
 * </p>
 * <p>
 * Usage example:
 * </p>
 * <p>
 * <pre><code>
 * JTable table = ...;
 * JComboBox comboBox = ...;
 * ...
 * TableColumn column = table.getColumnModel().getColumn(0);
 * column.setCellEditor(new ComboBoxCellEditor(comboBox));
 * </code></pre>
 * </p>
 */
public class ComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor, Serializable {
    
    /** the combo box */
    private JComboBox comboBox;

    /**
     * Creates a new ComboBoxCellEditor.
     * @param comboBox the comboBox that should be used as the cell editor.
     */
    public ComboBoxCellEditor(final JComboBox comboBox) {
        this.comboBox = comboBox;
        
        Handler handler = new Handler();
        
        // Don't do this:
        // this.comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        // it probably breaks various things
        
        // hitting enter in the combo box should stop cellediting
        JComponent editorComponent = (JComponent) comboBox.getEditor().getEditorComponent();
        editorComponent.addKeyListener(handler);
        // remove the editor's border - the cell itself already has one
        editorComponent.setBorder(null);

        // editor component might change (e.g. a look&feel change)
        // the new editor component needs to be modified then (keyListener, border)
        comboBox.addPropertyChangeListener(handler);
    }
    
    // ------ Implementing CellEditor ------
    /**
     * Returns the value contained in the combo box
     * @return the value contained in the combo box
     */
    public Object getCellEditorValue() {
        return comboBox.getSelectedItem();
    }
    
    /**
     * Tells the combo box to stop editing and accept any partially edited value as the value of the combo box.
     * Always returns true.
     * @return true
     */
    public boolean stopCellEditing() {
        if (comboBox.isEditable()) {
            // Notify the combo box that editing has stopped (e.g. User pressed F2)
            comboBox.actionPerformed(new ActionEvent(this, 0, ""));
        }
        fireEditingStopped();
        return true;
    }
    
    // ------ Implementing TableCellEditor ------
    /**
     * Sets an initial value for the combo box.
     * Returns the combo box that should be added to the client's Component hierarchy.
     * Once installed in the client's hierarchy this combo box will then be able to draw and receive user input.
     * @param table the JTable that is asking the editor to edit; can be null
     * @param value the value of the cell to be edited; null is a valid value
     * @param isSelected will be ignored
     * @param row the row of the cell being edited
     * @param column the column of the cell being edited
     * @return the combo box for editing
     */
    public java.awt.Component getTableCellEditorComponent(javax.swing.JTable table, Object value, boolean isSelected, int row, int column) {
        comboBox.setSelectedItem(value);
        return comboBox;
    }
    
    // ------ Implementing TreeCellEditor ------
//    public java.awt.Component getTreeCellEditorComponent(javax.swing.JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
//        String stringValue = tree.convertValueToText(value, isSelected, expanded, leaf, row, false);
//        comboBox.setSelectedItem(stringValue);
//        return comboBox;
//    }
    
    class Handler extends KeyAdapter implements PropertyChangeListener {
        public void keyPressed(KeyEvent keyEvent) {
            int keyCode = keyEvent.getKeyCode();
            if (keyCode==KeyEvent.VK_ENTER) stopCellEditing();
        }
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName().equals("editor")) {
                ComboBoxEditor editor = comboBox.getEditor();
                if (editor!=null && editor.getEditorComponent()!=null) {
                    JComponent editorComponent = (JComponent) comboBox.getEditor().getEditorComponent();
                    editorComponent.addKeyListener(this);
                    editorComponent.setBorder(null);
                }
            }
        }
    }
}
