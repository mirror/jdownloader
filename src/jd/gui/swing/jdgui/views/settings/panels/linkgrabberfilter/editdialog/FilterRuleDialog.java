package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.app.gui.MigPanel;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class FilterRuleDialog extends ConditionDialog<LinkgrabberFilterRule> {

    private LinkgrabberFilterRule rule;
    private JComboBox<String>     then;
    private JLabel                lbl;

    public FilterRuleDialog(LinkgrabberFilterRule filterRule) {
        super();
        this.rule = filterRule;

    }

    public static void main(String[] args) {
        try {
            LookAndFeelController.getInstance().setUIManager();
            Dialog.getInstance().showDialog(new FilterRuleDialog(new LinkgrabberFilterRule()));
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected LinkgrabberFilterRule createReturnValue() {
        return rule;
    }

    @Override
    protected void setReturnmask(boolean b) {
        super.setReturnmask(b);
        if (b) {
            save();
        }
    }

    private void save() {
        rule.setFilenameFilter(getFilenameFilter());
        rule.setHosterURLFilter(getHosterFilter());
        rule.setName(getName());
        rule.setFilesizeFilter(getFilersizeFilter());
        rule.setSourceURLFilter(getSourceFilter());
        rule.setFiletypeFilter(getFiletypeFilter());
        rule.setAccept(then.getSelectedIndex() == 1);

    }

    private void updateGUI() {
        setFilenameFilter(rule.getFilenameFilter());
        setHosterFilter(rule.getHosterURLFilter());
        setName(rule.getName());
        setFilesizeFilter(rule.getFilesizeFilter());
        setSourceFilter(rule.getSourceURLFilter());
        setFiletypeFilter(rule.getFiletypeFilter());
        then.setSelectedIndex(rule.isAccept() ? 1 : 0);
        lbl.setIcon(then.getSelectedIndex() == 0 ? NewTheme.I().getIcon("cancel", 20) : NewTheme.I().getIcon("ok", 20));
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel ret = (MigPanel) super.layoutDialogContent();
        ret.add(createHeader(_GUI._.FilterRuleDialog_layoutDialogContent_then()), "gaptop 10, spanx,growx,pushx");
        then = new JComboBox<String>(new String[] { _GUI._.FilterRuleDialog_layoutDialogContent_deny(), _GUI._.FilterRuleDialog_layoutDialogContent_accept() });
        final ListCellRenderer<? super String> org = then.getRenderer();
        then.setRenderer(new ListCellRenderer<String>() {

            public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel r = (JLabel) org.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                switch (index) {
                case 0:
                    r.setIcon(NewTheme.I().getIcon("cancel", 20));
                    break;
                case 1:
                    r.setIcon(NewTheme.I().getIcon("ok", 20));
                    break;

                }
                return r;
            }
        });
        lbl = new JLabel();
        ret.add(lbl, "gaptop 10");
        ret.add(then, "gaptop 10,spanx,growx,pushx");
        then.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                lbl.setIcon(then.getSelectedIndex() == 0 ? NewTheme.I().getIcon("cancel", 20) : NewTheme.I().getIcon("ok", 20));
            }
        });

        updateGUI();
        return ret;
    }

    public boolean isAccept() {
        return then.getSelectedIndex() == 1;
    }
}
