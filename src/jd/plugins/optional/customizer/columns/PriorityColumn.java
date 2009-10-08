package jd.plugins.optional.customizer.columns;

import java.awt.Component;

import javax.swing.JComboBox;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.views.downloadview.DownloadTable;
import jd.plugins.optional.customizer.CustomizeSetting;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class PriorityColumn extends JDTableColumn {

    private static final long serialVersionUID = 4640856288557573254L;
    private static String[] prioDescs;
    private JRendererLabel jlr;
    private JComboBox prio;

    public PriorityColumn(String name, JDTableModel table) {
        super(name, table);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        prio = new JComboBox(prioDescs = DownloadTable.prioDescs);
        prio.setBorder(null);
    }

    @Override
    public Object getCellEditorValue() {
        System.out.println(prio.getSelectedIndex());
        return prio.getSelectedIndex();
    }

    @Override
    public boolean isEditable(Object obj) {
        return isEnabled(obj);
    }

    @Override
    public boolean isEnabled(Object obj) {
        return ((CustomizeSetting) obj).isEnabled();
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        prio.setSelectedIndex(((CustomizeSetting) value).getPriority() + 1);
        return prio;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        jlr.setText(prioDescs[((CustomizeSetting) value).getPriority() + 1]);
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
        ((CustomizeSetting) object).setPriority((Integer) value - 1);
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

}
