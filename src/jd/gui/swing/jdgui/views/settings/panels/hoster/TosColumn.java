package jd.gui.swing.jdgui.views.settings.panels.hoster;


 import org.jdownloader.gui.translate.*;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.JTable;
import javax.swing.SwingConstants;

import jd.HostPluginWrapper;
import jd.gui.swing.components.linkbutton.JLink;
import jd.utils.locale.JDL;

import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.ExtTableModel;
import org.jdesktop.swingx.renderer.JRendererLabel;

public class TosColumn extends ExtColumn<HostPluginWrapper> implements ActionListener {

    private static final long serialVersionUID = 4600633634774184026L;

    private final JRendererLabel labelRend;
    private final JLink labelEdit;

    public TosColumn(String name, ExtTableModel<HostPluginWrapper> table) {
        super(name, table);

        labelRend = new JRendererLabel();
        labelRend.setBorder(null);
        labelRend.setHorizontalAlignment(SwingConstants.CENTER);
        labelRend.setText(T._.jd_gui_swing_jdgui_settings_panels_hoster_columns_TosColumn_read());

        labelEdit = new JLink(T._.jd_gui_swing_jdgui_settings_panels_hoster_columns_TosColumn_read());
        labelEdit.setHorizontalAlignment(SwingConstants.CENTER);
        labelEdit.getBroadcaster().addListener(this);
        labelEdit.removeMouseListener();
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        try {
            String url = ((HostPluginWrapper) value).getPlugin().getAGBLink();
            if (url == null || url.length() == 0) {
                labelEdit.setUrl(null);
            } else {
                labelEdit.setUrl(new URL(url));
            }
        } catch (Exception e) {
        }
        return labelEdit;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return labelRend;
    }

    public void actionPerformed(ActionEvent e) {
        this.fireEditingStopped();
    }

    @Override
    public boolean isEditable(HostPluginWrapper obj) {
        return true;
    }

    @Override
    public boolean isEnabled(HostPluginWrapper obj) {
        return true;
    }

    @Override
    public boolean isSortable(HostPluginWrapper obj) {
        return false;
    }

    @Override
    public void setValue(Object value, HostPluginWrapper object) {
    }

}