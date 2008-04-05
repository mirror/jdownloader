/*
 * $Id: IconValue.java,v 1.3 2008/02/20 13:44:44 kleopatra Exp $
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

import java.io.Serializable;

import javax.swing.Icon;

import org.jdesktop.swingx.icon.EmptyIcon;

/**
 * A simple converter to return a Icon representation of an Object.
 * <p>
 * 
 * This class is intended to be the "small coin" to configure/format icon cell
 * content of concrete subclasses of <code>ComponentProvider</code>.
 * <p>
 * 
 * 
 * NOTE: this is experimental, most probably will change. A (near) future
 * version with change the signature of the getIcon method to
 * 
 * <pre><code>
 * Icon getIcon(Object value, IconType type);
 * </code></pre>
 * 
 * That will allow a more fine-grained control of custom icons in tree rendering.
 * 
 * @author Jeanette Winzenburg
 */
public interface IconValue extends Serializable {
    
    /**
     * The cell type the icon is used for.
     */
    public enum IconType {
        
        LEAF,
        
        OPEN_FOLDER,
        
        CLOSED_FOLDER
        
    }
    
    /**
     * A marker icon used to indicate a null. 
     * 
     */
    public final static Icon NULL_ICON = new EmptyIcon();
    
    /**
     * Returns the value as Icon if possible or null.
     */
    public static final IconValue ICON = new IconValue() {

        public Icon getIcon(Object value) {
            if (value instanceof Icon) {
                return (Icon) value;
            }
            return null;
        }
        
    };
    
    /**
     * Always null. 
     * 
     * @deprecated use {@link #NONE} 
     */
    @Deprecated
    public static final IconValue EMPTY = new IconValue() {

        public Icon getIcon(Object value) {
            return null;
        }
        
    };
    
    
    /**
     * Always NULL_ICON. This is useful to indicate that we really want
     * no icon instead of f.i. a default provided by the CellContext. 
     */
    public static final IconValue NONE = new IconValue() {

        public Icon getIcon(Object value) {
            return NULL_ICON;
        }
        
    };
    
    /**
     * Returns a icon representation of the given value.
     * 
     * @param value the object to present as Icon
     * @return a Icon representation of the given value, 
     *  may be null if none available.
     *  
     */
    Icon getIcon(Object value);

}
