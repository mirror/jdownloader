package jd.gui.swing.laf.ext.jtattoo.acryl.themes;

import javax.swing.UIManager;

public class AcrylJDTheme extends com.jtattoo.plaf.acryl.AcrylDefaultTheme {
    /**
     * Overrides original alpha value to support blured menus
     */
    @Override
    public float getMenuAlpha() {
        try {
            Object def = UIManager.getDefaults().get("PopupMenuAlpha");
            if (def == null) { return 0.7f; }

            return (Float) def;
        } catch (Throwable e) {
            return 0.7f;
        }

    }
}
