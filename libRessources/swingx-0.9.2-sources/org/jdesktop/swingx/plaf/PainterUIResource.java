/*
 * $Id: PainterUIResource.java,v 1.2 2007/03/14 19:46:26 joshy Exp $
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

package org.jdesktop.swingx.plaf;

import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.plaf.UIResource;
import org.jdesktop.swingx.painter.Painter;

/**
 * An impl of Painter that implements UIResource.  UI
 * classes that create Painters should use this class.
 *
 * @author rbair
 */
public class PainterUIResource implements Painter<JComponent>, UIResource {
    private Painter p;
    
    /** Creates a new instance of PainterUIResource */
    public PainterUIResource(Painter p) {
        this.p = p;
    }
    
    public void paint(Graphics2D g, JComponent component, int width, int height) {
        if (p != null) {
            p.paint(g, component, width, height);
        }
    }
}
