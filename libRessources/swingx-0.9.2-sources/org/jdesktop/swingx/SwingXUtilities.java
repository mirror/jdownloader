/*
 * $Id: SwingXUtilities.java,v 1.3 2008/01/17 12:15:24 kleopatra Exp $
 *
 * Copyright 2008 Sun Microsystems, Inc., 4150 Network Circle,
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
package org.jdesktop.swingx;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Window;
import java.util.Locale;

import javax.swing.MenuElement;
import javax.swing.SwingUtilities;

/**
 * A collection of utility methods for Swing(X) classes.
 * 
 * <ul>
 * PENDING JW: think about location of this class and/or its methods, Options:
 * 
 *  <li> move this class to the swingx utils package which already has a bunch of xxUtils
 *  <li> move methods between xxUtils classes as appropriate (one window/comp related util)
 *  <li> keep here in swingx (consistent with swingutilities in core)
 * </ul>
 * @author Karl George Schaefer
 */
public final class SwingXUtilities {
    private SwingXUtilities() {
        //does nothing
    }
    
    
    
    private static Component[] getChildren(Component c) {
        Component[] children = null;
        
        if (c instanceof MenuElement) {
            MenuElement[] elements = ((MenuElement) c).getSubElements();
            children = new Component[elements.length];
            
            for (int i = 0; i < elements.length; i++) {
                children[i] = elements[i].getComponent();
            }
        } else if (c instanceof Container) {
            children = ((Container) c).getComponents();
        }
        
        return children;
    }
    
    /**
     * Enables or disables of the components in the tree starting with {@code c}.
     * 
     * @param c
     *                the starting component
     * @param enabled
     *                {@code true} if the component is to enabled; {@code false} otherwise
     */
    public void setComponentTreeEnabled(Component c, boolean enabled) {
        c.setEnabled(enabled);
        
        Component[] children = getChildren(c);
            
        if (children != null) {
            for(int i = 0; i < children.length; i++) {
                setComponentTreeEnabled(children[i], enabled);
            }
        }
    }
    
    /**
     * Sets the locale for an entire component hierarchy to the specified
     * locale.
     * 
     * @param c
     *                the starting component
     * @param locale
     *                the locale to set
     */
    public void setComponentTreeLocale(Component c, Locale locale) {
        c.setLocale(locale);
        
        Component[] children = getChildren(c);
        
        if (children != null) {
            for(int i = 0; i < children.length; i++) {
                setComponentTreeLocale(children[i], locale);
            }
        }
    }



    /**
     * Updates the componentTreeUI of all toplevel windows of the 
     * current application.
     * 
     */
    public static void updateAllComponentTreeUIs() {
        for (Frame frame : Frame.getFrames()) {
            updateAllComponentTreeUIs(frame);
        }
        
    }



    /**
     * Updates the componentTreeUI of the given window and all its
     * owned windows, recursively.
     * 
     * 
     * @param window the window to update
     */
    public static void updateAllComponentTreeUIs(Window window) {
        SwingUtilities.updateComponentTreeUI(window);
        for (Window owned : window.getOwnedWindows()) {
            updateAllComponentTreeUIs(owned);
        }
    }
}
