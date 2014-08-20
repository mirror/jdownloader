package org.jdownloader.gui.views.components.packagetable.context.rename;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtTableModel;

public class ResultTable extends BasicJDTable<Result> {

    public ResultTable(ExtTableModel<Result> model) {
        super(model);

        this.setPreferredScrollableViewportSize(new Dimension(450, 450));
    }

    @Override
    protected boolean onDoubleClick(MouseEvent e, Result obj) {

        return false;
    }
}
