/*
 * $Id: Resize.java,v 1.1 2006/03/24 02:36:09 rbair Exp $
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
package org.jdesktop.swingx.util;

/**
 * Indicates how resizing of a gradient paint should occur for a gradient based
 * painter. If HORIZONTAL, then the control point (or whatnot) will be resized
 * horizontally in coordination with the width of the component. If VERTICAL
 * then the control point (or whatnot) of the gradient paint will be resized
 * vertically in coordination with the height of the component. If BOTH then
 * both VERTICAL and HORIZONTAL behavior will occur. NONE of course, results in
 * no resizing whatsoever.
 *
 * @author rbair
 */
public enum Resize {
    HORIZONTAL,
    VERTICAL,
    BOTH,
    NONE
}
