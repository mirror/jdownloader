package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import org.appwork.uio.UIOManager;
import org.jdownloader.captcha.v2.SolverService;

import jd.gui.swing.jdgui.BasicJDTable;

public class SolverOrderTable extends BasicJDTable<SolverService> {

    public SolverOrderTable() {
        super(new SolverOrderTableModel());
        setShowHorizontalLineBelowLastEntry(false);
        setShowHorizontalLines(true);
        setFocusable(false);

    }

    @Override
    protected boolean onDoubleClick(MouseEvent e, SolverService obj) {

        SolverPropertiesDialog d = new SolverPropertiesDialog(obj, obj.getConfigPanel());
        UIOManager.I().show(null, d);
        return true;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        Dimension dim = super.getPreferredScrollableViewportSize();
        // here we return the pref height
        dim.height = getPreferredSize().height;
        return dim;
    }
}
