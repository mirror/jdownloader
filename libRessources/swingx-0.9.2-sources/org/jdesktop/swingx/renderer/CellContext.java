/*
 * $Id: CellContext.java,v 1.10 2007/01/23 13:15:22 kleopatra Exp $
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
import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * Encapsulates the display context passed into the getXXRendererComponent.
 * <p>
 * 
 * Introduced to extract common state on which renderer configuration might
 * rely. Similar to the view part of ComponentAdapter - difference is that the
 * properties are not "live" dependent on the component but those passed-in are
 * used.
 * <p>
 * 
 * Additionally, provides lookup services to accessing state-dependent
 * ui-specific default visual properties (like colors, borders, icons).
 * Typically, they are taken from the UIManager or from the component, if
 * supported in the component api.
 * <p>
 * 
 * NOTE: the generic parameterization is useful to have a type-safe
 * installContext. Reason enough?
 * 
 * <ul>
 * 
 * <li>PENDING: still incomplete? how about Font?
 * <li>PENDING: protected methods? Probably need to open up - derived
 * properties should be accessible in client code.
 * </ul>
 * 
 * @author Jeanette Winzenburg
 */
public class CellContext<T extends JComponent> implements Serializable {

    /** the default border for unfocused cells. */
    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    /** ?? the default border for unfocused cells. ?? */
    private static final Border SAFE_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1,
            1);

    /**
     * Returns the shared border for unfocused cells.
     * <p>
     * PENDING: ?? copied from default renderers - why is it done like this?
     * 
     * @return the border for unfocused cells.
     */
    private static Border getNoFocusBorder() {
        if (System.getSecurityManager() != null) {
            return SAFE_NO_FOCUS_BORDER;
        } else {
            return noFocusBorder;
        }
    }

    protected transient T component;

    protected transient Object value;

    protected transient int row;

    protected transient int column;

    protected transient boolean selected;

    protected transient boolean focused;

    protected transient boolean expanded;

    protected transient boolean leaf;

    // --------------------------- install context

    /**
     * Sets state of the cell's context. Note that the component might be null
     * to indicate a cell without a concrete context. All accessors must cope
     * with.
     * 
     * @param component the component the cell resides on, might be null
     * @param value the content value of the cell
     * @param row the cell's row index in view coordinates
     * @param column the cell's column index in view coordinates
     * @param selected the cell's selected state
     * @param focused the cell's focused state
     * @param expanded the cell's expanded state
     * @param leaf the cell's leaf state
     */
    public void installContext(T component, Object value, int row, int column,
            boolean selected, boolean focused, boolean expanded, boolean leaf) {
        this.component = component;
        this.value = value;
        this.row = row;
        this.column = column;
        this.selected = selected;
        this.focused = focused;
        this.expanded = expanded;
        this.leaf = leaf;
    }

    // -------------------- accessors of installed state

    /**
     * Returns the component the cell resides on, may be null.
     * 
     * @return the component the cell resides on.
     */
    public T getComponent() {
        return component;
    }

    /**
     * Returns the value of the cell as set in the install.
     * 
     * @return the content value of the cell.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Returns the cell's row index in view coordinates as set in the install.
     * 
     * @return the cell's row index.
     */
    public int getRow() {
        return row;
    }

    /**
     * Returns the cell's column index in view coordinates as set in the
     * install.
     * 
     * @return the cell's column index.
     */
    public int getColumn() {
        return column;
    }

    /**
     * Returns the selected state as set in the install.
     * 
     * @return the cell's selected state.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Returns the focused state as set in the install.
     * 
     * @return the cell's focused state.
     */
    public boolean isFocused() {
        return focused;
    }

    /**
     * Returns the expanded state as set in the install.
     * 
     * @return the cell's expanded state.
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * Returns the leaf state as set in the install.
     * 
     * @return the cell's leaf state.
     */
    public boolean isLeaf() {
        return leaf;
    }

    // -------------------- accessors for derived state
    /**
     * Returns the cell's editability. Subclasses should override to return a
     * reasonable cell-related state.
     * <p>
     * 
     * Here: false.
     * 
     * @return the cell's editable property.
     */
    public boolean isEditable() {
        return false;
    }

    /**
     * Returns the icon. Subclasses should override to return a reasonable
     * cell-related state.
     * <p>
     * 
     * Here: <code>null</code>.
     * 
     * @return the cell's icon.
     */
    public Icon getIcon() {
        return null;
    }

    /**
     * Returns the foreground color of the renderered component or null if the
     * component is null
     * <p>
     * 
     * PENDING: fallback to UI properties if comp == null?
     * 
     * @return the background color of the rendered component.
     */
    protected Color getForeground() {
        return getComponent() != null ? getComponent().getForeground() : null;
    }

    /**
     * Returns the background color of the renderered component or null if the
     * component is null
     * <p>
     * 
     * PENDING: fallback to UI properties if comp == null?
     * 
     * @return the background color of the rendered component.
     */
    protected Color getBackground() {
        return getComponent() != null ? getComponent().getBackground() : null;
    }

    /**
     * Returns the default selection background color of the renderered
     * component. Typically, the color is LF specific. It's up to subclasses to
     * look it up. Here: returns null.
     * <p>
     * 
     * PENDING: return UI properties here?
     * 
     * @return the selection background color of the rendered component.
     */
    protected Color getSelectionBackground() {
        return null;
    }

    /**
     * Returns the default selection foreground color of the renderered
     * component. Typically, the color is LF specific. It's up to subclasses to
     * look it up. Here: returns null.
     * <p>
     * 
     * PENDING: return UI properties here?
     * 
     * @return the selection foreground color of the rendered component.
     */
    protected Color getSelectionForeground() {
        return null;
    }

    /**
     * Returns the default focus border of the renderered component. Typically,
     * the border is LF specific.
     * 
     * @return the focus border of the rendered component.
     */
    protected Border getFocusBorder() {
        Border border = null;
        if (isSelected()) {
            border = UIManager
                    .getBorder(getUIKey("focusSelectedCellHighlightBorder"));
        }
        if (border == null) {
            border = UIManager.getBorder(getUIKey("focusCellHighlightBorder"));
        }
        return border;
    }

    /**
     * Returns the default border of the renderered component depending on cell
     * state. Typically, the border is LF specific.
     * <p>
     * 
     * Here: returns the focus border if the cell is focused, the context
     * defined no focus border otherwise.
     * 
     * @return the default border of the rendered component.
     */
    protected Border getBorder() {
        if (isFocused()) {
            return getFocusBorder();
        }
        return getNoFocusBorder();
    }

    /**
     * Returns the default focused foreground color of the renderered component.
     * Typically, the color is LF specific.
     * 
     * @return the focused foreground color of the rendered component.
     */
    protected Color getFocusForeground() {
        return UIManager.getColor(getUIKey("focusCellForeground"));
    }

    /**
     * Returns the default focused background color of the renderered component.
     * Typically, the color is LF specific.
     * 
     * @return the focused background color of the rendered component.
     */
    protected Color getFocusBackground() {
        return UIManager.getColor(getUIKey("focusCellBackground"));
    }

    // ----------------------- convenience

    /**
     * Convenience method to build a component type specific lookup key for the
     * UIManager.
     * 
     * @param key the general part of the key
     * @return a composed key build of a component type prefix and the input.
     */
    protected String getUIKey(String key) {
        return getUIPrefix() + key;
    }

    /**
     * Returns the component type specific prefix of keys for lookup in the
     * UIManager. Subclasses must override, here: returns the empty String.
     * 
     * @return the component type specific prefix.
     */
    protected String getUIPrefix() {
        return "";
    }

}
