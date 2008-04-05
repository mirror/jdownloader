/*
 * $Id: ScrollBarContextMenuSource.java,v 1.4 2005/10/10 18:02:13 rbair Exp $
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

import java.util.Map;

import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JScrollBar;

/**
 * @author Jeanette Winzenburg
 */
public class ScrollBarContextMenuSource extends ContextMenuSource {

    String[] keys = { /*null, null,  need to add scrollHere!*/ 
          "minScroll", "maxScroll",  
          null,  
          "negativeUnitIncrement", "positiveUnitIncrement",
          null,
          "negativeBlockIncrement", "positiveBlockIncrement",
    };
    
    String[] defaultValuesVertical = {
          "Top", "Bottom",
          null,
          "Scroll Up", "Scroll Down",
          null,
          "Page Up", "Page Down",
    };

    String[] defaultValuesHorizontal = {
            "Left Edge", "Right Edge",
            null,
            "Scroll Left", "Scroll Right",
            null,
            "Page Left", "Page Right",
      };
    
    private int orientation;

    public ScrollBarContextMenuSource(int orientation) {
        this.orientation = orientation;
    }

    public String[] getKeys() {
        // TODO Auto-generated method stub
        return keys;
    }

    public void updateActionEnabled(JComponent component, ActionMap map) {
        // TODO Auto-generated method stub

    }

    protected void initNames(Map<String, String> names) {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                names.put(keys[i],  getValue(keys[i], 
                        orientation == JScrollBar.VERTICAL ?
                                defaultValuesVertical[i] : defaultValuesHorizontal[i]));
            }
        }

    }

    protected String getResourcePrefix() {
        return "JScrollBar." + getOrientationToken();
    }

    private String getOrientationToken() {
        return orientation == JScrollBar.VERTICAL ? "vertical." : "horizontal.";
    }

}
