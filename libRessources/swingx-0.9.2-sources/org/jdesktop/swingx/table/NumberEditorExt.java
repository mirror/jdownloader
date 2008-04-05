package org.jdesktop.swingx.table;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.LineBorder;


/**
 * 
 * Issue #393-swingx: localized NumberEditor.
 * 
 * @author Noel Grandin
 */
public class NumberEditorExt extends DefaultCellEditor {
    
    private static Class[] argTypes = new Class[]{String.class};
    java.lang.reflect.Constructor constructor;
    
    public NumberEditorExt() {
        this(null);
    }
    public NumberEditorExt(NumberFormat formatter) {
        super(createFormattedTextField(formatter));
        final JFormattedTextField textField = ((JFormattedTextField)getComponent());
        
        textField.setName("Table.editor");
        textField.setHorizontalAlignment(JTextField.RIGHT);
        
        // remove action listener added in DefaultCellEditor
        textField.removeActionListener(delegate);
        // replace the delegate created in DefaultCellEditor
        delegate = new EditorDelegate() {
                public void setValue(Object value) {
                    ((JFormattedTextField)getComponent()).setValue(value);
                }

                public Object getCellEditorValue() {
                    JFormattedTextField textField = ((JFormattedTextField)getComponent());
                    try {
                        textField.commitEdit();
                        return textField.getValue();
                    } catch (ParseException ex) {
                        return null;
                    }
                }
        };
        textField.addActionListener(delegate);
    }
    
    @Override
    public boolean stopCellEditing() {
        // If the user tries to tab out of the field, the textField will call stopCellEditing().
        // Check for a valid edit, and don't let the focus leave until the edit is valid.
        if (!((JFormattedTextField) editorComponent).isEditValid()) return false;
        return super.stopCellEditing();
    }
    
    /** Override and set the border back to normal in case there was an error previously */
    public Component getTableCellEditorComponent(JTable table, Object value,
                                             boolean isSelected,
                                             int row, int column) {
        ((JComponent)getComponent()).setBorder(new LineBorder(Color.black));
        try {
            final Class type = table.getColumnClass(column);
            // Assume that the Number object we are dealing with has a constructor which takes
            // a single string parameter.
            if (!Number.class.isAssignableFrom(type)) {
                throw new IllegalStateException("NumberEditor can only handle subclasses of java.lang.Number");
            }
            constructor = type.getConstructor(argTypes);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Number subclass must have a constructor which takes a string", ex);
        }
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }
    
    @Override
    public Object getCellEditorValue() {
        Number number = (Number) super.getCellEditorValue();
        if (number==null) return null;
        // we use a String value as an intermediary between the Number object returned by the 
        // the NumberFormat and the kind of Object the column wants.
        try {
            return constructor.newInstance(new Object[]{number.toString()});
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("NumberEditor not propertly configured", ex);
        } catch (InstantiationException ex) {
            throw new RuntimeException("NumberEditor not propertly configured", ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("NumberEditor not propertly configured", ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("NumberEditor not propertly configured", ex);
        }
    }


    /**
     * Use a static method so that we can do some stuff before calling the
     * superclass.
     */
    private static JFormattedTextField createFormattedTextField(
            NumberFormat formatter) {
        final JFormattedTextField textField = new JFormattedTextField(
                new NumberEditorNumberFormat(formatter));
        /*
         * FIXME: I am sure there is a better way to do this, but I don't know
         * what it is. JTable sets up a binding for the ESCAPE key, but
         * JFormattedTextField overrides that binding with it's own. Remove the
         * JFormattedTextField binding.
         */
        InputMap map = textField.getInputMap();
        while (map != null) {
            map.remove(KeyStroke.getKeyStroke("pressed ESCAPE"));
            map = map.getParent();
        }
        /*
         * Set an input verifier to prevent the cell losing focus when the value
         * is invalid
         */
        textField.setInputVerifier(new InputVerifier() {
            public boolean verify(JComponent input) {
                JFormattedTextField ftf = (JFormattedTextField) input;
                return ftf.isEditValid();
            }
        });
        /*
         * The formatted text field will not call stopCellEditing() until the
         * value is valid. So do the red border thing here.
         */
        textField.addPropertyChangeListener("editValid",
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getNewValue() == Boolean.TRUE) {
                            ((JFormattedTextField) evt.getSource())
                                    .setBorder(new LineBorder(Color.black));
                        } else {
                            ((JFormattedTextField) evt.getSource())
                                    .setBorder(new LineBorder(Color.red));
                        }
                    }
                });
        return textField;
    }
}