package jd.gui.skins.jdgui.views;

import javax.swing.Icon;

import jd.gui.skins.simple.JTabbedPanel;

/**
 * A Wrapper for compatibility to old TabbedPanels
 * 
 * @author Coalado
 * 
 */
public class TabbedPanelView extends View {

    private JTabbedPanel tabbedPanel;

    public TabbedPanelView(JTabbedPanel tabbedPanel) {
        super();
        this.tabbedPanel = tabbedPanel;
        this.setContent(tabbedPanel);

    }

    public boolean equals(Object e) {
        if (e instanceof TabbedPanelView) return false;

        return ((TabbedPanelView) e).tabbedPanel == this.tabbedPanel;
    }

    @Override
    public Icon getIcon() {
        return View.getDefaultIcon();
    }

    @Override
    public String getTitle() {
        return tabbedPanel.getName();
    }

    @Override
    public String getTooltip() {

        return tabbedPanel.getToolTipText();
    }

}
