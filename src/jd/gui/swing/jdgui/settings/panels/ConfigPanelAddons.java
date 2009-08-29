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

import jd.OptionalPluginWrapper;
import jd.config.Configuration;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.menu.AddonsMenu;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.gui.swing.jdgui.views.sidebars.configuration.ConfigSidebar;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

/**
 * @author JD-Team
 * 
 */

public class ConfigPanelAddons extends ConfigPanel implements ActionListener, MouseListener {
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.ConfigPanelAddons.";

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "addons.title", "Extensions");
    }

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDL.L("gui.column_status", "Aktivieren");
            case 1:
                return JDL.L("gui.column_plugin", "Plugin");
            case 2:
                return JDL.L("gui.column_version", "Version");
            case 3:
                return JDL.L("gui.column_coder", "Ersteller");
            case 4:
                return JDL.L("gui.column_needs", "Needs");
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return pluginsOptional.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
            case 0:
                return pluginsOptional.get(rowIndex).isEnabled();
            case 1:
                return pluginsOptional.get(rowIndex).getHost();
            case 2:
                return pluginsOptional.get(rowIndex).getVersion();
            case 3:
                return pluginsOptional.get(rowIndex).getCoder();
            case 4:
                return pluginsOptional.get(rowIndex).getJavaVersion();
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 0) {
                OptionalPluginWrapper plgWrapper = pluginsOptional.get(row);
                configuration.setProperty(plgWrapper.getConfigParamKey(), (Boolean) value);
                configuration.save();
                if ((Boolean) value) {
                    plgWrapper.getPlugin().initAddon();
                    if (plgWrapper.getAnnotation().hasGui()) {
                        int ret = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, plgWrapper.getHost(), JDL.LF(JDL_PREFIX + "askafterinit", "Show %s now?\r\nYou may open it later using Mainmenu->Addon", plgWrapper.getHost()));

                        if (UserIO.isOK(ret)) {
                            plgWrapper.getPlugin().setGuiEnable(true);
                        }
                    }
                } else {
                    plgWrapper.getPlugin().setGuiEnable(false);
                    plgWrapper.getPlugin().onExit();
                }
                AddonsMenu.getInstance().update();
                ConfigSidebar.getInstance(null).updateAddons();
            }
        }
    }

    private static final long serialVersionUID = 4145243293360008779L;

    private JButton btnEdit;

    private Configuration configuration;

    private ArrayList<OptionalPluginWrapper> pluginsOptional;

    private JTable table;

    private InternalTableModel tableModel;

    public ConfigPanelAddons(Configuration configuration) {
        super();
        this.configuration = configuration;
        pluginsOptional = OptionalPluginWrapper.getOptionalWrapper();
        Collections.sort(pluginsOptional);
        initPanel();
        load();
    }

    @Override
    public void initPanel() {
        tableModel = new InternalTableModel();
        table = new JTable(tableModel);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                btnEdit.setEnabled(table.getSelectedRow() >= 0 && pluginsOptional.get(table.getSelectedRow()).hasConfig());
            }
        });
        table.getColumnModel().getColumn(0).setMaxWidth(80);

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

    private void editEntry() {
        UserIF.getInstance().requestPanel(UserIF.Panels.CONFIGPANEL, pluginsOptional.get(table.getSelectedRow()).getPlugin().getConfig());

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnEdit) {
            editEntry();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1 && pluginsOptional.get(table.getSelectedRow()).hasConfig()) {
            editEntry();
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

}