/*
 * $Id: ButtonProvider.java,v 1.9 2008/02/15 15:08:18 kleopatra Exp $
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

import javax.swing.AbstractButton;
import javax.swing.JLabel;

/**
 * A component provider which uses a AbstractButton.
 * <p>
 * 
 * This implementation respects a BooleanValue and a StringValue to configure
 * the button's selected and text property. By default, the selected is mapped
 * to a Boolean-type value and the text is empty.
 * <p>
 * 
 * To allow mapping to different types, client code can supply a custom
 * StringValue which also implements BooleanValue. F.i. to render a cell value
 * of type TableColumnExt with the column's visibility mapped to the selected
 * and the column's title to the text:
 * 
 * <pre><code>
 *            
 *     BooleanValue bv = new BooleanValue(){
 *        public boolean getBoolean(Object value) {
 *           if (value instanceof TableColumnExt) 
 *               return ((TableColumnExt) value).isVisible();
 *           return false;
 *        }
 *     };
 *     StringValue sv = new StringValue() {
 *         public String getString(Object value) {
 *           if (value instanceof TableColumnExt) {
 *               return ((TableColumnExt) value).getTitle();
 *           return &quot;&quot;;
 *         }
 *     };
 *     list.setCellRenderer(new DefaultListRenderer(
 *           new ButtonProvider(new MappedValue(sv, bv), JLabel.LEADING))); 
 * </code></pre>
 * 
 * PENDING: rename ... this is actually a CheckBoxProvider.
 * 
 * @see BooleanValue
 * @see StringValue
 * @see MappedValue
 * 
 * @author Jeanette Winzenburg
 */
public class ButtonProvider extends ComponentProvider<AbstractButton> {

    private boolean borderPainted;

    /**
     * Instantiates a ButtonProvider with default properties. <p> 
     *
     */
    public ButtonProvider() {
        this(null);
    }

    /**
     * Instantiates a ButtonProvider with the given StringValue and
     * alignment. 
     * 
     * @param stringValue the StringValue to use for formatting.
     * @param alignment the horizontalAlignment.
     */
    public ButtonProvider(StringValue stringValue, int alignment) {
        super(stringValue == null ? StringValue.EMPTY : stringValue, alignment);
        setBorderPainted(true);
    }

    /**
     * @param stringValue
     */
    public ButtonProvider(StringValue stringValue) {
        this(stringValue, JLabel.CENTER);
    }

    /**
     * Returns the border painted flag.
     * @return the borderpainted flag to use on the checkbox.
     * @see #setBorderPainted(boolean)
     */
    public boolean isBorderPainted() {
        return borderPainted;
    }

    /**
     * Sets the border painted flag. the underlying checkbox
     * is configured with this value on every request.<p>
     * 
     * The default value is true.
     * 
     * @param borderPainted the borderPainted property to configure
     *   the underlying checkbox with.
     *   
     *  @see #isBorderPainted() 
     */
    public void setBorderPainted(boolean borderPainted) {
        this.borderPainted = borderPainted;
    }

    /**
     * {@inheritDoc} <p>
     * Overridden to set the button's selected state and text.<p>
     * 
     *  PENDING: set icon?
     *  
     *  @see #getValueAsBoolean(CellContext)
     *  @see #getValueAsString(CellContext)
     */
    @Override
    protected void format(CellContext context) {
        rendererComponent.setSelected(getValueAsBoolean(context));
        rendererComponent.setText(getValueAsString(context));
    }

    /**
     * Returns a boolean representation of the content.<p>
     * 
     * This method messages the 
     * <code>BooleanValue</code> to get the boolean rep. If none available,
     * checks for Boolean type directly and returns its value. Returns
     * false otherwise. <p>
     * 
     * PENDING: fallback to check for boolean is convenient .. could cleanup
     *   to use a default BooleanValue instead.
     * 
     * @param context the cell context, must not be null.
     * @return a appropriate icon representation of the cell's content,
     *   or null if non if available.
     */
    protected boolean getValueAsBoolean(CellContext context) {
        if (formatter instanceof BooleanValue) {
            return ((BooleanValue) formatter).getBoolean(context.getValue());
        }
        return Boolean.TRUE.equals(context.getValue());
    }

    /**
     * {@inheritDoc}<p>
     * 
     * Here: set's the buttons horizontal alignment and borderpainted properties
     * to this controller's properties.
     */
    @Override
    protected void configureState(CellContext context) {
        rendererComponent.setBorderPainted(isBorderPainted());
        rendererComponent.setHorizontalAlignment(getHorizontalAlignment());
    }

    /**
     * {@inheritDoc}<p>
     * Here: returns a JCheckBox as rendering component.<p>
     * 
     */
    @Override
    protected AbstractButton createRendererComponent() {
        return new JRendererCheckBox();
    }

}
