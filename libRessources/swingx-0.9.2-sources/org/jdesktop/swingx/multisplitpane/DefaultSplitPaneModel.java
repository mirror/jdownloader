/*
 * DefaultSplitPaneLayout.java
 *
 * Created on November 21, 2006, 10:41 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.multisplitpane;

import org.jdesktop.swingx.MultiSplitLayout.Divider;
import org.jdesktop.swingx.MultiSplitLayout.Leaf;
import org.jdesktop.swingx.MultiSplitLayout.Split;
/**
 * A simplified SplitPaneLayout for common split pane needs. A common multi splitpane
 * need is:
 *
 * +-----------+-----------+
 * |           |           |
 * |           +-----------+
 * |           |           |
 * +-----------+-----------+
 *
 * @author rbair
 */
public class DefaultSplitPaneModel extends Split {
    public static final String LEFT = "left";
    public static final String TOP = "top";
    public static final String BOTTOM = "bottom";
    
    /** Creates a new instance of DefaultSplitPaneLayout */
    public DefaultSplitPaneModel() {
        Split row = new Split();
        Split col = new Split();
        col.setRowLayout(false);
        setChildren(new Leaf(LEFT), new Divider(), col);
        col.setChildren(new Leaf(TOP), new Divider(), new Leaf(BOTTOM));
    }
    
}
