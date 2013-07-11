package org.jdownloader.gui.laf.jddefault;

import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;

public class LAFOptions {

    private static LAFOptions INSTANCE;

    /**
     * get the only existing instance of LAFOptions. This is a singleton
     * 
     * @return
     */
    public static LAFOptions getInstance() {
        if (INSTANCE == null) {
            //
            throw new WTFException("LAFOptions Not initialized yet");
        }
        return LAFOptions.INSTANCE;
    }

    private LAFSettings cfg;

    public LAFSettings getCfg() {
        return cfg;
    }

    /**
     * Create a new instance of LAFOptions. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private LAFOptions(String laf) {
        int i = laf.lastIndexOf(".");
        String path = "cfg/laf/" + (i >= 0 ? laf.substring(i + 1) : laf);
        cfg = JsonConfig.create(Application.getResource(path), LAFSettings.class);

    }

    public synchronized static void init(String laf) {
        if (INSTANCE != null) return;
        INSTANCE = new LAFOptions(laf);

    }

    private static HashMap<String, Color> CACHE = new HashMap<String, Color>();

    private static String hex(int alpha) {
        String ret = Integer.toHexString(alpha);
        while (ret.length() < 2)
            ret = "0" + ret;
        return ret;
    }

    public static String toHex(Color c) {

        return hex(c.getAlpha()) + hex(c.getRed()) + hex(c.getGreen()) + hex(c.getBlue());
    }

    public static void main(String[] args) {
        System.out.println(0x474747);
    }

    public static Color createColor(String str) {
        // no synch required. in worth case we create the color twice
        Color ret = CACHE.get(str);
        if (ret != null) return ret;
        try {

            if (str == null) return null;
            str = str.toLowerCase(Locale.ENGLISH);
            if (str.startsWith("0x")) str = str.substring(2);
            if (str.startsWith("#")) str = str.substring(1);
            if (str.length() < 6) return null;
            // add alpha channel
            while (str.length() < 8) {
                str = "F" + str;
            }
            long rgb = Long.parseLong(str, 16);

            ret = new Color((int) rgb, true);
            CACHE.put(str, ret);
            return ret;
        } catch (Exception e) {
            return null;
        }

    }

    public void applyHeaderColorBackground(JLabel lbl) {

        Color c = createColor(cfg.getColorForPanelHeaderForeground());
        if (c != null) {
            lbl.setForeground(c);

        }
    }

    public void applyBackground(String color, JComponent field) {

        Color col = createColor(color);
        if (col != null) {
            applyBackground(col, field);
        }
    }

    public void applyPanelBackground(JComponent rightPanel) {
        applyBackground(cfg.getColorForPanelBackground(), rightPanel);

    }

    public Color getColorForPanelHeaderBackground() {
        return createColor(cfg.getColorForPanelHeaderBackground());
    }

    public int[] getPopupBorderInsets() {
        return cfg.getPopupBorderInsets();
    }

    public Color getColorForPanelBackground() {
        return createColor(cfg.getColorForPanelBackground());
    }

    public static void applyBackground(Color color, JComponent field) {

        field.setBackground(color);
        field.setOpaque(true);
    }

    public Color getColorForPanelHeaderLine() {
        return createColor(cfg.getColorForPanelHeaderLine());
    }

    public Color getColorForTooltipForeground() {
        return createColor(cfg.getColorForTooltipForeground());
    }

    public boolean isPaintStatusbarTopBorder() {
        return cfg.isPaintStatusbarTopBorder();
    }

    public Class<?> getMenuBackgroundPainterClass() {
        try {
            return Class.forName(cfg.getMenuBackgroundPainterClass());
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(LAFSettings.DE_JAVASOFT_PLAF_SYNTHETICA_SIMPLE2D_MENU_PAINTER);
            } catch (ClassNotFoundException e1) {
                throw new WTFException("No Synthetica Loaded");
            }
        }
    }

    public Color getColorForErrorForeground() {
        return createColor(cfg.getColorForErrorForeground());
    }

    public Color getColorForTableSelectedRowsForeground() {
        return createColor(cfg.getColorForTableSelectedRowsForeground());
    }

    public Color getColorForTableSelectedRowsBackground() {
        return createColor(cfg.getColorForTableSelectedRowsBackground());
    }

    public Color getColorForTableMouseOverRowForeground() {
        return createColor(cfg.getColorForTableMouseOverRowForeground());
    }

    public Color getColorForTableMouseOverRowBackground() {
        return createColor(cfg.getColorForTableMouseOverRowBackground());
    }

    public Color getColorForTableAlternateRowForeground() {
        return createColor(cfg.getColorForTableAlternateRowForeground());
    }

    public Color getColorForTableAlternateRowBackground() {
        return createColor(cfg.getColorForTableAlternateRowBackground());
    }

    public Color getColorForScrollbarsNormalState() {
        return createColor(cfg.getColorForScrollbarsNormalState());
    }

    public Color getColorForScrollbarsMouseOverState() {
        return createColor(cfg.getColorForScrollbarsMouseOverState());
    }

    public Color getColorForTableSortedColumnView() {
        return createColor(cfg.getColorForTableSortedColumnView());
    }

    public Color getColorForTableFilteredView() {
        return createColor(cfg.getColorForTableFilteredView());
    }

    public Color getColorForTablePackageRowForeground() {
        return createColor(cfg.getColorForTablePackageRowForeground());
    }

    public Color getColorForTablePackageRowBackground() {
        return createColor(cfg.getColorForTablePackageRowBackground());
    }
}
