package org.jdownloader.gui.views.components.packagetable.context.rename;

import java.awt.Component;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.JDGui;

import org.appwork.swing.MigPanel;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.translate._GUI;

public class TestWaitDialog extends AbstractDialog<Object> {

    private JLabel                                                                             lbl;

    private ExtTableModel<Result>                                                              model;

    private ArrayList<org.jdownloader.gui.views.components.packagetable.context.rename.Result> list;

    public TestWaitDialog(boolean regex, Pattern pattern, String rep, ArrayList<Result> list) {
        super(UIOManager.BUTTONS_HIDE_OK, _GUI._.lit_preview(), null, null, _GUI._.literally_close());
        this.list = list;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    private Component label(String lbl) {
        JLabel ret = new JLabel(lbl);
        ret.setEnabled(false);
        return ret;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 3", "[][][grow,fill]", "[][][grow,fill]");

        p.add(new JScrollPane(new ResultTable(new ResultTableModel(list))), "spanx,pushx,growx,newline");

        return p;
    }

    protected int getPreferredWidth() {
        // TODO Auto-generated method stub
        return JDGui.getInstance().getMainFrame().getWidth();
    }

    protected boolean isResizable() {

        return true;
    }

    @Override
    public void dispose() {
        super.dispose();

    }

}
