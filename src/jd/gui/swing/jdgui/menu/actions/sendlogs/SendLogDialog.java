package jd.gui.swing.jdgui.menu.actions.sendlogs;

import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.JDGui;

import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.translate._GUI;

public class SendLogDialog extends AbstractDialog<Object> {

    private ArrayList<LogFolder> folders;
    private LogModel             model;

    public SendLogDialog(ArrayList<LogFolder> folders) {
        super(0, _GUI._.SendLogDialog_SendLogDialog_title_(), null, _GUI._.literally_continue(), null);
        this.folders = folders;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    protected int getPreferredHeight() {
        int height = (int) Math.min(getDialog().getRawPreferredSize().getHeight(), JDGui.getInstance().getMainFrame().getHeight());
        System.out.println(height);

        return height;
    }

    protected boolean isResizable() {

        return true;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[][grow,fill]");
        JLabel lbl = new JLabel(_GUI._.SendLogDialog_layoutDialogContent_desc_());
        p.add(lbl);
        model = new LogModel(folders);
        LogTable table = new LogTable(model);
        p.add(new JScrollPane(table));

        return p;
    }

    public ArrayList<LogFolder> getSelectedFolders() {
        ArrayList<LogFolder> list = new ArrayList<LogFolder>();
        for (LogFolder lf : model.getTableData()) {
            if (lf.isSelected()) {
                list.add(lf);

            }
        }
        return list;
    }
}
