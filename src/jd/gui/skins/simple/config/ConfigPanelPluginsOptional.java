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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * @author JD-Team
 * 
 */
public class ConfigPanelPluginsOptional extends ConfigPanel implements ActionListener, MouseListener {

    private static final long serialVersionUID = 5794208138046480006L;

    private JButton btnEdit;

    private JTable table;

    private Configuration configuration;

    private JButton enableDisable;

    // private JarFile[] availablePluginJarFiles;

    private Vector<PluginOptional> plugins;

    private JButton openPluginDir;

    private JLinkButton link;

    public ConfigPanelPluginsOptional(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;

        // enabledList[i] =
        // configuration.getBooleanProperty(getConfigParamKey(availablePlugins
        // [i]), false);
        HashMap<String, PluginOptional> pluginsOptional = JDUtilities.getPluginsOptional();

        plugins = new Vector<PluginOptional>();
        if (pluginsOptional != null && pluginsOptional.size() > 0) {
            Iterator<String> iterator = pluginsOptional.keySet().iterator();
            String key;
            while (iterator.hasNext()) {
                key = iterator.next();
                plugins.add(pluginsOptional.get(key));
            }

            initPanel();

            load();
        }
    }

    private String getConfigParamKey(PluginOptional pluginOptional) {
        return "OPTIONAL_PLUGIN_" + pluginOptional.getPluginName();
    }

    /**
     * Lädt alle Informationen
     */
    public void load() {

    }

