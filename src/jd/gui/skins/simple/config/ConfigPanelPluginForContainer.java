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
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.plugins.PluginForContainer;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ConfigPanelPluginForContainer extends ConfigPanel implements ActionListener, MouseListener {

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDLocale.L("gui.config.plugin.container.column_host", "Host");
            case 1:
                // return
                // JDLocale.L("gui.config.plugin.container.column_id","ID");
                return JDLocale.L("gui.config.plugin.container.column_version", "Version");
            case 2:
                return JDLocale.L("gui.config.plugin.container.column_author", "Ersteller");

            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return pluginsForContainer.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {

            switch (columnIndex) {
            case 0:
                return pluginsForContainer.elementAt(rowIndex).getPluginName();
            case 1:
                // return pluginsForContainer.elementAt(rowIndex).getPluginID();
                return pluginsForContainer.elementAt(rowIndex).getVersion();
            case 2:
                return pluginsForContainer.elementAt(rowIndex).getCoder();

            }
            return null;
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = -169660462836773855L;
    /**
     * 
     */

    private JButton btnEdit;

    private Configuration configuration;
    // private PluginForDecrypt currentPlugin;
    private Vector<PluginForContainer> pluginsForContainer;
    private JTable table;

    public ConfigPanelPluginForContainer(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        pluginsForContainer = JDUtilities.getPluginsForContainer();
        initPanel();

        load();

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnEdit) {
            editEntry();
        }
    }

    private void editEntry() {
        PluginForContainer plugin = getSelectedPlugin();
        if (plugin != null && plugin.getConfig().getEntries().size() > 0) {
            openPopupPanel(new ConfigPanelPlugin(configuration, uiinterface, plugin));
        }
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.plugin.container.name", "Container");
    }

    private int getSelectedInteractionIndex() {
        return table.getSelectedRow();
    }

    private PluginForContainer getSelectedPlugin() {
        int index = getSelectedInteractionIndex();
        if (index < 0) { return null; }
        return pluginsForContainer.elementAt(index);
    }

    @Override
    public void initPanel() {
        setLayout(new BorderLayout());
        table = new JTable(); // new InternalTable();
        InternalTableModel internalTableModel = new InternalTableModel();
        table.setModel(new InternalTableModel());
        setPreferredSize(new Dimension(700, 350));

        TableColumn column = null;
        for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {

            case 0:
                column.setPreferredWidth(250);
                break;
            case 1:
                column.setPreferredWidth(200);
                break;
            case 2:
                column.setPreferredWidth(250);
                break;

            }
        }

        // add(scrollPane);
        // list = new JList();
        table.addMouseListener(this);
        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400, 200));

        btnEdit = new JButton(JDLocale.L("gui.config.plugin.container.btn_settings", "Einstellungen"));
        btnEdit.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (pluginsForContainer.get(((DefaultListSelectionModel) e.getSource()).getMinSelectionIndex()).getConfig().getEntries().size() != 0) {
                    btnEdit.setEnabled(true);
                } else {
                    btnEdit.setEnabled(false);
                }
            }
        });

        btnEdit.addActionListener(this);
        // JDUtilities.addToGridBag(panel, scrollpane, 0, 0, 3, 1, 1, 1, insets,
        // GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        //
        // JDUtilities.addToGridBag(panel, btnEdit, 0, 1, 1, 1, 0, 1, insets,
        // GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, scrollpane, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(panel, btnEdit, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);

        // JDUtilities.addToGridBag(this, panel,0, 0, 1, 1, 1, 1, insets,
        // GridBagConstraints.BOTH, GridBagConstraints.WEST);
        add(panel, BorderLayout.CENTER);

    }

    /**
     * Lädt alle Informationen
     */
    @Override
    public void load() {

    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
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

    private void openPopupPanel(ConfigPanel config) {
        JPanel panel = new JPanel(new BorderLayout());

        // InteractionTrigger[] triggers = InteractionTrigger.getAllTrigger();

        PluginForContainer plugin = getSelectedPlugin();
        // currentPlugin = plugin;
        if (plugin == null) { return; }

        JPanel topPanel = new JPanel();
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(config, BorderLayout.CENTER);
        ConfigurationPopup pop = new ConfigurationPopup(JDUtilities.getParentFrame(this), config, panel, uiinterface, configuration);
        pop.setLocation(JDUtilities.getCenterOfComponent(this, pop));
        pop.setVisible(true);
    }

    /**
     * Speichert alle Änderungen auf der Maske TODO: PluginsForDecrypt haben
     * noch keinen properties laoder.
     */
    @Override
    public void save() {
        // Interaction[] tmp= new Interaction[interactions.size()];
        PluginForContainer plg;
        for (int i = 0; i < pluginsForContainer.size(); i++) {
            plg = pluginsForContainer.elementAt(i);
            if (plg.getPluginConfig() != null) {
                configuration.setProperty("PluginConfig_" + plg.getPluginName(), plg.getPluginConfig());
            }
        }

    }
}
