package jd.gui.swing.laf;

import java.awt.Color;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.storage.Storable;

public class LAFOptions implements Storable {

    private boolean paintStatusbarTopBorder = true;
    private int     panelBackgroundColor    = new JTable().getBackground().getRGB();
    private int     panelHeaderLineColor    = Color.LIGHT_GRAY.getRGB();

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

    private int[] popupBorderInsets      = new int[] { 0, 0, 0, 0 };
    private int   errorForeground        = 16711680;
    private int   tooltipForegroundColor = 0xcccccc;

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
     * @return true if the gui should paint a horizontal top border above the
     *         Statusbar. Default is true.
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
}
