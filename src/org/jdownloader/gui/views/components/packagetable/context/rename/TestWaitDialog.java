package org.jdownloader.gui.views.components.packagetable.context.rename;

import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.translate._GUI;

import jd.gui.swing.jdgui.JDGui;

public class TestWaitDialog extends AbstractDialog<Object> {
    private ArrayList<org.jdownloader.gui.views.components.packagetable.context.rename.Result> list;

    public TestWaitDialog(boolean regex, Pattern pattern, String rep, ArrayList<Result> list) {
        super(UIOManager.BUTTONS_HIDE_OK, _GUI.T.lit_preview(), null, null, _GUI.T.literally_close());
        this.list = list;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 3", "[][][grow,fill]", "[][][grow,fill]");
        p.add(new JScrollPane(new ResultTable(new ResultTableModel(list))), "spanx,pushx,growx,newline");
        return p;
    }

    protected int getPreferredWidth() {
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
