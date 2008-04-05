/*
 * $Id: ContextMenuAuxScrollBarUI.java,v 1.4 2005/10/13 08:59:57 kleopatra Exp $
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

import java.awt.Graphics;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ScrollBarUI;

/**
 * @author Jeanette Winzenburg
 */
public class ContextMenuAuxScrollBarUI extends ScrollBarUI {
    
    private MouseListener mouseHandler;
    private JScrollBar scrollBar;
    
    public static ComponentUI createUI(JComponent c) {
        return new ContextMenuAuxScrollBarUI(); 
    }

    // PENDING: need to listen to orientation changes
    // 
    public void installUI(JComponent comp) {
        this.scrollBar = (JScrollBar) comp;
        comp.addMouseListener(getMouseListener());
    }

    // PENDING: need to cleanup references - 
    // DelegateAction holds a reference to the comp!
    public void uninstallUI(JComponent comp) {
        comp.removeMouseListener(getMouseListener());
        this.scrollBar = null;
    }



    private MouseListener getMouseListener() {
        if (mouseHandler == null) {
            mouseHandler = createPopupHandler();
        }
        return mouseHandler;
    }

    private MouseListener createPopupHandler() {
        return new ContextMenuHandler(createContextSource());
    }

    private ContextMenuSource createContextSource() {
        return new ScrollBarContextMenuSource(scrollBar.getOrientation());
    }

    public void update(Graphics g, JComponent c) {
    }
    

}
