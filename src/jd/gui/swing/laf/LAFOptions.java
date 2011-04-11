package jd.gui.swing.laf;

import org.appwork.storage.Storable;

public class LAFOptions implements Storable {

    private boolean paintStatusbarTopBorder    = true;
    private int     panelBackgroundColor       = -1;
    private int     panelHeaderColor           = -1;
    private int     panelHeaderForegroundColor = 0;

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
}
