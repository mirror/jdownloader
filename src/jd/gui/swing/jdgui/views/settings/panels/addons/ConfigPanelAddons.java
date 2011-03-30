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
import java.util.Comparator;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.table.ExtTable;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.SelectionHighlighter;
import org.appwork.utils.swing.table.columns.ExtLongColumn;
import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.extensions.AbstractExtensionWrapper;
import org.jdownloader.extensions.ExtensionController;

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

    private class InternalTableModel extends ExtTableModel<AbstractExtensionWrapper> {

        private static final long serialVersionUID = 5847076032639053531L;

        public InternalTableModel() {
            super("addonTable");

            tableData = new ArrayList<AbstractExtensionWrapper>(pluginsOptional);
        }

        @Override
        protected void initColumns() {
            this.addColumn(new ActivateColumn(JDL.L("gui.column_status", "Activate"), this, ConfigPanelAddons.this));
            this.addColumn(new ExtTextColumn<AbstractExtensionWrapper>(JDL.L("gui.column_plugin", "Plugin"), this) {

                private static final long serialVersionUID = -3960914415647488335L;

                @Override
                protected Icon getIcon(AbstractExtensionWrapper value) {
                    return value._getIcon(16);
                }

                @Override
                protected String getStringValue(AbstractExtensionWrapper value) {
                    return value.getName();
                }

            });
            this.addColumn(new ExtLongColumn<AbstractExtensionWrapper>(JDL.L("gui.column_version", "Version"), this) {

                private static final long serialVersionUID = -7390851512040553114L;

                @Override
                protected long getLong(AbstractExtensionWrapper value) {
                    return value.getVersion();
                }

            });

        }

    }

    private static final long                         serialVersionUID = 4145243293360008779L;

    private final ArrayList<AbstractExtensionWrapper> pluginsOptional;

    private ExtTable<AbstractExtensionWrapper>        table;

    private JLabel                                    lblName;
    private JLabel                                    lblVersion;
    private JTextPane                                 txtDescription;

    public ConfigPanelAddons() {
        super();

        pluginsOptional = new ArrayList<AbstractExtensionWrapper>(ExtensionController.getInstance().getExtensions());

        Collections.sort(pluginsOptional, new Comparator<AbstractExtensionWrapper>() {

            public int compare(AbstractExtensionWrapper o1, AbstractExtensionWrapper o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        init(false);
    }

    @Override
    protected ConfigContainer setupContainer() {
        table = new ExtTable<AbstractExtensionWrapper>(new InternalTableModel(), "addonTable");
        table.addRowHighlighter(new SelectionHighlighter(null, new Color(200, 200, 200, 80)));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateShowcase();
            }
        });

        lblName = new JLabel();

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
        AbstractExtensionWrapper opw = table.getExtTableModel().getElementAt(row);
        lblName.setIcon(opw._getIcon(24));
        lblName.setText(opw.getName());
        lblVersion.setText(JDL.LF(JDL_PREFIX + ".version", "Version: %s", opw.getVersion()));
        txtDescription.setText(opw.getDescription());
        txtDescription.setCaretPosition(0);
    }

}