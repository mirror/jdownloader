package jd.gui.swing.jdgui.views.settings.panels.proxy;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import jd.controlling.proxy.AbstractProxySelectorImpl;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.components.ExtTextArea;
import org.appwork.utils.Regex;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.updatev2.FilterList;
import org.jdownloader.updatev2.FilterList.Type;

public class ProxyDetailsDialog extends AbstractDialog<Object> {

    private AbstractProxySelectorImpl factory;
    private JComboBox<Type>           combo;
    private ExtTextArea               input;

    public ProxyDetailsDialog(AbstractProxySelectorImpl factory) {
        super(0, _GUI._.proxyDetailsDialog_filter_title(factory.toString()), null, _GUI._.lit_save(), _GUI._.lit_close());
        this.factory = factory;

    }

    @Override
    protected int getPreferredHeight() {
        return 450;
    }

    protected int getPreferredWidth() {
        return 728;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    protected void setReturnmask(boolean b) {
        super.setReturnmask(b);
        if (b) {

            factory.setFilter(new FilterList((FilterList.Type) combo.getSelectedItem(), Regex.getLines(input.getText())));
        }
    }

    @Override
    public JComponent layoutDialogContent() {

        JPanel content = new JPanel(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[]5[][][grow,fill][][]"));
        content.add(header(_GUI._.proxyDetailsDialog_white_blacklist()), "gapleft 5,pushx,growx");
        content.add(new JLabel("<html>" + _GUI._.proxyDetailsDialog_white_blacklist_explain() + "</html>"), "gapleft 24,wmin 10");

        combo = new JComboBox<FilterList.Type>(FilterList.Type.values());
        final ListCellRenderer org = combo.getRenderer();
        combo.setRenderer(new ListCellRenderer<FilterList.Type>() {

            @Override
            public Component getListCellRendererComponent(JList<? extends Type> list, Type value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value == null)
                    return org.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                switch (value) {
                case BLACKLIST:
                    return org.getListCellRendererComponent(list, _GUI._.proxyDetailsDialog_combo_blacklist(), index, isSelected, cellHasFocus);
                case WHITELIST:
                    return org.getListCellRendererComponent(list, _GUI._.proxyDetailsDialog_combo_whitelist(), index, isSelected, cellHasFocus);
                }
                return org.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        content.add(combo, "gapleft 24");
        input = new ExtTextArea();
        content.add(new JScrollPane(input), "gapleft 24");

        if (factory.getFilter() == null || factory.getFilter().getType() == null || factory.getFilter().getType() == FilterList.Type.BLACKLIST) {
            combo.setSelectedItem(FilterList.Type.BLACKLIST);
        } else {
            combo.setSelectedItem(factory.getFilter().getType());
        }
        StringBuilder sb = new StringBuilder();
        if (factory.getFilter() != null) {
            for (String line : factory.getFilter().getEntries()) {
                if (sb.length() > 0)
                    sb.append("\r\n");
                sb.append(line);
            }
        }
        input.setText(sb.toString());

        return content;
    }

    private JComponent header(String lbl) {
        JLabel ret = SwingUtils.toBold(new JLabel(lbl));
        ret.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ret.getForeground()));
        return ret;
    }

}
