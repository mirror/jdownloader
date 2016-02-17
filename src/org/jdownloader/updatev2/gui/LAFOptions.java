package org.jdownloader.updatev2.gui;

import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.utils.Application;
import org.appwork.utils.logging2.extmanager.LoggerFactory;

public class LAFOptions {

    public static BooleanKeyHandler TABLE_ALTERNATE_ROW_HIGHLIGHT_ENABLED;
    public static IntegerKeyHandler CUSTOM_TABLE_ROW_HEIGHT;
    private static LAFOptions       INSTANCE;

    /**
     * Increase this value and set ColorForTableRowGap to show a gap between two links. Check CustomTableRowHeight as well
     **/
    // public static IntegerKeyHandler LINK_TABLE_HORIZONTAL_ROW_LINE_WEIGHT;

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

    private final LAFSettings           cfg;
    private static LookAndFeelExtension LAFEXTENSION;

    public static LookAndFeelExtension getLookAndFeelExtension() {
        return LAFEXTENSION;
    }

    public LAFSettings getCfg() {
        return cfg;
    }

    /**
     * Create a new instance of LAFOptions. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     *
     * @param laf
     */
    private LAFOptions(String laf) {
        final int i = laf.lastIndexOf(".");
        final String name = (i >= 0 ? laf.substring(i + 1) : laf);
        final String path = "cfg/laf/" + name;
        LookAndFeelExtension ext = null;
        if (!"org.jdownloader.gui.laf.jddefault.JDDefaultLookAndFeel".equals(laf)) {
            try {
                ext = (LookAndFeelExtension) Class.forName(laf + "Extension").newInstance();
            } catch (Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
            }
        }
        if (ext == null) {
            ext = new DefaultLookAndFeelExtension();
        }

        LAFEXTENSION = ext;
        cfg = JsonConfig.create(Application.getResource(path), LAFSettings.class);

    }

    public LookAndFeelExtension getExtension() {
        return LAFEXTENSION;
    }

    public synchronized static void init(String laf) {
        if (INSTANCE != null) {
            return;
        }
        INSTANCE = new LAFOptions(laf);
        // LINK_TABLE_HORIZONTAL_ROW_LINE_WEIGHT = INSTANCE.getCfg()._getStorageHandler().getKeyHandler("LinkTableHorizontalRowLineWeight",
        // IntegerKeyHandler.class);
        CUSTOM_TABLE_ROW_HEIGHT = INSTANCE.getCfg()._getStorageHandler().getKeyHandler("CustomTableRowHeight", IntegerKeyHandler.class);
        TABLE_ALTERNATE_ROW_HIGHLIGHT_ENABLED = INSTANCE.getCfg()._getStorageHandler().getKeyHandler("TableAlternateRowHighlightEnabled", BooleanKeyHandler.class);
    }

    private static HashMap<String, Color> CACHE = new HashMap<String, Color>();

    private static String hex(int alpha) {
        String ret = Integer.toHexString(alpha);
        while (ret.length() < 2) {
            ret = "0" + ret;
        }
        return ret;
    }

