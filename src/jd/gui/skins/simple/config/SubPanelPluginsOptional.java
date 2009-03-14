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

package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.OptionalPluginWrapper;
import jd.config.Configuration;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.JLinkButton;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * @author JD-Team
 * 
 */
public class SubPanelPluginsOptional extends ConfigPanel implements ActionListener, MouseListener {

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
                return JDLocale.L("gui.column_status", "Aktivieren");
            case 1:
                return JDLocale.L("gui.column_plugin", "Plugin");
            case 2:
                return JDLocale.L("gui.column_version", "Version");
            case 3:
                return JDLocale.L("gui.column_coder", "Ersteller");
            case 4:
                return JDLocale.L("gui.column_needs", "Needs");
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
                return pluginsOptional.get(rowIndex).getPlugin().getHost();
            case 2:
                return pluginsOptional.get(rowIndex).getVersion();
            case 3:
                return pluginsOptional.get(rowIndex).getCoder();
            case 4:
                return pluginsOptional.get(rowIndex).getPlugin().getRequirements();
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
                if ((Boolean) value) {
                    configuration.setProperty(plgWrapper.getConfigParamKey(), true);
                    plgWrapper.getPlugin().initAddon();
                } else {
                    if ((plgWrapper.getFlag() & OptionalPluginWrapper.FLAG_ALWAYS_ENABLED) > 0) {
                        JDUtilities.getGUI().showMessageDialog(JDLocale.LF("gui.config.plugin.optional.forcedActive", "The addon %s cannot be disabled.", plgWrapper.getClassName()));
                    } else {
                        configuration.setProperty(plgWrapper.getConfigParamKey(), false);
                        plgWrapper.getPlugin().onExit();
                    }
                }
                ((SimpleGUI) JDUtilities.getGUI()).createOptionalPluginsMenuEntries();
            }
        }

    }

    private static final long serialVersionUID = 5794208138046480006L;

    private JButton btnEdit;

    private Configuration configuration;

    private ArrayList<OptionalPluginWrapper> pluginsOptional;

    private JTable table;

    private InternalTableModel tableModel;

    public SubPanelPluginsOptional(Configuration configuration) {
        super();
        this.configuration = configuration;
        pluginsOptional = OptionalPluginWrapper.getOptionalWrapper();
        Collections.sort(pluginsOptional);
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnEdit) {
            editEntry();
        }
    }

    private void editEntry() {
        SimpleGUI.showConfigDialog(SimpleGUI.CURRENTGUI.getFrame(), pluginsOptional.get(table.getSelectedRow()).getPlugin().getConfig());
    }

    @Override
    public void initPanel() {
        this.setLayout(new BorderLayout());

        tableModel = new InternalTableModel();
        table = new JTable(tableModel);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                btnEdit.setEnabled((table.getSelectedRow() >= 0) && pluginsOptional.get(table.getSelectedRow()).getPlugin().getConfig().getEntries().size() != 0);
            }
        });
        table.setDefaultRenderer(Object.class, new PluginTableCellRenderer<OptionalPluginWrapper>(pluginsOptional));

        TableColumn column = null;
        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setMaxWidth(80);
                column.setPreferredWidth(50);
                break;
            case 1:
                column.setPreferredWidth(300);
                break;
            }
        }

        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(600, 300));

        btnEdit = new JButton(JDLocale.L("gui.btn_settings", "Einstellungen"));
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(this);

        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        bpanel.add(btnEdit);
        bpanel.add(new JLinkButton(JDLocale.L("gui.config.plugin.optional.linktext_help", "Hilfe"), JDLocale.L("gui.config.plugin.optional.link_help", "  http://jdownloader.org/page.php?id=122")));

        this.add(scrollpane);
        this.add(bpanel, BorderLayout.SOUTH);
    }

    @Override
    public void load() {
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1 && pluginsOptional.get(table.getSelectedRow()).getPlugin().getConfig().getEntries().size() != 0) {
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

    @Override
    public void save() {
    }
}
