/*
 * $Id: AutoCompleteComboBoxEditor.java,v 1.3 2007/11/02 14:26:47 kschaefe Exp $
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

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.ComboBoxEditor;

/**
 * <p>
 * Wrapper around the combobox editor that translates combobox items into
 * strings. The methods <tt>setItem</tt> and <tt>getItem</tt> are modified
 * to account for the string conversion.
 * </p><p>
 * This is necessary for those cases where the combobox items have no useful
 * <tt>toString()</tt> method and a custom <tt>ObjectToStringConverter</tt> is
 * used.
 * </p><p>
 * If we do not do this, the interaction between ComboBoxEditor and JComboBox
 * will result in firing ActionListener events with the string value of
 * ComboBoxEditor as the currently selected value.
 * </p>
 * @author Noel Grandin noelgrandin@gmail.com
 * @author Thomas Bierhance
 */
public class AutoCompleteComboBoxEditor implements ComboBoxEditor {

    /** the original combo box editor*/
    private final ComboBoxEditor wrapped;
    /** the converter used to convert items into their string representation */
    private final ObjectToStringConverter stringConverter;
    /** last selected item */
    private Object oldItem;

    /**
     * Creates a new <tt>AutoCompleteComboBoxEditor</tt>.
     *
     * @param wrapped the original <tt>ComboBoxEditor</tt> to be wrapped
     * @param stringConverter the converter to use to convert items into their
     * string representation.
     */
    public AutoCompleteComboBoxEditor(ComboBoxEditor wrapped, ObjectToStringConverter stringConverter) {
        this.wrapped = wrapped;
        this.stringConverter = stringConverter;
    }

    /* (non-javadoc)
     * @see javax.swing.ComboBoxEditor#getEditorComponent()
     */
    public Component getEditorComponent() {
        return wrapped.getEditorComponent();
    }

    /* (non-javadoc)
     * @see javax.swing.ComboBoxEditor#setItem(java.lang.Object)
     */
    public void setItem(Object anObject) {
        this.oldItem = anObject;
        wrapped.setItem(stringConverter.getPreferredStringForItem(anObject));
    }

    /* (non-javadoc)
     * @see javax.swing.ComboBoxEditor#getItem()
     */
    public Object getItem() {
        final Object wrappedItem = wrapped.getItem();
        
        String[] oldAsStrings = stringConverter.getPossibleStringsForItem(oldItem);
        for (int i=0, n=oldAsStrings.length; i<n; i++) {
            String oldAsString = oldAsStrings[i];
            if (oldAsString.equals(wrappedItem)) {
                return oldItem;
            }
        }
        return null;
    }

    /* (non-javadoc)
     * @see javax.swing.ComboBoxEditor#selectAll()
     */
    public void selectAll() {
        wrapped.selectAll();
    }

    /* (non-javadoc)
     * @see javax.swing.ComboBoxEditor#addActionListener(java.awt.event.ActionListener)
     */
    public void addActionListener(ActionListener l) {
        wrapped.addActionListener(l);
    }

    /* (non-javadoc)
     * @see javax.swing.ComboBoxEditor#removeActionListener(java.awt.event.ActionListener)
     */
    public void removeActionListener(ActionListener l) {
        wrapped.removeActionListener(l);
    }
}