package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.appwork.app.gui.MigPanel;
import org.jdownloader.controlling.filter.FilterRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class FilterRuleDialog extends ConditionDialog {

    private FilterRule        rule;
    private JComboBox<String> then;
    private JLabel            lbl;

    public FilterRuleDialog(FilterRule filterRule) {
        super();
        this.rule = filterRule;

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
                    r.setIcon(NewTheme.I().getIcon("stopsign", 20));
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
                lbl.setIcon(then.getSelectedIndex() == 0 ? NewTheme.I().getIcon("stopsign", 20) : NewTheme.I().getIcon("ok", 20));
            }
        });
        lbl.setIcon(then.getSelectedIndex() == 0 ? NewTheme.I().getIcon("stopsign", 20) : NewTheme.I().getIcon("ok", 20));
        updateGUI();
        return ret;
    }

    private void updateGUI() {
        // txtCustumMime.setText()
    }

    public boolean isAccept() {
        return then.getSelectedIndex() == 1;
    }
}
