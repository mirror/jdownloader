package jd.gui.swing.laf;

import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JLabel;

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

    private boolean paintStatusbarTopBorder           = false;

    private String  colorForPanelBackground           = "ffF5FCFF";

    private String  colorForPanelHeader               = "ffD7E7F0";

    private String  colorForPanelHeaderForeground     = BLACK;
    private String  colorForPanelHeaderLine           = "ffC0C0C0";

    private int[]   popupBorderInsets                 = new int[] { 0, 2, 4, 2 };

    private String  colorForTablePackageRowBackground = "FFDEE7ED";
    private String  colorForTablePackageRowForeground = BLACK;
    private String  colorForTooltipForeground         = "ffF5FCFF";
    private String  colorForSelectedRowsForeground    = BLACK;
    private String  colorForSelectedRowsBackground    = "ffCAE8FA";
    private String  colorForMouseOverRowBackground    = "ccCAE8FA";
    private String  colorForMouseOverRowForeground    = null;
    private String  colorForAlternateRowForeground    = null;
    private String  colorForAlternateRowBackground    = "03000000";
    private String  colorForScrollbarsNormalState     = "ffD7E7F0";
    private String  colorForScrollbarsMouseOverState  = "ffABC7D8";

    // "panelBackgroundColor" :"#ffF5FCFF",
    // "panelHeaderColor" : "#ffD7E7F0",
    // "popupBorderInsets" : [ 0,2,4,2],
    // "panelHeaderLineColor": "#ffC0C0C0",
    // "panelHeaderLineForegroundColor" : "#ff000000",
    // "errorForeground": "#ffFF0000",
    // "tooltipForegroundColor": "#ffF5FCFF",
    // "downloadOverviewHeaderColor":"#ff474747"

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

    public String getColorForTableSortedColumnView() {
        return colorForSortedColumnView;
    }

    public String getColorForTableFilteredView() {
        return colorForFilteredTableView;
    }

    public String getMenuBackgroundPainterClass() {
        return menuBackgroundPainterClass;
    }

    public String getColorForPanelBackground() {
        return colorForPanelBackground;
    }

    public String getColorForPanelHeaderBackground() {
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

    public void setColorForTableSortedColumnView(String highlightColor1) {
        this.colorForSortedColumnView = highlightColor1;
    }

    public void setColorForTableFilteredView(String highlightColor2) {
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

    public void setColorForPanelHeaderBackground(String panelHeaderColor) {
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
    public String getColorForTableSelectedRowsForeground() {
        return colorForSelectedRowsForeground;
    }

    @Override
    public String getColorForTableSelectedRowsBackground() {
        return colorForSelectedRowsBackground;
    }

    @Override
    public void setColorForTableSelectedRowsForeground(String color) {
        colorForSelectedRowsForeground = color;
    }

    @Override
    public void setColorForTableSelectedRowsBackground(String color) {
        colorForSelectedRowsBackground = color;
    }

    @Override
    public String getColorForTableMouseOverRowForeground() {
        return colorForMouseOverRowForeground;
    }

    @Override
    public String getColorForTableMouseOverRowBackground() {
        return colorForMouseOverRowBackground;
    }

    @Override
    public void setColorForTableMouseOverRowForeground(String color) {
        colorForMouseOverRowForeground = color;
    }

    @Override
    public void setColorForTableMouseOverRowBackground(String color) {
        colorForMouseOverRowBackground = color;
    }

    @Override
    public String getColorForTableAlternateRowForeground() {
        return colorForAlternateRowForeground;
    }

    @Override
    public String getColorForTableAlternateRowBackground() {
        return colorForAlternateRowBackground;
    }

    @Override
    public void setColorForTableAlternateRowForeground(String color) {
        colorForAlternateRowForeground = color;
    }

    @Override
    public void setColorForTableAlternateRowBackground(String color) {
        colorForAlternateRowBackground = color;
    }

    @Override
    public String getColorForScrollbarsNormalState() {
        return colorForScrollbarsNormalState;
    }

    @Override
    public void setColorForScrollbarsNormalState(String color) {
        colorForScrollbarsNormalState = color;
    }

    @Override
    public String getColorForScrollbarsMouseOverState() {
        return colorForScrollbarsMouseOverState;
    }

    @Override
    public void setColorForScrollbarsMouseOverState(String color) {
        colorForScrollbarsMouseOverState = color;
    }

}
