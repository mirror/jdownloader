package jd.gui.swing.laf;

import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.storage.Storable;
import org.appwork.storage.config.handler.StorageHandler;
import org.jdownloader.settings.LAFSettings;

public class LAFOptions implements Storable, LAFSettings {
    private static final String           BLACK = "FF000000";
    private static HashMap<String, Color> CACHE = new HashMap<String, Color>();

    private static String hex(int alpha) {
        String ret = Integer.toHexString(alpha);
        while (ret.length() < 2)
            ret = "0" + ret;
        return ret;
    }

    public static void main(String[] args) {
        System.out.println(0x474747);
    }

    public static String toHex(Color c) {

        return hex(c.getAlpha()) + hex(c.getRed()) + hex(c.getGreen()) + hex(c.getBlue());
    }

    private String  colorForDownloadOverviewHeader    = null;
    private String  colorForErrorForeground           = "FFFF0000";

    private String  colorForSortedColumnView          = toHex(Color.ORANGE);

    private String  colorForFilteredTableView         = toHex(Color.GREEN);

    private String  menuBackgroundPainterClass        = "de.javasoft.plaf.synthetica.simple2D.MenuPainter";

    private boolean paintStatusbarTopBorder           = true;

    private String  colorForPanelBackground           = toHex(new JTable().getBackground());

    private String  colorForPanelHeader               = toHex(new JTableHeader().getBackground());

    private String  colorForPanelHeaderForeground     = BLACK;
    private String  colorForPanelHeaderLine           = toHex(Color.LIGHT_GRAY);

    private int[]   popupBorderInsets                 = new int[] { 0, 0, 0, 0 };

    private String  colorForTablePackageRowBackground = "FFDEE7ED";
    private String  colorForTablePackageRowForeground = BLACK;
    private String  colorForTooltipForeground         = "ffcccccc";
    private String  colorForSelectedRowsForeground    = BLACK;
    private String  colorForSelectedRowsBackground    = "ffCAE8FA";
    private String  colorForMouseOverRowBackground    = "ccCAE8FA";
    private String  colorForMouseOverRowForeground    = null;
    private String  colorForAlternateRowForeground    = null;
    private String  colorForAlternateRowBackground    = "03000000";

