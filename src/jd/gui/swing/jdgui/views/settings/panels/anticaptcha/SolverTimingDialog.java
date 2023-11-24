package jd.gui.swing.jdgui.views.settings.panels.anticaptcha;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.dimensor.RememberLastDialogDimension;
import org.appwork.utils.swing.dialog.locator.RememberAbsoluteDialogLocator;
import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.gui.translate._GUI;

import net.miginfocom.swing.MigLayout;

public class SolverTimingDialog extends AbstractDialog<Object> {
    private SolverService solver;
    private TimingTable   table;

    @Override
    public JComponent layoutDialogContent() {
        this.getDialog().setLayout(new MigLayout("ins 5 5 5 5,wrap 1", "[grow,fill]", "[grow,fill][]"));
        JScrollPane ret = new JScrollPane(table);
        return ret;
    }

    public SolverTimingDialog(SolverService editing) {
        super(UIOManager.BUTTONS_HIDE_CANCEL | Dialog.STYLE_HIDE_ICON, _GUI.T.SolverTimingDialog(editing.getType(), editing.getName()), null, _GUI.T.lit_close(), null);
        this.solver = editing;
        setLocator(new RememberAbsoluteDialogLocator(getClass().getSimpleName()));
        setDimensor(new RememberLastDialogDimension(getClass().getSimpleName()));
        table = new TimingTable(solver);
        setLeftActions(table.getResetAction());
    }

    @Override
    protected int getPreferredWidth() {
        return 600;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }
}
