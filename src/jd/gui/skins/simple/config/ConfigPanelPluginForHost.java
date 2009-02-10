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
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.HostPluginWrapper;
import jd.config.Configuration;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.JLinkButton;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ConfigPanelPluginForHost extends ConfigPanel implements ActionListener, MouseListener, DropTargetListener {

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        public int getColumnCount() {
            return 7;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDLocale.L("gui.column_id", "ID");
            case 1:
                return JDLocale.L("gui.column_host", "Host");
            case 2:
                return JDLocale.L("gui.column_version", "Version");
            case 3:
                return JDLocale.L("gui.column_coder", "Ersteller");
            case 4:
                return JDLocale.L("gui.column_agb", "AGB");
            case 5:
                return JDLocale.L("gui.column_agbChecked", "akzeptieren");
            case 6:
                return JDLocale.L("gui.column_usePlugin", "verwenden");
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return pluginsForHost.size();
        }

        public Object getValueAt(final int rowIndex, final int columnIndex) {
            switch (columnIndex) {
            case 0:
                return rowIndex;
            case 1:
                return pluginsForHost.get(rowIndex).getHost();
            case 2:
                return pluginsForHost.get(rowIndex).getVersion();
            case 3:
                return pluginsForHost.get(rowIndex).getCoder();
            case 4:
                return new JLinkButton(new AbstractAction(JDLocale.L("gui.config.plugin.host.readAGB", "AGB")) {

                    private static final long serialVersionUID = 5915595466511261075L;

                    public void actionPerformed(ActionEvent e) {
                        try {
                            JLinkButton.openURL(pluginsForHost.get(rowIndex).getPlugin().getAGBLink());
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }

                    }

                });
            case 5:
                return pluginsForHost.get(rowIndex).isAGBChecked();
            case 6:
                return pluginsForHost.get(rowIndex).usePlugin();
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 4 || columnIndex == 5 || columnIndex == 6;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 5) {
                if ((Boolean) value) {
                    String msg = JDLocale.LF("gui.config.plugin.abg_confirm", "Ich habe die AGB/TOS/FAQ von %s gelesen und erkläre mich damit einverstanden!", pluginsForHost.get(row).getHost());
                    if (JOptionPane.showConfirmDialog(ConfigurationDialog.DIALOG, msg) == JOptionPane.OK_OPTION) {
                        pluginsForHost.get(row).setAGBChecked((Boolean) value);
                    }
                } else {
                    pluginsForHost.get(row).setAGBChecked((Boolean) value);
                }
            } else if (col == 6) {
                pluginsForHost.get(row).setUsePlugin((Boolean) value);
            }
            ((SimpleGUI) JDUtilities.getGUI()).createHostPluginsMenuEntries();
        }
    }

    private static final long serialVersionUID = -5219586497809869375L;

    private JButton btnEdit;

    private JButton btnLoad;

    private Configuration configuration;

    private HostPluginWrapper draggedPlugin;

    private boolean changed = false;

    private ArrayList<HostPluginWrapper> pluginsForHost;

    private JTable table;

    private InternalTableModel tableModel;

    public ConfigPanelPluginForHost(Configuration configuration) {
        super();
        this.configuration = configuration;
        pluginsForHost = JDUtilities.getPluginsForHost();
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

    public void dragEnter(DropTargetDragEvent e) {
        draggedPlugin = pluginsForHost.get(table.getSelectedRow());
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void dragOver(DropTargetDragEvent e) {
        int oldId = pluginsForHost.indexOf(draggedPlugin);
        int id = table.rowAtPoint(e.getLocation());
        pluginsForHost.remove(draggedPlugin);
        pluginsForHost.add(id, draggedPlugin);
        tableModel.fireTableRowsUpdated(Math.min(oldId, id), Math.max(oldId, id));
        table.getSelectionModel().setSelectionInterval(id, id);
        changed = true;
    }

    public void drop(DropTargetDropEvent e) {
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    private void editEntry(HostPluginWrapper hpw) {
        SimpleGUI.showConfigDialog(ConfigurationDialog.DIALOG, hpw.getPlugin().getConfig());
    }

    private void editEntry() {
        editEntry(pluginsForHost.get(table.getSelectedRow()));
    }

    private void loadEntry(HostPluginWrapper hpw) {
        int cur = table.getSelectedRow();
        hpw.getPlugin();
        tableModel.fireTableRowsUpdated(cur, cur);
        btnEdit.setEnabled(hpw.hasConfig());
        btnLoad.setEnabled(false);
    }

    private void loadEntry() {
        loadEntry(pluginsForHost.get(table.getSelectedRow()));
    }

    @Override
    public void initPanel() {
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(650, 350));

        tableModel = new InternalTableModel();
        table = new JTable(tableModel);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (table.getSelectedColumn() < 0) return;
                HostPluginWrapper hpw = pluginsForHost.get(table.getSelectedRow());
                btnEdit.setEnabled(hpw.hasConfig());
                btnLoad.setEnabled(!hpw.isLoaded());
            }
        });
        table.setDefaultRenderer(Object.class, new PluginTableCellRenderer<HostPluginWrapper>(pluginsForHost));
        table.setDragEnabled(true);
        new DropTarget(table, this);

        TableColumn column = null;

        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setPreferredWidth(30);
                column.setMaxWidth(30);
                column.setMinWidth(30);
                break;
            case 1:
                column.setPreferredWidth(250);
                break;
            case 2:
                column.setPreferredWidth(60);
                column.setMinWidth(60);
                break;
            case 3:
                column.setPreferredWidth(200);
                break;
            case 4:
                column.setPreferredWidth(70);
                column.setMaxWidth(70);
                column.setMinWidth(70);
                column.setCellRenderer(JLinkButton.getJLinkButtonRenderer());
                column.setCellEditor(JLinkButton.getJLinkButtonEditor());
                break;
            case 5:
                column.setPreferredWidth(90);
                column.setMaxWidth(90);
                column.setMinWidth(90);
                break;
            case 6:
                column.setPreferredWidth(100);
                break;
            }
        }

        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400, 200));

        btnEdit = new JButton(JDLocale.L("gui.btn_settings", "Einstellungen"));
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(this);

        btnLoad = new JButton(JDLocale.L("gui.config.plugin.host.btn_load", "Load Plugin"));
        btnLoad.setEnabled(false);
        btnLoad.addActionListener(this);

        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        bpanel.add(btnEdit);
        bpanel.add(btnLoad);

        this.add(new JLabel("<html><body align=\"justify\" color=\"red\"><b>" + JDLocale.L("gui.config.plugin.host.desc", "ACHTUNG!! Das JD Team übernimmt keine Verantwortung für die Einhaltung der AGB \r\n der Hoster. Bitte lesen Sie die AGB aufmerksam und aktivieren Sie das Plugin nur,\r\nfalls Sie sich mit diesen Einverstanden erklären!\r\nDie Reihenfolge der Plugins bestimmt die Prioritäten der automatischen Mirrorauswahl\n\rBevorzugte Hoster sollten oben stehen!")), BorderLayout.NORTH);
        this.add(scrollpane);
        this.add(bpanel, BorderLayout.SOUTH);
    }

    @Override
    public void load() {
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
            int row = table.getSelectedRow();
            HostPluginWrapper hpw = pluginsForHost.get(row);
            if (!hpw.isLoaded()) {
                loadEntry(hpw);
            } else if (hpw.hasConfig()) {
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
        Vector<String> priority = new Vector<String>();
        for (HostPluginWrapper plg : pluginsForHost) {
            priority.add(plg.getHost());
        }
        configuration.setProperty(Configuration.PARAM_HOST_PRIORITY, priority);
        if (changed) ((SimpleGUI) JDUtilities.getGUI()).createHostPluginsMenuEntries();
    }

}
