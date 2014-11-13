package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import java.awt.Dimension;

import jd.gui.swing.jdgui.BasicJDTable;

import org.jdownloader.captcha.v2.SolverService;

public class SolverOrderTable extends BasicJDTable<SolverService> {

    public SolverOrderTable() {
        super(new SolverOrderTableModel());
        setShowHorizontalLineBelowLastEntry(false);
        setShowHorizontalLines(true);

    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        Dimension dim = super.getPreferredScrollableViewportSize();
        // here we return the pref height
        dim.height = getPreferredSize().height;
        return dim;
    }
}