    public static String toHex(Color c) {
        return hex(c.getAlpha()) + hex(c.getRed()) + hex(c.getGreen()) + hex(c.getBlue());
    }

    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(LAFSettings.class);
    }

    public static Color createColor(String str) {
        // no synch required. in worth case we create the color twice
        synchronized (CACHE) {
            Color ret = CACHE.get(str);
            if (ret != null) {
                return ret;
            }
            try {

                if (str == null) {
                    return null;
                }
                str = str.toLowerCase(Locale.ENGLISH);
                if (str.startsWith("0x")) {
                    str = str.substring(2);
                }
                if (str.startsWith("#")) {
                    str = str.substring(1);
                }
                if (str.length() < 6) {
                    return null;
                }
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

    }

    public void applyConfigDescriptionTextColor(JLabel lbl) {

        Color c = createColor(cfg.getColorForConfigPanelDescriptionText());
        if (c != null) {
            lbl.setForeground(c);

        }

    }

    public boolean applyConfigLabelEnabledTextColor(JLabel lbl) {
        Color c = createColor(cfg.getConfigLabelEnabledTextColor());
        if (c != null) {
            lbl.setForeground(c);
            return true;
        }
        return false;
    }

    public boolean applyConfigLabelDisabledTextColor(JLabel lbl) {

        Color c = createColor(cfg.getConfigLabelDisabledTextColor());
        if (c != null) {
            lbl.setForeground(c);
            return true;
        }
        return false;
    }

    public void applyConfigHeaderTextColor(JLabel lbl) {

        Color c = createColor(cfg.getColorForConfigHeaderTextColor());
        if (c != null) {
            lbl.setForeground(c);

        }

    }

    public void applyBackground(String color, JComponent field) {
        final Color col = createColor(color);
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

    public Color getColorForPanelBackground() {
        return createColor(cfg.getColorForPanelBackground());
    }

    public static void applyBackground(Color color, JComponent field) {
        field.setBackground(color);
        field.setOpaque(true);
    }

    public Color getColorForTooltipForeground() {
        return createColor(cfg.getColorForTooltipForeground());
    }

    public boolean isPaintStatusbarTopBorder() {
        return cfg.isPaintStatusbarTopBorder();
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

    public Color getColorForPanelHeaderForeGround() {
        return createColor(cfg.getColorForPanelHeaderForeground());
    }

    public Color getColorForSpeedMeterAverage() {
        return createColor(cfg.getColorForSpeedMeterAverage());
    }

    public Color getColorForSpeedMeterText() {
        return createColor(cfg.getColorForSpeedMeterText());
    }

    public Color getColorForSpeedMeterAverageText() {
        return createColor(cfg.getColorForSpeedMeterAverageText());
    }

    public Color getColorForSpeedmeterCurrentBottom() {
        return createColor(cfg.getColorForSpeedmeterCurrentBottom());
    }

    public Color getColorForSpeedmeterCurrentTop() {
        return createColor(cfg.getColorForSpeedmeterCurrentTop());
    }

    public Color getColorForSpeedmeterLimiterBottom() {
        return createColor(cfg.getColorForSpeedmeterLimiterBottom());
    }

    public Color getColorForSpeedmeterLimiterTop() {
        return createColor(cfg.getColorForSpeedmeterLimiterTop());
    }

    public Color getColorForTableAccountErrorRowForeground() {
        return createColor(cfg.getColorForTableAccountErrorRowForeground());
    }

    public Color getColorForTableAccountErrorRowBackground() {
        return createColor(cfg.getColorForTableAccountErrorRowBackground());
    }

    public Color getColorForTableAccountTempErrorRowForeground() {
        return createColor(cfg.getColorForTableAccountTempErrorRowForeground());
    }

    public Color getColorForTableAccountTempErrorRowBackground() {
        return createColor(cfg.getColorForTableAccountTempErrorRowBackground());
    }

    public Color getColorForProgressbar1() {
        return createColor(cfg.getColorForProgressbarForeground1());
    }

    public Color getColorForProgressbar2() {
        return createColor(cfg.getColorForProgressbarForeground2());
    }

    public Color getColorForProgressbar3() {
        return createColor(cfg.getColorForProgressbarForeground3());
    }

    public Color getColorForProgressbar4() {
        return createColor(cfg.getColorForProgressbarForeground4());
    }

    public Color getColorForProgressbar5() {
        return createColor(cfg.getColorForProgressbarForeground5());
    }

    public Color getColorForLinkgrabberDupeHighlighter() {
        return createColor(cfg.getColorForLinkgrabberDupeHighlighter());
    }

    public Color getColorForTableRowGap() {
        return createColor(cfg.getColorForTableRowGap());
    }

    public Color getColorForPanelBorders() {
        return createColor(cfg.getColorForPanelBorders());
    }

}