    public LAFOptions() {
        // empty cosntructor required for Storable
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

    public void applyDownloadOverviewHeaderColor(JLabel lbl) {

        Color c = createColor(getColorForDownloadOverviewHeader());
        if (c != null) {
            lbl.setForeground(c);

        }
    }

    public String getColorForDownloadOverviewHeader() {
        return colorForDownloadOverviewHeader;
    }

    public String getColorForErrorForeground() {
        return colorForErrorForeground;
    }

    public String getColorForSortedColumnView() {
        return colorForSortedColumnView;
    }

    public String getColorForFilteredTableView() {
        return colorForFilteredTableView;
    }

    public String getMenuBackgroundPainterClass() {
        return menuBackgroundPainterClass;
    }

    public String getColorForPanelBackground() {
        return colorForPanelBackground;
    }

    public String getColorForPanelHeader() {
        return colorForPanelHeader;
    }

    public String getColorForPanelHeaderForeground() {
        return colorForPanelHeaderForeground;
    }

    public String getColorForPanelHeaderLine() {
        return colorForPanelHeaderLine;
    }

    public int[] getPopupBorderInsets() {
        return popupBorderInsets;
    }

    public String getColorForTablePackageRowBackground() {
        return colorForTablePackageRowBackground;
    }

    public String getColorForTablePackageRowForeground() {
        return colorForTablePackageRowForeground;
    }

    public void setColorForErrorForeground(String errorForeground) {
        this.colorForErrorForeground = errorForeground;
    }

    public void setColorForTablePackageRowBackground(String tablePackageRowBackground) {
        this.colorForTablePackageRowBackground = tablePackageRowBackground;
    }

    public void setColorForTablePackageRowForeground(String tablePackageRowForeground) {
        this.colorForTablePackageRowForeground = tablePackageRowForeground;
    }

    public String getColorForTooltipForeground() {
        return colorForTooltipForeground;
    }

    /**
     * @return true if the gui should paint a horizontal top border above the Statusbar. Default is true.
     */
    public boolean isPaintStatusbarTopBorder() {
        return paintStatusbarTopBorder;
    }

    public void setColorForDownloadOverviewHeader(String downloadOverviewHeaderColor) {
        this.colorForDownloadOverviewHeader = downloadOverviewHeaderColor;
    }

    public void setColorForSortedColumnView(String highlightColor1) {
        this.colorForSortedColumnView = highlightColor1;
    }

    public void setColorForFilteredTableView(String highlightColor2) {
        this.colorForFilteredTableView = highlightColor2;
    }

    public void setMenuBackgroundPainterClass(String menuBackgroundPainterClass) {
        this.menuBackgroundPainterClass = menuBackgroundPainterClass;
    }

    public void setPaintStatusbarTopBorder(boolean paintStatusbarTopBorder) {
        this.paintStatusbarTopBorder = paintStatusbarTopBorder;
    }

    public void setColorForPanelBackground(String panelBackgroundColor) {
        this.colorForPanelBackground = panelBackgroundColor;
    }

    public void setColorForPanelHeader(String panelHeaderColor) {
        this.colorForPanelHeader = panelHeaderColor;
    }

    public void setColorForPanelHeaderForeground(String panelHeaderForegroundColor) {
        this.colorForPanelHeaderForeground = panelHeaderForegroundColor;
    }

    public void setColorForPanelHeaderLine(String panelHeaderLineColor) {
        this.colorForPanelHeaderLine = panelHeaderLineColor;
    }

    public void setPopupBorderInsets(int[] popupBorderInsets) {
        this.popupBorderInsets = popupBorderInsets;
    }

    public void setColorForTooltipForeground(String tooltipForegroundColor) {
        this.colorForTooltipForeground = tooltipForegroundColor;
    }

    public static void applyBackground(String color, JComponent field) {

        Color col = createColor(color);
        if (col != null) {
            field.setBackground(col);
            field.setOpaque(true);
        }
    }

    public static void applyPanelBackground(JComponent rightPanel) {
        applyBackground(LookAndFeelController.getInstance().getLAFOptions().getColorForPanelBackground(), rightPanel);

    }

    @Override
    public StorageHandler<?> _getStorageHandler() {
        return null;
    }

    @Override
    public String getColorForSelectedRowsForeground() {
        return colorForSelectedRowsForeground;
    }

    @Override
    public String getColorForSelectedRowsBackground() {
        return colorForSelectedRowsBackground;
    }

    @Override
    public void setColorForSelectedRowsForeground(String color) {
        colorForSelectedRowsForeground = color;
    }

    @Override
    public void setColorForSelectedRowsBackground(String color) {
        colorForSelectedRowsBackground = color;
    }

    @Override
    public String getColorForMouseOverRowForeground() {
        return colorForMouseOverRowForeground;
    }

    @Override
    public String getColorForMouseOverRowBackground() {
        return colorForMouseOverRowBackground;
    }

    @Override
    public void setColorForMouseOverRowForeground(String color) {
        colorForMouseOverRowForeground = color;
    }

    @Override
    public void setColorForMouseOverRowBackground(String color) {
        colorForMouseOverRowBackground = color;
    }

    @Override
    public String getColorForAlternateRowForeground() {
        return colorForAlternateRowForeground;
    }

    @Override
    public String getColorForAlternateRowBackground() {
        return colorForAlternateRowBackground;
    }

    @Override
    public void setColorForAlternateRowForeground(String color) {
        colorForAlternateRowForeground = color;
    }

    @Override
    public void setColorForAlternateRowBackground(String color) {
        colorForAlternateRowBackground = color;
    }

}
