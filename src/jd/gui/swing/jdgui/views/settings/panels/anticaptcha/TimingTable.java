package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import java.awt.Dimension;

import javax.swing.AbstractAction;

import jd.gui.swing.jdgui.BasicJDTable;

import org.jdownloader.captcha.v2.SolverService;

public class TimingTable extends BasicJDTable<SolverService> {

    public TimingTable(SolverService solver) {
        super(new TimingTableModel(solver));
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

    public AbstractAction getResetAction() {
        return ((TimingTableModel) getModel()).getResetAction();
    }
}
