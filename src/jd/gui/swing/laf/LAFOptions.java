package jd.gui.swing.laf;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.storage.Storable;

public class LAFOptions implements Storable {
    public static void main(String[] args) {
        System.out.println(0x474747);
    }

    private boolean paintStatusbarTopBorder     = true;
    private int     panelBackgroundColor        = new JTable().getBackground().getRGB();
    private int     panelHeaderLineColor        = Color.LIGHT_GRAY.getRGB();
    private int     downloadOverviewHeaderColor = -1;

    public int getDownloadOverviewHeaderColor() {
        return downloadOverviewHeaderColor;
    }

    public void setDownloadOverviewHeaderColor(int downloadOverviewHeaderColor) {
        this.downloadOverviewHeaderColor = downloadOverviewHeaderColor;
    }

    public int getPanelHeaderLineColor() {
        return panelHeaderLineColor;
    }

    public void setPanelHeaderLineColor(int panelHeaderLineColor) {
        this.panelHeaderLineColor = panelHeaderLineColor;
    }

    private int panelHeaderColor           = new JTableHeader().getBackground().getRGB();
    private int panelHeaderForegroundColor = 0;

    public void setPopupBorderInsets(int[] popupBorderInsets) {
        this.popupBorderInsets = popupBorderInsets;
    }

    private int[]  popupBorderInsets          = new int[] { 0, 0, 0, 0 };
    private int    errorForeground            = 16711680;
    private int    tooltipForegroundColor     = 0xcccccc;
    private int    highlightColor1            = Color.ORANGE.getRGB();
    private int    highlightColor2            = Color.GREEN.getRGB();
    private String menuBackgroundPainterClass = "de.javasoft.plaf.synthetica.simple2D.MenuPainter";

    public void setMenuBackgroundPainterClass(String menuBackgroundPainterClass) {
        this.menuBackgroundPainterClass = menuBackgroundPainterClass;
    }

    public int getHighlightColor2() {
        return highlightColor2;
    }

    public void setHighlightColor2(int highlightColor2) {
        this.highlightColor2 = highlightColor2;
    }

    public void setHighlightColor1(int highlightColor1) {
        this.highlightColor1 = highlightColor1;
    }

    public void setTooltipForegroundColor(int tooltipForegroundColor) {
        this.tooltipForegroundColor = tooltipForegroundColor;
    }

    public void setPanelHeaderForegroundColor(int panelHeaderForegroundColor) {
        this.panelHeaderForegroundColor = panelHeaderForegroundColor;
    }

    public void setPanelHeaderColor(int panelHeaderColor) {
        this.panelHeaderColor = panelHeaderColor;
    }

    public void setPanelBackgroundColor(int panelBackgroundColor) {
        this.panelBackgroundColor = panelBackgroundColor;
    }

    public void setPaintStatusbarTopBorder(boolean paintStatusbarTopBorder) {
        this.paintStatusbarTopBorder = paintStatusbarTopBorder;
    }

    public LAFOptions() {
        // empty cosntructor required for Storable
    }

    /**
     * @return true if the gui should paint a horizontal top border above the Statusbar. Default is true.
     */
    public boolean isPaintStatusbarTopBorder() {
        return paintStatusbarTopBorder;
    }

    public int getPanelBackgroundColor() {
        return panelBackgroundColor;
    }

    public int getPanelHeaderColor() {
        return panelHeaderColor;
    }

    public int getPanelHeaderForegroundColor() {
        return panelHeaderForegroundColor;
    }

    public int[] getPopupBorderInsets() {
        return popupBorderInsets;
    }

    public int getErrorForeground() {
        return errorForeground;
    }

    public int getTooltipForegroundColor() {
        return tooltipForegroundColor;
    }

    public int getHighlightColor1() {
        return highlightColor1;
    }

    public void applyPanelBackgroundColor(JComponent overViewScrollBar) {

        int c = getPanelBackgroundColor();
        if (c >= 0) {
            overViewScrollBar.setBackground(new Color(c));
            overViewScrollBar.setOpaque(true);

        }
    }

    public void applyDownloadOverviewHeaderColor(JLabel lbl) {

        int c = getDownloadOverviewHeaderColor();
        if (c >= 0) {
            lbl.setForeground(new Color(c));

        }
    }

    public String getMenuBackgroundPainterClass() {
        return menuBackgroundPainterClass;
    }
}