    /**
     * Speichert alle Änderungen auf der Maske
     */
    public void save() {

        // for (int i = 0; i < availablePlugins.length; i++) {
        // table.getValueAt(0., column)
        // if (plg.getProperties() != null)
        // configuration.setProperty("PluginConfig_" + plg.getPluginName(),
        // plg.getProperties());
        // }

    }

    
    public void initPanel() {
        // int n = 10;
        // setBorder(new EmptyBorder(n,n,n,n));
        setLayout(new BorderLayout(10, 10));
        table = new JTable(); // new InternalTable();
        InternalTableModel internalTableModel = new InternalTableModel();

        table.setModel(new InternalTableModel());
        // this.setPreferredSize(new Dimension(700, 350));

        TableColumn column = null;
        for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setPreferredWidth(70);
                break;
            case 1:
                column.setPreferredWidth(300);
                break;

            }
        }

        // add(scrollPane);
        // list = new JList();
        table.addMouseListener(this);
        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400, 200));

        btnEdit = new JButton(JDLocale.L("gui.config.plugin.optional.btn_settings", "Einstellungen"));
        btnEdit.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if ((((DefaultListSelectionModel) e.getSource()).getMinSelectionIndex() != -1) && (plugins.get(((DefaultListSelectionModel) e.getSource()).getMinSelectionIndex()).getConfig().getEntries().size() != 0))
                    btnEdit.setEnabled(true);
                else
                    btnEdit.setEnabled(false);
            }
        });

        btnEdit.addActionListener(this);
        enableDisable = new JButton(JDLocale.L("gui.config.plugin.optional.btn_toggleStatus", "An/Aus"));

        enableDisable.addActionListener(this);

        openPluginDir = new JButton(JDLocale.L("gui.config.plugin.optional.btn_openDir", "Addon Ordner öffnen"));

        openPluginDir.addActionListener(this);

        link = new JLinkButton(JDLocale.L("gui.config.plugin.optional.linktext_help", "Hilfe"), JDLocale.L("gui.config.plugin.optional.link_help", "  http://jdownloader.org/page.php?id=122"));
        // JDUtilities.addToGridBag(panel, new
        // JLabel(JDLocale.L("gui.warning.restartNeeded"
        // ,"JD-Restart needed after changes!")), 0, 0, 3, 1, 1, 1, insets,
        // GridBagConstraints.BOTH, GridBagConstraints.CENTER);

        // JDUtilities.addToGridBag(panel, scrollpane, 0, 1, 20, 1, 1, 1,
        // insets, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        //
        // JDUtilities.addToGridBag(panel, btnEdit, 0, 2, 1, 1, 0, 0, insets,
        // GridBagConstraints.NONE, GridBagConstraints.WEST);
        // JDUtilities.addToGridBag(panel, enableDisable, 1, 2, 1, 1, 0, 0,
        // insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        // JDUtilities.addToGridBag(panel, openPluginDir, 2, 2, 1, 1, 0, 0,
        // insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        // JDUtilities.addToGridBag(panel, link, 3, 2, 1, 1, 1, 0, insets,
        // GridBagConstraints.NONE, GridBagConstraints.WEST);

        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        int n = 5;
        contentPanel.setBorder(new EmptyBorder(0, n, 0, n));
        String text = JDLocale.L("gui.warning.restartNeeded", "JD-Restart needed after changes!");
        contentPanel.add(new JLabel(text), BorderLayout.NORTH);
        contentPanel.add(scrollpane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(5, 5, FlowLayout.LEFT));
        buttonPanel.add(btnEdit);
        buttonPanel.add(enableDisable);
        buttonPanel.add(openPluginDir);
        buttonPanel.add(link);

        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // JDUtilities.addToGridBag(this, panel,0, 0, 1, 1, 1, 1, insets,
        // GridBagConstraints.BOTH, GridBagConstraints.WEST);
    }

    
    public String getName() {
        return JDLocale.L("gui.config.plugin.optional.name", "Optional Plugins");
    }

    // private void openPopupPanel(ConfigPanel config) {
    // JPanel panel = new JPanel(new BorderLayout());
    //
    // // InteractionTrigger[] triggers = InteractionTrigger.getAllTrigger();
    //
    // PluginForSearch plugin = this.getSelectedPlugin();
    // // currentPlugin = plugin;
    // if (plugin == null) return;
    //
    // JPanel topPanel = new JPanel();
    // panel.add(topPanel, BorderLayout.NORTH);
    // panel.add(config, BorderLayout.CENTER);
    // ConfigurationPopup pop = new
    // ConfigurationPopup(JDUtilities.getParentFrame(this), config, panel,
    // uiinterface, configuration);
    // pop.setLocation(JDUtilities.getCenterOfComponent(this, pop));
    // pop.setVisible(true);
    // }

    public void fireTableChanged() {
        int rowIndex = table.getSelectedRow();

        table.tableChanged(new TableModelEvent(this.table.getModel()));
        table.getSelectionModel().addSelectionInterval(rowIndex, rowIndex);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == btnEdit) {
            editEntry();

        }

        if (e.getSource() == enableDisable) {
            int rowIndex = table.getSelectedRow();
            boolean b = configuration.getBooleanProperty(getConfigParamKey(plugins.get(rowIndex)), false);
            configuration.setProperty(getConfigParamKey(plugins.get(rowIndex)), !b);
            fireTableChanged();
        }
        if (e.getSource() == openPluginDir) {

            try {
                new GetExplorer().openExplorer(JDUtilities.getResourceFile("plugins"));
            } catch (Exception ec) {
            }
        }

    }

    private int getSelectedIndex() {
        return table.getSelectedRow();
    }

    private PluginOptional getSelectedPlugin() {
        int index = getSelectedIndex();
        if (index < 0) return null;
        return this.plugins.elementAt(index);
    }

    private void editEntry() {
        PluginOptional plugin = getSelectedPlugin();
        if (plugin != null && plugin.getConfig().getEntries().size() > 0) {
            openPopupPanel(new ConfigPanelPlugin(configuration, uiinterface, plugin));
        }
    }

    private void openPopupPanel(ConfigPanel config) {
        JPanel panel = new JPanel(new BorderLayout());

        // InteractionTrigger[] triggers = InteractionTrigger.getAllTrigger();

        PluginOptional plugin = this.getSelectedPlugin();
        // currentPlugin = plugin;
        if (plugin == null) return;

        JPanel topPanel = new JPanel();
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(config, BorderLayout.CENTER);
        ConfigurationPopup pop = new ConfigurationPopup(JDUtilities.getParentFrame(this), config, panel, uiinterface, configuration);
        pop.setLocation(JDUtilities.getCenterOfComponent(this, pop));
        pop.setVisible(true);
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

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        public int getColumnCount() {
            return 5;
        }

        public int getRowCount() {
            return plugins.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {            
            case 0:
                return configuration.getBooleanProperty(getConfigParamKey(plugins.get(rowIndex)), false) ? JDLocale.L("gui.config.plugin.optional.statusActive", "An") : JDLocale.L("gui.config.plugin.optional.statusInactive", "Aus");
            case 1:
                return plugins.get(rowIndex).getPluginName();
            case 2:
                return plugins.get(rowIndex).getVersion();
            case 3:
                return plugins.get(rowIndex).getCoder();
            case 4:
                return plugins.get(rowIndex).getRequirements();
            }
            return null;
        }

        public String getColumnName(int column) {
            switch (column) {            
            case 0:
                return JDLocale.L("gui.config.plugin.optional.column_status", "Status");
            case 1:
                return JDLocale.L("gui.config.plugin.optional.column_plugin", "Plugin");
            case 2:
                return JDLocale.L("gui.config.plugin.optional.column_version", "Version");
            case 3:
                return JDLocale.L("gui.config.plugin.optional.column_author", "Coder");
            case 4:
                return JDLocale.L("gui.config.plugin.optional.column_needs", "Needs");
            }
            return super.getColumnName(column);
        }
    }
}
