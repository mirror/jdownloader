/**
 * Based on jtattoo BorderFactory
 */

package jd.gui.skins.simple.components.borders;

import javax.swing.Icon;
import javax.swing.border.Border;

public class JDBorderFactory {

    public static Border createTitleBorder(Icon icon) {
        return new TitleBorder(icon, "", 3, 4);
    }

    public static Border createTitleBorder(String title) {
        return new TitleBorder(null, title, 3, 4);
    }

    public static Border createTitleBorder(Icon icon, String title) {
        return new TitleBorder(icon, title, 3, 4);
    }

    public static Border createTitleBorder(Icon icon, String title, int shadowSize) {
        return new TitleBorder(icon, title, Math.max(Math.min(shadowSize, 32), 0), 4);
    }

    public static Border createTitleBorder(Icon icon, String title, int shadowSize, int innerSpace) {
        return new TitleBorder(icon, title, Math.max(Math.min(shadowSize, 32), 0), Math.max(Math.min(innerSpace, 32), 0));
    }

    public static Border createLineTitleBorder(Icon icon, String text) {
    
        return new LineTitleBorder(icon, text, 2);
    }

    public static Border createInsideShadowBorder(int top, int left, int bottom, int right) {
        // TODO Auto-generated method stub
        return new InsideShadowBorder(top,left,bottom,right);
    }
}
