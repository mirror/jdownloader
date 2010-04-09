package jd.gui.swing.jdgui.views.settings.panels.hoster.columns;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.SwingConstants;

import jd.HostPluginWrapper;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class TosColumn extends JDTableColumn implements ActionListener {

    private static final long serialVersionUID = 4600633634774184026L;
    private JRendererLabel jlr;
    private JLink jlink;

    public TosColumn(String name, JDTableModel table) {
        super(name, table);
        this.setClickstoEdit(1);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        jlr.setHorizontalAlignment(SwingConstants.CENTER);
        jlr.setText(JDL.L("jd.gui.swing.jdgui.settings.panels.hoster.columns.TosColumn.read", "Read TOS"));
        jlink = new JLink(JDL.L("jd.gui.swing.jdgui.settings.panels.hoster.columns.TosColumn.read", "Read TOS"));
        jlink.setHorizontalAlignment(SwingConstants.CENTER);
        jlink.getBroadcaster().addListener(this);
        jlink.removeMouseListener();
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isEditable(Object obj) {
        return true;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        try {
            jlink.setUrl(new URL(((HostPluginWrapper) value).getPlugin().getAGBLink()));
        } catch (Exception e) {
        }
        return jlink;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == jlink) {
            this.fireEditingStopped();
        }
    }

}