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

package jd.gui.swing.jdgui.views.settings.panels.addons;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.OptionalPluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.gui.swing.components.table.JDTable;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.gui.swing.jdgui.views.settings.panels.addons.columns.ActivateColumn;
import jd.gui.swing.jdgui.views.settings.panels.addons.columns.NeedsColumn;
import jd.gui.swing.jdgui.views.settings.panels.addons.columns.PluginColumn;
import jd.gui.swing.jdgui.views.settings.panels.addons.columns.VersionColumn;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

/**
 * @author JD-Team
 */
public class ConfigPanelAddons extends ConfigPanel {
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.ConfigPanelAddons.";

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "addons.title", "Extensions");
    }

    public static String getIconKey() {
        return "gui.images.config.packagemanager";
    }

    private class InternalTableModel extends JDTableModel {

        private static final long serialVersionUID = 5847076032639053531L;

        public InternalTableModel() {
            super("hostertable");

            list.clear();
            list.addAll(pluginsOptional);
        }

        @Override
        protected void initColumns() {
            this.addColumn(new ActivateColumn(JDL.L("gui.column_status", "Activate"), this, ConfigPanelAddons.this));
            this.addColumn(new PluginColumn(JDL.L("gui.column_plugin", "Plugin"), this));
            this.addColumn(new VersionColumn(JDL.L("gui.column_version", "Version"), this));
            this.addColumn(new NeedsColumn(JDL.L("gui.column_needs", "Needs"), this));
        }

        @Override
        public void refreshModel() {
        }

    }

    private static final long serialVersionUID = 4145243293360008779L;

    private final ImageIcon defaultIcon;
    private final ArrayList<OptionalPluginWrapper> pluginsOptional;

    private JDTable table;

    private JLabel lblName;
    private JLabel lblVersion;
    private JTextPane txtDescription;

    public ConfigPanelAddons() {
        super();

        defaultIcon = JDTheme.II(ConfigPanel.getIconKey(), 24, 24);
        pluginsOptional = new ArrayList<OptionalPluginWrapper>(OptionalPluginWrapper.getOptionalWrapper());
        Collections.sort(pluginsOptional);

        init();
    }

    @Override
    protected ConfigContainer setupContainer() {
        table = new JDTable(new InternalTableModel());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateShowcase();
            }
        });

        lblName = new JLabel();
        lblName.setIcon(defaultIcon);
        lblName.setFont(lblName.getFont().deriveFont(Font.BOLD));
        lblName.setHorizontalAlignment(JLabel.LEADING);

        lblVersion = new JLabel();

        txtDescription = new JTextPane();
        txtDescription.setEditable(false);
        txtDescription.setBackground(null);
        txtDescription.setOpaque(false);
        txtDescription.putClientProperty("Synthetica.opaque", Boolean.FALSE);

        JScrollPane scrollPane = new JScrollPane(txtDescription);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        table.getSelectionModel().setSelectionInterval(0, 0);

        JPanel panel = new JPanel(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill]10[]5[]"));
        panel.add(new JScrollPane(table), "growy, pushy");
        panel.add(lblName, "split 3");
        panel.add(new JSeparator(JSeparator.HORIZONTAL), "growx, pushx, gapleft 10, gapright 10");
        panel.add(lblVersion);
        panel.add(scrollPane, "h 60!");

        ConfigContainer container = new ConfigContainer();

        container.setGroup(new ConfigGroup(getTitle(), getIconKey()));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, panel, "growy, pushy"));

        return container;
    }

    public void updateShowcase() {
        if (table.getSelectedRow() < 0) return;
        OptionalPluginWrapper opw = pluginsOptional.get(table.getSelectedRow());
        ImageIcon icon;
        if (opw.isLoaded() && (icon = JDTheme.II(opw.getPlugin().getIconKey(), 24, 24)) != null) {
            lblName.setIcon(icon);
        } else {
            lblName.setIcon(defaultIcon);
        }
        lblName.setText(opw.getHost());
        lblVersion.setText(JDL.LF(JDL_PREFIX + ".version", "Version: %s", opw.getVersion()));
        txtDescription.setText(opw.getDescription());
        txtDescription.setCaretPosition(0);
    }

}