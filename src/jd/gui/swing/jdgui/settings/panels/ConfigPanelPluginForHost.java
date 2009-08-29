//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.settings.panels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.HostPluginWrapper;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.ConfigEntry.PropertyType;
import jd.gui.UserIF;
import jd.gui.swing.dialog.AgbDialog;
import jd.gui.swing.jdgui.menu.PremiumMenu;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class ConfigPanelPluginForHost extends ConfigPanel implements ActionListener, MouseListener {

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "host.title", "Hoster & Premium");
    }

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.ConfigPanelPluginForHost.";

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDL.L("gui.column_host", "Host");
            case 1:
                return JDL.L("gui.column_version", "Version");
            case 2:
                return JDL.L("gui.column_premium", "Premium");
            case 3:
                return JDL.L("gui.column_settings", "Settings");
            case 4:
                return JDL.L("gui.column_agbChecked", "akzeptieren");
            case 5:
                return JDL.L("gui.column_usePlugin", "verwenden");
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return pluginsForHost.size();
        }

        public Object getValueAt(final int rowIndex, final int columnIndex) {
            switch (columnIndex) {
            case 0:
                return pluginsForHost.get(rowIndex).getHost();
            case 1:
                return pluginsForHost.get(rowIndex).getVersion();
            case 2:
                return pluginsForHost.get(rowIndex).isLoaded() && pluginsForHost.get(rowIndex).isPremiumEnabled();
            case 3:
                return pluginsForHost.get(rowIndex).hasConfig();
            case 4:
                return pluginsForHost.get(rowIndex).isAGBChecked();
            case 5:
                return pluginsForHost.get(rowIndex).usePlugin();
            }

            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex >= 4;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 4) {
                if ((Boolean) value) {
                    AgbDialog.showDialog(pluginsForHost.get(row).getPlugin());
                } else {
                    pluginsForHost.get(row).setAGBChecked(false);
                }
            } else if (col == 5) {
                pluginsForHost.get(row).setUsePlugin((Boolean) value);
                PremiumMenu.getInstance().update();
            }
        }
    }

    private static final long serialVersionUID = -5219586497809869375L;

    private JButton btnEdit;

    private ArrayList<HostPluginWrapper> pluginsForHost;

    private JTable table;

    private InternalTableModel tableModel;

    public ConfigPanelPluginForHost(Configuration configuration) {
        super();
        pluginsForHost = JDUtilities.getPluginsForHost();
        Collections.sort(pluginsForHost);
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnEdit) {
            editEntry(pluginsForHost.get(table.getSelectedRow()));
        }
    }

    private void editEntry(HostPluginWrapper hpw) {
        hpw.getPlugin().getConfig().setGroup(new ConfigGroup(hpw.getPlugin().getHost(), JDTheme.II("gui.images.taskpanes.premium", 24, 24)));

        UserIF.getInstance().requestPanel(UserIF.Panels.CONFIGPANEL, hpw.getPlugin().getConfig());
    }

    @Override
    public void initPanel() {
        tableModel = new InternalTableModel();
        table = new JTable(tableModel);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (table.getSelectedRow() < 0) return;
                HostPluginWrapper hpw = pluginsForHost.get(table.getSelectedRow());
                btnEdit.setEnabled(hpw.hasConfig());
            }
        });
        table.getTableHeader().setReorderingAllowed(false);

        TableColumn column = null;

        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setPreferredWidth(100);
                break;
            case 1:
                column.setPreferredWidth(60);
                break;
            case 2:
                column.setPreferredWidth(60);
                break;
            case 3:
                column.setPreferredWidth(60);
                break;
            case 4:
                column.setPreferredWidth(90);
                break;
            case 5:
                column.setPreferredWidth(100);
                break;
            }
        }

        btnEdit = new JButton(JDL.L("gui.btn_settings", "Einstellungen"));
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(this);

        panel.setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]", "[fill,grow][]"));
        panel.add(new JScrollPane(table));
        panel.add(btnEdit, "w pref!");

        JTabbedPane tabbed = new JTabbedPane();
        tabbed.setOpaque(false);
        tabbed.add(getBreadcrum(), panel);

        this.add(tabbed);
    }

    @Override
    public void load() {
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
            HostPluginWrapper hpw = pluginsForHost.get(table.getSelectedRow());
            if (hpw.hasConfig()) {
                editEntry(hpw);
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void save() {

    }

    @Override
    public PropertyType hasChanges() {
        return PropertyType.NONE;
    }

}
