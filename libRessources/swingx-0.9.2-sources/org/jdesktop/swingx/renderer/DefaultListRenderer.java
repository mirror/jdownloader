/*
 * $Id: DefaultListRenderer.java,v 1.16 2008/02/25 09:27:26 kleopatra Exp $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
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
 *
 */
package org.jdesktop.swingx.renderer;


import java.awt.Color;
import java.awt.Component;
import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.jdesktop.swingx.RolloverRenderer;


/**
 * Adapter to glue SwingX renderer support to core api. It has convenience
 * constructors to create a LabelProvider, optionally configured with a
 * StringValue and horizontal alignment. Typically, client code does not
 * interact with this class except at instantiation time.
 * <p>
 * 
 * Note: core DefaultListCellRenderer shows either an icon or the element's
 * toString representation, depending on whether or not the given value given
 * value is of type icon or implementors. The empty/null provider taking
 * constructor takes care of configuring the default provider with a converter
 * which mimics that behaviour. When using of the converter taking constructors,
 * it's up to the client code to supply the appropriate converter, if needed:
 * 
 * 
 * <pre><code>
 * StringValue sv = new StringValue() {
 * 
 *     public String getString(Object value) {
 *         if (value instanceof Icon) {
 *             return &quot;&quot;;
 *         }
 *         return StringValue.TO_STRING.getString(value);
 *     }
 * 
 * };
 * StringValue lv = new MappedValue(sv, IconValue.ICON);
 * listRenderer = new DefaultListRenderer(lv, alignment);
 * 
 * </code></pre>
 * 
 * <p>
 * 
 * PENDING: better support core consistent icon handling? 
 * 
 * @author Jeanette Winzenburg
 * 
 * @see ComponentProvider
 * @see StringValue
 * @see IconValue
 * @see MappedValue
 * 
 * 
 */
public class DefaultListRenderer 
    implements ListCellRenderer, RolloverRenderer, StringValue,
        Serializable {

    protected ComponentProvider componentController;

    protected CellContext<JList> cellContext;

    /**
     * Instantiates a default list renderer with the default component
     * provider.
     *
     */
    public DefaultListRenderer() {
        this((ComponentProvider) null);
    }

    /**
     * Instantiates a ListCellRenderer with the given ComponentProvider.
     * If the provider is null, creates and uses a default. The default
     * provider is of type <code>LabelProvider</code><p>
     * 
     * Note: the default provider is configured with a custom StringValue
     * which behaves exactly as core DefaultListCellRenderer: depending on 
     * whether or not given value is of type icon or implementors, it shows 
     * the icon or the element's toString.  
     * 
     * @param componentProvider the provider of the configured component to
     *   use for cell rendering
     */
    public DefaultListRenderer(ComponentProvider componentProvider) {
        if (componentProvider == null) {
            componentProvider = new LabelProvider(createDefaultStringValue());
        }
        this.componentController = componentProvider;
        this.cellContext = new ListCellContext();
    }

    /**
     * Creates and returns the default StringValue for a JList.<p>
     * This is added to keep consistent with core list rendering which
     * shows either the Icon (for Icon value types) or the default 
     * to-string for non-icon types.
     * 
     * @return the StringValue to use by default.
     */
    private StringValue createDefaultStringValue() {
        StringValue sv = new StringValue() {

            public String getString(Object value) {
                if (value instanceof Icon) {
                    return "";
                }
                return StringValue.TO_STRING.getString(value);
            }

        };
        return new MappedValue(sv, IconValue.ICON);
    }

    /**
     * Instantiates a default table renderer with a default component controller
     * using the given converter.<p>
     * 
     * PENDING JW: how to guarantee core consistent icon handling? Leave to 
     * client code?
     * 
     * @param converter the converter to use for mapping the content value to a
     *        String representation.
     * 
     */
    public DefaultListRenderer(StringValue converter) {
        this(new LabelProvider(converter));
    }

    /**
     * Instantiates a default list renderer with a default component
     * controller using the given converter and horizontal 
     * alignment. 
     * 
     * PENDING JW: how to guarantee core consistent icon handling? Leave to
     * client code?
     * 
     * 
     * @param converter the converter to use for mapping the
     *   content value to a String representation.
     * @param alignment the horizontal alignment.
     */
    public DefaultListRenderer(StringValue converter, int alignment) {
        this(new LabelProvider(converter, alignment));
    }

    // -------------- implements javax.swing.table.ListCellRenderer
    /**
     * 
     * Returns a configured component, appropriate to render the given
     * list cell.  
     * 
     * @param list the <code>JList</code> to render on
     * @param value the value to assign to the cell 
     * @param isSelected true if cell is selected
     * @param cellHasFocus true if cell has focus
     * @param index the row index (in view coordinates) of the cell to render
     * @return a component to render the given list cell.
     */
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        cellContext.installContext(list, value, index, 0, isSelected,
                cellHasFocus, true, true);
        return componentController.getRendererComponent(cellContext);
    }

    /**
     * @param background
     */
    public void setBackground(Color background) {
        componentController.getRendererController().setBackground(background);

    }

    /**
     * @param foreground
     */
    public void setForeground(Color foreground) {
        componentController.getRendererController().setForeground(foreground);
    }

    //----------------- RolloverRenderer

    /**
     * {@inheritDoc}
     */
    public void doClick() {
        if (isEnabled()) {
            ((RolloverRenderer) componentController).doClick();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEnabled() {
        return (componentController instanceof RolloverRenderer)
                && ((RolloverRenderer) componentController).isEnabled();
    }

 // ------------ implement StringValue
    
    /**
     * {@inheritDoc}
     */
    public String getString(Object value) {
        return componentController.getString(value);
    }


}


