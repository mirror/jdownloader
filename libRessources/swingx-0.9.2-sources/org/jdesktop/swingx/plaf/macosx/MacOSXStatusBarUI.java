/*
 * $Id: MacOSXStatusBarUI.java,v 1.5 2007/02/08 05:12:15 rbair Exp $
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

package org.jdesktop.swingx.plaf.macosx;

import java.awt.FlowLayout;
import java.awt.LayoutManager;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.StatusBarUI;

/**
 *
 * @author rbair
 */
public class MacOSXStatusBarUI extends StatusBarUI {
    /**
     * The one and only JXStatusBar for this UI delegate
     */
    private JXStatusBar statusBar;
    
    /** Creates a new instance of BasicStatusBarUI */
    public MacOSXStatusBarUI() {
    }
    
    /**
     * Returns an instance of the UI delegate for the specified component.
     * Each subclass must provide its own static <code>createUI</code>
     * method that returns an instance of that UI delegate subclass.
     * If the UI delegate subclass is stateless, it may return an instance
     * that is shared by multiple components.  If the UI delegate is
     * stateful, then it should return a new instance per component.
     * The default implementation of this method throws an error, as it
     * should never be invoked.
     */
    public static ComponentUI createUI(JComponent c) {
        return new MacOSXStatusBarUI();
    }	
    
    /**
     * Configures the specified component appropriate for the look and feel.
     * This method is invoked when the <code>ComponentUI</code> instance is being installed
     * as the UI delegate on the specified component.  This method should
     * completely configure the component for the look and feel,
     * including the following:
     * <ol>
     * <li>Install any default property values for color, fonts, borders,
     *     icons, opacity, etc. on the component.  Whenever possible, 
     *     property values initialized by the client program should <i>not</i> 
     *     be overridden.
     * <li>Install a <code>LayoutManager</code> on the component if necessary.
     * <li>Create/add any required sub-components to the component.
     * <li>Create/install event listeners on the component.
     * <li>Create/install a <code>PropertyChangeListener</code> on the component in order
     *     to detect and respond to component property changes appropriately.
     * <li>Install keyboard UI (mnemonics, traversal, etc.) on the component.
     * <li>Initialize any appropriate instance data.
     * </ol>
     * @param c the component where this UI delegate is being installed
     *
     * @see #uninstallUI
     * @see javax.swing.JComponent#setUI
     * @see javax.swing.JComponent#updateUI
     */
    @Override
    public void installUI(JComponent c) {
        assert c instanceof JXStatusBar;
        statusBar = (JXStatusBar)c;
        
        Border b = statusBar.getBorder();
        if (b == null || b instanceof UIResource) {
            statusBar.setBorder(new BorderUIResource(BorderFactory.createEmptyBorder(4, 5, 4, 22)));
        }
        
        LayoutManager m = statusBar.getLayout();
        if (m == null) {
            statusBar.setLayout(new FlowLayout());
        }
        installDefaults();
    }
    
    protected void installDefaults() {
        statusBar.setOpaque(false);
    }

    /**
     * Reverses configuration which was done on the specified component during
     * <code>installUI</code>.  This method is invoked when this 
     * <code>UIComponent</code> instance is being removed as the UI delegate 
     * for the specified component.  This method should undo the
     * configuration performed in <code>installUI</code>, being careful to 
     * leave the <code>JComponent</code> instance in a clean state (no 
     * extraneous listeners, look-and-feel-specific property objects, etc.).
     * This should include the following:
     * <ol>
     * <li>Remove any UI-set borders from the component.
     * <li>Remove any UI-set layout managers on the component.
     * <li>Remove any UI-added sub-components from the component.
     * <li>Remove any UI-added event/property listeners from the component.
     * <li>Remove any UI-installed keyboard UI from the component.
     * <li>Nullify any allocated instance data objects to allow for GC.
     * </ol>
     * @param c the component from which this UI delegate is being removed;
     *          this argument is often ignored,
     *          but might be used if the UI object is stateless
     *          and shared by multiple components
     *
     * @see #installUI
     * @see javax.swing.JComponent#updateUI
     */
    @Override
    public void uninstallUI(JComponent c) {
        assert c instanceof JXStatusBar;
    }
}
