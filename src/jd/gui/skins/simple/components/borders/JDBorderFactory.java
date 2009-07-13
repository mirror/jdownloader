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
}
