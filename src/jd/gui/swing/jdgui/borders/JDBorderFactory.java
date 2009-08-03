/**
 * Based on jtattoo BorderFactory
 */

package jd.gui.swing.jdgui.borders;

import javax.swing.border.Border;

public class JDBorderFactory {

    public static Border createInsideShadowBorder(int top, int left, int bottom, int right) {

        return new InsideShadowBorder(top, left, bottom, right);
    }
}
