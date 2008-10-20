//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import jd.DecryptPluginWrapper;
import jd.config.Configuration;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ConfigPanelPluginForDecrypt extends ConfigPanel implements ActionListener, MouseListener {

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        public int getColumnCount() {
            return 4;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 3;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 3) {
                pluginsForDecrypt.get(row).setUsePlugin((Boolean) value);
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDLocale.L("gui.column_host", "Host");
            case 1:
                return JDLocale.L("gui.column_version", "Version");
            case 2:
                return JDLocale.L("gui.column_coder", "Ersteller");
            case 3:
                return JDLocale.L("gui.column_usePlugin", "Plugin benutzen");
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return pluginsForDecrypt.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
            case 0:
                return pluginsForDecrypt.get(rowIndex).getHost();
            case 1:
                return pluginsForDecrypt.get(rowIndex).getVersion();
            case 2:
                return pluginsForDecrypt.get(rowIndex).getCoder();
            case 3:
                return pluginsForDecrypt.get(rowIndex).usePlugin();
            }
            return null;
        }
    }

    private static final long serialVersionUID = -5308908915544580923L;

    private JButton btnEdit;

    private JButton btnLoad;

    private ArrayList<DecryptPluginWrapper> pluginsForDecrypt;

    private JTable table;

    private InternalTableModel tableModel;

    public ConfigPanelPluginForDecrypt(Configuration configuration) {
        super();
        pluginsForDecrypt = DecryptPluginWrapper.getDecryptWrapper();
        Collections.sort(pluginsForDecrypt);
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnEdit) {
            editEntry();
        } else if (e.getSource() == btnLoad) {
            loadEntry();
        }
    }

    private void editEntry(DecryptPluginWrapper dpw) {
        SimpleGUI.showConfigDialog(JDUtilities.getParentFrame(this), dpw.getPlugin().getConfig());
    }

    private void editEntry() {
        editEntry(pluginsForDecrypt.get(table.getSelectedRow()));
    }

    private void loadEntry(DecryptPluginWrapper dpw) {
        int cur = table.getSelectedRow();
        dpw.getPlugin();
        tableModel.fireTableRowsUpdated(cur, cur);
        btnEdit.setEnabled(dpw.hasConfig());
        btnLoad.setEnabled(false);
    }

    private void loadEntry() {
        loadEntry(pluginsForDecrypt.get(table.getSelectedRow()));
    }

    @Override
    public void initPanel() {
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(550, 350));

        tableModel = new InternalTableModel();
        table = new JTable(tableModel);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (table.getSelectedColumn() < 0) return;
                DecryptPluginWrapper dpw = pluginsForDecrypt.get(table.getSelectedRow());
                btnEdit.setEnabled(dpw.hasConfig());
                btnLoad.setEnabled(!dpw.isLoaded());
            }
        });
        table.setDefaultRenderer(Object.class, new PluginTableCellRenderer<DecryptPluginWrapper>(pluginsForDecrypt));

        TableColumn column = null;
        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setPreferredWidth(200);
                break;
            case 1:
                column.setPreferredWidth(60);
                column.setMinWidth(60);
                break;
            case 2:
                column.setPreferredWidth(100);
                break;
            case 3:
                column.setPreferredWidth(90);
                column.setMaxWidth(90);
                column.setMinWidth(90);
                break;
            }
        }

        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400, 200));

        btnEdit = new JButton(JDLocale.L("gui.btn_settings", "Einstellungen"));
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(this);

        btnLoad = new JButton(JDLocale.L("gui.config.plugin.decrypt.btn_load", "Load Plugin"));
        btnLoad.setEnabled(false);
        btnLoad.addActionListener(this);

        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        bpanel.add(btnEdit);
        bpanel.add(btnLoad);

        this.add(scrollpane);
        this.add(bpanel, BorderLayout.SOUTH);
    }

    @Override
    public void load() {
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
            DecryptPluginWrapper dpw = pluginsForDecrypt.get(table.getSelectedRow());
            if (!dpw.isLoaded()) {
                loadEntry(dpw);
            } else if (dpw.hasConfig()) {
                editEntry(dpw);
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
}
