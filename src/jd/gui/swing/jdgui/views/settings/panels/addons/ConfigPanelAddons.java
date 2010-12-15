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

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.Icon;
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
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.table.ExtTable;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.SelectionHighlighter;
import org.appwork.utils.swing.table.columns.ExtLongColumn;
import org.appwork.utils.swing.table.columns.ExtTextColumn;

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

    private class InternalTableModel extends ExtTableModel<OptionalPluginWrapper> {

        private static final long serialVersionUID = 5847076032639053531L;

        public InternalTableModel() {
            super("addonTable");

            tableData = new ArrayList<OptionalPluginWrapper>(pluginsOptional);
        }

        @Override
        protected void initColumns() {
            this.addColumn(new ActivateColumn(JDL.L("gui.column_status", "Activate"), this, ConfigPanelAddons.this));
            this.addColumn(new ExtTextColumn<OptionalPluginWrapper>(JDL.L("gui.column_plugin", "Plugin"), this) {

                private static final long serialVersionUID = -3960914415647488335L;

                @Override
                protected Icon getIcon(OptionalPluginWrapper value) {
                    ImageIcon icon = null;
                    if (value.isLoaded() && (icon = JDTheme.II(value.getPlugin().getIconKey(), 16, 16)) != null) {
                        return icon;
                    } else {
                        return smallDefaultIcon;
                    }
                }

                @Override
                protected String getStringValue(OptionalPluginWrapper value) {
                    return value.getHost();
                }

            });
            this.addColumn(new ExtLongColumn<OptionalPluginWrapper>(JDL.L("gui.column_version", "Version"), this) {

                private static final long serialVersionUID = -7390851512040553114L;

                @Override
                protected long getLong(OptionalPluginWrapper value) {
                    return value.getVersion();
                }

            });
            this.addColumn(new ExtTextColumn<OptionalPluginWrapper>(JDL.L("gui.column_needs", "Needs"), this) {

                private static final long serialVersionUID = 92691792879468744L;

                @Override
                protected String getStringValue(OptionalPluginWrapper value) {
                    return "JRE " + value.getJavaVersion();
                }

            });
        }

    }

    private static final long                      serialVersionUID = 4145243293360008779L;

    private final ImageIcon                        smallDefaultIcon;
    private final ImageIcon                        defaultIcon;
    private final ArrayList<OptionalPluginWrapper> pluginsOptional;

    private ExtTable<OptionalPluginWrapper>        table;

    private JLabel                                 lblName;
    private JLabel                                 lblVersion;
    private JTextPane                              txtDescription;

    public ConfigPanelAddons() {
        super();

        smallDefaultIcon = JDTheme.II(ConfigPanel.getIconKey(), 16, 16);
        defaultIcon = JDTheme.II(ConfigPanel.getIconKey(), 24, 24);
        pluginsOptional = new ArrayList<OptionalPluginWrapper>(OptionalPluginWrapper.getOptionalWrapper());
        Collections.sort(pluginsOptional);

        init();
    }

    @Override
    protected ConfigContainer setupContainer() {
        table = new ExtTable<OptionalPluginWrapper>(new InternalTableModel(), "addonTable");
        table.addRowHighlighter(new SelectionHighlighter(null, new Color(200, 200, 200, 80)));
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
        int row = table.getSelectedRow();
        if (row < 0) return;

        table.getExtTableModel().fireTableRowsUpdated(row, row);
        OptionalPluginWrapper opw = pluginsOptional.get(row);
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